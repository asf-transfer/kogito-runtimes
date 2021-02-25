/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.codegen.process.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import org.jbpm.compiler.canonical.TriggerMetaData;
import org.kie.kogito.codegen.api.context.KogitoBuildContext;
import org.kie.kogito.codegen.api.context.impl.JavaKogitoBuildContext;
import org.kie.kogito.codegen.api.template.InvalidTemplateException;
import org.kie.kogito.codegen.api.template.TemplatedGenerator;
import org.kie.kogito.codegen.process.ProcessExecutableModelGenerator;
import org.kie.kogito.event.CloudEventMeta;
import org.kie.kogito.event.EventKind;
import org.kie.kogito.services.event.DataEventAttrBuilder;

public class CloudEventMetaFactoryGenerator extends AbstractEventResourceGenerator {

    private static final String CLASS_NAME = "CloudEventMetaFactory";

    private final KogitoBuildContext context;
    private final Map<String, List<TriggerMetaData>> triggers;

    public CloudEventMetaFactoryGenerator(final KogitoBuildContext context,
                                          final List<ProcessExecutableModelGenerator> generators) {
        super(TemplatedGenerator.builder()
                .withTemplateBasePath(TEMPLATE_EVENT_FOLDER)
                .withFallbackContext(JavaKogitoBuildContext.CONTEXT_NAME)
                .build(context, CLASS_NAME));
        this.context = context;
        this.triggers = this.filterTriggers(generators);
    }

    Map<String, List<TriggerMetaData>> getTriggers() {
        return triggers;
    }

    public String generate() {
        final CompilationUnit compilationUnit = generator.compilationUnitOrThrow("Cannot generate CloudEventMetaFactory");

        final ClassOrInterfaceDeclaration classDefinition = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new InvalidTemplateException(generator, "Compilation unit doesn't contain a class or interface declaration!"));

        final MethodDeclaration templatedBuildMethod = classDefinition
                .findAll(MethodDeclaration.class, x -> x.getName().toString().startsWith("buildCloudEventMeta_")).get(0);

        validateTemplatedBuildMethodDeclaration(generator, templatedBuildMethod);

        this.triggers.forEach((processId, triggerList) -> triggerList.forEach(trigger -> {
            EventKind eventKind = TriggerMetaData.TriggerType.ProduceMessage.equals(trigger.getType())
                    ? EventKind.PRODUCED
                    : EventKind.CONSUMED;

            String strEventType = eventKind == EventKind.PRODUCED
                    ? DataEventAttrBuilder.toType(trigger.getName(), processId)
                    : trigger.getName();
            String strEventSource = eventKind == EventKind.PRODUCED
                    ? DataEventAttrBuilder.toSource(processId)
                    : "";

            MethodDeclaration builderMethod = templatedBuildMethod.clone();

            String builderMethodName = String.format("%s_%s", eventKind.name(), trigger.getName());
            builderMethod.setName(builderMethod.getName().asString().replace("$methodName$", builderMethodName));

            ObjectCreationExpr objectCreationExpr = builderMethod.findAll(ObjectCreationExpr.class).get(0);
            objectCreationExpr.setArgument(0, new StringLiteralExpr(strEventType));
            objectCreationExpr.setArgument(1, new StringLiteralExpr(strEventSource));
            objectCreationExpr.setArgument(2, new FieldAccessExpr(new NameExpr(new SimpleName(EventKind.class.getName())), eventKind.name()));

            if (context.hasDI()) {
                context.getDependencyInjectionAnnotator().withFactoryMethod(builderMethod);
            }

            classDefinition.addMember(builderMethod);
        }));

        templatedBuildMethod.remove();

        if (context.hasDI()) {
            context.getDependencyInjectionAnnotator().withFactoryClass(classDefinition);
        }

        return compilationUnit.toString();
    }

    private Map<String, List<TriggerMetaData>> filterTriggers(final List<ProcessExecutableModelGenerator> generators) {
        if (generators != null) {
            final Map<String, List<TriggerMetaData>> filteredTriggers = new HashMap<>();
            generators
                    .stream()
                    .filter(m -> m.generate().getTriggers() != null && !m.generate().getTriggers().isEmpty())
                    .forEach(m -> filteredTriggers.put(m.getProcessId(),
                            m.generate().getTriggers().stream()
                                    .filter(t -> !TriggerMetaData.TriggerType.Signal.equals(t.getType()))
                                    .collect(Collectors.toList())));
            return filteredTriggers;
        }
        return Collections.emptyMap();
    }

    public static void validateTemplatedBuildMethodDeclaration(TemplatedGenerator generator, MethodDeclaration methodDeclaration) {
        if (methodDeclaration == null) {
            throw new InvalidTemplateException(generator, "templated build method declaration is null");
        }

        if (!methodDeclaration.getName().toString().contains("$methodName$")) {
            throw new InvalidTemplateException(generator, "missing $methodName$ placeholder in templated build method declaration");
        }

        List<ReturnStmt> returnStmtList = methodDeclaration.findAll(ReturnStmt.class);
        if (returnStmtList.size() != 1) {
            throw new InvalidTemplateException(generator, "templated build method declaration must contain exactly one return statement");
        }

        Optional<ObjectCreationExpr> optObjectCreationExprExpr = returnStmtList.get(0).getExpression()
                .filter(Expression::isObjectCreationExpr)
                .map(Expression::asObjectCreationExpr)
                .filter(ocExpr -> {
                    String typeName = ocExpr.getType().getNameAsString();
                    return typeName.equals(CloudEventMeta.class.getSimpleName()) || typeName.equals(CloudEventMeta.class.getName());
                })
                .filter(ocExpr -> ocExpr.getArguments().size() == 3)
                .filter(ocExpr -> ocExpr.getArguments().get(0).toString().equals("$type$")
                        && ocExpr.getArguments().get(1).toString().equals("$source$")
                        && ocExpr.getArguments().get(2).toString().equals("$kind$")
                );

        if (!optObjectCreationExprExpr.isPresent()) {
            throw new InvalidTemplateException(generator, "templated build method declaration return statement must be an ObjectCreationExpr of type CloudEventMeta");
        }
    }
}

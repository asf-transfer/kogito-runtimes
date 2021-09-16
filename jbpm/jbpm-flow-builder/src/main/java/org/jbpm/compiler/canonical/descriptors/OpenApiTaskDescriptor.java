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
package org.jbpm.compiler.canonical.descriptors;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jbpm.process.core.ParameterDefinition;
import org.jbpm.workflow.core.node.WorkItemNode;
import org.kie.api.definition.process.Node;
import org.kie.kogito.process.workitem.WorkItemExecutionException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import static java.util.Objects.requireNonNull;

public class OpenApiTaskDescriptor extends AbstractServiceTaskDescriptor {

    public static final String TYPE = "OpenApi Task";
    private static final String PARAM_PREFIX = "ServiceParam_";
    public static final String PARAM_META_RESULT_HANDLER = "ResultHandler";
    public static final String PARAM_META_RESULT_HANDLER_TYPE = "ResultHandlerType";
    private static final String PARAM_META_SPEC_PARAMETERS = "SpecParameters";

    private static final String VAR_INPUT_MODEL = "inputModel";
    private static final String METHOD_GET_PARAM = "getParameter";
    private static final NameExpr workItemNameExpr = new NameExpr("workItem");

    private static final ClassOrInterfaceType JSON_NODE_CLASS = new ClassOrInterfaceType(null, JsonNode.class.getCanonicalName());

    protected OpenApiTaskDescriptor(WorkItemNode workItemNode) {
        super(workItemNode);
    }

    /**
     * Creates a new {@link WorkItemModifier} for the given {@link WorkItemNode}, normally created with {@link #builderFor(String, String)}.
     *
     * @param workItemNode the given {@link WorkItemNode}
     * @return a new {@link WorkItemModifier}
     */
    public static WorkItemModifier modifierFor(final WorkItemNode workItemNode) {
        return new WorkItemModifier(workItemNode);
    }

    public static boolean isOpenApiTask(Node node) {
        return node instanceof WorkItemNode &&
                ((WorkItemNode) node).getWork() != null &&
                TYPE.equals(((WorkItemNode) node).getWork().getName());
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getName() {
        return String.format("%s_%s_%s_Handler", interfaceName, operationName, workItemNode.getName()).replaceAll("\\s+", "");
    }

    @Override
    public CompilationUnit generateHandlerClassForService() {
        CompilationUnit compilationUnit = new CompilationUnit("org.kie.kogito.handlers");
        compilationUnit.getTypes().add(classDeclaration());
        compilationUnit.addImport(WorkItemExecutionException.class);
        return compilationUnit;
    }

    @Override
    protected Collection<Class<?>> getCompleteWorkItemExceptionTypes() {
        return Collections.emptyList();
    }

    private static Collection<String> getParameters(WorkItemNode workItemNode) {
        return workItemNode.getWork().getParameterDefinitions().stream().map(ParameterDefinition::getName).collect(Collectors.toList());
    }

    @Override
    protected void handleParametersForServiceCall(final BlockStmt executeWorkItemBody, final MethodCallExpr callServiceMethod) {
        // declare the input model
        getParameters(workItemNode)
                .forEach(p -> callServiceMethod.addArgument(new CastExpr(JSON_NODE_CLASS, new MethodCallExpr(workItemNameExpr, METHOD_GET_PARAM).addArgument(new StringLiteralExpr(p)))));
    }

    @Override
    protected Expression handleServiceCallResult(final BlockStmt executeWorkItemBody, final MethodCallExpr callService) {
        final MethodCallExpr getInputModel = new MethodCallExpr(workItemNameExpr, METHOD_GET_PARAM).addArgument(new StringLiteralExpr("Parameter"));
        final VariableDeclarationExpr inputModel =
                new VariableDeclarationExpr(new VariableDeclarator(new ClassOrInterfaceType(null, Object.class.getCanonicalName()), VAR_INPUT_MODEL, getInputModel));
        executeWorkItemBody.addStatement(inputModel);
        // fetch the handler type
        final ClassOrInterfaceType resultHandlerType = new ClassOrInterfaceType(null, ((Class<?>) workItemNode.getMetaData(PARAM_META_RESULT_HANDLER_TYPE)).getCanonicalName());
        // get the handler
        final MethodCallExpr getResultHandler = new MethodCallExpr(workItemNameExpr, METHOD_GET_PARAM).addArgument(new StringLiteralExpr(PARAM_META_RESULT_HANDLER));
        // convert the result into the given type
        final CastExpr castToHandler = new CastExpr(resultHandlerType, getResultHandler);
        // temp to hold the result handler with the correct cast
        final VariableDeclarationExpr resultHandler =
                new VariableDeclarationExpr(new VariableDeclarator(castToHandler.getType(), "resultHandler", castToHandler));
        executeWorkItemBody.addStatement(resultHandler);
        return new MethodCallExpr(resultHandler.getVariable(0).getNameAsExpression(), "apply")
                .addArgument(new NameExpr(VAR_INPUT_MODEL))
                .addArgument(callService);
    }

    /**
     * Facilitates the interaction with a given OpenApi {@link WorkItemNode}.
     */
    public static final class WorkItemModifier {
        private final WorkItemNode workItemNode;
        private final Set<String> specParameters;

        private WorkItemModifier(final WorkItemNode workItemNode) {
            this.workItemNode = workItemNode;
            this.specParameters = new LinkedHashSet<>();
        }

        public String getOperation() {
            return (String) this.workItemNode.getWork().getParameter(KEY_WORKITEM_OPERATION);
        }

        public String getInterface() {
            return (String) this.workItemNode.getWork().getParameter(KEY_WORKITEM_INTERFACE);
        }

        /**
         * Set all the runtime information to the given {@link WorkItemNode}
         *
         * @param generatedClass canonical name of OpenApi generated class
         * @param methodName method name for the generated class responsible to perform the REST call
         * @param specParams parameters as defined by the OpenApi Spec definition
         */
        public void modify(final String generatedClass, final String methodName, final List<String> specParams) {
            this.defineJavaImplementation(generatedClass, methodName);
            specParams.forEach(this::addSpecParameter);
            this.validateAndAddMissingParameters();
        }

        /**
         * Defines the generated Java class and method for the given Task.
         *
         * @param generatedClass canonical name of OpenApi generated class
         * @param methodName method name for the generated class responsible to perform the REST call
         */
        private void defineJavaImplementation(final String generatedClass, final String methodName) {
            requireNonNull(methodName, "Method name for Java implementation can't be null");
            requireNonNull(generatedClass, "Generated class for Java implementation can't be null");
            this.workItemNode.getWork().setParameter(KEY_WORKITEM_OPERATION, methodName);
            this.workItemNode.getWork().setParameter(KEY_WORKITEM_INTERFACE, generatedClass);
        }

        /**
         * Adds a parameter as defined in a given OpenApi Spec file. The internal list will retain the added order.
         *
         * @param name the name of the parameter
         */
        private void addSpecParameter(final String name) {
            this.specParameters.add(PARAM_PREFIX + name);
            this.workItemNode.setMetaData(PARAM_META_SPEC_PARAMETERS, this.specParameters);
        }

        /**
         * Adds all non-required parameters
         */
        private void validateAndAddMissingParameters() {
            final Collection<String> paramResolvers = getParameters(workItemNode);
            if (this.specParameters.size() != paramResolvers.size() || this.specParameters.size() > 1) {
                this.specParameters.stream()
                        .filter(p -> !paramResolvers.contains(p))
                        .forEach(p -> this.workItemNode.getWork().setParameter(p, null));
                final List<String> unexpectedParams = paramResolvers.stream().filter(p -> !this.specParameters.contains(p)).collect(Collectors.toList());
                if (!unexpectedParams.isEmpty()) {
                    throw new IllegalArgumentException("Found unexpected parameters in the Task definition: " + unexpectedParams + ". Expected parameters are: " + this.specParameters);
                }
            }
        }

    }
}

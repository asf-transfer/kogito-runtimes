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
package org.kie.kogito.codegen.rules;

import java.util.Map;
import java.util.NoSuchElementException;

import org.drools.modelcompiler.builder.ModelSourceClass;
import org.kie.kogito.codegen.api.context.KogitoBuildContext;
import org.kie.kogito.codegen.api.context.impl.JavaKogitoBuildContext;
import org.kie.kogito.codegen.api.template.InvalidTemplateException;
import org.kie.kogito.codegen.api.template.TemplatedGenerator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;

import static com.github.javaparser.StaticJavaParser.parseStatement;
import static org.kie.kogito.codegen.rules.IncrementalRuleCodegen.TEMPLATE_RULE_FOLDER;

public class ProjectRuntimeGenerator {

    private final ModelSourceClass.KieModuleModelMethod modelMethod;
    private final KogitoBuildContext context;
    private final TemplatedGenerator generator;

    public ProjectRuntimeGenerator(ModelSourceClass.KieModuleModelMethod modelMethod, KogitoBuildContext context) {
        this.modelMethod = modelMethod;
        this.context = context;
        this.generator = TemplatedGenerator.builder()
                .withTemplateBasePath(TEMPLATE_RULE_FOLDER)
                .withPackageName("org.drools.project.model")
                .withFallbackContext(JavaKogitoBuildContext.CONTEXT_NAME)
                .build(context, "ProjectRuntime");
    }

    public String generate() {

        CompilationUnit cu = generator.compilationUnitOrThrow("Could not create CompilationUnit");
        ClassOrInterfaceDeclaration clazz = cu
                .findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new InvalidTemplateException(generator, "Compilation unit doesn't contain a class or interface declaration!"));

        if (context.hasDI()) {
            context.getDependencyInjectionAnnotator().withApplicationComponent(clazz);
        }

        writeInitKieBasesMethod(clazz);
        writeGetDefaultKieBaseMethod(clazz);
        writeNewDefaultKieSessionMethod(clazz);
        writeGetKieBaseForSessionMethod(clazz);
        writeGetConfForSessionMethod(clazz);

        return cu.toString();
    }

    private void writeInitKieBasesMethod(ClassOrInterfaceDeclaration clazz) {
        MethodDeclaration initKieBasesMethod = clazz.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.getNameAsString().equals("initKieBases"))
                .findFirst()
                .orElseThrow(() -> new InvalidTemplateException(generator, "Cannot find initKieBases method"));

        IfStmt ifStmt = initKieBasesMethod.findFirst(IfStmt.class).orElseThrow(() -> new NoSuchElementException());
        BlockStmt ifBlock = ifStmt.getThenStmt().asBlockStmt();
        for (String kbaseName : modelMethod.getKieBaseNames()) {
            ifBlock.addStatement("kbaseMap.put( \"" + kbaseName + "\", " +
                    "new KieBaseImpl( KieBaseBuilder.createKieBaseFromModel( model.getModelsForKieBase( \"" + kbaseName + "\" ), " +
                    "model.getKieModuleModel().getKieBaseModels().get( \"" + kbaseName + "\" ) ) ) );\n");
        }
    }

    private void writeGetDefaultKieBaseMethod(ClassOrInterfaceDeclaration clazz) {
        MethodDeclaration getDefaultKieBaseMethod = clazz.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.getNameAsString().equals("getKieBase"))
                .filter(m -> m.getParameters().isEmpty())
                .findFirst()
                .orElseThrow(() -> new InvalidTemplateException(generator, "Cannot find getKieBase method"));

        if (modelMethod.getDefaultKieBaseName() != null) {
            getDefaultKieBaseMethod.findFirst(StringLiteralExpr.class).get().setString(modelMethod.getDefaultKieBaseName());
        }
    }

    private void writeNewDefaultKieSessionMethod(ClassOrInterfaceDeclaration clazz) {
        MethodDeclaration newDefaultKieSessionMethod = clazz.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.getNameAsString().equals("newKieSession"))
                .filter(m -> m.getParameters().isEmpty())
                .findFirst()
                .orElseThrow(() -> new InvalidTemplateException(generator, "Cannot find newKieSession method"));

        if (modelMethod.getDefaultKieSessionName() != null) {
            newDefaultKieSessionMethod.findFirst(StringLiteralExpr.class).get().setString(modelMethod.getDefaultKieSessionName());
        }
    }

    private void writeGetKieBaseForSessionMethod(ClassOrInterfaceDeclaration clazz) {
        MethodDeclaration getKieBaseForSessionMethod = clazz.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.getNameAsString().equals("getKieBaseForSession"))
                .findFirst()
                .orElseThrow(() -> new InvalidTemplateException(generator, "Cannot find getKieBaseForSession method"));

        SwitchStmt switchStmt = getKieBaseForSessionMethod.findFirst(SwitchStmt.class).get();

        for (Map.Entry<String, String> entry : modelMethod.getkSessionForkBase().entrySet()) {
            StringLiteralExpr sessionName = new StringLiteralExpr(entry.getKey());
            Statement stmt = parseStatement("return getKieBase(\"" + entry.getValue() + "\");");
            SwitchEntry switchEntry = new SwitchEntry(new NodeList<Expression>(sessionName), SwitchEntry.Type.STATEMENT_GROUP, new NodeList<>(stmt));
            switchStmt.getEntries().add(switchEntry);
        }
    }

    private void writeGetConfForSessionMethod(ClassOrInterfaceDeclaration clazz) {
        MethodDeclaration getConfForSessionMethod = clazz.findAll(MethodDeclaration.class).stream()
                .filter(m -> m.getNameAsString().equals("getConfForSession"))
                .findFirst()
                .orElseThrow(() -> new InvalidTemplateException(generator, "Cannot find getConfForSession method"));

        SwitchStmt switchStmt = getConfForSessionMethod.findFirst(SwitchStmt.class).get();

        for (Map.Entry<String, BlockStmt> entry : modelMethod.getkSessionConfs().entrySet()) {
            StringLiteralExpr sessionName = new StringLiteralExpr(entry.getKey());
            SwitchEntry switchEntry = new SwitchEntry(new NodeList<Expression>(sessionName), SwitchEntry.Type.STATEMENT_GROUP, new NodeList<>(entry.getValue()));
            switchStmt.getEntries().add(switchEntry);
        }
    }

    public String getName() {
        return generator.generatedFilePath();
    }
}

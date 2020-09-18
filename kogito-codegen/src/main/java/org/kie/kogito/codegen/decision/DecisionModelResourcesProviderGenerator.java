/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.codegen.decision;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.kie.api.management.GAV;
import org.kie.kogito.codegen.AddonsConfig;
import org.kie.kogito.codegen.di.DependencyInjectionAnnotator;
import org.kie.kogito.decision.DecisionModelType;
import org.kie.kogito.dmn.DecisionModelJarResource;
import org.kie.kogito.dmn.DecisionModelRelativeResource;

import static com.github.javaparser.StaticJavaParser.parse;
import static org.kie.kogito.codegen.CodegenUtils.newObject;

public class DecisionModelResourcesProviderGenerator {

    private static final String TEMPLATE_JAVA = "/class-templates/DecisionModelResourcesProviderTemplate.java";

    private final String packageName;
    private final String applicationCanonicalName;
    private final List<DMNResource> resources;

    private DependencyInjectionAnnotator annotator;
    private AddonsConfig addonsConfig = AddonsConfig.DEFAULT;

    public DecisionModelResourcesProviderGenerator(final String packageName,
                                                   final String applicationCanonicalName,
                                                   final List<DMNResource> resources) {
        this.packageName = packageName;
        this.applicationCanonicalName = applicationCanonicalName;
        this.resources = resources;
    }

    public DecisionModelResourcesProviderGenerator withDependencyInjection(final DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
        return this;
    }

    public DecisionModelResourcesProviderGenerator withAddons(final AddonsConfig addonsConfig) {
        this.addonsConfig = addonsConfig;
        return this;
    }

    public String generate() {
        final CompilationUnit compilationUnit =
                parse(this.getClass().getResourceAsStream(TEMPLATE_JAVA))
                        .setPackageDeclaration(packageName);

        final ClassOrInterfaceDeclaration clazz = compilationUnit
                .findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new NoSuchElementException("Compilation unit doesn't contain a class or interface declaration!"));

        if (Objects.nonNull(this.annotator)) {
            annotator.withSingletonComponent(clazz);
        }

        if (addonsConfig.useTracing()) {
            setupResourcesVariable(clazz);
        }
        return compilationUnit.toString();
    }

    public String generatedFilePath() {
        return (this.packageName + ".DecisionModelResourcesProvider").replace('.', '/') + ".java";
    }

    private void setupResourcesVariable(final ClassOrInterfaceDeclaration typeDeclaration) {
        final List<MethodDeclaration> getResourcesMethods = typeDeclaration.getMethodsBySignature("getResources");
        if (getResourcesMethods.size() != 1) {
            throw (new RuntimeException("A \"getResources()\" method was not found in " + TEMPLATE_JAVA));
        }
        final MethodDeclaration getResourcesMethod = getResourcesMethods.get(0);
        final BlockStmt body = getResourcesMethod.getBody().orElseThrow(() -> new RuntimeException("Can't find the body of the \"get()\" method."));
        final VariableDeclarator variable = getResourcesMethod.findFirst(VariableDeclarator.class).orElseThrow(() -> new RuntimeException("Can't find a variable declaration in the \"get()\" method."));
        for (DMNResource resource : resources) {
            final MethodCallExpr add = new MethodCallExpr(variable.getNameAsExpression(), "add");
            if (resource.getPath().toString().endsWith(".jar")) {
                add.addArgument(newObject(DecisionModelJarResource.class,
                                          mockGAV(),
                                          new StringLiteralExpr(getDecisionModelJarResourcePath(resource)),
                                          new StringLiteralExpr(resource.getDmnModel().getNamespace()),
                                          new StringLiteralExpr(resource.getDmnModel().getName()),
                                          makeDMNType()));
            } else {
                final ClassOrInterfaceType applicationClass = StaticJavaParser.parseClassOrInterfaceType(applicationCanonicalName);
                add.addArgument(newObject(DecisionModelRelativeResource.class,
                                          mockGAV(),
                                          new StringLiteralExpr(getDecisionModelRelativeResourcePath(resource)),
                                          new StringLiteralExpr(resource.getDmnModel().getNamespace()),
                                          new StringLiteralExpr(resource.getDmnModel().getName()),
                                          makeDMNType(),
                                          new FieldAccessExpr(applicationClass.getNameAsExpression(), "class")));
            }
            body.addStatement(body.getStatements().size() - 1, add);
        }
    }

    private ObjectCreationExpr mockGAV() {
        //TODO See https://issues.redhat.com/browse/FAI-239
        return newObject(GAV.class,
                         new StringLiteralExpr("dummy"),
                         new StringLiteralExpr("dummy"),
                         new StringLiteralExpr("0.0"));
    }

    private FieldAccessExpr makeDMNType() {
        NameExpr clazz = new NameExpr(DecisionModelType.class.getName());
        return new FieldAccessExpr(clazz, "DMN");
    }

    private String getDecisionModelJarResourcePath(final DMNResource resource) {
        return resource.getDmnModel().getResource().getSourcePath();
    }

    private String getDecisionModelRelativeResourcePath(final DMNResource resource) {
        String source = getDecisionModelJarResourcePath(resource);
        Path relativizedPath = resource.getPath().relativize(Paths.get(source));
        return "/" + relativizedPath.toString().replace(File.separatorChar, '/');
    }
}

/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.codegen.decision;

import java.net.URLEncoder;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.drools.core.util.StringUtils;
import org.kie.kogito.codegen.BodyDeclarationComparator;
import org.kie.dmn.feel.codegen.feel11.CodegenStringUtil;
import org.kie.dmn.model.api.DecisionService;
import org.kie.dmn.model.api.Definitions;
import org.kie.kogito.codegen.di.DependencyInjectionAnnotator;
import org.kie.kogito.codegen.process.CodegenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protostream.javassist.NotFoundException;

import static com.github.javaparser.StaticJavaParser.parse;
import static com.github.javaparser.StaticJavaParser.parseImport;
import static com.github.javaparser.StaticJavaParser.parseStatement;

public class DMNRestResourceGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DMNRestResourceGenerator.class);
    private final Definitions definitions;
    private final String decisionName;
    private final String nameURL;
    private final String packageName;
    private final String decisionId;
    private final String relativePath;
    private final String resourceClazzName;
    private final String appCanonicalName;
    private DependencyInjectionAnnotator annotator;
    private boolean useMonitoring;

    public DMNRestResourceGenerator(Definitions definitions, String appCanonicalName) {
        this.definitions = definitions;
        this.packageName = CodegenStringUtil.escapeIdentifier(definitions.getNamespace());
        this.decisionId = definitions.getId();
        this.decisionName = CodegenStringUtil.escapeIdentifier(definitions.getName());
        this.nameURL = URLEncoder.encode(definitions.getName()).replaceAll("\\+", "%20");
        this.appCanonicalName = appCanonicalName;
        String classPrefix = StringUtils.capitalize(decisionName);
        this.resourceClazzName = classPrefix + "Resource";
        this.relativePath = packageName.replace(".", "/") + "/" + resourceClazzName + ".java";
    }

    public String generate() {
        CompilationUnit clazz = parse(this.getClass().getResourceAsStream("/class-templates/DMNRestResourceTemplate.java"));
        clazz.setPackageDeclaration(this.packageName);

        ClassOrInterfaceDeclaration template = clazz
                .findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new NoSuchElementException("Compilation unit doesn't contain a class or interface declaration!"));

        template.setName(resourceClazzName);

        template.findAll(StringLiteralExpr.class).forEach(this::interpolateStrings);
        template.findAll(MethodDeclaration.class).forEach(this::interpolateMethods);

        if (useInjection()) {
            template.findAll(FieldDeclaration.class,
                             CodegenUtils::isApplicationField).forEach(fd -> annotator.withInjection(fd));
        } else {
            template.findAll(FieldDeclaration.class,
                             CodegenUtils::isApplicationField).forEach(this::initializeApplicationField);
        }
        
        MethodDeclaration dmnMethod = template.findAll(MethodDeclaration.class, x -> x.getName().toString().equals("dmn")).get(0);

        for (DecisionService ds : definitions.getDecisionService()) {
            if (ds.getAdditionalAttributes().keySet().stream().anyMatch(qn -> qn.getLocalPart().equals("dynamicDecisionService"))) {
                continue;
            }

            MethodDeclaration clonedMethod = dmnMethod.clone();
            String name = CodegenStringUtil.escapeIdentifier("decisionService_" + ds.getName());
            clonedMethod.setName(name);
            MethodCallExpr evaluateCall = clonedMethod.findFirst(MethodCallExpr.class, x -> x.getName().toString().equals("evaluateAll")).orElseThrow(() -> new RuntimeException("Template was modified!"));
            evaluateCall.setName(new SimpleName("evaluateDecisionService"));
            evaluateCall.addArgument(new StringLiteralExpr(ds.getName()));
            clonedMethod.addAnnotation(new SingleMemberAnnotationExpr(new Name("javax.ws.rs.Path"), new StringLiteralExpr("/" + ds.getName())));
            ReturnStmt returnStmt = clonedMethod.findFirst(ReturnStmt.class).orElseThrow(() -> new RuntimeException("Template was modified!"));
            if (ds.getOutputDecision().size() == 1) {
                MethodCallExpr rewrittenReturnExpr = new MethodCallExpr(new MethodCallExpr(new MethodCallExpr(new NameExpr("result"), "getDecisionResults"), "get").addArgument(new IntegerLiteralExpr(0)), "getResult");
                returnStmt.setExpression(rewrittenReturnExpr);
            }

            if (useMonitoring){
                addMonitoringToMethod(clonedMethod, ds.getName());
            }

            template.addMember(clonedMethod);
        }

        if (useMonitoring){
            // TODO: Better way to identify class?
            // TODO: Use annotator spring/quarkus
            addMonitoringImports(clazz);
            ClassOrInterfaceDeclaration exceptionClazz = clazz.findAll(ClassOrInterfaceDeclaration.class).stream().filter(f -> !f.equals(template)).collect(Collectors.toList()).get(0);
            addExceptionMetricsLogging(clazz, exceptionClazz, nameURL);
            addMonitoringToMethod(dmnMethod, nameURL);
        }

        template.getMembers().sort(new BodyDeclarationComparator());
        return clazz.toString();
    }

    public String getNameURL() {
        return nameURL;
    }

    public Definitions getDefinitions(){
        return this.definitions;
    }

    public DMNRestResourceGenerator withDependencyInjection(DependencyInjectionAnnotator annotator) {
        this.annotator = annotator;
        return this;
    }

    public DMNRestResourceGenerator withMonitoring(boolean useMonitoring) {
        this.useMonitoring = useMonitoring;
        return this;
    }

    public String className() {
        return resourceClazzName;
    }

    private void addExceptionMetricsLogging(CompilationUnit cu, ClassOrInterfaceDeclaration template, String nameURL){
        // TODO: Improve code generation
        MethodDeclaration method = template.findAll(MethodDeclaration.class, x -> x.getName().toString().equals("toResponse")).get(0);
        cu.addImport(parseImport("import org.kie.addons.monitoring.system.metrics.SystemMetricsCollector;"));

        Optional<BlockStmt> body = method.getBody();
        if (body.isPresent()) {
            Optional<ReturnStmt> returnStmt = body.get().findFirst(ReturnStmt.class);
            if (returnStmt.isPresent()) {
                NodeList<Statement> statements = body.get().getStatements();
                statements.addBefore(parseStatement("SystemMetricsCollector.registerException(\"" + nameURL + "\", e.getStackTrace()[0].toString());"), returnStmt.get());
                return;
            }
        }

        logger.warn("DMN Exception handler not found: can't add monitoring to exceptions");
    }

    private void addMonitoringImports(CompilationUnit cu){
        cu.addImport(parseImport("import org.kie.addons.monitoring.system.metrics.SystemMetricsCollector;"));
        cu.addImport(parseImport("import org.kie.addons.monitoring.system.metrics.DMNResultMetricsBuilder;"));
    }

    private void addMonitoringToMethod(MethodDeclaration method, String nameURL){
        Optional<BlockStmt> body = method.getBody();
        if (body.isPresent()) {
            NodeList<Statement> statements = body.get().getStatements();
            Optional<IfStmt> ifStmt = body.get().findFirst(IfStmt.class);
            if (ifStmt.isPresent()) {
                statements.addFirst(parseStatement("double startTime = System.nanoTime();"));
                statements.addBefore(parseStatement("double endTime = System.nanoTime();"), ifStmt.get());
                statements.addBefore(parseStatement("SystemMetricsCollector.registerElapsedTimeSampleMetrics(\"" + nameURL + "\", endTime - startTime);"), ifStmt.get());
                statements.addBefore(parseStatement("DMNResultMetricsBuilder.generateMetrics(result);"), ifStmt.get());
                return;
            }
        }
        logger.warn("Could not inject monitoring in endpoint.");
    }

    private void initializeApplicationField(FieldDeclaration fd) {
        fd.getVariable(0).setInitializer(new ObjectCreationExpr().setType(appCanonicalName));
    }

    private void interpolateStrings(StringLiteralExpr vv) {
        String s = vv.getValue();
        String documentation = "";
        String interpolated = s.replace("$name$", decisionName)
                               .replace("$nameURL$", nameURL)
                               .replace("$id$", decisionId)
                               .replace("$modelName$", definitions.getName())
                               .replace("$modelNamespace$", definitions.getNamespace())
                               .replace("$documentation$", documentation);

        vv.setString(interpolated);
    }
    
    private void interpolateMethods(MethodDeclaration m) {
        SimpleName methodName = m.getName();
        String interpolated = methodName.asString().replace("$name$", decisionName);
        m.setName(interpolated);
    }
    
    public String generatedFilePath() {
        return relativePath;
    }
    
    protected boolean useInjection() {
        return this.annotator != null;
    }
}
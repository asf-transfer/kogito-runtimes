/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.codegen.process;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.drools.io.FileSystemResource;
import org.jbpm.compiler.canonical.ProcessMetaData;
import org.jbpm.compiler.canonical.ProcessToExecModelGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.definition.process.Process;
import org.kie.kogito.codegen.api.AddonsConfig;
import org.kie.kogito.codegen.api.context.KogitoBuildContext;
import org.kie.kogito.codegen.api.context.impl.JavaKogitoBuildContext;
import org.kie.kogito.codegen.api.context.impl.QuarkusKogitoBuildContext;
import org.kie.kogito.codegen.api.context.impl.SpringBootKogitoBuildContext;
import org.kie.kogito.internal.process.runtime.KogitoWorkflowProcess;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kie.kogito.codegen.process.ProcessResourceGenerator.JAKARTA_REST_ANNOTATIONS;
import static org.kie.kogito.codegen.process.ProcessResourceGenerator.JAKARTA_TRANSACTIONAL_IMPORT;
import static org.kie.kogito.codegen.process.ProcessResourceGenerator.SPRING_REST_ANNOTATIONS;
import static org.kie.kogito.codegen.process.ProcessResourceGenerator.SPRING_TRANSACTIONAL_IMPORT;
import static org.kie.kogito.codegen.process.ProcessResourceGenerator.TRANSACTIONAL_ANNOTATION;

class ProcessResourceGeneratorTest {

    private static final String SIGNAL_METHOD = "signal_";
    private static final List<String> JAVA_AND_QUARKUS_REST_ANNOTATIONS = List.of("DELETE", "GET", "POST");
    private static final List<String> SPRING_BOOT_REST_ANNOTATIONS = List.of("DeleteMapping", "GetMapping",
            "PostMapping");

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void testGenerateProcessWithDocumentation(KogitoBuildContext.Builder contextBuilder) {
        String fileName = "src/test/resources/ProcessWithDocumentation.bpmn";
        String expectedSummary = "This is the documentation";
        String expectedDescription = "This is the process instance description";

        testOpenApiDocumentation(contextBuilder, fileName, expectedSummary, expectedDescription);
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void testGenerateProcessWithoutDocumentation(KogitoBuildContext.Builder contextBuilder) {
        String fileName = "src/test/resources/ProcessWithoutDocumentation.bpmn";
        String expectedSummary = "ProcessWithoutDocumentation";
        String expectedDescription = "";

        testOpenApiDocumentation(contextBuilder, fileName, expectedSummary, expectedDescription);
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void testGenerateBoundarySignalEventOnTask(KogitoBuildContext.Builder contextBuilder) {
        String fileName = "src/test/resources/signalevent/BoundarySignalEventOnTask.bpmn2";

        ClassOrInterfaceDeclaration classDeclaration = getResourceClassDeclaration(contextBuilder, fileName);

        KogitoWorkflowProcess process = parseProcess(fileName);

        String outputType = new ModelClassGenerator(contextBuilder.build(), process).simpleName() + "Output";

        classDeclaration.getMethods().stream()
                .filter(method -> isRestMethod(method) && method.getNameAsString().startsWith(SIGNAL_METHOD))
                .forEach(method -> assertMethodOutputModelType(method, outputType));
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void testGenerateStartSignalEventStringPayload(KogitoBuildContext.Builder contextBuilder) {
        String fileName = "src/test/resources/startsignal/StartSignalEventStringPayload.bpmn2";
        String signalName = "start";

        ClassOrInterfaceDeclaration classDeclaration = getResourceClassDeclaration(contextBuilder, fileName);

        KogitoWorkflowProcess process = parseProcess(fileName);

        MethodDeclaration startSignalMethod = classDeclaration.getMethods().stream()
                .filter(method -> isRestMethod(method) && method.getNameAsString().equals(SIGNAL_METHOD + signalName))
                .findFirst()
                .orElseThrow();

        assertThat(startSignalMethod.isPublic()).isTrue();
        assertThat(startSignalMethod.getParameters()).hasSize(4);
        assertThat(startSignalMethod.getParameters().stream()
                .filter(parameter -> parameter.getNameAsString().equals("data"))
                .findFirst()
                .get()).matches(parameter -> parameter.getTypeAsString().equals("java.lang.String"));

        assertThat(startSignalMethod.getBody().get().getChildNodes()
                .stream()
                .anyMatch(child -> child.toString().equals("model.setMessage(data);"))).isTrue();

        String outputType = new ModelClassGenerator(contextBuilder.build(), process).simpleName() + "Output";

        classDeclaration.getMethods().stream()
                .filter(method -> isRestMethod(method) && method.getNameAsString().startsWith(SIGNAL_METHOD + "0"))
                .forEach(method -> assertMethodOutputModelType(method, outputType));
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void testGenerateStartSignalEventNoPayload(KogitoBuildContext.Builder contextBuilder) {
        String fileName = "src/test/resources/startsignal/StartSignalEventNoPayload.bpmn2";
        String signalName = "start";

        ClassOrInterfaceDeclaration classDeclaration = getResourceClassDeclaration(contextBuilder, fileName);

        KogitoWorkflowProcess process = parseProcess(fileName);

        MethodDeclaration startSignalMethod = classDeclaration.getMethods().stream()
                .filter(method -> isRestMethod(method) && method.getNameAsString().equals(SIGNAL_METHOD + signalName))
                .findFirst()
                .orElseThrow();

        assertThat(startSignalMethod.isPublic()).isTrue();
        assertThat(startSignalMethod.getParameters()).hasSize(3);
        assertThat(startSignalMethod.getParameters()
                .stream()
                .anyMatch(parameter -> parameter.getNameAsString().equals("data"))).isFalse();

        assertThat(startSignalMethod.getBody().get().getChildNodes()
                .stream()
                .anyMatch(child -> child.toString().equals("model.setMessage(data);"))).isFalse();

        String outputType = new ModelClassGenerator(contextBuilder.build(), process).simpleName() + "Output";

        classDeclaration.getMethods().stream()
                .filter(method -> isRestMethod(method) && method.getNameAsString().startsWith(SIGNAL_METHOD + "0"))
                .forEach(method -> assertMethodOutputModelType(method, outputType));
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void testTransactionEnabled(KogitoBuildContext.Builder contextBuilder) {
        String fileName = "src/test/resources/startsignal/StartSignalEventNoPayload.bpmn2";

        ProcessResourceGenerator processResourceGenerator = getProcessResourceGenerator(contextBuilder, fileName, true);
        CompilationUnit compilationUnit =
                processResourceGenerator.createCompilationUnit(processResourceGenerator.createTemplatedGeneratorBuilder());
        assertThat(compilationUnit).isNotNull();
        Optional<ClassOrInterfaceDeclaration> classOrInterfaceDeclarationOptional = compilationUnit.getChildNodes().stream()
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .findFirst();
        assertThat(classOrInterfaceDeclarationOptional).isPresent();
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = classOrInterfaceDeclarationOptional.get();
        switch (contextBuilder.build().name()) {
            case QuarkusKogitoBuildContext.CONTEXT_NAME -> testTransactionEnabled(compilationUnit, classOrInterfaceDeclaration, JAKARTA_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    JAKARTA_REST_ANNOTATIONS);
            case SpringBootKogitoBuildContext.CONTEXT_NAME -> testTransactionEnabled(compilationUnit, classOrInterfaceDeclaration, SPRING_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    SPRING_REST_ANNOTATIONS);
            case JavaKogitoBuildContext.CONTEXT_NAME -> testTransactionEnabled(compilationUnit, classOrInterfaceDeclaration, JAKARTA_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    JAKARTA_REST_ANNOTATIONS);
        }
        processResourceGenerator.manageTransactional(compilationUnit);
        switch (contextBuilder.build().name()) {
            case QuarkusKogitoBuildContext.CONTEXT_NAME -> testTransactionEnabled(compilationUnit, classOrInterfaceDeclaration, JAKARTA_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    JAKARTA_REST_ANNOTATIONS);
            case SpringBootKogitoBuildContext.CONTEXT_NAME -> testTransactionEnabled(compilationUnit, classOrInterfaceDeclaration, SPRING_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    SPRING_REST_ANNOTATIONS);
            case JavaKogitoBuildContext.CONTEXT_NAME -> testTransactionEnabled(compilationUnit, classOrInterfaceDeclaration, JAKARTA_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    JAKARTA_REST_ANNOTATIONS);
        }
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void testTransactionDisabled(KogitoBuildContext.Builder contextBuilder) {
        String fileName = "src/test/resources/startsignal/StartSignalEventNoPayload.bpmn2";

        ProcessResourceGenerator processResourceGenerator = getProcessResourceGenerator(contextBuilder, fileName,
                false);
        CompilationUnit compilationUnit =
                processResourceGenerator.createCompilationUnit(processResourceGenerator.createTemplatedGeneratorBuilder());
        assertThat(compilationUnit).isNotNull();
        Optional<ClassOrInterfaceDeclaration> classOrInterfaceDeclarationOptional = compilationUnit.getChildNodes().stream()
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .findFirst();
        assertThat(classOrInterfaceDeclarationOptional).isPresent();
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = classOrInterfaceDeclarationOptional.get();
        switch (contextBuilder.build().name()) {
            case QuarkusKogitoBuildContext.CONTEXT_NAME -> testTransactionEnabled(compilationUnit, classOrInterfaceDeclaration, JAKARTA_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    JAKARTA_REST_ANNOTATIONS);
            case SpringBootKogitoBuildContext.CONTEXT_NAME -> testTransactionEnabled(compilationUnit, classOrInterfaceDeclaration, SPRING_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    SPRING_REST_ANNOTATIONS);
            case JavaKogitoBuildContext.CONTEXT_NAME -> testTransactionEnabled(compilationUnit, classOrInterfaceDeclaration, JAKARTA_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    JAKARTA_REST_ANNOTATIONS);
        }
        processResourceGenerator.manageTransactional(compilationUnit);
        switch (contextBuilder.build().name()) {
            case QuarkusKogitoBuildContext.CONTEXT_NAME -> testTransactionDisabled(compilationUnit, classOrInterfaceDeclaration, JAKARTA_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    JAKARTA_REST_ANNOTATIONS);
            case SpringBootKogitoBuildContext.CONTEXT_NAME -> testTransactionDisabled(compilationUnit, classOrInterfaceDeclaration, SPRING_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    SPRING_REST_ANNOTATIONS);
            case JavaKogitoBuildContext.CONTEXT_NAME -> testTransactionDisabled(compilationUnit, classOrInterfaceDeclaration, JAKARTA_TRANSACTIONAL_IMPORT, TRANSACTIONAL_ANNOTATION,
                    JAKARTA_REST_ANNOTATIONS);
        }
    }

    void testTransactionEnabled(CompilationUnit toTest,
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
            String transactionalImport, String transactionalAnnotation,
            List<String> restMappings) {
        assertThat(toTest.getImports().stream().filter(importDeclaration -> importDeclaration.getName().toString().equals(transactionalImport))).hasSize(1);
        classOrInterfaceDeclaration.getMethods().stream()
                .filter(methodDeclaration -> methodDeclaration.getAnnotations().stream().anyMatch(annotationExpr -> restMappings.contains(annotationExpr.getNameAsString())))
                .forEach(methodDeclaration -> assertThat(
                        methodDeclaration.getAnnotations().stream().filter(annotationExpr -> annotationExpr.getNameAsString().equals(transactionalAnnotation))).hasSize(1));
    }

    void testTransactionDisabled(CompilationUnit toTest,
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
            String transactionalImport,
            String transactionalAnnotation,
            List<String> restMappings) {
        assertThat(toTest.getImports().stream().filter(importDeclaration -> importDeclaration.getName().toString().equals(transactionalImport))).isEmpty();
        classOrInterfaceDeclaration.getMethods().stream()
                .filter(methodDeclaration -> methodDeclaration.getAnnotations().stream().anyMatch(annotationExpr -> restMappings.contains(annotationExpr.getNameAsString())))
                .forEach(methodDeclaration -> assertThat(
                        methodDeclaration.getAnnotations().stream().filter(annotationExpr -> annotationExpr.getNameAsString().equals(transactionalAnnotation))).isEmpty());
    }

    void testOpenApiDocumentation(KogitoBuildContext.Builder contextBuilder, String fileName, String expectedSummary,
            String expectedDescription) {
        ClassOrInterfaceDeclaration classDeclaration = getResourceClassDeclaration(contextBuilder, fileName);

        classDeclaration.getMethods().stream()
                .filter(this::isRestMethod)
                .forEach(method -> assertThatMethodHasOpenApiDocumentation(method, expectedSummary,
                        expectedDescription));
    }

    private ClassOrInterfaceDeclaration getResourceClassDeclaration(KogitoBuildContext.Builder contextBuilder,
            String fileName) {
        KogitoWorkflowProcess process = parseProcess(fileName);
        CompilationUnit compilationUnit = getCompilationUnit(contextBuilder, process);
        Optional<ClassOrInterfaceDeclaration> classDeclaration = compilationUnit.getClassByName(process.getId() +
                "Resource");
        assertThat(classDeclaration).isNotEmpty();
        return classDeclaration.orElseThrow();
    }

    private CompilationUnit getCompilationUnit(KogitoBuildContext.Builder contextBuilder,
            KogitoWorkflowProcess process) {
        ProcessResourceGenerator processResourceGenerator = getProcessResourceGenerator(contextBuilder, process, true);
        return StaticJavaParser.parse(processResourceGenerator.generate());
    }

    private ProcessResourceGenerator getProcessResourceGenerator(KogitoBuildContext.Builder contextBuilder,
            String fileName, boolean withTransaction) {
        return getProcessResourceGenerator(contextBuilder, parseProcess(fileName), withTransaction);
    }

    private ProcessResourceGenerator getProcessResourceGenerator(KogitoBuildContext.Builder contextBuilder,
            KogitoWorkflowProcess process,
            boolean withTransaction) {
        KogitoBuildContext context = createContext(contextBuilder);

        ProcessExecutableModelGenerator execModelGen =
                new ProcessExecutableModelGenerator(process, new ProcessToExecModelGenerator(context.getClassLoader()));

        KogitoWorkflowProcess workFlowProcess = execModelGen.process();
        ProcessResourceGenerator toReturn = new ProcessResourceGenerator(
                context,
                workFlowProcess,
                new ModelClassGenerator(context, workFlowProcess).className(),
                execModelGen.className(),
                context.getPackageName() + ".Application");

        ProcessMetaData metaData = execModelGen.generate();

        toReturn
                .withSignals(metaData.getSignals())
                .withTriggers(metaData.isStartable(), metaData.isDynamic(), metaData.getTriggers())
                .withTransaction(withTransaction);
        return toReturn;
    }

    private void assertThatMethodHasOpenApiDocumentation(MethodDeclaration method, String summary, String description) {
        Optional<AnnotationExpr> annotation = method.getAnnotationByName("Operation");
        assertThat(annotation).isNotEmpty();
        assertThatAnnotationHasSummaryAndDescription(annotation.orElseThrow(), summary, description);
    }

    private void assertMethodOutputModelType(MethodDeclaration method, String outputType) {
        assertThat(method.getType()).isNotNull();
        assertThat(method.getType().asString()).isEqualTo(outputType);
    }

    private void assertThatAnnotationHasSummaryAndDescription(AnnotationExpr annotation, String summary,
            String description) {
        NodeList<MemberValuePair> pairs = ((NormalAnnotationExpr) annotation).getPairs();

        assertThat(pairs).containsExactlyInAnyOrder(
                new MemberValuePair("summary", new StringLiteralExpr(summary)),
                new MemberValuePair("description", new StringLiteralExpr(description)));
    }

    private boolean isRestMethod(MethodDeclaration method) {
        return method.getAnnotations().stream()
                .anyMatch(isOpenApiAnnotation().or(isSwaggerAnnotation()));
    }

    private Predicate<AnnotationExpr> isOpenApiAnnotation() {
        return annotation -> JAVA_AND_QUARKUS_REST_ANNOTATIONS.contains(annotation.getNameAsString());
    }

    private Predicate<AnnotationExpr> isSwaggerAnnotation() {
        return annotation -> SPRING_BOOT_REST_ANNOTATIONS.contains(annotation.getNameAsString());
    }

    private KogitoWorkflowProcess parseProcess(String fileName) {
        Collection<Process> processes = ProcessCodegen.parseProcessFile(new FileSystemResource(new File(fileName)));
        assertThat(processes).hasSize(1);
        Process process = processes.stream().findAny().orElseThrow();
        assertThat(process).isInstanceOf(KogitoWorkflowProcess.class);
        return (KogitoWorkflowProcess) process;
    }

    private KogitoBuildContext createContext(KogitoBuildContext.Builder contextBuilder) {
        AddonsConfig addonsConfig = AddonsConfig.builder()
                .withMonitoring(false)
                .withPrometheusMonitoring(false)
                .build();

        return contextBuilder.withAddonsConfig(addonsConfig).build();
    }
}

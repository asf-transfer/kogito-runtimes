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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.kogito.codegen.AddonsConfig;
import org.kie.kogito.codegen.GeneratedFile;
import org.kie.kogito.codegen.GeneratorContext;
import org.kie.kogito.grafana.JGrafana;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DecisionCodegenTest {

    @Test
    public void generateAllFiles() throws Exception {

        GeneratorContext context = stronglyTypedContext();

        DecisionCodegen codeGenerator = DecisionCodegen.ofPath(Paths.get("src/test/resources/decision/models/vacationDays").toAbsolutePath());
        codeGenerator.setContext(context);

        List<GeneratedFile> generatedFiles = codeGenerator.generate();
        assertEquals(5, generatedFiles.size());

        assertIterableEquals(Arrays.asList(
                "decision/InputSet.java",
                "decision/TEmployee.java",
                "decision/TAddress.java",
                "decision/TPayroll.java",
                "decision/VacationsResource.java"
                             ),
                             fileNames(generatedFiles)
        );

        ClassOrInterfaceDeclaration classDeclaration = codeGenerator.section().classDeclaration();
        assertNotNull(classDeclaration);
    }

    private GeneratorContext stronglyTypedContext() {
        Properties properties = new Properties();
        properties.put(DecisionCodegen.STRONGLY_TYPED_CONFIGURATION_KEY, Boolean.TRUE.toString());
        return GeneratorContext.ofProperties(properties);
    }

    private List<String> fileNames(List<GeneratedFile> generatedFiles) {
        return generatedFiles.stream().map(GeneratedFile::relativePath).collect(Collectors.toList());
    }

    @Test
    public void doNotGenerateTypesafeInfo() throws Exception {
        GeneratorContext context = stronglyTypedContext();

        DecisionCodegen codeGenerator = DecisionCodegen.ofPath(Paths.get("src/test/resources/decision/alltypes/").toAbsolutePath());
        codeGenerator.setContext(context);

        List<GeneratedFile> generatedFiles = codeGenerator.generate();
        assertEquals(2, generatedFiles.size());

        assertIterableEquals(Arrays.asList("http_58_47_47www_46trisotech_46com_47definitions_47__4f5608e9_454d74_454c22_45a47e_45ab657257fc9c/InputSet.java",
                                           "http_58_47_47www_46trisotech_46com_47definitions_47__4f5608e9_454d74_454c22_45a47e_45ab657257fc9c/OneOfEachTypeResource.java"),
                             fileNames(generatedFiles)
        );

        ClassOrInterfaceDeclaration classDeclaration = codeGenerator.section().classDeclaration();
        assertNotNull(classDeclaration);
    }

    @Test
    public void givenADMNModelWhenMonitoringIsActiveThenGrafanaDashboardsAreGenerated() throws Exception {
        List<GeneratedFile> dashboards = generateTestDashboards(new AddonsConfig().withMonitoring(true));

        JGrafana vacationOperationalDashboard = JGrafana.parse(new String(dashboards.stream().filter(x -> x.relativePath().contains("operational-dashboard-Vacations.json")).findFirst().get().contents()));

        assertEquals(6, vacationOperationalDashboard.getDashboard().panels.size());
        assertEquals(0, vacationOperationalDashboard.getDashboard().links.size());

        JGrafana vacationDomainDashboard = JGrafana.parse(new String(dashboards.stream().filter(x -> x.relativePath().contains("domain-dashboard-Vacations.json")).findFirst().get().contents()));

        assertEquals(1, vacationDomainDashboard.getDashboard().panels.size());
        assertEquals(0, vacationDomainDashboard.getDashboard().links.size());
    }

    @Test
    public void givenADMNModelWhenMonitoringAndTracingAreActiveThenTheGrafanaDashboardsContainsTheAuditUILink() throws Exception {
        List<GeneratedFile> dashboards = generateTestDashboards(new AddonsConfig().withMonitoring(true).withTracing(true));

        JGrafana vacationOperationalDashboard = JGrafana.parse(new String(dashboards.stream().filter(x -> x.relativePath().contains("operational-dashboard-Vacations.json")).findFirst().get().contents()));

        assertEquals(1, vacationOperationalDashboard.getDashboard().links.size());

        JGrafana vacationDomainDashboard = JGrafana.parse(new String(dashboards.stream().filter(x -> x.relativePath().contains("domain-dashboard-Vacations.json")).findFirst().get().contents()));

        assertEquals(1, vacationDomainDashboard.getDashboard().links.size());
    }

    @Test
    public void resilientToDuplicateDMNIDs() throws Exception {
        DecisionCodegen codeGenerator = DecisionCodegen.ofPath(Paths.get("src/test/resources/decision-test20200507").toAbsolutePath());

        List<GeneratedFile> generatedFiles = codeGenerator.generate();
        assertEquals(2, generatedFiles.size());

        ClassOrInterfaceDeclaration classDeclaration = codeGenerator.section().classDeclaration();
        assertNotNull(classDeclaration);
    }

    @Test
    public void emptyName() throws Exception {
        DecisionCodegen codeGenerator = DecisionCodegen.ofPath(Paths.get("src/test/resources/decision-empty-name").toAbsolutePath());
        RuntimeException re = Assertions.assertThrows(RuntimeException.class, () -> {
            codeGenerator.generate();
        });
        assertEquals("Model name should not be empty", re.getMessage());
    }

    private List<GeneratedFile> generateTestDashboards(AddonsConfig addonsConfig) throws IOException {
        DecisionCodegen codeGenerator = DecisionCodegen.ofPath(Paths.get("src/test/resources/decision/models/vacationDays").toAbsolutePath())
                .withAddons(addonsConfig);

        List<GeneratedFile> generatedFiles = codeGenerator.generate();

        List<GeneratedFile> dashboards = generatedFiles.stream().filter(x -> x.getType() == GeneratedFile.Type.RESOURCE).collect(Collectors.toList());

        assertEquals(2, dashboards.size());

        return dashboards;
    }

    @Test
    public void generateResources() throws Exception {

        GeneratorContext context = stronglyTypedContext();

        DecisionCodegen codeGenerator = DecisionCodegen.ofPath(Paths.get("src/test/resources/decision/models/vacationDays").toAbsolutePath()).withAddons(new AddonsConfig().withTracing(true));
        codeGenerator.setContext(context);

        ClassOrInterfaceDeclaration classDeclaration = codeGenerator.section().classDeclaration();
        assertNotNull(classDeclaration);

        MethodDeclaration methodDeclaration = classDeclaration.findAll(MethodDeclaration.class, d -> d.getName().getIdentifier().equals("getResources")).get(0);
        assertNotNull(methodDeclaration);
        assertTrue(methodDeclaration.getBody().isPresent());

        BlockStmt body = methodDeclaration.getBody().get();
        assertTrue(body.getStatements().size() > 2);
        assertTrue(body.getStatements().get(1).isExpressionStmt());

        ExpressionStmt expression = (ExpressionStmt) body.getStatements().get(1);
        assertTrue(expression.getExpression() instanceof MethodCallExpr);

        MethodCallExpr call = (MethodCallExpr) expression.getExpression();
        assertEquals(call.getName().getIdentifier(), "add");
        assertTrue(call.getScope().isPresent());
        assertTrue(call.getScope().get().isNameExpr());

        NameExpr nameExpr = call.getScope().get().asNameExpr();
        assertEquals(nameExpr.getName().getIdentifier(), "resourcePaths");
    }
}

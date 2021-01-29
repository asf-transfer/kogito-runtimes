/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.codegen.prediction;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.drools.core.util.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.dmn.feel.codegen.feel11.CodegenStringUtil;
import org.kie.kogito.codegen.TemplatedGenerator;
import org.kie.kogito.codegen.context.KogitoBuildContext;
import org.kie.kogito.codegen.context.QuarkusKogitoBuildContext;
import org.kie.kogito.codegen.context.SpringBootKogitoBuildContext;
import org.kie.kogito.codegen.di.CDIDependencyInjectionAnnotator;
import org.kie.pmml.commons.model.KiePMMLModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.kie.kogito.codegen.prediction.PMMLRestResourceGenerator.CONTENT;
import static org.kie.kogito.codegen.prediction.PMMLRestResourceGenerator.QUARKUS_API_RESPONSE;
import static org.kie.kogito.codegen.prediction.PMMLRestResourceGenerator.QUARKUS_REQUEST_BODY;
import static org.kie.kogito.codegen.prediction.PMMLRestResourceGenerator.QUARKUS_SCHEMA;
import static org.kie.kogito.codegen.prediction.PMMLRestResourceGenerator.REF;
import static org.kie.kogito.codegen.prediction.PMMLRestResourceGenerator.SCHEMA;
import static org.kie.kogito.codegen.prediction.PMMLRestResourceGenerator.SPRING_API_RESPONSE;
import static org.kie.kogito.codegen.prediction.PMMLRestResourceGenerator.SPRING_REQUEST_BODY;
import static org.kie.kogito.codegen.prediction.PMMLRestResourceGenerator.SPRING_SCHEMA;
import static org.kie.pmml.commons.utils.KiePMMLModelUtils.getSanitizedClassName;

class PMMLRestResourceGeneratorTest {

    private final static String APP_CANONICAL_NAME = "APP_CANONICAL_NAME";
    private final static KiePMMLModel KIE_PMML_MODEL = getKiePMMLModelInternal();
    private final static String INPUT_REF = "inputRef";
    private final static String OUTPUT_REF = "outputRef";
    private static PMMLRestResourceGenerator pmmlRestResourceGenerator;
    private static ClassOrInterfaceDeclaration template = getClassOrInterfaceDeclaration(QuarkusKogitoBuildContext.builder().build());
    private static KogitoBuildContext context;

    @BeforeAll
    public static void setup() {
        context = QuarkusKogitoBuildContext.builder().build();
        pmmlRestResourceGenerator = new PMMLRestResourceGenerator(context, KIE_PMML_MODEL, APP_CANONICAL_NAME);
        assertNotNull(pmmlRestResourceGenerator);
    }

    private static KiePMMLModel getKiePMMLModelInternal() {
        String modelName = "MODEL_NAME";
        return new KiePMMLModel(modelName, Collections.emptyList()) {

            @Override
            public Object evaluate(Object o, Map<String, Object> map) {
                return null;
            }
        };
    }

    private static ClassOrInterfaceDeclaration getClassOrInterfaceDeclaration(KogitoBuildContext context) {
        CompilationUnit clazz = TemplatedGenerator.builder()
                .build(context, "PMMLRestResource")
                .compilationUnitOrThrow();

        clazz.setPackageDeclaration(CodegenStringUtil.escapeIdentifier("IDENTIFIER"));
        return clazz
                .findFirst(ClassOrInterfaceDeclaration.class)
                .orElseThrow(() -> new NoSuchElementException("Compilation unit doesn't contain a class or interface " +
                                                                      "declaration!"));
    }

    @Test
    void constructor() {
        assertTrue(pmmlRestResourceGenerator.restPackageName.startsWith("org.kie.kogito"));
        assertEquals(APP_CANONICAL_NAME, pmmlRestResourceGenerator.appCanonicalName);
    }

    @Test
    void generateWithDependencyInjection() {
        context.setDependencyInjectionAnnotator(new CDIDependencyInjectionAnnotator());
        String retrieved = pmmlRestResourceGenerator.generate();
        commonEvaluateGenerate(retrieved);
        String expected = "Application application;";
        assertTrue(retrieved.contains(expected));
    }

    @Test
    void generateWithoutDependencyInjection() {
        context.setDependencyInjectionAnnotator(null);
        String retrieved = pmmlRestResourceGenerator.generate();
        commonEvaluateGenerate(retrieved);
        String expected = String.format("Application application = new %s();", APP_CANONICAL_NAME);
        assertTrue(retrieved.contains(expected));
    }

    @Test
    void getNameURL() {
        String classPrefix = getSanitizedClassName(KIE_PMML_MODEL.getName());
        String expected = URLEncoder.encode(classPrefix).replaceAll("\\+", " ");
        assertEquals(expected, pmmlRestResourceGenerator.getNameURL());
    }

    @Test
    void getKiePMMLModel() {
        assertEquals(KIE_PMML_MODEL, pmmlRestResourceGenerator.getKiePMMLModel());
    }

    @Test
    void className() {
        String classPrefix = getSanitizedClassName(KIE_PMML_MODEL.getName());
        String expected = StringUtils.ucFirst(classPrefix) + "Resource";
        assertEquals(expected, pmmlRestResourceGenerator.className());
    }

    @Test
    void generatedFilePath() {
        String retrieved = pmmlRestResourceGenerator.generatedFilePath();
        assertTrue(retrieved.startsWith("org/kie/kogito"));
        String classPrefix = getSanitizedClassName(KIE_PMML_MODEL.getName());
        String expected = StringUtils.ucFirst(classPrefix) + "Resource.java";
        assertTrue(retrieved.endsWith(expected));
    }

    @Test
    void setPathValue() {
        final Optional<SingleMemberAnnotationExpr> retrievedOpt = template.findFirst(SingleMemberAnnotationExpr.class);
        assertTrue(retrievedOpt.isPresent());
        SingleMemberAnnotationExpr retrieved = retrievedOpt.get();
        assertEquals("Path", retrieved.getName().asString());
        pmmlRestResourceGenerator.setPathValue(template);
        try {
            String classPrefix = getSanitizedClassName(KIE_PMML_MODEL.getName());
            String expected = URLEncoder.encode(classPrefix).replaceAll("\\+", " ");
            assertEquals(expected, retrieved.getMemberValue().asStringLiteralExpr().asString());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void setPredictionModelName() {
        assertEquals(1, template.getMethodsByName("pmml").size());
        Optional<BlockStmt> retrievedOpt = template.getMethodsByName("pmml").get(0).getBody();
        assertTrue(retrievedOpt.isPresent());
        BlockStmt retrieved = retrievedOpt.get();
        assertTrue(retrieved.getStatement(0) instanceof ExpressionStmt);
        assertTrue(retrieved.getStatement(0).asExpressionStmt().getExpression() instanceof VariableDeclarationExpr);
        VariableDeclarationExpr variableDeclarationExpr = retrieved.getStatement(0).asExpressionStmt().getExpression().asVariableDeclarationExpr();
        Optional<Expression> expressionOpt = variableDeclarationExpr.getVariable(0).getInitializer();
        assertTrue(expressionOpt.isPresent());
        assertTrue(expressionOpt.get() instanceof MethodCallExpr);
        MethodCallExpr methodCallExpr = expressionOpt.get().asMethodCallExpr();
        assertTrue(methodCallExpr.getArgument(0) instanceof StringLiteralExpr);
        pmmlRestResourceGenerator.setPredictionModelName(template);
        try {
            assertEquals(KIE_PMML_MODEL.getName(), methodCallExpr.getArgument(0).asStringLiteralExpr().asString());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void setQuarkusASAnnotations() {
        ClassOrInterfaceDeclaration quarkusTemplate = getClassOrInterfaceDeclaration(QuarkusKogitoBuildContext.builder().build());
        KogitoBuildContext quarkusContext = QuarkusKogitoBuildContext.builder().build();
        PMMLRestResourceGenerator quarkusPMMLRestResourceGenerator = new PMMLRestResourceGenerator(quarkusContext, KIE_PMML_MODEL, APP_CANONICAL_NAME);
        NodeList<AnnotationExpr> annotations = quarkusTemplate.getMethodsByName("pmml").get(0)
                .getAnnotations();
        quarkusPMMLRestResourceGenerator.setQuarkusOASAnnotations(annotations, INPUT_REF, OUTPUT_REF);
        String retrieved = quarkusTemplate.toString();
        String expected = String.format("@%1$s(%2$s = @org.eclipse.microprofile.openapi.annotations.media.Content(mediaType = \"application/json\", " +
                                                "%3$s = @%4$s(%5$s = \"%6$s\")), description = \"PMML input\")",
                                        QUARKUS_REQUEST_BODY,
                                        CONTENT,
                                        SCHEMA,
                                        QUARKUS_SCHEMA,
                                        REF,
                                        INPUT_REF);
        assertTrue(retrieved.contains(expected));
        expected = String.format("@%1$s(%2$s = @org.eclipse.microprofile.openapi.annotations.media.Content(mediaType = \"application/json\", " +
                                         "%3$s = @%4$s(%5$s = \"%6$s\")), description = \"PMML output\")",
                                 QUARKUS_API_RESPONSE,
                                 CONTENT,
                                 SCHEMA,
                                 QUARKUS_SCHEMA,
                                 REF,
                                 OUTPUT_REF);
        assertTrue(retrieved.contains(expected));
    }

    @Test
    void setSpringOASAnnotations() {
        ClassOrInterfaceDeclaration springTemplate = getClassOrInterfaceDeclaration(SpringBootKogitoBuildContext.builder().build());
        KogitoBuildContext springContext = SpringBootKogitoBuildContext.builder().build();
        PMMLRestResourceGenerator  springPMMLRestResourceGenerator = new PMMLRestResourceGenerator(springContext, KIE_PMML_MODEL, APP_CANONICAL_NAME);
        NodeList<AnnotationExpr> annotations = springTemplate.getMethodsByName("pmml").get(0)
                .getAnnotations();
        springPMMLRestResourceGenerator.setSpringOASAnnotations(annotations, INPUT_REF, OUTPUT_REF);
        String retrieved = springTemplate.toString();
        String expected = String.format("@%1$s(%2$s = @io.swagger.v3.oas.annotations.media.Content(mediaType = \"application/json\", " +
                                                "%3$s = @%4$s(%5$s = \"%6$s\")), description = \"PMML input\")",
                                        SPRING_REQUEST_BODY,
                                        CONTENT,
                                        SCHEMA,
                                        SPRING_SCHEMA,
                                        REF,
                                        INPUT_REF);
        assertTrue(retrieved.contains(expected));
        expected = String.format("@%1$s(%2$s = @io.swagger.v3.oas.annotations.media.Content(mediaType = \"application/json\", " +
                                         "%3$s = @%4$s(%5$s = \"%6$s\")), description = \"PMML output\")",
                                 SPRING_API_RESPONSE,
                                 CONTENT,
                                 SCHEMA,
                                 SPRING_SCHEMA,
                                 REF,
                                 OUTPUT_REF);
        assertTrue(retrieved.contains(expected));
    }

    private void commonEvaluateGenerate(String retrieved) {
        assertNotNull(retrieved);
        String classPrefix = getSanitizedClassName(KIE_PMML_MODEL.getName());
        String expected = String.format("@Path(\"%s\")", classPrefix);
        assertTrue(retrieved.contains(expected));
        expected = StringUtils.ucFirst(classPrefix) + "Resource";
        expected = String.format("public class %s {", expected);
        assertTrue(retrieved.contains(expected));
        expected = String.format("org.kie.kogito.prediction.PredictionModel prediction = application" +
                ".get(org.kie.kogito.prediction.PredictionModels.class).getPredictionModel(\"%s\");", KIE_PMML_MODEL.getName());
        assertTrue(retrieved.contains(expected));
    }

}
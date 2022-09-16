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
package org.kie.kogito.codegen.prediction;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.drools.codegen.common.GeneratedFile;
import org.drools.codegen.common.GeneratedFileType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.kogito.codegen.api.ApplicationSection;
import org.kie.kogito.codegen.api.context.KogitoBuildContext;
import org.kie.kogito.codegen.core.io.CollectedResourceProducer;
import org.kie.pmml.commons.model.HasNestedModels;
import org.kie.pmml.commons.model.HasSourcesMap;
import org.kie.pmml.commons.model.KiePMMLModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.drools.codegen.common.GeneratedFileType.COMPILED_CLASS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kie.efesto.common.api.constants.Constants.INDEXFILE_DIRECTORY_PROPERTY;
import static org.kie.kogito.codegen.api.Generator.REST_TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class PredictionCodegenTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictionCodegenTest.class);

    private static final Path BASE_PATH = Paths.get("src/test/resources/").toAbsolutePath();
    private static final String REGRESSION_SOURCE = "prediction/test_regression.pmml";
    private static final Path REGRESSION_FULL_SOURCE = BASE_PATH.resolve(REGRESSION_SOURCE);
    private static final String SCORECARD_SOURCE = "prediction/test_scorecard.pmml";
    private static final Path SCORECARD_FULL_SOURCE = BASE_PATH.resolve(SCORECARD_SOURCE);
    private static final String MINING_SOURCE = "prediction/test_miningmodel.pmml";
    private static final Path MINING_FULL_SOURCE = BASE_PATH.resolve(MINING_SOURCE);
    private static final String MULTIPLE_SOURCE = "prediction/test_multiplemodels.pmml";
    private static final Path MULTIPLE_FULL_SOURCE = BASE_PATH.resolve(MULTIPLE_SOURCE);
    private static final String REFLECT_JSON = "reflect-config.json";

    private static final String EMPTY = "";
    private static final String MOCK = "mock";
    private static final String NESTED_MOCK = "nestedMock";
    private static final String PMML = "PMML";

    @BeforeAll
    public static void setup() {
        System.setProperty(INDEXFILE_DIRECTORY_PROPERTY, "target/test-classes");
    }

    @AfterAll
    public static void cleanup() {
        System.clearProperty(INDEXFILE_DIRECTORY_PROPERTY);
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    public void isEmpty(KogitoBuildContext.Builder contextBuilder) {
        KogitoBuildContext context = contextBuilder.build();
        PredictionCodegen emptyCodeGenerator = PredictionCodegen.ofCollectedResources(context, Collections.emptyList());

        assertThat(emptyCodeGenerator.isEmpty()).isTrue();
        assertThat(emptyCodeGenerator.isEnabled()).isFalse();

        Collection<GeneratedFile> emptyGeneratedFiles = emptyCodeGenerator.generate();
        assertThat(emptyGeneratedFiles.size()).isZero();

        PredictionCodegen codeGenerator = PredictionCodegen.ofCollectedResources(
                context, CollectedResourceProducer.fromFiles(BASE_PATH, REGRESSION_FULL_SOURCE.toFile()));

        assertThat(codeGenerator.isEmpty()).isFalse();
        assertThat(codeGenerator.isEnabled()).isTrue();

        Collection<GeneratedFile> generatedFiles = codeGenerator.generate();
        assertThat(generatedFiles.size()).isPositive();
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void generateAllFilesRegression(KogitoBuildContext.Builder contextBuilder) {
        KogitoBuildContext context = contextBuilder.build();
        PredictionCodegen codeGenerator = PredictionCodegen.ofCollectedResources(
                context, CollectedResourceProducer.fromFiles(BASE_PATH, REGRESSION_FULL_SOURCE.toFile()));
        generateAllFiles(context, codeGenerator, 5, 3, 1, false);
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void generateAllFilesScorecard(KogitoBuildContext.Builder contextBuilder) {
        KogitoBuildContext context = contextBuilder.build();
        PredictionCodegen codeGenerator = PredictionCodegen.ofCollectedResources(
                context, CollectedResourceProducer.fromFiles(BASE_PATH, SCORECARD_FULL_SOURCE.toFile()));
        generateAllFiles(context, codeGenerator, 36, 34, 1, false);
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void generateAllFilesMining(KogitoBuildContext.Builder contextBuilder) {
        KogitoBuildContext context = contextBuilder.build();
        PredictionCodegen codeGenerator = PredictionCodegen.ofCollectedResources(
                context, CollectedResourceProducer.fromFiles(BASE_PATH, MINING_FULL_SOURCE.toFile()));
        generateAllFiles(context, codeGenerator, 310, 308, 1, false);
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void generateAllFilesMultiple(KogitoBuildContext.Builder contextBuilder) {

        KogitoBuildContext context = contextBuilder.build();
        PredictionCodegen codeGenerator = PredictionCodegen.ofCollectedResources(
                context, CollectedResourceProducer.fromFiles(BASE_PATH, MULTIPLE_FULL_SOURCE.toFile()));
        generateAllFiles(context, codeGenerator, 88, 84, 2, false);
    }

    private static void generateAllFiles(KogitoBuildContext context, PredictionCodegen codeGenerator, int expectedTotalFiles, int expectedCompiledClasses, int expectedRestEndpoints,
            boolean assertReflect) {
        Collection<GeneratedFile> generatedFiles = codeGenerator.generate();

        int expectedGeneratedFilesSize = expectedTotalFiles - (context.hasRESTForGenerator(codeGenerator) ? 0 : expectedRestEndpoints * 2);
        assertEquals(expectedGeneratedFilesSize, generatedFiles.size());

        assertEquals(expectedCompiledClasses, generatedFiles.stream()
                .filter(generatedFile -> generatedFile.category().equals(GeneratedFileType.Category.COMPILED_CLASS) &&
                        generatedFile.type().equals(COMPILED_CLASS))
                .count());

        int expectedReflectResource = assertReflect ? 1 : 0;
        assertEquals(expectedReflectResource, generatedFiles.stream()
                .filter(generatedFile -> generatedFile.category().equals(GeneratedFileType.Category.INTERNAL_RESOURCE) &&
                        generatedFile.type().name().equals(GeneratedFileType.INTERNAL_RESOURCE.name()) &&
                        generatedFile.relativePath().endsWith(REFLECT_JSON))
                .count());

        assertEndpoints(context, generatedFiles, expectedRestEndpoints, codeGenerator);

        Optional<ApplicationSection> optionalApplicationSection = codeGenerator.section();
        assertTrue(optionalApplicationSection.isPresent());

        CompilationUnit compilationUnit = optionalApplicationSection.get().compilationUnit();
        assertNotNull(compilationUnit);
    }

    private static void assertEndpoints(KogitoBuildContext context, Collection<GeneratedFile> generatedFiles, int expectedRestEndpoints, PredictionCodegen codeGenerator) {
        if (context.hasRESTForGenerator(codeGenerator)) {
            // REST resource
            assertEquals(expectedRestEndpoints, generatedFiles.stream()
                    .filter(generatedFile -> generatedFile.type().equals(REST_TYPE))
                    .count());
            // OpenAPI Json schema
            assertEquals(expectedRestEndpoints, generatedFiles.stream()
                    .filter(generatedFile -> generatedFile.category().equals(GeneratedFileType.Category.STATIC_HTTP_RESOURCE) &&
                            generatedFile.type().name().equals(GeneratedFileType.STATIC_HTTP_RESOURCE.name()) &&
                            !generatedFile.relativePath().endsWith(REFLECT_JSON))
                    .count());
        }
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void generateThrowsExceptionWithInvalidModel(KogitoBuildContext.Builder contextBuilder) {
        KogitoBuildContext context = contextBuilder.build();

        KiePMMLModel nullNameMock = buildInvalidMockedModel(null);
        assertThrows(IllegalStateException.class, buildMockedGenerateExecutable(context, nullNameMock));

        KiePMMLModel emptyNameMock = buildInvalidMockedModel(EMPTY);
        assertThrows(IllegalStateException.class, buildMockedGenerateExecutable(context, emptyNameMock));

        KiePMMLModel invalidClassMock = buildInvalidMockedModel(MOCK);
        assertThrows(IllegalStateException.class, buildMockedGenerateExecutable(context, invalidClassMock));
    }

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void generateThrowsExceptionWithInvalidNestedModel(KogitoBuildContext.Builder contextBuilder) {
        KogitoBuildContext context = contextBuilder.build();

        KiePMMLModel nullNameMock = buildMockedModelWithInvalidNestedMockedModel(null);
        assertThrows(IllegalStateException.class, buildMockedGenerateExecutable(context, nullNameMock));

        KiePMMLModel emptyNameMock = buildMockedModelWithInvalidNestedMockedModel(EMPTY);
        assertThrows(IllegalStateException.class, buildMockedGenerateExecutable(context, emptyNameMock));

        KiePMMLModel invalidClassMock = buildMockedModelWithInvalidNestedMockedModel(NESTED_MOCK);
        assertThrows(IllegalStateException.class, buildMockedGenerateExecutable(context, invalidClassMock));
    }

    private static KiePMMLModel buildInvalidMockedModel(String name) {
        KiePMMLModel mock = mock(KiePMMLModel.class);
        when(mock.getName()).thenReturn(name);
        return mock;
    }

    private static KiePMMLModel buildMockedModelWithInvalidNestedMockedModel(String name) {
        KiePMMLModel mock = mock(KiePMMLModel.class, withSettings().extraInterfaces(HasSourcesMap.class, HasNestedModels.class));
        when(mock.getName()).thenReturn(MOCK);

        HasSourcesMap smMock = (HasSourcesMap) mock;
        when(smMock.getSourcesMap()).thenReturn(Collections.emptyMap());

        List<KiePMMLModel> nestedModelsMock = Collections.singletonList(buildInvalidMockedModel(name));
        HasNestedModels nmMock = (HasNestedModels) mock;
        when(nmMock.getNestedModels()).thenReturn(nestedModelsMock);

        return mock;
    }

    private static PMMLResource buildMockedResource(KiePMMLModel mockedModel) {
        PMMLResource mock = mock(PMMLResource.class);
        when(mock.getModelPath()).thenReturn(EMPTY);
        when(mock.getKiePmmlModels()).thenReturn(Collections.singletonList(mockedModel));
        return mock;
    }

    private static Executable buildMockedGenerateExecutable(KogitoBuildContext context, KiePMMLModel mockedModel) {
        return () -> {
            List<PMMLResource> mockedResourceList = Collections.singletonList(buildMockedResource(mockedModel));
            PredictionCodegen codeGenerator = new PredictionCodegen(context, mockedResourceList, new HashSet<>());
            codeGenerator.generate();
        };
    }
}

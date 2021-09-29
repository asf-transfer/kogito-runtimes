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
package org.kie.kogito.codegen.process.persistence;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.kogito.codegen.api.AddonsConfig;
import org.kie.kogito.codegen.api.GeneratedFile;
import org.kie.kogito.codegen.api.context.KogitoBuildContext;
import org.kie.kogito.codegen.data.GeneratedPOJO;
import org.kie.kogito.codegen.process.persistence.proto.ReflectionProtoGenerator;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import static com.github.javaparser.StaticJavaParser.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.kie.kogito.codegen.process.persistence.PersistenceGenerator.JDBC_PERSISTENCE_TYPE;
import static org.kie.kogito.codegen.process.persistence.PersistenceGenerator.KOGITO_PERSISTENCE_TYPE;

class JDBCPersistenceGeneratorTest extends AbstractPersistenceGeneratorTest {

    @ParameterizedTest
    @MethodSource("org.kie.kogito.codegen.api.utils.KogitoContextTestUtils#contextBuilders")
    void test(KogitoBuildContext.Builder contextBuilder) {
        KogitoBuildContext context = contextBuilder
                .withApplicationProperties(new File(TEST_RESOURCES))
                .withPackageName(this.getClass().getPackage().getName())
                .withAddonsConfig(AddonsConfig.builder().withPersistence(true).build())
                .build();

        context.setApplicationProperty(KOGITO_PERSISTENCE_TYPE, persistenceType());

        ReflectionProtoGenerator protoGenerator = ReflectionProtoGenerator.builder().build(Collections.singleton(GeneratedPOJO.class));
        PersistenceGenerator persistenceGenerator = new PersistenceGenerator(
                context,
                protoGenerator);
        assertEquals(persistenceType(), persistenceGenerator.persistenceType());
        Collection<GeneratedFile> generatedFiles = persistenceGenerator.generate();

        if (context.hasDI()) {
            Optional<GeneratedFile> persistenceFactoryImpl = generatedFiles.stream()
                    .filter(gf -> gf.relativePath().equals("org/kie/kogito/persistence/KogitoProcessInstancesFactoryImpl.java"))
                    .findFirst();
            List<GeneratedFile> marshallerFiles = generatedFiles.stream().filter(gf -> gf.relativePath().endsWith("MessageMarshaller.java")).collect(Collectors.toList());

            String expectedMarshaller = "PersonMessageMarshaller";
            assertThat(persistenceFactoryImpl).isNotEmpty();
            assertThat(marshallerFiles.size()).isEqualTo(1);
            assertThat(marshallerFiles.get(0).relativePath()).endsWith(expectedMarshaller + ".java");

            final CompilationUnit compilationUnit = parse(new ByteArrayInputStream(persistenceFactoryImpl.get().contents()));

            assertThat(compilationUnit.findFirst(ClassOrInterfaceDeclaration.class))
                    .isNotEmpty();
        }
    }

    @Override
    protected String persistenceType() {
        return JDBC_PERSISTENCE_TYPE;
    }
}

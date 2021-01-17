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

package org.kie.kogito.quarkus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;
import org.kie.kogito.codegen.process.persistence.proto.Proto;
import org.kie.kogito.quarkus.deployment.JandexProtoGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JandexProtoGeneratorTest {

    @Test
    void testGenerate() {
        List<ClassInfo> dataClasses = new ArrayList<>();

        DotName enumName = DotName.createComponentized(DotName.createComponentized(DotName.createComponentized(null, "com"), "acme"), "ExampleEnum");
        ClassInfo enumClassInfo = ClassInfo.create(enumName, DotName.createSimple(Enum.class.getName()), (short) 0, new DotName[0], new HashMap<>(), false);
        dataClasses.add(enumClassInfo);

        DotName objectName = DotName.createComponentized(DotName.createComponentized(DotName.createComponentized(null, "com"), "acme"), "ExampleObject");
        ClassInfo objectClassName = ClassInfo.create(objectName, DotName.createSimple(Object.class.getName()), (short) 0, new DotName[0], new HashMap<>(), false);
        dataClasses.add(objectClassName);

        JandexProtoGenerator generator = JandexProtoGenerator.builder(null, null, null)
                .buildWithDataClasses(dataClasses);
        Proto proto = generator.generate("com.acme");
        assertEquals(1, proto.getEnums().size());
        assertEquals(enumName.local(), proto.getEnums().get(0).getName());
        assertEquals(1, proto.getMessages().size());
        assertEquals(objectName.local(), proto.getMessages().get(0).getName());
    }

    @Test
    void testGenerateComments() {
        List<ClassInfo> dataModel = new ArrayList<>();

        DotName enumName = DotName.createComponentized(DotName.createComponentized(DotName.createComponentized(null, "com"), "acme"), "ExampleEnum");
        ClassInfo enumClassInfo = ClassInfo.create(enumName, DotName.createSimple(Enum.class.getName()), (short) 0, new DotName[0], new HashMap<>(), false);
        dataModel.add(enumClassInfo);

        DotName objectName = DotName.createComponentized(DotName.createComponentized(DotName.createComponentized(null, "com"), "acme"), "ExampleObject");
        ClassInfo objectClassName = ClassInfo.create(objectName, DotName.createSimple(Object.class.getName()), (short) 0, new DotName[0], new HashMap<>(), false);
        dataModel.add(objectClassName);

        JandexProtoGenerator generator = JandexProtoGenerator.builder(null, null, null)
                .buildWithDataClasses(dataModel);
        Proto enumProto = generator.generate("message comment", "field comment", "com.acme", enumClassInfo);
        assertEquals(1, enumProto.getEnums().size());
        assertEquals(enumName.local(), enumProto.getEnums().get(0).getName());

        Proto objectProto = generator.generate("message comment", "field comment", "com.acme", objectClassName);
        assertEquals(1, objectProto.getMessages().size());
        assertEquals(objectName.local(), objectProto.getMessages().get(0).getName());
    }

    @Test
    void persistenceClassParams() throws IOException {
        Indexer indexer = new Indexer();
        JandexProtoGenerator noPersistenceClassGenerator = JandexProtoGenerator.builder(null, null, null)
                .withPersistenceClass(null)
                .buildWithDataClasses(null);
        assertTrue(noPersistenceClassGenerator.getPersistenceClassParams().isEmpty());

        ClassInfo emptyConstructor = indexer.index(this.getClass().getClassLoader()
                .getResourceAsStream(toPath(EmptyConstructor.class)));
        JandexProtoGenerator emptyGenerator = JandexProtoGenerator.builder(null, null, null)
                .withPersistenceClass(emptyConstructor)
                .buildWithDataClasses(null);

        assertTrue(emptyGenerator.getPersistenceClassParams().isEmpty());

        ClassInfo notEmptyConstructor = indexer.index(this.getClass().getClassLoader()
                .getResourceAsStream(toPath(NotEmptyConstructor.class)));
        JandexProtoGenerator notEmptyGenerator = JandexProtoGenerator.builder(null, null, null)
                .withPersistenceClass(notEmptyConstructor)
                .buildWithDataClasses(null);

        Collection<String> notEmptyClassParams = notEmptyGenerator.getPersistenceClassParams();
        assertFalse(notEmptyClassParams.isEmpty());
        assertEquals(2, notEmptyClassParams.size());
        assertEquals(Arrays.asList(String.class.getCanonicalName(), int.class.getCanonicalName()), notEmptyClassParams);
    }

    private static String toPath(Class<?> clazz) {
        return clazz.getCanonicalName().replace('.', '/') + ".class";
    }
}

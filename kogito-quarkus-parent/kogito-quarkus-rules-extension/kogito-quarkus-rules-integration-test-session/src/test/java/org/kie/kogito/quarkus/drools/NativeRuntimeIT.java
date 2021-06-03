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
package org.kie.kogito.quarkus.drools;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.kogito.legacy.rules.KieRuntimeBuilder;
import org.kie.kogito.legacy.rules.KieSession;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class NativeRuntimeIT {

    @Inject
    KieRuntimeBuilder runtimeBuilder;

    @Test
    public void testRuleEvaluation() {
        KieSession ksession = runtimeBuilder.newKieSession();

        Result result = new Result();
        ksession.insert(result);
        ksession.insert(new Person("Mark", 17));
        ksession.fireAllRules();

        assertEquals("Mark can NOT drink", result.toString());
    }
}

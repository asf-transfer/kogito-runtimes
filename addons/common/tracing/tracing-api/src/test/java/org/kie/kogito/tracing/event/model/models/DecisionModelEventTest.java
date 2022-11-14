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
package org.kie.kogito.tracing.event.model.models;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.kie.kogito.KogitoGAV;
import org.kie.kogito.decision.DecisionModelMetadata;
import org.kie.kogito.tracing.event.TracingTestUtils;
import org.kie.kogito.tracing.event.model.ModelEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionModelEventTest {

    @Test
    public void testDeserialization() throws JsonProcessingException {
        String toRead = TracingTestUtils.readResourceAsString("/decisionmodelevent.json");
        ModelEvent retrieved = new ObjectMapper().readValue(toRead, ModelEvent.class);
        assertThat(retrieved).isInstanceOf(DecisionModelEvent.class);
    }

    @Test
    public void testSerialization() throws JsonProcessingException {
        ModelEvent modelEvent = getDecisionModelEvent(new Random().nextInt(5));
        String retrieved = new ObjectMapper().writeValueAsString(modelEvent);
        assertThat(retrieved).isNotNull();
    }

    private ModelEvent getDecisionModelEvent(int id) {
        return new DecisionModelEvent(getKogitoGAV(),
                "name-" + id,
                "namespace-" + id,
                getDecisionModelMetadata(id),
                "definition-" + id);
    }

    private KogitoGAV getKogitoGAV() {
        return new KogitoGAV("groupId", "artifactId", "version");
    }

    private DecisionModelMetadata getDecisionModelMetadata(int id) {
        return new DecisionModelMetadata("specVersion-" + id);
    }
}

/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.tracing.decision;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.cloudevents.json.Json;
import io.cloudevents.v1.CloudEventImpl;
import org.junit.jupiter.api.Test;
import org.kie.dmn.feel.util.Pair;
import org.kie.kogito.tracing.decision.event.evaluate.EvaluateEvent;
import org.kie.kogito.tracing.decision.event.trace.TraceEvent;
import org.kie.kogito.tracing.decision.mock.MockDefaultAggregator;
import org.kie.kogito.tracing.decision.terminationdetector.BoundariesTerminationDetector;
import org.kie.kogito.tracing.decision.terminationdetector.CounterTerminationDetector;
import org.kie.kogito.tracing.decision.terminationdetector.TerminationDetector;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kie.kogito.tracing.decision.DecisionTestUtils.EVALUATE_ALL_EXECUTION_ID;
import static org.kie.kogito.tracing.decision.DecisionTestUtils.EVALUATE_ALL_JSON_RESOURCE;
import static org.kie.kogito.tracing.decision.DecisionTestUtils.EVALUATE_DECISION_SERVICE_EXECUTION_ID;
import static org.kie.kogito.tracing.decision.DecisionTestUtils.EVALUATE_DECISION_SERVICE_JSON_RESOURCE;
import static org.kie.kogito.tracing.decision.DecisionTestUtils.readEvaluateEventsFromJsonResource;
import static org.kie.kogito.tracing.decision.mock.MockUtils.mockedModel;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DecisionTracingCollectorTest {

//    private static final String TEST_EXECUTION_ID_1 = "c91da8ec-05f7-4dbd-adf4-c7aa88f7888b";
//    private static final String EVALUATE_DECISION_SERVICE_EXECUTION_ID = "550e2947-0952-4225-81a0-ea6e1064efd2";

    @Test
    public void test_Collector_InterleavedEvaluations_BoundariesDetector_Working() {
        testInterleavedEvaluations(BoundariesTerminationDetector::new);
    }

    @Test
    public void test_Collector_InterleavedEvaluations_CounterDetector_Working() {
        testInterleavedEvaluations(CounterTerminationDetector::new);
    }

    private void testInterleavedEvaluations(Supplier<TerminationDetector> terminationDetectorSupplier) {
        MockDefaultAggregator aggregator = new MockDefaultAggregator();
        Consumer<String> payloadConsumer = mock(Consumer.class);

        DecisionTracingCollector collector = new DecisionTracingCollector(
                aggregator,
                payloadConsumer,
                (namespace, name) -> mockedModel(),
                terminationDetectorSupplier
        );

        List<EvaluateEvent> evaluateAllEvents = readEvaluateEventsFromJsonResource(EVALUATE_ALL_JSON_RESOURCE);
        List<EvaluateEvent> evaluateDecisionServiceEvents = readEvaluateEventsFromJsonResource(EVALUATE_DECISION_SERVICE_JSON_RESOURCE);

        for (int i = 0; i < Math.max(evaluateAllEvents.size(), evaluateDecisionServiceEvents.size()); i++) {
            if (i < evaluateAllEvents.size()) {
                collector.addEvent(evaluateAllEvents.get(i));
            }
            if (i < evaluateDecisionServiceEvents.size()) {
                collector.addEvent(evaluateDecisionServiceEvents.get(i));
            }
        }

        Map<String, Pair<List<EvaluateEvent>, CloudEventImpl<TraceEvent>>> aggregatorCalls = aggregator.getCalls();
        assertEquals(2, aggregatorCalls.size());
        assertTrue(aggregatorCalls.containsKey(EVALUATE_ALL_EXECUTION_ID));
        assertEquals(evaluateAllEvents.size(), aggregatorCalls.get(EVALUATE_ALL_EXECUTION_ID).getLeft().size());
        assertTrue(aggregatorCalls.containsKey(EVALUATE_DECISION_SERVICE_EXECUTION_ID));
        assertEquals(evaluateDecisionServiceEvents.size(), aggregatorCalls.get(EVALUATE_DECISION_SERVICE_EXECUTION_ID).getLeft().size());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(payloadConsumer, times(2)).accept(payloadCaptor.capture());

        int evaluateAllIndex = evaluateAllEvents.size() > evaluateDecisionServiceEvents.size() ? 1 : 0;
        int evaluateDecisionServiceIndex = evaluateAllIndex == 1 ? 0 : 1;

        List<String> payloads = payloadCaptor.getAllValues();
        assertEquals(Json.encode(aggregatorCalls.get(EVALUATE_ALL_EXECUTION_ID).getRight()), payloads.get(evaluateAllIndex));
        assertEquals(Json.encode(aggregatorCalls.get(EVALUATE_DECISION_SERVICE_EXECUTION_ID).getRight()), payloads.get(evaluateDecisionServiceIndex));
    }

}

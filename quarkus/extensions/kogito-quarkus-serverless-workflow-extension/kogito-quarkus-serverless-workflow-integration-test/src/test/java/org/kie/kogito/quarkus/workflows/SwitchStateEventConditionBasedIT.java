/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
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

package org.kie.kogito.quarkus.workflows;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.jackson.JsonFormat;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;
import io.restassured.path.json.JsonPath;
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.kie.kogito.quarkus.workflows.WorkflowTestUtils.assertProcessInstanceHasFinished;
import static org.kie.kogito.quarkus.workflows.WorkflowTestUtils.newProcessInstanceAndGetId;

@QuarkusIntegrationTest
@QuarkusTestResource(value = KafkaCompanionResource.class)
class SwitchStateEventConditionBasedIT extends AbstractSwitchStateIT {

    private static final String SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION_URL = "/switch_state_event_condition_timeouts_transition";
    private static final String SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION_URL_GET_BY_ID_URL = SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION_URL + "/{id}";

    private static final String SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION2_URL = "/switch_state_event_condition_timeouts_transition2";
    private static final String SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION2_URL_GET_BY_ID_URL = SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION2_URL + "/{id}";

    private static final String SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_END_URL = "/switch_state_event_condition_timeouts_end";
    private static final String SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_END_GET_BY_ID_URL = SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_END_URL + "/{id}";

    private static final String VISA_APPROVED_EVENT_TOPIC = "visa_approved_topic";
    private static final String VISA_APPROVED_EVENT_TYPE = "visa_approved_in";
    private static final String VISA_DENIED_EVENT_TOPIC = "visa_denied_topic";
    private static final String VISA_DENIED_EVENT_TYPE = "visa_denied_in";

    private static final String EVENT_DECISION_PATH = "data.decision";
    private static final String EVENT_PROCESS_INSTANCE_ID_PATH = "kogitoprocinstanceid";
    private static final String EVENT_TYPE_PATH = "type";

    private static final String PROCESS_RESULT_EVENT_TYPE = "process_result_event";

    private static final String KOGITO_OUTGOING_STREAM_TOPIC = "kogito-sw-out-events";

    private static final String EMPTY_WORKFLOW_DATA = "{\"workflowdata\" : \"\"}";

    @InjectKafkaCompanion
    KafkaCompanion kafkaCompanion;

    ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(JsonFormat.getCloudEventJacksonModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @AfterEach
    void cleanUp() {
        kafkaCompanion.close();
    }

    @Test
    void switchStateEventConditionTimeoutsTransitionApproved() throws Exception {
        switchStateEventConditionTimeoutsTransitionBasedWithEvent(SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION_URL,
                SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION_URL_GET_BY_ID_URL,
                VISA_APPROVED_EVENT_TYPE,
                VISA_APPROVED_EVENT_TOPIC,
                DECISION_APPROVED);
    }

    @Test
    void switchStateEventConditionTimeoutsTransitionDenied() throws Exception {
        switchStateEventConditionTimeoutsTransitionBasedWithEvent(SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION_URL,
                SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION_URL_GET_BY_ID_URL,
                VISA_DENIED_EVENT_TYPE,
                VISA_DENIED_EVENT_TOPIC,
                DECISION_DENIED);
    }

    @Test
    void switchStateEventConditionTimeoutsTransitionTimeoutsExceeded() throws Exception {
        switchStateEventConditionTimeoutsTransitionBasedWithTimeoutsExceeded(SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION_URL,
                SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION_URL_GET_BY_ID_URL,
                DECISION_NO_DECISION);
    }

    @Test
    void switchStateEventConditionTimeoutsTransition2Approved() throws Exception {
        switchStateEventConditionTimeoutsTransitionBasedWithEvent(SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION2_URL,
                SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION2_URL_GET_BY_ID_URL,
                VISA_APPROVED_EVENT_TYPE,
                VISA_APPROVED_EVENT_TOPIC,
                DECISION_APPROVED);
    }

    @Test
    void switchStateEventConditionTimeoutsTransition2Denied() throws Exception {
        switchStateEventConditionTimeoutsTransitionBasedWithEvent(SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION2_URL,
                SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION2_URL_GET_BY_ID_URL,
                VISA_DENIED_EVENT_TYPE,
                VISA_DENIED_EVENT_TOPIC,
                DECISION_DENIED);
    }

    @Test
    void switchStateEventConditionTimeoutsTransition2TimeoutsExceeded() throws Exception {
        switchStateEventConditionTimeoutsTransitionBasedWithTimeoutsExceeded(SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION2_URL,
                SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION2_URL_GET_BY_ID_URL,
                DECISION_DENIED);
    }

    @Test
    void switchStateEventConditionTimeoutsEndTApproved() throws Exception {
        switchStateEventConditionTimeoutsTransitionBasedWithEvent(SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_END_URL,
                SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_END_GET_BY_ID_URL,
                VISA_APPROVED_EVENT_TYPE,
                VISA_APPROVED_EVENT_TOPIC,
                DECISION_APPROVED);
    }

    @Test
    void switchStateEventConditionTimeoutsEndDenied() throws Exception {
        switchStateEventConditionTimeoutsTransitionBasedWithEvent(SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_END_URL,
                SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_END_GET_BY_ID_URL,
                VISA_DENIED_EVENT_TYPE,
                VISA_DENIED_EVENT_TOPIC,
                DECISION_DENIED);
    }

    @Test
    void switchStateEventConditionTimeoutsEndTimeoutsExceeded() throws Exception {
        // Start a new process instance.
        String processInstanceId = newProcessInstanceAndGetId(SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_END_URL, EMPTY_WORKFLOW_DATA);
        // Give enough time for the timeout to exceed.
        assertProcessInstanceHasFinished(SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_END_GET_BY_ID_URL, processInstanceId, 1, 180);
        // When the process has finished the default case event must arrive.
        JsonPath result = waitForEvent(KOGITO_OUTGOING_STREAM_TOPIC, 50);
        assertThat(result.getString("data")).isEmpty();
    }

    /**
     * Executes the happy path for the SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION_URL and the
     * SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION2_URL processes.
     */
    private void switchStateEventConditionTimeoutsTransitionBasedWithEvent(String processUrl,
            String processGetByIdUrl,
            String eventTypeToSend,
            String eventTopicToSend,
            String expectedDecision) throws Exception {
        // Start a new process instance.
        String processInstanceId = newProcessInstanceAndGetId(processUrl, EMPTY_WORKFLOW_DATA);

        // Send the event to activate the switch state.
        String response = objectMapper.writeValueAsString(CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create(""))
                .withType(eventTypeToSend)
                .withTime(OffsetDateTime.now())
                .withExtension("kogitoprocrefid", processInstanceId)
                .withData(JsonCloudEventData.wrap(objectMapper.createObjectNode()))
                .build());
        kafkaCompanion.produce(String.class).fromRecords(new ProducerRecord<>(eventTopicToSend, response)).awaitCompletion();
        // Give some time for the event to be processed and the process to finish.
        assertProcessInstanceHasFinished(processGetByIdUrl, processInstanceId, 1, 180);

        // Give some time to consume the event and very the expected decision was made.
        JsonPath result = waitForEvent(KOGITO_OUTGOING_STREAM_TOPIC, 50);
        assertDecisionEvent(result, processInstanceId, PROCESS_RESULT_EVENT_TYPE, expectedDecision);
    }

    /**
     * Executes timeout exceeded path for the SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION_URL and the
     * SWITCH_STATE_EVENT_CONDITION_TIMEOUTS_TRANSITION2_URL processes.
     */
    private void switchStateEventConditionTimeoutsTransitionBasedWithTimeoutsExceeded(String processUrl,
            String processGetByIdUrl,
            String expectedDecision) throws Exception {
        // Start a new process instance.
        String processInstanceId = newProcessInstanceAndGetId(processUrl, EMPTY_WORKFLOW_DATA);
        // Give enough time for the timeout to exceed.
        assertProcessInstanceHasFinished(processGetByIdUrl, processInstanceId, 1, 180);
        // When the process has finished the default case event must arrive.
        JsonPath result = waitForEvent(KOGITO_OUTGOING_STREAM_TOPIC, 50);
        assertDecisionEvent(result, processInstanceId, PROCESS_RESULT_EVENT_TYPE, expectedDecision);
    }

    protected JsonPath waitForEvent(String topic, long seconds) {
        ConsumerTask<String, String> event_consumer = kafkaCompanion.consumeStrings()
                .withGroupId("kogito-quarkus-serverless-workflow-integration-test")
                .withAutoCommit()
                .fromTopics(topic, 1);
        event_consumer.awaitCompletion(Duration.ofSeconds(seconds));
        assertEquals(1, event_consumer.count());
        return new JsonPath(event_consumer.getLastRecord().value());
    }

    protected static void assertDecisionEvent(JsonPath cloudEventJsonPath,
            String expectedProcessInstanceId,
            String expectedEventType,
            String expectedDecision) {
        assertThat(cloudEventJsonPath.getString(EVENT_PROCESS_INSTANCE_ID_PATH)).isEqualTo(expectedProcessInstanceId);
        assertThat(cloudEventJsonPath.getString(EVENT_TYPE_PATH)).isEqualTo(expectedEventType);
        assertThat(cloudEventJsonPath.getString(EVENT_DECISION_PATH)).isEqualTo(expectedDecision);
    }
}

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
import java.util.Collections;
import java.util.UUID;

import org.kie.kogito.event.CloudEventMarshaller;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

class AssuredTestUtils {

    private AssuredTestUtils() {
    }

    static String startProcess(String flowName) {
        String id = startProcessNoCheck(flowName);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/" + flowName + "/{id}", id)
                .then()
                .statusCode(200);
        return id;
    }

    static String startProcessNoCheck(String flowName) {
        return given()
                .contentType(ContentType.JSON)
                .when()
                .body(Collections.singletonMap("workflowdata", Collections.emptyMap()))
                .post("/" + flowName)
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    static void waitForFinish(String flowName, String id, Duration duration) {
        await("dead").atMost(duration)
                .with().pollInterval(1, SECONDS)
                .untilAsserted(() -> given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .get("/" + flowName + "/{id}", id)
                        .then()
                        .statusCode(404));
    }

    static CloudEvent buildCloudEvent(String id, String type, CloudEventMarshaller<byte[]> marshaller) {
        return CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create(""))
                .withType(type)
                .withTime(OffsetDateTime.now())
                .withExtension("kogitoprocrefid", id)
                .withData(marshaller.cloudEventDataFactory().apply(Collections.singletonMap(type, "This has been injected by the event")))
                .build();
    }

}

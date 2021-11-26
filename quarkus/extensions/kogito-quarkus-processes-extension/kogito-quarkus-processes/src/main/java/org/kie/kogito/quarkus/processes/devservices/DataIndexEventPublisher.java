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

package org.kie.kogito.quarkus.processes.devservices;

import java.util.Collection;

import org.kie.kogito.event.DataEvent;
import org.kie.kogito.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;

public class DataIndexEventPublisher implements EventPublisher {

    public static final String KOGITO_DATA_INDEX = "kogito.data-index.url";
    private static final Logger LOGGER = LoggerFactory.getLogger(DataIndexEventPublisher.class);

    private final String dataIndexUrl;
    private final WebClient webClient;

    public DataIndexEventPublisher(String dataIndexUrl, WebClient webClient) {
        this.dataIndexUrl = dataIndexUrl;
        this.webClient = webClient;
    }

    @Override
    public void publish(DataEvent<?> event) {
        LOGGER.debug("Sending event to data index: {}", event);
        switch (event.getType()) {
            case "ProcessInstanceEvent":
                webClient.postAbs(dataIndexUrl + "/processes")
                        .expect(ResponsePredicate.SC_ACCEPTED)
                        .sendJson(event, result -> {
                            if (result.failed()) {
                                LOGGER.error("Failed to send message to Data Index", result.cause());
                            } else {
                                LOGGER.debug("Event published to Data Index");
                            }
                        });
                break;
            case "UserTaskInstanceEvent":
                webClient.postAbs(dataIndexUrl + "/tasks")
                        .expect(ResponsePredicate.SC_ACCEPTED)
                        .sendJson(event, result -> {
                            if (result.failed()) {
                                LOGGER.error("Failed to send message to Data Index", result.cause());
                            } else {
                                LOGGER.debug("Event published to Data Index");
                            }
                        });
                break;
            default:
                LOGGER.debug("Unknown type of event '{}', ignoring for this publisher", event.getType());
        }
    }

    @Override
    public void publish(Collection<DataEvent<?>> events) {
        events.forEach(this::publish);
    }

}

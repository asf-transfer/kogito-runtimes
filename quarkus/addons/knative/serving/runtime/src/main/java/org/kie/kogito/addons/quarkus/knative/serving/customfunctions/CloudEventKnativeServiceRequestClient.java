/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.addons.quarkus.knative.serving.customfunctions;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import io.cloudevents.SpecVersion;
import io.cloudevents.core.v1.CloudEventV1;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

import static java.util.function.Predicate.not;

@ApplicationScoped
class CloudEventKnativeServiceRequestClient extends KnativeServiceRequestClient {

    private static final Logger logger = LoggerFactory.getLogger(CloudEventKnativeServiceRequestClient.class);

    private final WebClient webClient;

    private final Duration requestTimeout;

    @Inject
    CloudEventKnativeServiceRequestClient(Vertx vertx, @ConfigProperty(name = REQUEST_TIMEOUT_PROPERTY_NAME) Optional<Long> requestTimeout) {
        this.webClient = WebClient.create(vertx);
        this.requestTimeout = Duration.ofMillis(requestTimeout.orElse(DEFAULT_REQUEST_TIMEOUT_VALUE));
    }

    @Override
    protected JsonNode sendRequest(KnativeServiceAddress serviceAddress, String path, Map<String, Object> cloudEvent) {
        JsonObject body = new JsonObject(cloudEvent);

        validateCloudEvent(body);

        HttpRequest<Buffer> request = webClient.post(serviceAddress.getPort(), serviceAddress.getHost(), path)
                .putHeader("Content-Type", APPLICATION_CLOUDEVENTS_JSON_CHARSET_UTF_8)
                .ssl(serviceAddress.isSsl());

        logger.debug("Sending request with CloudEvent - host: {}, port: {}, path: {}, CloudEvent: {}",
                serviceAddress.getHost(), serviceAddress.getPort(), path, body);

        HttpResponse<Buffer> response = request.sendBuffer(Buffer.buffer(body.encode())).await().atMost(requestTimeout);

        return responseAsJsonObject(response);
    }

    private static void validateCloudEvent(JsonObject cloudEvent) {
        SpecVersion specVersion = SpecVersion.parse(cloudEvent.getString(CloudEventV1.SPECVERSION, "1.0"));

        List<String> missingAttributes = specVersion.getMandatoryAttributes().stream()
                .filter(not(cloudEvent::containsKey))
                .collect(Collectors.toList());

        if (!missingAttributes.isEmpty()) {
            throw new IllegalArgumentException("Invalid CloudEvent. The following mandatory attributes are missing: "
                    + String.join(", ", missingAttributes));
        }
    }

    @PreDestroy
    void preDestroy() {
        webClient.close();
    }
}

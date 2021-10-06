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
package org.kie.kogito.addons.quarkus.k8s;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.mock.EnableKnativeMockClient;
import io.fabric8.knative.serving.v1.Route;
import io.fabric8.knative.serving.v1.RouteBuilder;
import io.fabric8.knative.serving.v1.RouteStatus;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnableKnativeMockClient(crud = true)
public class KnativeRouteEndpointDiscoveryTest {

    static KnativeClient knativeClient;

    @Test
    public void testBaseCase() {
        final KnativeRouteEndpointDiscovery endpointDiscovery = new KnativeRouteEndpointDiscovery();
        endpointDiscovery.setKnativeClient(knativeClient);

        // configure mock
        final RouteStatus status = new RouteStatus();
        status.setUrl("http://192.168.2.32");
        final Route route = new RouteBuilder().withNewMetadata().withName("ksvc1").withNamespace("test").and().withStatus(status).build();
        knativeClient.routes().create(route);

        final Optional<Endpoint> endpoint = endpointDiscovery.findEndpoint("test", "ksvc1");
        assertTrue(endpoint.isPresent());

        try {
            new URL(endpoint.get().getURL());
        } catch (MalformedURLException e) {
            fail("The generated URL " + endpoint.get().getURL() + " is invalid"); //verbose
        }
    }
}

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
package org.kie.kogito.addons.quarkus.k8s.utils;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.kie.kogito.addons.quarkus.k8s.KubeResourceDiscovery;
import org.kie.kogito.addons.quarkus.k8s.parser.KubeURI;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test covers the queryIngressByName method from {@link IngressUtils}
 */
@QuarkusTest
@WithKubernetesTestServer
public class IngressUtilsTest {

    @KubernetesTestServer
    KubernetesServer mockServer;
    KubeResourceDiscovery kubeResourceDiscovery;
    private final String namespace = "serverless-workflow-greeting-quarkus";

    @Test
    public void testIngressNotFound() {
        kubeResourceDiscovery = new KubeResourceDiscovery(mockServer.getClient());

        Ingress ingress = mockServer.getClient()
                .network().v1().ingresses()
                .inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("ingress/ingress-with-ip.yaml")).get();
        mockServer.getClient().resource(ingress).inNamespace(namespace).createOrReplace();

        assertEquals(Optional.empty(),
                kubeResourceDiscovery.query(new KubeURI("kubernetes:networking.k8s.io/v1/ingress/" + namespace + "/invalid")));
    }

    @Test
    public void testIngressWithIP() {
        kubeResourceDiscovery = new KubeResourceDiscovery(mockServer.getClient());
        KubeURI kubeURI = new KubeURI("kubernetes:networking.k8s.io/v1/ingress/" + namespace + "/process-quarkus-ingress");

        Ingress ingress = mockServer.getClient().network().v1().ingresses().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("ingress/ingress-with-ip.yaml")).get();

        mockServer.getClient().resource(ingress).inNamespace(namespace).createOrReplace();

        Optional<String> url = kubeResourceDiscovery.query(kubeURI);
        assertEquals("http://80.80.25.9:80", url.get());
    }

    @Test
    public void testIngressWithTLS() {
        kubeResourceDiscovery = new KubeResourceDiscovery(mockServer.getClient());
        KubeURI kubeURI = new KubeURI("kubernetes:networking.k8s.io/v1/ingress/" + namespace + "/hello-app-ingress-tls");

        Ingress ingress = mockServer.getClient().network().v1().ingresses().inNamespace(namespace)
                .load(this.getClass().getClassLoader().getResourceAsStream("ingress/ingress-with-tls-and-host.yaml")).get();

        mockServer.getClient().resource(ingress).inNamespace(namespace).createOrReplace();

        Optional<String> url = kubeResourceDiscovery.query(kubeURI);
        assertEquals("https://80.80.25.9:443", url.get());
    }
}

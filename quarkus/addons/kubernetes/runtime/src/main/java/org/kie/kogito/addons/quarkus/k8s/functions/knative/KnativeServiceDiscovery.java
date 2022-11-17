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
package org.kie.kogito.addons.quarkus.k8s.functions.knative;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.kogito.addons.quarkus.k8s.KnativeResourceDiscovery;
import org.kie.kogito.addons.quarkus.k8s.ServiceDiscoveryException;
import org.kie.kogito.addons.quarkus.k8s.parser.KubeURI;

@ApplicationScoped
public class KnativeServiceDiscovery {

    private final KnativeResourceDiscovery knativeResourceDiscovery;

    private final String currentNamespace;

    @Inject
    KnativeServiceDiscovery(KnativeResourceDiscovery knativeResourceDiscovery) {
        this.knativeResourceDiscovery = knativeResourceDiscovery;
        this.currentNamespace = knativeResourceDiscovery.getCurrentNamespace();
    }

    public Optional<Server> discover(String serviceName) {
        KnativeService knativeService = new KnativeService(serviceName);

        KubeURI kubeURI = new KubeURI(String.format("knative:serving.knative.dev/v1/Service/%s/%s",
                knativeService.getNamespace().orElse(currentNamespace),
                knativeService.getName()));

        try {
            return knativeResourceDiscovery.queryServiceByName(kubeURI)
                    .map(url -> new Server(url.getProtocol(), url.getHost(), url.getPort() == -1 ? 80 : url.getPort()));
        } catch (RuntimeException e) {
            throw new ServiceDiscoveryException("An exception occurred while discovering the Knative service with name: " + serviceName, e);
        }
    }
}

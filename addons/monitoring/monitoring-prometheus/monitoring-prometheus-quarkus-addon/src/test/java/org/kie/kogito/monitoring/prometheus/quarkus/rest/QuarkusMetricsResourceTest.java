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
package org.kie.kogito.monitoring.prometheus.quarkus.rest;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class QuarkusMetricsResourceTest {

    @Test
    public void getMetrics() {
        QuarkusMetricsResource resource = new MockQuarkusMetricsResource("metric");

        Response metrics = resource.getMetrics();
        assertThat(metrics.getStatus()).isEqualTo(200);
        assertThat(metrics.getEntity()).isEqualTo("metric");
    }

    static class MockQuarkusMetricsResource extends QuarkusMetricsResource {

        final String expectedValue;

        MockQuarkusMetricsResource(String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public String scrape() {
            return expectedValue;
        }
    }

}

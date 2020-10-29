/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.monitoring.system.metrics.dmnhandlers;

import java.util.ArrayList;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Tag;
import org.kie.kogito.monitoring.MonitoringRegistry;

public interface TypeHandlerWithSummary<T> extends TypeHandler<T> {

    default DistributionSummary getDefaultSummary(String dmnType, String decision, String endpoint) {
        ArrayList<Tag> tags = new ArrayList<Tag>() {
            {
                add(Tag.of("decision", decision));
                add(Tag.of("endpoint", endpoint));
            }
        };
        DistributionSummary summary = DistributionSummary
                .builder(dmnType.replace(" ", "_") + DecisionConstants.DECISIONS_NAME_SUFFIX)
                .description(DecisionConstants.DECISIONS_HELP)
                .tags(tags)
                .register(MonitoringRegistry.getCompositeMeterRegistry());
        return summary;
    }
}

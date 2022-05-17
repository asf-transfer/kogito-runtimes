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
package org.kie.kogito.quarkus.conf;

import javax.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;
import org.kie.kogito.KogitoGAV;

import io.quarkus.runtime.annotations.Recorder;

@Singleton
@Recorder
public class ConfigBeanRecorder {

    private static KogitoGAV gav;

    public void setRuntimeGav(String groupId, String artifactId, String version) {
        this.gav = new KogitoGAV(groupId, artifactId, version);
    }

    public KogitoGAV getGav() {
        return gav;
    }

    public String getServiceUrl() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.kogito.service.url", String.class).orElse("");
    }

    public boolean isUseCloudEvents() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.kogito.messaging.as-cloudevents", Boolean.class).orElse(true);
    }

    public boolean isFailOnEmptyBean() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.kogito.jackson.fail-on-empty-bean", Boolean.class).orElse(false);
    }
}

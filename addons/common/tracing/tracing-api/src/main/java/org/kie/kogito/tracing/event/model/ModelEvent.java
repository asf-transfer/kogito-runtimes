/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.tracing.event.model;

import org.kie.kogito.KogitoGAV;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstract <code>ModelEvent</code> to be extended by actual model-specific implementations
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class") // Needed for inheritance
public abstract class ModelEvent {

    private final KogitoGAV gav;

    private final String name;

    @JsonCreator
    protected ModelEvent(final @JsonProperty("gav") KogitoGAV gav,
            final @JsonProperty("name") String name) {
        this.gav = gav;
        this.name = name;
    }

    public KogitoGAV getGav() {
        return gav;
    }

    public String getName() {
        return name;
    }

}

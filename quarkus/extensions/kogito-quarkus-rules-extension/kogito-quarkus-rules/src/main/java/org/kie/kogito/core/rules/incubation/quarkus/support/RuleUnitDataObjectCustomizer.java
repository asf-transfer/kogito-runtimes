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

package org.kie.kogito.core.rules.incubation.quarkus.support;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.kogito.incubation.common.objectmapper.quarkus.QuarkusInternalObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.Startup;

/**
 * Internal use only. Wraps and configures a custom ObjectMapper for DataSources
 */
@ApplicationScoped
@Startup
class RuleUnitDataObjectCustomizer {

    @Inject
    QuarkusInternalObjectMapper mapper;

    @PostConstruct
    public void customize() {
        mapper.getObjectMapper().registerModule(new RuleUnitDataJacksonModule());
    }
}

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
package org.kie.kogito.tracing.decision.quarkus.deployment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;

import static org.kie.kogito.tracing.decision.quarkus.deployment.DevServicesConfig.Property.HibernateOrmDatabaseGeneration;

/**
 * Configures DevServices properties.
 */
public class DevServicesSetupEnvironmentProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevServicesSetupEnvironmentProcessor.class);
    private static final String HIBERNATE_ORM_DATABASE_GENERATION_STRATEGY = "drop-and-create";

    @SuppressWarnings("unused")
    @BuildStep(onlyIf = IsDevelopment.class)
    public void extractDevServicesDefaultDataSourceConfiguration(final BuildProducer<DevServicesConfigResultBuildItem> devServicesConfigResultBuilder,
            final BuildProducer<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItemBuildProducer) {

        LOGGER.info("Checking DevService configuration...");
        if (!ConfigUtils.isPropertyPresent(HibernateOrmDatabaseGeneration.getPropertyName())) {
            LOGGER.info(String.format("Setting %s=%s to initialize DevServices managed database",
                    HibernateOrmDatabaseGeneration.getPropertyName(),
                    HIBERNATE_ORM_DATABASE_GENERATION_STRATEGY));
            devServicesConfigResultBuilder.produce(new DevServicesConfigResultBuildItem(
                    HibernateOrmDatabaseGeneration.getPropertyName(),
                    HIBERNATE_ORM_DATABASE_GENERATION_STRATEGY));
        }

        LOGGER.info("Enabling use of TestContainers 'Shared Network' for all containers started by Quarkus.");
        devServicesSharedNetworkBuildItemBuildProducer.produce(new DevServicesSharedNetworkBuildItem());
    }
}

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
package org.kie.kogito.monitoring.core.springboot;

import org.drools.core.config.DefaultRuleEventListenerConfig;
import org.kie.kogito.KogitoGAV;
import org.kie.kogito.conf.ConfigBean;
import org.kie.kogito.internal.process.event.KogitoProcessEventListener;
import org.kie.kogito.monitoring.MonitoringRegistryManager;
import org.kie.kogito.monitoring.core.common.Constants;
import org.kie.kogito.monitoring.core.common.process.MetricsProcessEventListener;
import org.kie.kogito.monitoring.core.common.rule.RuleMetricsListenerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringbootEventListenerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringbootEventListenerFactory.class);

    ConfigBean configBean;

    MonitoringRegistryManager monitoringRegistryManager;

    @Autowired
    public SpringbootEventListenerFactory(ConfigBean configBean, MonitoringRegistryManager monitoringRegistryManager) {
        this.configBean = configBean;
        this.monitoringRegistryManager = monitoringRegistryManager;
    }

    @Bean
    @ConditionalOnProperty(
            value = Constants.MONITORING_PROCESS_USE_DEFAULT,
            havingValue = "true",
            matchIfMissing = true)
    public KogitoProcessEventListener produceProcessListener() {
        LOGGER.debug("Producing default listener for process monitoring.");
        return new MetricsProcessEventListener("default-process-monitoring-listener",
                configBean.getGav().orElse(KogitoGAV.EMPTY_GAV),
                monitoringRegistryManager.getDefaultMeterRegistry());
    }

    @ConditionalOnProperty(
            value = Constants.MONITORING_RULE_USE_DEFAULT,
            havingValue = "true",
            matchIfMissing = true)
    @Bean
    public DefaultRuleEventListenerConfig produceRuleListener() {
        LOGGER.debug("Producing default listener for rule monitoring.");
        return new RuleMetricsListenerConfig(configBean.getGav().orElse(KogitoGAV.EMPTY_GAV), monitoringRegistryManager.getDefaultMeterRegistry());
    }
}

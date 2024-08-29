/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.codegen.tests;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.junit.jupiter.api.Test;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.kogito.Application;
import org.kie.kogito.Model;
import org.kie.kogito.codegen.AbstractCodegenIT;
import org.kie.kogito.internal.process.event.DefaultKogitoProcessEventListener;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessConfig;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.Processes;
import org.kie.kogito.process.impl.CachedProcessEventListenerConfig;
import org.kie.kogito.process.impl.Sig;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageStartEventIT extends AbstractCodegenIT {

    @Test
    public void testMessageStartEventProcess() throws Exception {

        Application app = generateCodeProcessesOnly("messagestartevent/MessageStartEvent.bpmn2");
        ((CachedProcessEventListenerConfig) app.config().get(ProcessConfig.class).processEventListeners()).register(new DefaultKogitoProcessEventListener() {

            @Override
            public void afterProcessStarted(ProcessStartedEvent event) {
                assertThat(event.getProcessInstance().getState()).isEqualTo(ProcessInstance.STATE_COMPLETED);
                assertThat(((RuleFlowProcessInstance) event.getProcessInstance()).getVariable("customerId")).isEqualTo("CUS-00998877");
            }

        });
        Process<? extends Model> p = app.get(Processes.class).processById("MessageStartEvent");

        assertThat(app).isNotNull();
        p.send(Sig.of("customers", "CUS-00998877"));

    }

    @Test
    public void testMessageStartAndEndEventProcess() throws Exception {

        Application app = generateCodeProcessesOnly("messagestartevent/MessageStartAndEndEvent.bpmn2");
        assertThat(app).isNotNull();
        ((CachedProcessEventListenerConfig) app.config().get(ProcessConfig.class).processEventListeners()).register(new DefaultKogitoProcessEventListener() {

            @Override
            public void afterProcessStarted(ProcessStartedEvent event) {
                assertThat(event.getProcessInstance().getState()).isEqualTo(ProcessInstance.STATE_COMPLETED);
                assertThat(((RuleFlowProcessInstance) event.getProcessInstance()).getVariable("customerId")).isEqualTo("CUS-00998877");
            }

        });

        Process<? extends Model> p = app.get(Processes.class).processById("MessageStartEvent");
        p.send(Sig.of("customers", "CUS-00998877"));

    }

    @Test
    public void testNoneAndMessageStartEventProcess() throws Exception {

        Application app = generateCodeProcessesOnly("messagestartevent/NoneAndMessageStartEvent.bpmn2");
        assertThat(app).isNotNull();
        Mutable<String> mutablePath = new MutableObject<>();
        ((CachedProcessEventListenerConfig) app.config().get(ProcessConfig.class).processEventListeners()).register(new DefaultKogitoProcessEventListener() {

            @Override
            public void afterProcessStarted(ProcessStartedEvent event) {
                assertThat(event.getProcessInstance().getState()).isEqualTo(ProcessInstance.STATE_COMPLETED);
                assertThat(((RuleFlowProcessInstance) event.getProcessInstance()).getVariable("customerId")).isEqualTo("CUS-00998877");
                assertThat(((RuleFlowProcessInstance) event.getProcessInstance()).getVariable("path")).isNotNull().isEqualTo(mutablePath.getValue());
            }

        });

        mutablePath.setValue("none");

        Process<? extends Model> p = app.get(Processes.class).processById("MessageStartEvent");
        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("customerId", "CUS-00998877");
        m.fromMap(parameters);
        // first start it via none start event
        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        mutablePath.setValue("message");
        p.send(Sig.of("customers", "CUS-00998877"));

    }
}

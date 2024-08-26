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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.kie.kogito.Application;
import org.kie.kogito.Model;
import org.kie.kogito.auth.IdentityProvider;
import org.kie.kogito.auth.IdentityProviders;
import org.kie.kogito.auth.SecurityPolicy;
import org.kie.kogito.codegen.AbstractCodegenIT;
import org.kie.kogito.codegen.data.Person;
import org.kie.kogito.internal.process.event.DefaultKogitoProcessEventListener;
import org.kie.kogito.internal.process.event.ProcessWorkItemTransitionEvent;
import org.kie.kogito.internal.process.runtime.KogitoProcessInstance;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.workitem.NotAuthorizedException;
import org.kie.kogito.internal.process.workitem.Policy;
import org.kie.kogito.internal.process.workitem.WorkItemNotFoundException;
import org.kie.kogito.internal.process.workitem.WorkItemTransition;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessConfig;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.Processes;
import org.kie.kogito.process.VariableViolationException;
import org.kie.kogito.process.WorkItem;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.kie.kogito.jbpm.usertask.handler.UserTaskKogitoWorkItemHandler.ACTIVATED;
import static org.kie.kogito.jbpm.usertask.handler.UserTaskKogitoWorkItemHandler.RESERVED;
import static org.kie.kogito.jbpm.usertask.handler.UserTaskKogitoWorkItemHandler.TRANSITION_ACTIVATED_CLAIM;
import static org.kie.kogito.jbpm.usertask.handler.UserTaskKogitoWorkItemHandler.TRANSITION_RESERVED_COMPLETE;
import static org.kie.kogito.jbpm.usertask.handler.UserTaskKogitoWorkItemHandler.TRANSITION_RESERVED_RELEASE;

public class UserTaskIT extends AbstractCodegenIT {

    private Policy securityPolicy = SecurityPolicy.of("john", emptyList());

    @Test
    public void testBasicUserTaskProcess() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();
        final List<String> workItemTransitionEvents = new ArrayList<>();
        app.config().get(ProcessConfig.class).processEventListeners().listeners().add(new DefaultKogitoProcessEventListener() {

            @Override
            public void beforeWorkItemTransition(ProcessWorkItemTransitionEvent event) {
                workItemTransitionEvents.add("BEFORE:: " + event);
            }

            @Override
            public void afterWorkItemTransition(ProcessWorkItemTransitionEvent event) {
                workItemTransitionEvents.add("AFTER:: " + event);
            }
        });

        Process<? extends Model> p = app.get(Processes.class).processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        assertThat(workItems.get(0).getName()).isEqualTo("FirstTask");
        WorkItem wi = workItems.get(0);

        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        WorkItemTransition transition = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        transition = handler.newTransition(TRANSITION_RESERVED_COMPLETE.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        assertThat(workItems.get(0).getName()).isEqualTo("SecondTask");

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        assertThat(workItemTransitionEvents).hasSize(12);
    }

    @Test
    public void testBasicUserTaskProcessPhases() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        WorkItem wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");

        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        WorkItemTransition transition = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        transition = handler.newTransition(TRANSITION_RESERVED_COMPLETE.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("SecondTask");

        handler = getWorkItemHandler(p, wi);
        transition = handler.completeTransition(workItems.get(0).getPhaseStatus(), parameters, securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicUserTaskProcessClaimAndCompletePhases() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        WorkItem wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getResults()).isEmpty();

        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        WorkItemTransition transition = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), Map.of("ACTUAL_OWNER", "john", "test", "value"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");

        assertThat(wi.getResults()).hasSize(1)
                .containsEntry("test", "value");

        handler = getWorkItemHandler(p, wi);
        transition = handler.newTransition(TRANSITION_RESERVED_COMPLETE.id(), wi.getPhaseStatus(), emptyMap(), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("SecondTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        assertThat(wi.getResults()).isEmpty();

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testBasicUserTaskProcessReleaseAndCompletePhases() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        WorkItem wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(ACTIVATED.getName());
        assertThat(wi.getResults()).isEmpty();

        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        WorkItemTransition claim = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(wi.getId(), claim);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        assertThat(wi.getResults()).isEmpty();

        WorkItemTransition release = handler.newTransition(TRANSITION_RESERVED_RELEASE.id(), wi.getPhaseStatus(), emptyMap(), securityPolicy);
        processInstance.transitionWorkItem(wi.getId(), release);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(ACTIVATED.getName());
        assertThat(wi.getResults()).isEmpty();

        claim = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(wi.getId(), claim);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        assertThat(wi.getResults()).isEmpty();
        WorkItemTransition transition = handler.completeTransition(wi.getPhaseStatus(), emptyMap(), securityPolicy);
        processInstance.transitionWorkItem(wi.getId(), transition);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("SecondTask");

        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        assertThat(wi.getResults()).isEmpty();

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testBasicUserTaskProcessClaimAndCompletePhasesWithIdentity() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();
        final List<String> workItemTransitionEvents = new ArrayList<>();
        app.config().get(ProcessConfig.class).processEventListeners().listeners().add(new DefaultKogitoProcessEventListener() {

            @Override
            public void beforeWorkItemTransition(ProcessWorkItemTransitionEvent event) {
                workItemTransitionEvents.add("BEFORE:: " + event);
            }

            @Override
            public void afterWorkItemTransition(ProcessWorkItemTransitionEvent event) {
                workItemTransitionEvents.add("AFTER:: " + event);
            }
        });

        Process<? extends Model> p = app.get(Processes.class).processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        WorkItem wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(ACTIVATED.getName());
        assertThat(wi.getResults()).isEmpty();

        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        WorkItemTransition transition = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), Map.of("ACTUAL_OWNER", "john", "test", "value"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        assertThat(wi.getResults()).hasSize(1)
                .containsEntry("test", "value");

        transition = handler.completeTransition(wi.getPhaseStatus(), emptyMap(), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("SecondTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        assertThat(wi.getResults()).isEmpty();

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);

        assertThat(workItemTransitionEvents).hasSize(12);
    }

    @Test
    public void testBasicUserTaskProcessClaimAndCompleteWrongUser() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        WorkItem wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(ACTIVATED.getName());
        assertThat(wi.getResults()).isEmpty();

        final String wiId = wi.getId();
        IdentityProvider identity = IdentityProviders.of("kelly");

        // if user that is not authorized to work on work item both listing and getting by id should apply it
        List<WorkItem> securedWorkItems = processInstance.workItems(SecurityPolicy.of(identity));
        assertThat(securedWorkItems).isEmpty();

        assertThatExceptionOfType(WorkItemNotFoundException.class).isThrownBy(() -> processInstance.workItem(wiId, SecurityPolicy.of(identity)));

        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        WorkItemTransition claimKelly = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "kelly"), SecurityPolicy.of(identity));
        assertThatExceptionOfType(NotAuthorizedException.class).isThrownBy(() -> processInstance.transitionWorkItem(wiId, claimKelly));

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(ACTIVATED.getName());
        assertThat(wi.getResults()).isEmpty();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        WorkItemTransition claimJohn = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(wiId, claimJohn);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        assertThat(wi.getResults()).isEmpty();
        WorkItemTransition completeJohn = handler.newTransition(TRANSITION_RESERVED_COMPLETE.id(), wi.getPhaseStatus(), emptyMap(), securityPolicy);
        processInstance.transitionWorkItem(wiId, completeJohn);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("SecondTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        assertThat(wi.getResults()).isEmpty();

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testApprovalWithExcludedOwnerViaPhases() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        assertThat(processInstance.status()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);

        IdentityProvider identity = IdentityProviders.of("manager", emptyList());
        SecurityPolicy policy = SecurityPolicy.of(identity);

        processInstance.workItems(policy);

        List<WorkItem> workItems = processInstance.workItems(policy);
        assertThat(workItems).hasSize(1);

        WorkItem wi = workItems.get(0);
        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        WorkItemTransition transition = handler.newTransition(TRANSITION_RESERVED_COMPLETE.id(), wi.getPhaseStatus(), singletonMap("ActorId", "manager"), policy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);
        // actual owner of the first task is excluded owner on the second task so won't find it
        workItems = processInstance.workItems(policy);
        assertThat(workItems).isEmpty();

        identity = IdentityProviders.of("john", singletonList("managers"));
        policy = SecurityPolicy.of(identity);

        workItems = processInstance.workItems(policy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        transition = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), policy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        workItems = processInstance.workItems(policy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        transition = handler.newTransition(TRANSITION_RESERVED_COMPLETE.id(), wi.getPhaseStatus(), emptyMap(), policy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        assertThat(processInstance.status()).isEqualTo(KogitoProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testApprovalWithExcludedOwner() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("approver", "manager");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        assertThat(processInstance.status()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);

        IdentityProvider identity = IdentityProviders.of("manager", emptyList());
        SecurityPolicy policy = SecurityPolicy.of(identity);

        List<WorkItem> workItems = processInstance.workItems(policy);
        assertThat(workItems).hasSize(1);

        processInstance.completeWorkItem(workItems.get(0).getId(), singletonMap("ActorId", "manager"), policy);
        // actual owner of the first task is excluded owner on the second task so won't find it
        workItems = processInstance.workItems(policy);
        assertThat(workItems).isEmpty();

        identity = IdentityProviders.of("john", Collections.singletonList("managers"));
        policy = SecurityPolicy.of(identity);

        workItems = processInstance.workItems(policy);
        assertThat(workItems).hasSize(1);

        assertThat(workItems).hasSize(1);
        WorkItem wi = workItems.get(0);

        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        WorkItemTransition transition = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), policy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        processInstance.completeWorkItem(workItems.get(0).getId(), null, policy);

        assertThat(processInstance.status()).isEqualTo(KogitoProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicUserTaskProcessCancelAndTriggerNode() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        WorkItem wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(ACTIVATED.getName());

        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        WorkItemTransition transition = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        transition = handler.newTransition(TRANSITION_RESERVED_COMPLETE.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("SecondTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());

        String firstSecondTaskNodeInstanceId = wi.getNodeInstanceId();

        processInstance.cancelNodeInstance(wi.getNodeInstanceId());
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        processInstance.triggerNode("UserTask_2");
        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("SecondTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        // since it was triggered again it must have different node instance id
        assertThat(wi.getNodeInstanceId()).isNotEqualTo(firstSecondTaskNodeInstanceId);
        transition = handler.completeTransition(workItems.get(0).getPhaseStatus(), parameters, securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicUserTaskProcessCancelAndRetriggerNode() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        WorkItem wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");

        assertThat(wi.getPhaseStatus()).isEqualTo(ACTIVATED.getName());

        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        WorkItemTransition transition = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        transition = handler.newTransition(TRANSITION_RESERVED_COMPLETE.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("SecondTask");

        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());

        String firstSecondTaskNodeInstanceId = wi.getNodeInstanceId();

        processInstance.retriggerNodeInstance(wi.getNodeInstanceId());
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("SecondTask");

        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        // since it was retriggered it must have different node instance id
        assertThat(wi.getNodeInstanceId()).isNotEqualTo(firstSecondTaskNodeInstanceId);

        transition = handler.completeTransition(workItems.get(0).getPhaseStatus(), parameters, securityPolicy);
        processInstance.transitionWorkItem(wi.getId(), transition);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicUserTaskProcessClaimReleaseClaimAndCompletePhases() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        WorkItem wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(ACTIVATED.getName());
        assertThat(wi.getResults()).isEmpty();

        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        WorkItemTransition transition = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), Map.of("ACTUAL_OWNER", "john", "test", "value"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");

        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        assertThat(wi.getResults()).hasSize(1)
                .containsEntry("test", "value");

        transition = handler.newTransition(TRANSITION_RESERVED_RELEASE.id(), wi.getPhaseStatus(), emptyMap(), securityPolicy);

        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(ACTIVATED.getName());
        assertThat(wi.getResults()).hasSize(0);

        transition = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), Map.of("ACTUAL_OWNER", "john", "test", "value"), securityPolicy);
        processInstance.transitionWorkItem(wi.getId(), transition);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        assertThat(wi.getResults()).hasSize(1)
                .containsEntry("test", "value");

        transition = handler.newTransition(TRANSITION_RESERVED_COMPLETE.id(), wi.getPhaseStatus(), emptyMap(), securityPolicy);
        processInstance.transitionWorkItem(wi.getId(), transition);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("SecondTask");
        assertThat(wi.getPhaseStatus()).isEqualTo(RESERVED.getName());
        assertThat(wi.getResults()).isEmpty();

        processInstance.abort();
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ABORTED);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testApprovalWithReadonlyVariableTags() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval-with-readonly-variable-tags.bpmn2");
        assertThat(app).isNotNull();

        Class<?> resourceClazz = Class.forName("org.acme.travels.ApprovalsModel", true, testClassLoader());
        assertThat(resourceClazz).isNotNull();

        Field approverField = resourceClazz.getDeclaredField("approver");
        assertThat(approverField).isNotNull();
        assertThat(approverField.getType().getCanonicalName()).isEqualTo(String.class.getCanonicalName());

        Process<? extends Model> p = app.get(Processes.class).processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("approver", "john");
        m.fromMap(parameters);

        ProcessInstance processInstance = p.createInstance(m);
        processInstance.start();
        assertThat(processInstance.status()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);

        final Model updates = p.createModel();
        parameters = new HashMap<>();
        parameters.put("approver", "mary");
        updates.fromMap(parameters);
        // updating readonly variable should fail
        assertThatExceptionOfType(VariableViolationException.class).isThrownBy(() -> processInstance.updateVariables(updates));

        processInstance.abort();

        assertThat(processInstance.status()).isEqualTo(KogitoProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testApprovalWithInternalVariableTags() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval-with-internal-variable-tags.bpmn2");
        assertThat(app).isNotNull();

        Class<?> resourceClazz = Class.forName("org.acme.travels.ApprovalsModel", true, testClassLoader());
        assertThat(resourceClazz).isNotNull();
        // internal variables are not exposed on the model
        assertThatExceptionOfType(NoSuchFieldException.class).isThrownBy(() -> resourceClazz.getDeclaredField("approver"));

        Process<? extends Model> p = app.get(Processes.class).processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        assertThat(processInstance.status()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);

        processInstance.abort();

        assertThat(processInstance.status()).isEqualTo(KogitoProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testApprovalWithRequiredVariableTags() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval-with-required-variable-tags.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        assertThatExceptionOfType(VariableViolationException.class).isThrownBy(() -> {
            ProcessInstance<?> processInstance = p.createInstance(m);
            processInstance.start();
        });
    }

    @Test
    public void testApprovalWithIOVariableTags() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/approval-with-io-variable-tags.bpmn2");
        assertThat(app).isNotNull();

        Class<?> modelClazz = Class.forName("org.acme.travels.ApprovalsModel", true, testClassLoader());
        assertThat(modelClazz).isNotNull();
        assertThat(modelClazz.getDeclaredField("decision")).isNotNull();
        assertThat(modelClazz.getDeclaredField("approver")).isNotNull();

        Class<?> inputModelClazz = Class.forName("org.acme.travels.ApprovalsModelInput", true, testClassLoader());
        assertThat(inputModelClazz).isNotNull();
        assertThat(inputModelClazz.getDeclaredField("approver")).isNotNull();
        assertThatExceptionOfType(NoSuchFieldException.class).isThrownBy(() -> inputModelClazz.getDeclaredField("decision"));
        assertThatExceptionOfType(NoSuchFieldException.class).isThrownBy(() -> inputModelClazz.getDeclaredField("id"));

        Class<?> outputModelClazz = Class.forName("org.acme.travels.ApprovalsModelOutput", true, testClassLoader());
        assertThat(outputModelClazz).isNotNull();
        assertThat(outputModelClazz.getDeclaredField("decision")).isNotNull();
        assertThat(outputModelClazz.getDeclaredField("id")).isNotNull();
        assertThatExceptionOfType(NoSuchFieldException.class).isThrownBy(() -> outputModelClazz.getDeclaredField("approver"));

        Process<? extends Model> p = app.get(Processes.class).processById("approvals");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("approver", "mary");
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();
        assertThat(processInstance.status()).isEqualTo(KogitoProcessInstance.STATE_ACTIVE);

        processInstance.abort();

        assertThat(processInstance.status()).isEqualTo(KogitoProcessInstance.STATE_ABORTED);
    }

    @Test
    public void testUserTaskWithIOexpressionProcess() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTaskWithIOexpression.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("UserTask");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("person", new Person("john", 0));
        m.fromMap(parameters);

        ProcessInstance<?> processInstance = p.createInstance(m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        assertThat(workItems.get(0).getName()).isEqualTo("Hello");
        assertThat(workItems.get(0).getParameters()).containsEntry("personName", "john");

        processInstance.completeWorkItem(workItems.get(0).getId(), singletonMap("personAge", 50), securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);

        Model output = (Model) processInstance.variables();
        Person person = (Person) output.toMap().get("person");
        assertThat(person.getAge()).isEqualTo(50);
    }

    @Test
    public void testBasicUserTaskProcessWithBusinessKey() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        // assign custom business key for process instance
        String businessKey = "business key";
        ProcessInstance<?> processInstance = p.createInstance(businessKey, m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        // verify that custom business key is assigned properly
        assertThat(processInstance.businessKey()).isEqualTo(businessKey);

        // find the process instance by ID and verify business key
        Optional<? extends ProcessInstance<? extends Model>> processInstanceByBussinesKey = p.instances().findById(processInstance.id());
        assertThat(processInstanceByBussinesKey).isPresent();
        assertThat(processInstanceByBussinesKey.get().businessKey()).isEqualTo(businessKey);

        List<WorkItem> workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        assertThat(workItems.get(0).getName()).isEqualTo("FirstTask");
        WorkItem wi = workItems.get(0);

        KogitoWorkItemHandler handler = getWorkItemHandler(p, wi);
        WorkItemTransition transition = handler.newTransition(TRANSITION_ACTIVATED_CLAIM.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        wi = workItems.get(0);
        assertThat(wi.getName()).isEqualTo("FirstTask");
        transition = handler.newTransition(TRANSITION_RESERVED_COMPLETE.id(), wi.getPhaseStatus(), singletonMap("ACTUAL_OWNER", "john"), securityPolicy);
        processInstance.transitionWorkItem(workItems.get(0).getId(), transition);

        workItems = processInstance.workItems(securityPolicy);
        assertThat(workItems).hasSize(1);
        assertThat(workItems.get(0).getName()).isEqualTo("SecondTask");

        processInstance.completeWorkItem(workItems.get(0).getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }

    @Test
    public void testBasicUserTaskProcessWithDuplicatedBusinessKey() throws Exception {

        Application app = generateCodeProcessesOnly("usertask/UserTasksProcess.bpmn2");
        assertThat(app).isNotNull();

        Process<? extends Model> p = app.get(Processes.class).processById("UserTasksProcess");

        Model m = p.createModel();
        Map<String, Object> parameters = new HashMap<>();
        m.fromMap(parameters);

        // assign custom business key for process instance
        String businessKey = "business key";
        ProcessInstance<?> processInstance = p.createInstance(businessKey, m);
        processInstance.start();

        assertThat(processInstance.status()).isEqualTo(ProcessInstance.STATE_ACTIVE);
        // verify that custom business key is assigned properly
        assertThat(processInstance.businessKey()).isEqualTo(businessKey);

        // start another process instance with assigned duplicated business key of already active instance
        ProcessInstance<? extends Model> otherProcessInstance = p.createInstance(businessKey, m);
        assertThat(otherProcessInstance.id()).isNotEqualTo(processInstance.id());
        assertThat(otherProcessInstance.businessKey()).isEqualTo(processInstance.businessKey()).isEqualTo(businessKey);
    }
}

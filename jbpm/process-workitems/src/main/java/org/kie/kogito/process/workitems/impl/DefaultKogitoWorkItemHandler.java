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
package org.kie.kogito.process.workitems.impl;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.kie.kogito.internal.process.workitem.KogitoWorkItem;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler;
import org.kie.kogito.internal.process.workitem.KogitoWorkItemManager;
import org.kie.kogito.internal.process.workitem.Policy;
import org.kie.kogito.internal.process.workitem.WorkItemLifeCycle;
import org.kie.kogito.internal.process.workitem.WorkItemLifeCyclePhase;
import org.kie.kogito.internal.process.workitem.WorkItemPhaseState;
import org.kie.kogito.internal.process.workitem.WorkItemTerminationType;
import org.kie.kogito.internal.process.workitem.WorkItemTransition;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;

public class DefaultKogitoWorkItemHandler implements KogitoWorkItemHandler {

    public static final WorkItemPhaseState initialized = WorkItemPhaseState.initialized();
    public static final WorkItemPhaseState completed = WorkItemPhaseState.of("Completed", WorkItemTerminationType.COMPLETE);
    public static final WorkItemPhaseState aborted = WorkItemPhaseState.of("Aborted", WorkItemTerminationType.ABORT);
    public static final WorkItemPhaseState activated = WorkItemPhaseState.of("Activated");

    protected WorkItemLifeCycle workItemLifeCycle;

    public DefaultKogitoWorkItemHandler(WorkItemLifeCycle workItemLifeCycle) {
        this.workItemLifeCycle = workItemLifeCycle;
    }

    public DefaultKogitoWorkItemHandler() {
        this.workItemLifeCycle = init();
    }

    public WorkItemLifeCycle init() {
        DefaultWorkItemLifeCyclePhase complete = new DefaultWorkItemLifeCyclePhase("complete", activated, completed, this::completeWorkItemHandler);
        DefaultWorkItemLifeCyclePhase abort = new DefaultWorkItemLifeCyclePhase("abort", activated, aborted, this::abortWorkItemHandler);
        DefaultWorkItemLifeCyclePhase active = new DefaultWorkItemLifeCyclePhase("activate", initialized, activated, this::activateWorkItemHandler);
        return new DefaultWorkItemLifeCycle(active, abort, complete);
    }

    @Override
    public WorkItemTransition startingTransition(Map<String, Object> data, Policy... policies) {
        return workItemLifeCycle.newTransition("activate", null, data, policies);
    }

    @Override
    public WorkItemTransition abortTransition(String phaseStatus, Policy... policies) {
        return workItemLifeCycle.newTransition("abort", phaseStatus, emptyMap(), policies);
    }

    @Override
    public WorkItemTransition completeTransition(String phaseStatus, Map<String, Object> data, Policy... policies) {
        return workItemLifeCycle.newTransition("complete", phaseStatus, data, policies);
    }

    @Override
    public Optional<WorkItemTransition> transitionToPhase(KogitoWorkItemManager manager, KogitoWorkItem workItem, WorkItemTransition transition) {
        return workItemLifeCycle.transitionTo(manager, this, workItem, transition);
    }

    @Override
    public Set<String> allowedTransitions(String phaseStatus) {
        return workItemLifeCycle.allowedPhases(phaseStatus).stream().map(WorkItemLifeCyclePhase::id).collect(toSet());
    }

    @Override
    public WorkItemTransition newTransition(String phaseId, String phaseStatus, Map<String, Object> map, Policy... policy) {
        return workItemLifeCycle.newTransition(phaseId, phaseStatus, map, policy);
    }

    public Optional<WorkItemTransition> activateWorkItemHandler(KogitoWorkItemManager manager, KogitoWorkItemHandler handler, KogitoWorkItem workitem, WorkItemTransition transition) {
        return Optional.empty();
    }

    public Optional<WorkItemTransition> abortWorkItemHandler(KogitoWorkItemManager manager, KogitoWorkItemHandler handler, KogitoWorkItem workitem, WorkItemTransition transition) {
        return Optional.empty();
    }

    public Optional<WorkItemTransition> completeWorkItemHandler(KogitoWorkItemManager manager, KogitoWorkItemHandler handler, KogitoWorkItem workitem, WorkItemTransition transition) {
        return Optional.empty();
    }

}

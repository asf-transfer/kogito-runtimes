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
package org.jbpm.workflow.core.node;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.workflow.core.impl.NodeImpl;
import org.jbpm.workflow.instance.NodeInstanceContainer;
import org.jbpm.workflow.instance.node.EventNodeInstance;
import org.kie.api.definition.process.Node;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.kogito.internal.process.event.KogitoEventListener;
import org.kie.kogito.internal.process.runtime.KogitoNodeInstance;
import org.kie.kogito.jobs.ExactExpirationTime;
import org.kie.kogito.jobs.ExpirationTime;
import org.kie.kogito.jobs.JobsService;
import org.kie.kogito.jobs.ProcessInstanceJobDescription;
import org.kie.kogito.uow.WorkUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime counterpart of an event node.
 *
 */
public class AsyncEventNodeInstance extends EventNodeInstance {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AsyncEventNodeInstance.class);
    private static final String TIMER_TRIGGERED_EVENT = "timerTriggered";

    private final KogitoEventListener listener = new AsyncExternalEventListener();
    private String jobId = "";

    //receive the signal when it is the node is executed
    private class AsyncExternalEventListener implements KogitoEventListener {
        @Override
        public String[] getEventTypes() {
            return new String[] { getEventType() };
        }

        @Override
        public void signalEvent(String type,
                Object event) {
            triggerCompleted();
        }
    }

    @Override
    public void internalTrigger(KogitoNodeInstance from, String type) {
        addEventListeners();
        final InternalProcessRuntime processRuntime = (InternalProcessRuntime) getProcessInstance().getKnowledgeRuntime().getProcessRuntime();
        //Deffer the timer scheduling to the end of current UnitOfWork execution chain
        processRuntime.getUnitOfWorkManager().currentUnitOfWork().intercept(
                WorkUnit.create(this, instance -> {
                    ExpirationTime expirationTime = ExactExpirationTime.of(ZonedDateTime.now().plus(1, ChronoUnit.MILLIS));
                    ProcessInstanceJobDescription jobDescription =
                            ProcessInstanceJobDescription.of(instance.getStringId(),
                                    expirationTime,
                                    instance.getProcessInstance().getStringId(),
                                    instance.getProcessInstance().getRootProcessInstanceId(),
                                    instance.getProcessInstance().getProcessId(),
                                    instance.getProcessInstance().getRootProcessId(),
                                    Optional.ofNullable(from).map(KogitoNodeInstance::getStringId).orElse(null));
                    JobsService jobService = processRuntime.getJobsService();
                    String jobId = jobService.scheduleProcessInstanceJob(jobDescription);
                    setJobId(jobId);
                }, i -> {
                }, WorkUnit.LOW_PRIORITY));
    }

    @Override
    public void addEventListeners() {
        getProcessInstance().addEventListener(getEventType(), getEventListener(), true);
    }

    @Override
    public String getEventType() {
        return TIMER_TRIGGERED_EVENT + ":" + getStringId();
    }

    @Override
    public Node getNode() {
        return new AsyncEventNode(super.getNode());
    }

    public Node getActualNode() {
        return super.getNode();
    }

    @Override
    protected KogitoEventListener getEventListener() {
        return listener;
    }

    @Override
    public void cancel() {
        ((InternalProcessRuntime) getProcessInstance().getKnowledgeRuntime().getProcessRuntime()).getJobsService().cancelJob(getJobId());
        super.cancel();
    }

    public String getJobId() {
        return this.jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public void triggerCompleted() {
        getProcessInstance().removeEventListener(getEventType(), getEventListener(), true);
        NodeInstanceContainer instanceContainer = (NodeInstanceContainer) getNodeInstanceContainer();
        instanceContainer.setCurrentLevel(getLevel());
        instanceContainer.removeNodeInstance(this);

        NodeInstance actualInstance = instanceContainer.getNodeInstance(getNode());
        //trigger the actual node
        triggerNodeInstance((org.jbpm.workflow.instance.NodeInstance) actualInstance, NodeImpl.CONNECTION_DEFAULT_TYPE);
    }
}

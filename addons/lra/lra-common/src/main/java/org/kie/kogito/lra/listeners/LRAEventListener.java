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

package org.kie.kogito.lra.listeners;

import java.net.URI;
import java.time.temporal.ChronoUnit;
import javax.ws.rs.core.UriBuilder;

import io.narayana.lra.LRAConstants;
import io.narayana.lra.client.NarayanaLRAClient;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.kogito.lra.KogitoLRA;
import org.kie.kogito.lra.model.LRAContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.kogito.lra.KogitoLRA.LRA_CONTEXT;
import static org.kie.kogito.lra.KogitoLRA.METADATA_TIMEOUT;

public class LRAEventListener extends DefaultProcessEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LRAEventListener.class);
    private final NarayanaLRAClient lraClient;

    LRAEventListener(NarayanaLRAClient lraClient) {
        this.lraClient = lraClient;
    }

    @Override
    public void beforeProcessStarted(ProcessStartedEvent event) {
        super.afterProcessStarted(event);
        ProcessInstance instance = event.getProcessInstance();
        if (!(instance instanceof RuleFlowProcessInstance)) {
            LOGGER.warn("Unable to start/join LRA. Process instance is not a RuleFlowProcessInstance");
            return;
        }
        RuleFlowProcessInstance wfInstance = (RuleFlowProcessInstance) instance;
        LRAContext context = (LRAContext) wfInstance.getMetaData().get(LRA_CONTEXT);
        if (context == null) {
            context = new LRAContext();
            wfInstance.setMetaData(LRA_CONTEXT, context);
        }
        //TODO: Implement all cases for org.eclipse.microprofile.lra.annotation.ws.rs.LRA#Type
        if (context.getUri() == null) {
            context.setUri(lraClient.startLRA(null, event.getProcessInstance().getProcessId(), 0L, ChronoUnit.SECONDS));
            LOGGER.debug("started LRA: {}", context.getUri());
        } else {
            context.setRecoverUri(join(context, instance));
            LOGGER.debug("joined existing LRA: {}", context.getUri());
        }
    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        super.beforeProcessCompleted(event);
        ProcessInstance instance = event.getProcessInstance();
        if (!(instance instanceof RuleFlowProcessInstance)) {
            LOGGER.warn("Unable to close LRA. Process instance is not a RuleFlowProcessInstance");
            return;
        }
        RuleFlowProcessInstance wfInstance = (RuleFlowProcessInstance) instance;
        LRAContext context = (LRAContext) wfInstance.getMetaData().get(LRA_CONTEXT);
        if (context == null || context.getRecoverUri() != null) {
            return;
        }
        if (ProcessInstance.STATE_COMPLETED == event.getProcessInstance().getState()) {
            lraClient.closeLRA(context.getUri());
            LOGGER.debug("Completed LRA {}", context.getUri());
        } else {
            lraClient.cancelLRA(context.getUri());
            LOGGER.debug("Cancelled LRA {}", context.getUri());
        }
    }

    private URI join(LRAContext context, ProcessInstance processInstance) {
        UriBuilder uriBuilder = UriBuilder.fromUri(context.getBasePath())
                .path(KogitoLRA.LRA_RESOURCE)
                .path("{action}");

        return lraClient.joinLRA(context.getUri(),
                (Long) processInstance.getProcess().getMetaData().getOrDefault(METADATA_TIMEOUT, 0L),
                uriBuilder.build(LRAConstants.COMPENSATE),
                uriBuilder.build(LRAConstants.COMPLETE),
                uriBuilder.build(LRAConstants.FORGET),
                uriBuilder.build(LRAConstants.LEAVE),
                uriBuilder.build(LRAConstants.AFTER),
                null,
                null);
    }
}

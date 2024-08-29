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
package org.jbpm.ruleflow.instance;

import java.util.List;

import org.jbpm.ruleflow.core.Metadata;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.workflow.core.node.StartNode;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.jbpm.workflow.instance.node.StartNodeInstance;
import org.kie.api.definition.process.Node;

public class RuleFlowProcessInstance extends WorkflowProcessInstanceImpl {

    private static final long serialVersionUID = 510l;

    public RuleFlowProcess getRuleFlowProcess() {
        return (RuleFlowProcess) getProcess();
    }

    @Override
    public void internalStart(String trigger, Object payload) {
        StartNode startNode = getRuleFlowProcess().getStart(trigger, payload);
        if (startNode != null) {
            if (Metadata.EVENT_TYPE_NONE.equals(startNode.getMetaData(Metadata.EVENT_TYPE))) {
                getNodeInstance(startNode).trigger(null, null);
            } else {
                ((StartNodeInstance) getNodeInstance(startNode)).signalEvent(trigger, payload);
            }
        } else if (!getRuleFlowProcess().isDynamic()) {
            throw new IllegalArgumentException("There is no start node that matches the trigger " + (trigger == null ? "none" : trigger));
        }

        // activate ad hoc fragments if they are marked as such
        List<Node> autoStartNodes = getRuleFlowProcess().getAutoStartNodes();
        autoStartNodes.forEach(autoStartNode -> signalEvent(autoStartNode.getName(), null));
    }

}

/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.ruleflow.core.factory;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.process.instance.impl.Action;
import org.jbpm.ruleflow.core.RuleFlowNodeContainerFactory;
import org.jbpm.workflow.core.DroolsAction;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.core.NodeContainer;
import org.jbpm.workflow.core.node.EndNode;

/**
 *
 */
public class EndNodeFactory extends ExtendedNodeFactory {

    public EndNodeFactory(RuleFlowNodeContainerFactory nodeContainerFactory, NodeContainer nodeContainer, long id) {
        super(nodeContainerFactory, nodeContainer, id);
    }

    protected Node createNode() {
        return new EndNode();
    }

    protected EndNode getEndNode() {
        return (EndNode) getNode();
    }

    @Override
    public EndNodeFactory name(String name) {
        super.name(name);
        return this;
    }

    @Override
    public EndNodeFactory metaData(String name, Object value) {
        super.metaData(name, value);
        return this;
    }

    public EndNodeFactory terminate(boolean terminate) {
        getEndNode().setTerminate(terminate);
        return this;
    }
    
    public EndNodeFactory action(Action action) {
        DroolsAction droolsAction = new DroolsAction();
        droolsAction.setMetaData(DroolsAction.METADATA_ACTION, action);
        List<DroolsAction> enterActions = getEndNode().getActions(EndNode.EVENT_NODE_ENTER);
        if (enterActions == null) {
            enterActions = new ArrayList<>();
            getEndNode().setActions(EndNode.EVENT_NODE_ENTER, enterActions);
        }
        enterActions.add(droolsAction);
        return this;
    }
}

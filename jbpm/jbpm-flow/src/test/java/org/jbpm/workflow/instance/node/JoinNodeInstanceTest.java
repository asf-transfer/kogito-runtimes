/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates.
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
package org.jbpm.workflow.instance.node;

import java.util.ArrayList;
import java.util.List;

import org.drools.core.common.InternalKnowledgeRuntime;
import org.jbpm.process.core.event.EventFilter;
import org.jbpm.process.core.event.EventTypeFilter;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.test.util.AbstractBaseTest;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.core.impl.ConnectionImpl;
import org.jbpm.workflow.core.impl.WorkflowProcessImpl;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.EventNode;
import org.jbpm.workflow.core.node.Join;
import org.jbpm.workflow.instance.impl.NodeInstanceFactoryRegistry;
import org.junit.jupiter.api.Test;
import org.kie.kogito.internal.process.runtime.KogitoProcessRuntime;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class JoinNodeInstanceTest extends AbstractBaseTest {

    public void addLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testJoinNode() {
        KogitoProcessRuntime kruntime = createKogitoProcessRuntime();
        MockNode mockNode = new MockNode();
        MockNodeInstanceFactory factory = new MockNodeInstanceFactory(new MockNodeInstance(mockNode));
        NodeInstanceFactoryRegistry.getInstance(kruntime.getKieRuntime().getEnvironment()).register(mockNode.getClass(), factory);

        WorkflowProcessImpl process = new WorkflowProcessImpl();
        mockNode.setId(1);

        EventNode eventNode1 = new EventNode();
        eventNode1.setName("Event1");
        eventNode1.setId(2);
        List<EventFilter> filters = new ArrayList<EventFilter>();
        EventTypeFilter filter = new EventTypeFilter();
        filter.setType("signal");
        filters.add(filter);
        eventNode1.setEventFilters(filters);
        new ConnectionImpl(mockNode, Node.CONNECTION_DEFAULT_TYPE, eventNode1, Node.CONNECTION_DEFAULT_TYPE);

        Join joinNode = new Join();
        joinNode.setId(3);
        joinNode.setName("join node");
        joinNode.setType(2);

        new ConnectionImpl(eventNode1, Node.CONNECTION_DEFAULT_TYPE, joinNode, Node.CONNECTION_DEFAULT_TYPE);

        EventNode eventNode2 = new EventNode();
        eventNode2.setName("Event2");
        eventNode2.setId(4);

        new ConnectionImpl(eventNode2, Node.CONNECTION_DEFAULT_TYPE, joinNode, Node.CONNECTION_DEFAULT_TYPE);

        process.addNode(mockNode);
        process.addNode(eventNode1);
        process.addNode(eventNode2);
        process.addNode(joinNode);

        EndNode endNode = new EndNode();
        endNode.setName("End");
        endNode.setId(5);
        process.addNode(endNode);

        new ConnectionImpl(
                joinNode, Node.CONNECTION_DEFAULT_TYPE,
                endNode, Node.CONNECTION_DEFAULT_TYPE);

        RuleFlowProcessInstance processInstance = new RuleFlowProcessInstance();
        processInstance.setId("1223");
        processInstance.setState(ProcessInstance.STATE_ACTIVE);
        processInstance.setProcess(process);
        processInstance.setKnowledgeRuntime((InternalKnowledgeRuntime) kruntime.getKieSession());
        processInstance.signalEvent("signal", null);

        MockNodeInstance mockNodeInstance = (MockNodeInstance) processInstance.getNodeInstance(mockNode);
        mockNodeInstance.triggerCompleted();
        assertThat(processInstance.getState()).isEqualTo(ProcessInstance.STATE_COMPLETED);
    }
}

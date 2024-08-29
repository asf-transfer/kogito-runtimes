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
package org.jbpm.ruleflow.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jbpm.process.core.context.exception.CompensationScope;
import org.jbpm.process.core.context.exception.ExceptionScope;
import org.jbpm.process.core.context.swimlane.SwimlaneContext;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.core.event.EventFilter;
import org.jbpm.workflow.core.impl.NodeContainerImpl;
import org.jbpm.workflow.core.impl.WorkflowProcessImpl;
import org.jbpm.workflow.core.node.ConstraintTrigger;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.EventTrigger;
import org.jbpm.workflow.core.node.FaultNode;
import org.jbpm.workflow.core.node.StartNode;
import org.jbpm.workflow.core.node.Trigger;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.NodeContainer;

public class RuleFlowProcess extends WorkflowProcessImpl {

    private static final long serialVersionUID = 510l;

    public RuleFlowProcess() {
        setType(RULEFLOW_TYPE);
        // TODO create contexts on request ?
        VariableScope variableScope = new VariableScope();
        addContext(variableScope);
        setDefaultContext(variableScope);
        SwimlaneContext swimLaneContext = new SwimlaneContext();
        addContext(swimLaneContext);
        setDefaultContext(swimLaneContext);
        ExceptionScope exceptionScope = new ExceptionScope();
        addContext(exceptionScope);
        setDefaultContext(exceptionScope);
        // backward compatibility.
        setMetaData("jbpm.enable.multi.con", System.getProperty("jbpm.enable.multi.con"));
    }

    public VariableScope getVariableScope() {
        return (VariableScope) getDefaultContext(VariableScope.VARIABLE_SCOPE);
    }

    public SwimlaneContext getSwimlaneContext() {
        return (SwimlaneContext) getDefaultContext(SwimlaneContext.SWIMLANE_SCOPE);
    }

    public ExceptionScope getExceptionScope() {
        return (ExceptionScope) getDefaultContext(ExceptionScope.EXCEPTION_SCOPE);
    }

    public CompensationScope getCompensationScope() {
        return (CompensationScope) getDefaultContext(CompensationScope.COMPENSATION_SCOPE);
    }

    @Override
    protected NodeContainer createNodeContainer() {
        return new WorkflowProcessNodeContainer();
    }

    public List<Node> getStartNodes() {
        return getStartNodes(this.getNodes());
    }

    public static List<Node> getStartNodes(Node[] nodes) {
        List<Node> startNodes = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof StartNode) {
                startNodes.add(node);
            }
        }

        return startNodes;
    }

    public List<Node> getEndNodes() {
        return getEndNodes(this.getNodes());
    }

    public static List<Node> getEndNodes(Node[] nodes) {
        final List<Node> endNodes = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof EndNode || node instanceof FaultNode) {
                endNodes.add(node);
            }
        }

        return endNodes;
    }

    public StartNode getStart(String trigger, Object payload) {
        Node[] nodes = getNodes();

        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] instanceof StartNode start) {
                // no events
                if (trigger == null && ((start.getTriggers() == null || start.getTriggers().isEmpty()) && start.getTimer() == null)) {
                    return start;
                }

                // there is a timer
                if (start.getTimer() != null && "timer".equals(trigger)) {
                    return start;
                }

                // there is an event trigger
                if (start.getTriggers() != null) {
                    for (Trigger t : start.getTriggers()) {
                        if (t instanceof EventTrigger eventTrigger) {
                            for (EventFilter filter : eventTrigger.getEventFilters()) {
                                if (filter.acceptsEvent(trigger, payload, key -> null)) {
                                    return start;
                                }
                            }
                        } else if (t instanceof ConstraintTrigger && "conditional".equals(trigger)) {
                            return start;
                        }
                    }

                }
            }
        }
        return null;
    }

    public List<Node> getAutoStartNodes() {
        if (!isDynamic()) {
            return Collections.emptyList();
        }

        return Arrays.stream(getNodes())
                .filter(n -> n.getIncomingConnections().isEmpty() && "true".equalsIgnoreCase((String) n.getMetaData().get("customAutoStart")))
                .collect(Collectors.toList());
    }

    private class WorkflowProcessNodeContainer extends NodeContainerImpl {

        private static final long serialVersionUID = 510l;

    }

}

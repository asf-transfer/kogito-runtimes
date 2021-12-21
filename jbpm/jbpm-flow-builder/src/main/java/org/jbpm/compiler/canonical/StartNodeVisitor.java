/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
package org.jbpm.compiler.canonical;

import java.util.Map;

import org.jbpm.process.core.context.variable.Variable;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.core.timer.Timer;
import org.jbpm.ruleflow.core.factory.StartNodeFactory;
import org.jbpm.workflow.core.node.StartNode;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import static org.jbpm.ruleflow.core.Metadata.EVENT_TYPE_SIGNAL;
import static org.jbpm.ruleflow.core.Metadata.MESSAGE_TYPE;
import static org.jbpm.ruleflow.core.Metadata.TRIGGER_MAPPING;
import static org.jbpm.ruleflow.core.Metadata.TRIGGER_REF;
import static org.jbpm.ruleflow.core.Metadata.TRIGGER_TYPE;
import static org.jbpm.ruleflow.core.factory.StartNodeFactory.METHOD_INTERRUPTING;
import static org.jbpm.ruleflow.core.factory.StartNodeFactory.METHOD_TIMER;
import static org.jbpm.ruleflow.core.factory.StartNodeFactory.METHOD_TRIGGER;

public class StartNodeVisitor extends AbstractNodeVisitor<StartNode> {

    @Override
    protected String getNodeKey() {
        return "startNode";
    }

    @Override
    public void visitNode(String factoryField, StartNode node, BlockStmt body, VariableScope variableScope, ProcessMetaData metadata) {
        body.addStatement(getAssignedFactoryMethod(factoryField, StartNodeFactory.class, getNodeId(node), getNodeKey(), new LongLiteralExpr(node.getId())))
                .addStatement(getNameMethod(node, "Start"))
                .addStatement(getFactoryMethod(getNodeId(node), METHOD_INTERRUPTING, new BooleanLiteralExpr(node.isInterrupting())));

        addNodeMappings(node, body, getNodeId(node));
        visitMetaData(node.getMetaData(), body, getNodeId(node));
        body.addStatement(getDoneMethod(getNodeId(node)));
        if (node.getTimer() != null) {
            Timer timer = node.getTimer();
            body.addStatement(getFactoryMethod(getNodeId(node), METHOD_TIMER, getOrNullExpr(timer.getDelay()),
                    getOrNullExpr(timer.getPeriod()),
                    getOrNullExpr(timer.getDate()),
                    new IntegerLiteralExpr(node.getTimer().getTimeType())));

        } else if (node.getTriggers() != null && !node.getTriggers().isEmpty()) {
            TriggerMetaData triggerMetaData = buildTriggerMetadata(node);
            metadata.addTrigger(triggerMetaData);
            handleSignal(node, node.getMetaData(), body, variableScope, metadata);
        } else {
            // since there is start node without trigger then make sure it is startable
            metadata.setStartable(true);
        }
    }

    private TriggerMetaData buildTriggerMetadata(StartNode node) {
        return TriggerMetaData.of(node, (String) node.getMetaData(TRIGGER_MAPPING));
    }

    protected void handleSignal(StartNode startNode, Map<String, Object> nodeMetaData, BlockStmt body, VariableScope variableScope, ProcessMetaData metadata) {
        if (EVENT_TYPE_SIGNAL.equalsIgnoreCase((String) startNode.getMetaData(TRIGGER_TYPE))) {
            Variable variable = null;
            if (!startNode.getIoSpecification().getDataOutputAssociation().isEmpty()) {

                body.addStatement(getFactoryMethod(getNodeId(startNode), METHOD_TRIGGER,
                        new StringLiteralExpr((String) nodeMetaData.get(TRIGGER_REF)),
                        buildDataAssociationsExpression(startNode.getIoSpecification().getDataOutputAssociation())));

                String triggerMapping = (String) nodeMetaData.get(TRIGGER_MAPPING);
                variable = variableScope.findVariable(triggerMapping);

                if (variable == null) {
                    // check parent node container
                    VariableScope vscope = (VariableScope) startNode.resolveContext(VariableScope.VARIABLE_SCOPE, triggerMapping);
                    variable = vscope.findVariable(triggerMapping);
                }

                metadata.addSignal((String) nodeMetaData.get(MESSAGE_TYPE), variable != null ? variable.getType().getStringType() : null);
            } else {
                body.addStatement(getFactoryMethod(getNodeId(startNode), METHOD_TRIGGER,
                        new StringLiteralExpr((String) nodeMetaData.get(MESSAGE_TYPE)),
                        buildDataAssociationsExpression(startNode.getIoSpecification().getDataOutputAssociation())));
            }
            metadata.addSignal((String) nodeMetaData.get(MESSAGE_TYPE), variable != null ? variable.getType().getStringType() : null);
        } else {
            body.addStatement(getFactoryMethod(getNodeId(startNode), METHOD_TRIGGER,
                    new StringLiteralExpr((String) nodeMetaData.get(TRIGGER_REF)),
                    buildDataAssociationsExpression(startNode.getIoSpecification().getDataOutputAssociation())));
        }
    }

}

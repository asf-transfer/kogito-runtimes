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
package org.jbpm.bpmn2.xml;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.compiler.xml.Parser;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.core.impl.DataAssociation;
import org.jbpm.workflow.core.node.Assignment;
import org.jbpm.workflow.core.node.RuleSetNode;
import org.jbpm.workflow.instance.rule.RuleType;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import static org.jbpm.workflow.instance.rule.RuleType.DMN_LANG;
import static org.jbpm.workflow.instance.rule.RuleType.DRL_LANG;

public class BusinessRuleTaskHandler extends AbstractNodeHandler {

    private static final String NAMESPACE_PROP = "namespace";
    private static final String MODEL_PROP = "model";
    private static final String DECISION_PROP = "decision";

    protected Node createNode(Attributes attrs) {
        return new RuleSetNode();
    }

    public Class<RuleSetNode> generateNodeFor() {
        return RuleSetNode.class;
    }

    @Override
    protected Node handleNode(final Node node, final Element element, final String uri,
            final String localName, final Parser parser) throws SAXException {
        super.handleNode(node, element, uri, localName, parser);

        RuleSetNode ruleSetNode = (RuleSetNode) node;
        ruleSetNode.setIoSpecification(readIOEspecification(parser, element));

        String language = element.getAttribute("implementation");
        if (language == null || language.equalsIgnoreCase("##unspecified") || language.isEmpty()) {
            language = DRL_LANG;
        }
        ruleSetNode.setLanguage(language);

        if (DMN_LANG.equals(language)) {
            Map<String, String> parameters = new HashMap<>();
            for (DataAssociation dataAssociation : ruleSetNode.getIoSpecification().getDataInputAssociation()) {
                for (Assignment assignment : dataAssociation.getAssignments()) {
                    parameters.put(assignment.getTo().getLabel(), assignment.getFrom().getExpression());
                }
            }

            String namespace = parameters.getOrDefault(NAMESPACE_PROP, "");
            String model = parameters.getOrDefault(MODEL_PROP, "");//we have to actually get #{university}
            //but we were not getting that. we were just getting null as label for this expression was empty
            String decision = parameters.getOrDefault(DECISION_PROP, "");
            String variableRegex = "#\\{[^}]+}";
            if (namespace.matches(variableRegex)) {
                namespace = resolveProcessVariable(namespace, parser, element);
            }
            if (model.matches(variableRegex)) {
                model = resolveProcessVariable(model, parser, element);
            }
            if (decision.matches(variableRegex)) {
                decision = resolveProcessVariable(decision, parser, element);
            }

            ruleSetNode.setRuleType(RuleType.decision(
                    namespace,
                    model,
                    decision));
        } else {
            String ruleFlowGroup = element.getAttribute("ruleFlowGroup");
            ruleSetNode.setRuleType(RuleType.of(ruleFlowGroup, language));
        }

        handleScript(ruleSetNode, element, "onEntry");
        handleScript(ruleSetNode, element, "onExit");

        return ruleSetNode;
    }

    public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
        RuleSetNode ruleSetNode = (RuleSetNode) node;
        writeNode("businessRuleTask", ruleSetNode, xmlDump, metaDataType);
        RuleType ruleType = ruleSetNode.getRuleType();
        if (ruleType != null) {
            xmlDump.append("g:ruleFlowGroup=\"" + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(ruleType.getName()) + "\" " + EOL);
        }

        xmlDump.append(" implementation=\"" + XmlBPMNProcessDumper.replaceIllegalCharsAttribute(ruleSetNode.getLanguage()) + "\" >" + EOL);

        writeExtensionElements(ruleSetNode, xmlDump);
        writeIO(ruleSetNode.getIoSpecification(), xmlDump);
        endNode("businessRuleTask", xmlDump);
    }

    private String resolveProcessVariable(String expression, Parser parser, Element element) {

        String varName = expression.substring(expression.indexOf("#{") + 2, expression.indexOf("}"));

        String variableValue = element.getAttribute(varName);//as we understand that the variable is university. But how to get the value since the variables are not in the attribute list?

        return expression.replace("#{" + varName + "}", variableValue);
    }

}

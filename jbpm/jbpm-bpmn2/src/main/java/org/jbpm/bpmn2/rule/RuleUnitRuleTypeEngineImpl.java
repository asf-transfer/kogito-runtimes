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
package org.jbpm.bpmn2.rule;

import org.drools.core.process.AbstractProcessContext;
import org.drools.ruleunits.api.RuleUnitData;
import org.drools.ruleunits.api.RuleUnitInstance;
import org.jbpm.util.ContextFactory;
import org.jbpm.workflow.core.node.RuleUnitFactory;
import org.jbpm.workflow.instance.node.RuleSetNodeInstance;
import org.jbpm.workflow.instance.rule.RuleUnitRuleTypeEngine;

public class RuleUnitRuleTypeEngineImpl implements RuleUnitRuleTypeEngine {

    public void evaluate(RuleSetNodeInstance rsni) {
        RuleUnitFactory<RuleUnitData> factory = rsni.getRuleSetNode().getRuleUnitFactory();
        AbstractProcessContext context = ContextFactory.fromNode(rsni);
        RuleUnitData model = factory.bind(context);
        try (RuleUnitInstance<RuleUnitData> instance = factory.unit().createInstance(model)) {
            instance.fire();
            factory.unbind(context, model);
            rsni.triggerCompleted();
        }
    }

}

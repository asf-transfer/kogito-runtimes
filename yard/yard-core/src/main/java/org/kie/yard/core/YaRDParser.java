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
package org.kie.yard.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.drools.ruleunits.api.DataSource;
import org.kie.yard.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YaRDParser {
    private static final Logger LOG = LoggerFactory.getLogger(YaRDParser.class);
    private final YaRDDefinitions definitions = new YaRDDefinitions(new HashMap<>(), new ArrayList<>(), new HashMap<>());

    public YaRDParser() {
    }

    public YaRDDefinitions parse(String yaml) throws Exception {
        final YaRD sd = new YaRD_YamlMapperImpl().read(yaml);
        if (!sd.getExpressionLang().equals("jshell")) {
            throw new IllegalArgumentException("Only `jshell` is supported as an expression language");
        }
        appendInputs(sd.getInputs());
        appendUnits(sd.getElements());
        return definitions;
    }

    private void appendUnits(List<Element> list) {
        for (Element hi : list) {
            String nameString = hi.getName();
            LOG.debug("parsing {}", nameString);
            Firable decisionLogic = createDecisionLogic(nameString, hi.getLogic());
            definitions.units().add(decisionLogic);
        }
    }

    private Firable createDecisionLogic(String nameString, DecisionLogic decisionLogic) {
        if (decisionLogic instanceof org.kie.yard.api.model.DecisionTable decisionTable) {
            return new SyntheticRuleUnitWrapper(new DTableUnitBuilder(definitions, nameString, decisionTable).build());
        } else if (decisionLogic instanceof org.kie.yard.api.model.LiteralExpression literalExpression) {
            return new LiteralExpressionBuilder(definitions, nameString, literalExpression).build();
        } else {
            throw new UnsupportedOperationException("Not implemented.");
        }
    }

    private void appendInputs(List<Input> list) {
        for (Input hi : list) {
            String nameString = hi.getName();
            @SuppressWarnings("unused")
            Class<?> typeRef = processType(hi.getType());
            definitions.inputs().put(nameString, DataSource.createSingleton());
        }
    }

    private Class<?> processType(String string) {
        switch (string) {
            case "string":
            case "number":
            case "boolean":
            default:
                return Object.class;
        }
    }
}

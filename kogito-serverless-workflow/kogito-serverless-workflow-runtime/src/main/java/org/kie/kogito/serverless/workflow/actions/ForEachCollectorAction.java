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
package org.kie.kogito.serverless.workflow.actions;

import org.jbpm.workflow.instance.node.ForEachNodeInstance;
import org.kie.kogito.internal.process.runtime.KogitoProcessContext;
import org.kie.kogito.jackson.utils.JsonObjectUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import static org.kie.kogito.serverless.workflow.actions.ActionUtils.getWorkflowData;

public class ForEachCollectorAction extends BaseExpressionAction {

    public ForEachCollectorAction(String lang, String expr) {
        super(lang, expr);
    }

    @Override
    public void execute(KogitoProcessContext context) throws Exception {
        Iterable<?> collectedValues = (Iterable<?>) context.getVariable(ForEachNodeInstance.TEMP_OUTPUT_VAR);
        JsonNode arrayNode = evaluate(context, JsonNode.class);
        if (arrayNode instanceof ArrayNode) {
            JsonObjectUtils.mapToArray(collectedValues, (ArrayNode) arrayNode);
        } else {
            getWorkflowData(context).set(expr.varName().orElseThrow(() -> new IllegalArgumentException("Cannot get a valid var name from expression " + expr)),
                    JsonObjectUtils.mapToArray(collectedValues));
        }
    }
}

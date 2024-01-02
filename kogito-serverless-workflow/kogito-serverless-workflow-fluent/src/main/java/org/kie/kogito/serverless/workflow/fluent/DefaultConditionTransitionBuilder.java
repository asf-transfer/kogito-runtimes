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
package org.kie.kogito.serverless.workflow.fluent;

import io.serverlessworkflow.api.defaultdef.DefaultConditionDefinition;
import io.serverlessworkflow.api.end.End;
import io.serverlessworkflow.api.states.DefaultState;
import io.serverlessworkflow.api.transitions.Transition;

public class DefaultConditionTransitionBuilder<T> extends TransitionBuilder<T> {

    private DefaultConditionDefinition condition;

    protected DefaultConditionTransitionBuilder(T container, WorkflowBuilder workflow, DefaultState lastState, DefaultConditionDefinition condition) {
        super(container, workflow, lastState);
        this.condition = condition;
    }

    @Override
    protected void addTransition(DefaultState state) {
        if (condition != null) {
            condition.setTransition(new Transition(state.getName()));
            condition = null;
        } else {
            super.addTransition(state);
        }
    }

    @Override
    protected void addEnd(End end) {
        if (condition != null) {
            condition.setEnd(end);
            condition = null;
        } else {
            super.addEnd(end);
        }
    }
}

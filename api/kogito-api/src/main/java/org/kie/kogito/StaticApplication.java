/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito;

import org.kie.kogito.decision.DecisionModels;
import org.kie.kogito.prediction.PredictionModels;
import org.kie.kogito.process.ProcessConfig;
import org.kie.kogito.process.Processes;
import org.kie.kogito.rules.RuleUnits;
import org.kie.kogito.uow.UnitOfWorkManager;

import java.util.HashMap;
import java.util.Map;

public class StaticApplication implements Application {

    protected Config config;
    private final Map<String, KogitoEngine> engineMap = new HashMap<>();

    public StaticApplication() {

    }

    public StaticApplication(
            Config config,
            Processes processes,
            RuleUnits ruleUnits,
            DecisionModels decisionModels,
            PredictionModels predictionModels) {
        this.config = config;
        loadEngine(processes);
        loadEngine(ruleUnits);
        loadEngine(decisionModels);
        loadEngine(predictionModels);

        if (config().get(org.kie.kogito.process.ProcessConfig.class) != null) {
            unitOfWorkManager().eventManager().setAddons(config().addons());
        }
    }

    public Config config() {
        return config;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends KogitoEngine> T get(Class<T> clazz) {
        return (T) engineMap.get(clazz.getCanonicalName());
    }

    private void loadEngine(KogitoEngine engine) {
        if(engine != null) {
            engineMap.put(engine.getClass().getCanonicalName(), engine);
        }
    }

    public UnitOfWorkManager unitOfWorkManager() {
        return config().get(ProcessConfig.class).unitOfWorkManager();
    }

}

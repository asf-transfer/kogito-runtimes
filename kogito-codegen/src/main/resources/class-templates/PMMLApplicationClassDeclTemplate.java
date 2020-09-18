/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
import java.util.Map;
import java.util.function.Function;

import org.kie.api.KieBase;
import org.kie.api.runtime.KieRuntimeFactory;
import org.kie.pmml.evaluator.api.executor.PMMLRuntime;

public class PredictionModels implements org.kie.kogito.prediction.PredictionModels {

    public final static java.util.function.Function<String, org.kie.api.runtime.KieRuntimeFactory> kieRuntimeFactoryFunction;

    static {
        final java.util.Map<org.kie.api.KieBase, org.kie.api.runtime.KieRuntimeFactory> kieRuntimeFactories = org.kie.kogito.pmml.PMMLKogito.createKieRuntimeFactories();
        kieRuntimeFactoryFunction = new java.util.function.Function<java.lang.String, org.kie.api.runtime.KieRuntimeFactory>() {
            @Override
            public org.kie.api.runtime.KieRuntimeFactory apply(java.lang.String s) {
                return kieRuntimeFactories.keySet().stream()
                        .filter(kieBase -> org.kie.pmml.evaluator.core.utils.KnowledgeBaseUtils.getModel(kieBase, s).isPresent())
                        .map(kieBase ->  kieRuntimeFactories.get(kieBase))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Failed to fine KieRuntimeFactory for model " +s));
            }
        };
    }

    public PredictionModels(org.kie.kogito.Application app) {
    }

    public org.kie.kogito.prediction.PredictionModel getPredictionModel(java.lang.String modelName) {
        return new org.kie.kogito.pmml.PmmlPredictionModel(getPMMLRuntime(modelName), modelName);
    }

    private org.kie.pmml.evaluator.api.executor.PMMLRuntime getPMMLRuntime(java.lang.String modelName) {
        return kieRuntimeFactoryFunction.apply(modelName).get(org.kie.pmml.evaluator.api.executor.PMMLRuntime.class);
    }
}


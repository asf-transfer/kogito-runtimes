/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.codegen.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.drools.modelcompiler.builder.ModelSourceClass;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.kogito.codegen.api.GeneratedFile;
import org.kie.kogito.codegen.api.context.KogitoBuildContext;

import static java.util.stream.Collectors.toList;

public class KieSessionModelBuilder {

    private final Map<String, KieBaseModel> kieBaseModels;
    private final Map<String, List<String>> modelByKBase;
    private KogitoBuildContext context;

    public KieSessionModelBuilder(KogitoBuildContext context, Map<String, KieBaseModel> kieBaseModels, Collection<KogitoPackageSources> packageSources) {
        this.context = context;
        this.kieBaseModels = kieBaseModels;
        this.modelByKBase = getModelByKBase(packageSources, this.kieBaseModels);

    }

    List<GeneratedFile> generate() {

        ModelSourceClass modelSourceClass =
                new ModelSourceClass(DroolsModelBuilder.DUMMY_RELEASE_ID, kieBaseModels, modelByKBase);
        ProjectRuntimeGenerator projectRuntimeGenerator =
                new ProjectRuntimeGenerator(modelSourceClass.getModelMethod(), context);

        return List.of(
                new GeneratedFile(RuleCodegen.RULE_TYPE,
                        modelSourceClass.getName(), modelSourceClass.generate()),
                new GeneratedFile(RuleCodegen.RULE_TYPE,
                        projectRuntimeGenerator.getName(), projectRuntimeGenerator.generate()));

    }

    private Map<String, List<String>> getModelByKBase(Collection<KogitoPackageSources> packageSources, Map<String, KieBaseModel> kieBaseModels) {
        Map<String, String> modelsByPackage = getModelsByPackage(packageSources);
        Map<String, List<String>> modelsByKBase = new HashMap<>();
        for (Map.Entry<String, KieBaseModel> entry : kieBaseModels.entrySet()) {
            List<String> kieBasePackages = entry.getValue().getPackages();
            boolean isAllPackages = kieBasePackages.isEmpty() || (kieBasePackages.size() == 1 && kieBasePackages.get(0).equals("*"));
            modelsByKBase.put(entry.getKey(),
                    isAllPackages ? new ArrayList<>(modelsByPackage.values()) : kieBasePackages.stream().map(modelsByPackage::get).filter(Objects::nonNull).collect(toList()));
        }
        return modelsByKBase;
    }

    private Map<String, String> getModelsByPackage(Collection<KogitoPackageSources> packageSources) {
        Map<String, String> modelsByPackage = new HashMap<>();
        for (KogitoPackageSources pkgSources : packageSources) {
            modelsByPackage.put(pkgSources.getPackageName(), pkgSources.getPackageName() + "." + pkgSources.getRulesFileName());
        }
        return modelsByPackage;
    }

}

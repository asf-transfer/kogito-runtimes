/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.kogito.pmml;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.compiler.builder.impl.KnowledgeBuilderImpl;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.drools.model.Model;
import org.drools.modelcompiler.builder.KieBaseBuilder;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.kogito.prediction.PredictionRuleMapper;
import org.kie.pmml.commons.model.KiePMMLModel;
import org.kie.pmml.evaluator.api.container.PMMLPackage;
import org.kie.pmml.evaluator.api.executor.PMMLRuntime;
import org.kie.pmml.evaluator.assembler.container.PMMLPackageImpl;
import org.kie.pmml.evaluator.core.executor.PMMLModelEvaluatorFinderImpl;
import org.kie.pmml.evaluator.core.service.PMMLRuntimeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kie.pmml.evaluator.assembler.service.PMMLAssemblerService.getFactoryClassNamePackageName;
import static org.kie.pmml.evaluator.assembler.service.PMMLLoaderService.getKiePMMLModelsLoadedFromResource;

/**
 * Utility class to replace the <b>Assembler</b> mechanism where this is not available
 */
public class PMMLRuntimeBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PMMLRuntimeBuilder.class);

    public static Map<String, PMMLRuntime> fromResources(final List<Resource> resources,
                                                         final PMMLModelEvaluatorFinderImpl pmmlModelExecutorFinder) {
        Map<String, PMMLRuntime> toReturn = new HashMap<>();
        resources.forEach(resource -> {
            final String[] factoryClassNamePackageName = getFactoryClassNamePackageName(resource);
            final KnowledgeBuilderImpl kbuilderImpl = createKnowledgeBuilderImpl(resource);
            List<KiePMMLModel> toAdd = getKiePMMLModelsLoadedFromResource(kbuilderImpl, resource);
            if (toAdd.isEmpty()) {
                throw new RuntimeException("Failed to retrieve compiled models");
            }
            for (KiePMMLModel kiePMMLModel : toAdd) {
                final InternalKnowledgePackage internalKnowledgePackage =
                        kbuilderImpl.getKnowledgeBase().getPackage(factoryClassNamePackageName[1]);
                if (internalKnowledgePackage != null) {
                    PMMLPackage pmmlPkg = internalKnowledgePackage.getResourceTypePackages().computeIfAbsent(ResourceType.PMML, rtp -> new PMMLPackageImpl());
                    pmmlPkg.addAll(Collections.singletonList(kiePMMLModel));
                }
            }
            toReturn.put(resource.getSourcePath(), new PMMLRuntimeImpl(kbuilderImpl.getKnowledgeBase(), pmmlModelExecutorFinder));
        });
        return toReturn;
    }

    private static KnowledgeBuilderImpl createKnowledgeBuilderImpl(final Resource resource) {
        KnowledgeBaseImpl defaultKnowledgeBase = new KnowledgeBaseImpl("PMML", null);
        KnowledgeBuilderImpl toReturn = new KnowledgeBuilderImpl(defaultKnowledgeBase);
        PredictionRuleMapper pmmlRuleMapper = loadPMMLRuleMapper(toReturn.getRootClassLoader(), resource);
        if (pmmlRuleMapper != null) {
            String ruleName = pmmlRuleMapper.getRuleName();
            Model model = loadModel(toReturn.getRootClassLoader(), ruleName);
            toReturn = new KnowledgeBuilderImpl(KieBaseBuilder.createKieBaseFromModel(model));
        }
        return toReturn;
    }

    private static PredictionRuleMapper loadPMMLRuleMapper(final ClassLoader classLoader,
                                                           final Resource resource) {
        String[] classNamePackageName = getFactoryClassNamePackageName(resource);
        String packageName = classNamePackageName[1];
        String fullPMMLRuleMapperClassName = packageName + ".PredictionRuleMapperImpl";
        try {
            return (PredictionRuleMapper) classLoader.loadClass(fullPMMLRuleMapperClassName).newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | ClassCastException e) {
            logger.info(String.format("%s class not found in rootClassLoader", fullPMMLRuleMapperClassName));
            return null;
        }
    }

    private static Model loadModel(final ClassLoader classLoader, final String ruleName) {
        try {
            return (Model) classLoader.loadClass(ruleName).newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | ClassCastException e) {
            logger.info(String.format("%s class not found in rootClassLoader", ruleName));
            throw new RuntimeException(String.format("Failed to load or instantiate %s ", ruleName));
        }
    }
}

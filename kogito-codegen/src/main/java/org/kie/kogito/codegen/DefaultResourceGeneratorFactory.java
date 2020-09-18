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
package org.kie.kogito.codegen;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.kie.api.definition.process.WorkflowProcess;
import org.kie.kogito.codegen.process.AbstractResourceGenerator;
import org.kie.kogito.codegen.process.ReactiveResourceGenerator;
import org.kie.kogito.codegen.process.ResourceGenerator;

/**
 * This should be used to only create JAX-RS Resource Generators.
 * IMPORTANT: it will not consider Spring Generators.
 */
public class DefaultResourceGeneratorFactory extends ResourceGeneratorFactory {

    @Override
    public Optional<AbstractResourceGenerator> create(GeneratorContext context,
                                                      WorkflowProcess process,
                                                      String modelfqcn,
                                                      String processfqcn,
                                                      String appCanonicalName) {

        return GeneratorType
                .from(context)
                .map(type -> {
                    switch (type) {
                        case QUARKUS:
                        case SPRING:
                            return new ResourceGenerator(context,
                                                         process,
                                                         modelfqcn,
                                                         processfqcn,
                                                         appCanonicalName);
                        case QUARKUS_REACTIVE:
                        case SPRING_REACTIVE:
                            return new ReactiveResourceGenerator(context,
                                                                 process,
                                                                 modelfqcn,
                                                                 processfqcn,
                                                                 appCanonicalName);
                        default:
                            throw new NoSuchElementException("No Resource Generator for: " + type);
                    }
                });
    }
}
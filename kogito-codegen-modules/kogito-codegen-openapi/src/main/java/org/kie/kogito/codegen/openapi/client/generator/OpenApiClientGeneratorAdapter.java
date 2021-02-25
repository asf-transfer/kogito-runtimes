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
package org.kie.kogito.codegen.openapi.client.generator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kie.kogito.codegen.openapi.client.OpenApiSpecDescriptor;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.languages.JavaClientCodegen;

class OpenApiClientGeneratorAdapter extends JavaClientCodegen {

    private final DefaultGenerator generator;

    public OpenApiClientGeneratorAdapter(final DefaultGenerator generator) {
        this.generator = generator;
    }

    /**
     * Fills the required operations generated data in a given {@link OpenApiSpecDescriptor}.
     *
     * @param descriptor the resource with the required operations
     */
    public void processGeneratedOperations(final OpenApiSpecDescriptor descriptor) {
        Map<String, List<CodegenOperation>> paths = this.generator.processPaths(this.openAPI.getPaths());
        paths.forEach((api, operations) -> operations.forEach(operation -> descriptor.getRequiredOperations().stream()
                .filter(resourceOperation -> resourceOperation.getOperationId().equals(operation.operationId))
                .findFirst()
                .ifPresent(resourceOperation -> {
                    resourceOperation.setApi(api);
                    resourceOperation.setMethodName(operation.operationId);
                    resourceOperation.setGeneratedClass(this.apiPackage + "." + this.toApiName(api));
                    if (operation.hasParams) {
                        resourceOperation.setParameters(operation.allParams.stream().map(p -> p.paramName).collect(Collectors.toList()));
                    }
                })));
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        // TODO: open an issue on OpenApi project, this should be added by them
        objs.put(JavaClientCodegen.USE_RUNTIME_EXCEPTION, true);
        return super.postProcessSupportingFileData(objs);
    }
}

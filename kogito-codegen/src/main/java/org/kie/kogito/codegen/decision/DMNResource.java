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
package org.kie.kogito.codegen.decision;

import java.nio.file.Path;

import org.kie.dmn.api.core.DMNModel;
import org.kie.kogito.codegen.io.CollectedResource;

/**
 * A (DMNModel, CollectedResource) pair
 */
public class DMNResource {
    private final DMNModel dmnModel;
    private final CollectedResource collectedResource;

    public DMNResource(DMNModel dmnModel, CollectedResource collectedResource) {
        this.dmnModel = dmnModel;
        this.collectedResource = collectedResource;
    }

    public DMNModel getDmnModel() {
        return dmnModel;
    }

    public Path getPath() {
        return collectedResource.basePath();
    }

    public CollectedResource getCollectedResource() {
        return collectedResource;
    }
}

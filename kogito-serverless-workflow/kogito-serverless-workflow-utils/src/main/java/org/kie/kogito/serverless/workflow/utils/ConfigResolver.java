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
package org.kie.kogito.serverless.workflow.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface ConfigResolver {

    <T> Optional<T> getConfigProperty(String name, Class<T> clazz);

    Iterable<String> getPropertyNames();

    default Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        for (String name : getPropertyNames()) {
            map.put(name, getConfigProperty(name, Object.class).orElseThrow());
        }
        return map;
    }
}

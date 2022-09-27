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
package org.kie.kogito.event.impl;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonMarshallUtils {

    public static <T> T unmarshall(ObjectMapper mapper, Object input, Class<T> outputClass, Class<?>... parametrizedClasses) throws IOException {
        if (input == null) {
            return null;
        }
        if (outputClass.isAssignableFrom(input.getClass())) {
            return outputClass.cast(input);
        } else {
            final JavaType type = Objects.isNull(parametrizedClasses) ? mapper.getTypeFactory().constructType(outputClass)
                    : mapper.getTypeFactory().constructParametricType(outputClass, parametrizedClasses);

            if (input instanceof byte[]) {
                return mapper.readValue((byte[]) input, type);
            } else {
                return mapper.readValue(input.toString(), type);
            }
        }
    }
}

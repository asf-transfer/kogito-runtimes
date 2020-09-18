/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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
package org.drools.dynamic;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.drools.reflective.classloader.ProjectClassLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class ClassLoaderTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    public void testParallelClassLoading() {

        final Integer THREAD_COUNT = 100;

        final ClassLoader projectClassLoader = ProjectClassLoader.createProjectClassLoader();
        final ClassLoader internalTypesClassLoader = (ClassLoader) ((ProjectClassLoader) projectClassLoader).makeClassLoader();

        ((ProjectClassLoader) projectClassLoader).setInternalClassLoader((ProjectClassLoader.InternalTypesClassLoader) internalTypesClassLoader);

        final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            final List<Future> futures = new ArrayList<>();

            for (int i = 0; i < THREAD_COUNT; i++) {
                if (i % 2 == 0) {
                    futures.add(executorService.submit(() -> {
                        try {
                            Class.forName("nonexistant", true, projectClassLoader);
                        } catch (ClassNotFoundException e) {
                            //
                        }
                    }));
                } else {
                    futures.add(executorService.submit(() -> {
                        try {
                            Class.forName("nonexistant", true, internalTypesClassLoader);
                        } catch (ClassNotFoundException e) {
                            //
                        }
                    }));
                }
            }

            for (int i = 1; i <= THREAD_COUNT; i++) {
                final int threadId = i - 1;
                assertTimeoutPreemptively(TIMEOUT, () -> {
                    try {
                        futures.get(threadId).get();
                    } catch (final InterruptedException | ExecutionException e) {
                        // Nothing
                    }
                }, "Thread " + threadId + " did not finish in time.");
            }
        } finally {
            executorService.shutdownNow();
        }
    }

}

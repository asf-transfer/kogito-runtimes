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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.kie.kogito.Model;
import org.kie.kogito.correlation.CorrelationKeyResolver;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessEventDispatcher<M extends Model> {

    private CorrelationKeyResolver kogitoReferenceCorrelationResolver = new KogitoReferenceCorrelationResolver();
    private CorrelationKeyResolver eventTypeResolver = new SimpleAttributeCorrelationResolver("type");
    private CorrelationKeyResolver dataResolver = new SimpleAttributeCorrelationResolver("data");

    private ProcessService processService;
    private Function<Object, M> modelConverter;
    private Process<M> process;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEventDispatcher.class);
    private ExecutorService executor;

    public ProcessEventDispatcher(Process<M> process, Function<Object, M> modelConverter, ProcessService processService) {
        this.process = process;
        this.modelConverter = modelConverter;
        this.processService = processService;
        this.executor = Executors.newCachedThreadPool();//todo
    }

    public CompletableFuture<?> dispatch(String trigger, Object event) {
        if (ignoredMessageType(trigger, event)) {
            LOGGER.warn("Ignoring message for trigger '{}',  event '{}'",
                    trigger,
                    event);
            return CompletableFuture.completedFuture(null);
        }

        if (ignoredMessageType(trigger, event)) {
            return CompletableFuture.completedFuture(null);
        }

        String kogitoReferenceId = String.valueOf(kogitoReferenceCorrelationResolver.resolve(event).getValue());
        if (kogitoReferenceId != null && !kogitoReferenceId.isEmpty()) {
            LOGGER.debug("Received message with reference id '{}' going to use it to send signal '{}'",
                    kogitoReferenceId,
                    trigger);
            return CompletableFuture.supplyAsync(() -> {
                Optional<ProcessInstance<M>> instance = process.instances().findById(kogitoReferenceId);
                if (instance.isPresent()) {
                    return signalProcessInstance(trigger, kogitoReferenceId, event);
                } else {
                    LOGGER.info("Process instance with id '{}' not found for triggering signal '{}', starting a new one",
                            kogitoReferenceId,
                            trigger);
                    return startNewInstance(trigger, event);
                }
            }, executor);
        } else {
            LOGGER.debug("Received message without reference id, starting new process instance with trigger '{}'", trigger);
            return CompletableFuture.supplyAsync(() -> startNewInstance(trigger, event), executor);
        }

        //Extract correlation information

        //Existing target instance to send the event

        //Creating a new instance to the given trigger
    }

    private Optional signalProcessInstance(String trigger, String id, Object event) {
        return processService.signalProcessInstance((Process) process, id, dataResolver.resolve(event).getValue(), "Message-" + trigger);
    }

    private ProcessInstance<M> startNewInstance(String trigger, Object event) {
        String businessKey = null;
        String fromNode = null;
        String referenceId = null; //keep a reference with the caller starting the process instance
        Object data = dataResolver.resolve(event).getValue();
        return processService.createProcessInstance(process, businessKey, modelConverter.apply(data), fromNode, trigger, referenceId);
    }

    private boolean ignoredMessageType(String trigger, Object event) {
        String eventType = eventTypeResolver.resolve(event).asString();//todo get from event
        String source = null;//todo get from event
        return !trigger.equals(eventType) && !event.getClass().getSimpleName().equals(source);
    }
}

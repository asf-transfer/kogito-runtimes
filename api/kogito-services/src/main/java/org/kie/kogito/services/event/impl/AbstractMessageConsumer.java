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
package org.kie.kogito.services.event.impl;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import org.kie.kogito.Application;
import org.kie.kogito.Model;
import org.kie.kogito.event.*;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessService;
import org.kie.kogito.services.event.ProcessDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMessageConsumer<M extends Model, D> {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractMessageConsumer.class);

    private Process<M> process;
    private Application application;
    private String trigger;
    private EventConsumer<M, Object> eventConsumer;

    // in general we should favor the non-empty constructor
    // but there is an issue with Quarkus https://github.com/quarkusio/quarkus/issues/2949#issuecomment-513017781
    // use this in conjuction with setParams()
    public AbstractMessageConsumer() {
    }

    public AbstractMessageConsumer(Application application,
            Process<M> process,
            String trigger,
            EventConsumerFactory eventConsumerFactory,
            EventReceiver eventReceiver,
            Class<D> dataClass,
            boolean useCloudEvents,
            ProcessService processService,
            ExecutorService executorService,
            EventUnmarshaller<Object> eventUnmarshaller) {
        init(application, process, trigger, eventConsumerFactory, eventReceiver, dataClass, useCloudEvents, processService, executorService, eventUnmarshaller);
    }

    public void init(Application application,
            Process<M> process,
            String trigger,
            EventConsumerFactory eventConsumerFactory,
            EventReceiver eventReceiver,
            Class<D> dataClass,
            boolean useCloudEvents,
            ProcessService processService,
            ExecutorService executorService,
            EventUnmarshaller<Object> eventUnmarshaller) {
        this.process = process;
        this.application = application;
        this.trigger = trigger;
        this.eventConsumer = (EventConsumer<M, Object>) eventConsumerFactory.get(processService, executorService, getModelConverter(), useCloudEvents, this::getData);

        if (useCloudEvents) {
            eventReceiver.subscribe(this::consume,
                    SubscriptionInfo.builder().converter(eventUnmarshaller).outputClass(ProcessDataEvent.class).parametrizedClasses(dataClass).type(trigger).createSubscriptionInfo());
        } else {
            eventReceiver.subscribe(this::consume, SubscriptionInfo.builder().converter(eventUnmarshaller).outputClass(dataClass).type(trigger).createSubscriptionInfo());
        }
        logger.info("Consumer for {} started", trigger);
    }

    private CompletionStage<?> consume(Object payload) {
        logger.trace("Received {} for trigger {}", payload, trigger);
        return eventConsumer.consume(application, process, payload, trigger)
                .thenAccept(v -> logger.trace("Consume completed {} for trigger {}", payload, trigger));
    }

    protected Optional<Function<D, M>> getModelConverter() {
        return Optional.empty();
    }

    protected D getData(DataEvent<D> cloudEvent) {
        return cloudEvent.getData();
    }
}

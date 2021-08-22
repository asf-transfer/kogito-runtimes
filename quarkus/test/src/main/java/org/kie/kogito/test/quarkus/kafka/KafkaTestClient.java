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
package org.kie.kogito.test.quarkus.kafka;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka client for Kogito Example tests.
 */
public class KafkaTestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaTestClient.class);

    private final KafkaProducer<String, String> producer;
    private final KafkaConsumer<String, String> consumer;
    private final Object shutdownLock = new Object();
    private final CountDownLatch latch = new CountDownLatch(1);
    private boolean shutdown = false;

    public KafkaTestClient(String hosts) {
        this(createDefaultProducer(hosts), createDefaultConsumer(hosts));
    }

    public KafkaTestClient(KafkaProducer<String, String> producer, KafkaConsumer<String, String> consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }

    private static KafkaConsumer<String, String> createDefaultConsumer(String hosts) {
        Properties consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, hosts);
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, KafkaTestClient.class.getName() + "Consumer");
        return new KafkaConsumer<>(consumerConfig);
    }

    private static KafkaProducer<String, String> createDefaultProducer(String hosts) {
        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, hosts);
        producerConfig.put(ProducerConfig.CLIENT_ID_CONFIG, KafkaTestClient.class.getName() + "Producer");
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(producerConfig);
    }

    public void consume(Collection<String> topics, Consumer<String> callback) {
        CompletableFuture.runAsync(() -> {
            consumer.subscribe(topics);
            while (!shutdown) {
                synchronized (shutdownLock) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

                    StreamSupport.stream(records.spliterator(), true)
                            .map(ConsumerRecord::value)
                            .forEach(callback::accept);

                    consumer.commitSync();
                }
            }
        });
    }

    public void consume(String topic, Consumer<String> callback) {
        consume(Collections.singletonList(topic), callback);
    }

    public void produce(String data, String topic) {
        LOGGER.info("Publishing event with data {} for topic {}", data, topic);
        producer.send(new ProducerRecord<>(topic, data), this::produceCallback);
    }

    public void produceCallback(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            LOGGER.error("Event publishing failed", exception);
        } else {
            LOGGER.info("Event published {}", metadata);
        }
    }

    public void shutdown() {
        shutdown = true;
        latch.countDown();
        synchronized (shutdownLock) {
            consumer.unsubscribe();
            consumer.close();
        }

        producer.close();
    }
}

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
package org.kie.kogito.persistence.postgresql;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.drools.core.util.IoUtils;
import org.infinispan.protostream.BaseMarshaller;
import org.kie.kogito.persistence.protobuf.ProtoStreamObjectMarshallingStrategy;
import org.kie.kogito.process.MutableProcessInstances;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstanceReadMode;
import org.kie.kogito.process.impl.AbstractProcessInstance;
import org.kie.kogito.process.impl.marshalling.ProcessInstanceMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@SuppressWarnings({ "rawtypes" })
public class PostgreProcessInstances implements MutableProcessInstances {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreProcessInstances.class);
    private static final int TIMEOUT_IN_SECONDS = 10;

    private final Process<?> process;
    private final PgPool client;
    private final ProcessInstanceMarshaller marshaller;
    private final boolean autoDDL;

    public PostgreProcessInstances(Process<?> process, PgPool client, boolean autoDDL, String proto,
            BaseMarshaller<?>... marshallers) {
        this(process, client, autoDDL, new ProcessInstanceMarshaller(new ProtoStreamObjectMarshallingStrategy(proto,
                marshallers)));
    }

    public PostgreProcessInstances(Process<?> process, PgPool client, boolean autoDDL,
            ProcessInstanceMarshaller marshaller) {
        this.process = process;
        this.client = client;
        this.autoDDL = autoDDL;
        this.marshaller = marshaller;
        init();
    }

    @Override
    public boolean exists(String id) {
        return findById(id).isPresent();
    }

    @Override
    public void create(String id, ProcessInstance instance) {
        insertInternal(UUID.fromString(id), marshaller.marshallProcessInstance(instance));
        disconnect(instance);
    }

    @Override
    public void update(String id, ProcessInstance instance) {
        updateInternal(UUID.fromString(id), marshaller.marshallProcessInstance(instance));
        disconnect(instance);
    }

    @Override
    public void remove(String id) {
        deleteInternal(UUID.fromString(id));
    }

    @Override
    public Optional<ProcessInstance> findById(String id, ProcessInstanceReadMode mode) {
        return findByIdInternal(UUID.fromString(id)).map(b -> marshaller.unmarshallProcessInstance(b, process));
    }

    @Override
    public Collection<ProcessInstance> values(ProcessInstanceReadMode mode) {
        return findAllInternal().stream().map(i -> marshaller.unmarshallProcessInstance(i, process)).collect(Collectors.toList());
    }

    @Override
    public Integer size() {
        return countInternal().intValue();
    }

    private void disconnect(ProcessInstance instance) {
        ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {
            try {
                byte[] reloaded = findByIdInternal(UUID.fromString(instance.id())).get();
                return marshaller.unmarshallWorkflowProcessInstance(reloaded, process);
            } catch (RuntimeException e) {
                LOGGER.error("Unexpected exception thrown when reloading process instance {}", instance.id(), e);
                return null;
            }
        });
    }

    private boolean insertInternal(UUID id, byte[] payload) {
        try {
            final CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();
            client.preparedQuery("INSERT INTO process_instances (id, payload, process_id) VALUES ($1, $2, $3)")
                    .execute(Tuple.of(id, Buffer.buffer(payload), process.id()), getAsyncResultHandler(future));
            RowSet<Row> rows = getResultFromFuture(future);
            return rows.rowCount() == 1;
        } catch (Exception e) {
            LOGGER.error("Error inserting process instance {}", id, e);
            throw new RuntimeException(e);
        }
    }

    private Handler<AsyncResult<RowSet<Row>>> getAsyncResultHandler(CompletableFuture<RowSet<Row>> future) {
        return ar -> {
            if (ar.succeeded()) {
                future.complete(ar.result());
            } else {
                future.completeExceptionally(ar.cause());
            }
        };
    }

    private boolean updateInternal(UUID id, byte[] payload) {
        try {
            final CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();
            client.preparedQuery("UPDATE process_instances SET payload = $1 WHERE id = $2)")
                    .execute(Tuple.of(Buffer.buffer(payload), id), getAsyncResultHandler(future));
            RowSet<Row> rows = getResultFromFuture(future);
            return rows.rowCount() == 1;
        } catch (Exception e) {
            LOGGER.error("Error updating process instance {}", id, e);
            throw new RuntimeException(e);
        }
    }

    private boolean deleteInternal(UUID id) {
        try {
            final CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();
            client.preparedQuery("DELETE FROM process_instances WHERE id = $1")
                    .execute(Tuple.of(id), getAsyncResultHandler(future));
            RowSet<Row> rows = getResultFromFuture(future);
            return rows.rowCount() == 1;
        } catch (Exception e) {
            LOGGER.error("Error deleting process instance {}", id, e);
            throw new RuntimeException(e);
        }
    }

    private RowSet<Row> getResultFromFuture(CompletableFuture<RowSet<Row>> future) throws InterruptedException,
            ExecutionException, TimeoutException {
        return future.get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    private Optional<byte[]> findByIdInternal(UUID id) {
        try {
            final CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();
            client.preparedQuery("SELECT payload FROM process_instances WHERE id = $1")
                    .execute(Tuple.of(id), getAsyncResultHandler(future));
            RowSet<Row> rows = getResultFromFuture(future);
            return Optional.ofNullable(rows.iterator())
                    .filter(Iterator::hasNext)
                    .map(Iterator::next)
                    .map(row -> row.getBuffer("payload"))
                    .map(Buffer::getBytes);
        } catch (Exception e) {
            LOGGER.error("Error finding process instance {}", id, e);
            throw new RuntimeException(e);
        }
    }

    private List<byte[]> findAllInternal() {
        try {
            final CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();
            client.preparedQuery("SELECT payload FROM process_instances WHERE process_id = $1")
                    .execute(Tuple.of(process.id()), getAsyncResultHandler(future));
            RowSet<Row> rows = getResultFromFuture(future);
            return StreamSupport.stream(rows.spliterator(), false)
                    .map(row -> row.getBuffer("payload"))
                    .map(Buffer::getBytes)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Error finding all process instances", e);
            throw new RuntimeException(e);
        }
    }

    private Long countInternal() {
        try {
            final CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();
            client.preparedQuery("SELECT COUNT(id) FROM process_instances WHERE process_id = $1")
                    .execute(Tuple.of(process.id()), getAsyncResultHandler(future));
            RowSet<Row> rows = getResultFromFuture(future);
            return rows.iterator().next().getLong("count");
        } catch (Exception e) {
            LOGGER.error("Error counting process instances", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Try to create the table using the same application user, this should not be necessary since the database
     * is recommended to be configured properly before starting the application.
     *
     * Note:
     * This method could be useful for development and testing purposes and does not break the execution flow,
     * throwing any exception.
     * This is only executed in case the configuration for auto DDL is enabled.
     */
    private void init() {
        if (!autoDDL) {
            LOGGER.debug("Auto DDL is disabled, do not running initializer scripts");
            return;
        }

        try {
            final CompletableFuture<RowSet<Row>> future = new CompletableFuture<>();
            client.query(getQueryFromFile("exists_tables"))
                    .execute(getAsyncResultHandler(future));
            final CompletableFuture<RowSet<Row>> futureCompose = future.thenCompose(rows -> {
                final CompletableFuture<RowSet<Row>> futureCreate = new CompletableFuture<>();
                return Optional.ofNullable(rows.iterator())
                        .filter(Iterator::hasNext)
                        .map(Iterator::next)
                        .map(row -> row.getBoolean("exists"))
                        .filter(Boolean.FALSE::equals)
                        .map(e -> client.query(getQueryFromFile("create_tables")))
                        .map(q -> {
                            q.execute(getAsyncResultHandler(futureCreate));
                            LOGGER.info("Creating process_instances table.");
                            return futureCreate;
                        })
                        .orElseGet(() -> {
                            futureCreate.complete(null);
                            LOGGER.info("Table process_instances already exists.");
                            return futureCreate;
                        });
            });
            RowSet<Row> rows = getResultFromFuture(futureCompose);
            if (Objects.nonNull(rows) && rows.rowCount() == 1) {
                LOGGER.info("DDL successfully done for ProcessInstance");
            } else {
                LOGGER.info("DDL executed with no changes for ProcessInstance");
            }
        } catch (Exception e) {
            //not break the execution flow in case of any missing permission for db application user, for instance.
            LOGGER.error("Error creating process_instances table, the database should be configured properly before " +
                    "starting the application", e);
        }
    }

    private String getQueryFromFile(String scriptName) {
        try {
            return new String(IoUtils.readBytesFromInputStream(
                    Thread.currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream(String.format("sql/%s.sql", scriptName))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

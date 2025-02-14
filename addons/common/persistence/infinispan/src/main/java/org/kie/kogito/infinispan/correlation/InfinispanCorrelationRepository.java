package org.kie.kogito.infinispan.correlation;

import java.io.UncheckedIOException;
import java.util.Optional;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.kie.kogito.correlation.CompositeCorrelation;
import org.kie.kogito.correlation.Correlation;
import org.kie.kogito.correlation.CorrelationInstance;
import org.kie.kogito.correlation.SimpleCorrelation;
import org.kie.kogito.internal.utils.ConversionUtils;
import org.kie.kogito.jackson.utils.ObjectMapperFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class InfinispanCorrelationRepository {

    public static final String CORRELATIONS_CACHE_NAME = "correlations";

    // TODO: I would like to use <String, CorrelationType>
    private final RemoteCache<String, String> cache;
    private final ObjectMapper objectMapper;

    public InfinispanCorrelationRepository(
            RemoteCacheManager cacheManager,
            String templateName) {
        if (ConversionUtils.isEmpty(templateName)) {
            this.cache = cacheManager.administration().getOrCreateCache(CORRELATIONS_CACHE_NAME, DefaultTemplate.LOCAL);
        } else {
            this.cache = cacheManager.administration().getOrCreateCache(CORRELATIONS_CACHE_NAME, templateName);
        }
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addAbstractTypeMapping(Correlation.class, SimpleCorrelation.class);
        this.objectMapper = ObjectMapperFactory.get().copy().registerModule(simpleModule);
    }

    public CorrelationInstance insert(String encodedCorrelationId, String correlatedId, Correlation correlation) {
        CorrelationInstance correlationInstance = new CorrelationInstance(encodedCorrelationId, correlatedId, correlation);

        try {
//            TODO: I would like to remove 2 calls for writeValueAsString
            String json = this.objectMapper.writeValueAsString(correlation);
            CorrelationType correlationType = new CorrelationType(
                    encodedCorrelationId, correlatedId, json);
            String value = this.objectMapper.writeValueAsString(correlationType);
            this.cache.put(encodedCorrelationId, value);
            return correlationInstance;
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public CorrelationInstance findByEncodedCorrelationId(String encodedCorrelationId) {
        String json = this.cache.get(encodedCorrelationId);
        if (json == null) {
            return null;
        }
        try {
            CorrelationType correlation = this.objectMapper.readValue(json, CorrelationType.class);
            CompositeCorrelation compositeCorrelation = this.objectMapper.readValue(correlation.correlation(), CompositeCorrelation.class);
            return new CorrelationInstance(
                    correlation.encodedCorrelationId(),
                    correlation.correlatedId(),
                    compositeCorrelation);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public CorrelationInstance findByCorrelatedId(String correlatedId) {

        Optional<String> first = this.cache.values().stream().filter(item -> item.contains("\"correlatedId\":\"%s\"".formatted(correlatedId)))
                .findFirst();
        if (first.isEmpty()) {
            return null;
        }

        try {
            CorrelationType correlationType = this.objectMapper.readValue(first.get(), CorrelationType.class);
            CompositeCorrelation compositeCorrelation = this.objectMapper.readValue(correlationType.correlation(), CompositeCorrelation.class);
            return new CorrelationInstance(
                    correlationType.encodedCorrelationId(),
                    correlationType.correlatedId(),
                    compositeCorrelation);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void delete(String encodedCorrelationId) {
        this.cache.remove(encodedCorrelationId);
    }
}

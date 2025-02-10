package org.kie.kogito.infinispan.correlation;

import java.util.Collections;
import java.util.Optional;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.kogito.correlation.CompositeCorrelation;
import org.kie.kogito.correlation.CorrelationInstance;
import org.kie.kogito.correlation.CorrelationService;
import org.kie.kogito.correlation.SimpleCorrelation;
import org.kie.kogito.testcontainers.KogitoInfinispanContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Testcontainers
class InfinispanCorrelationServiceIT {

    @Container
    final static KogitoInfinispanContainer inifinispanContainer = new KogitoInfinispanContainer();
    private static RemoteCacheManager remoteCacheManager;

    @BeforeAll
    static void setup() {
        inifinispanContainer.start();
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addServer()
                .host("127.0.0.1")
                .port(inifinispanContainer.getMappedPort());
        remoteCacheManager = new RemoteCacheManager(configurationBuilder.build());
    }

    @AfterEach
    void tearDown() {
        remoteCacheManager.getCache(InfinispanCorrelationRepository.CORRELATIONS_CACHE_NAME)
                .clear();
    }

    @Test
    void shouldSaveGetCorrelationCorrectly() {
        // arrange
        InfinispanCorrelationRepository repository = new InfinispanCorrelationRepository(remoteCacheManager, null);
        CorrelationService sut = new InfinispanCorrelationService(repository);
        String correlatedId = "id";
        CompositeCorrelation correlation = new CompositeCorrelation(Collections.singleton(new SimpleCorrelation<>("city", "Rio de Janeiro")));

        // act
        sut.create(correlation, correlatedId);

        // assert
        long quantity = remoteCacheManager.getCache(InfinispanCorrelationRepository.CORRELATIONS_CACHE_NAME)
                .keySet()
                .size();

        remoteCacheManager.getCache(InfinispanCorrelationRepository.CORRELATIONS_CACHE_NAME)
                .forEach((key, value) -> System.out.println(value));

        assertThat(quantity).isPositive();
    }

    @Test
    void shouldDeleteGetCorrelation() {
        // arrange
        String correlatedId = "id";
        CompositeCorrelation correlation = new CompositeCorrelation(Collections.singleton(new SimpleCorrelation<>("city", "São Paulo")));
        InfinispanCorrelationRepository repository = new InfinispanCorrelationRepository(remoteCacheManager, null);
        CorrelationService sut = new InfinispanCorrelationService(repository);
        sut.create(correlation, correlatedId);

        // act
        sut.delete(correlation);

        // assert
        assertThat(remoteCacheManager.getCache(InfinispanCorrelationRepository.CORRELATIONS_CACHE_NAME).size()).isZero();
    }

    @Test
    void shouldFindByGetCorrelatedId() {
        // arrange
        String correlatedId = "id";
        InfinispanCorrelationRepository repository = new InfinispanCorrelationRepository(remoteCacheManager, null);
        InfinispanCorrelationService sut = new InfinispanCorrelationService(repository);
        CompositeCorrelation correlation = new CompositeCorrelation(Collections.singleton(new SimpleCorrelation<>("city", "São Paulo")));
        sut.create(correlation, correlatedId);

        // act
        Optional<CorrelationInstance> byCorrelatedId = sut.findByCorrelatedId(correlatedId);

        // assert
        assertThat(byCorrelatedId.isPresent()).isTrue();
    }

    @Test
    void shouldFindByCorrelation() {
        // arrange
        CompositeCorrelation correlation = new CompositeCorrelation(Collections.singleton(new SimpleCorrelation<>("city", "Osasco")));
        String correlatedId = "id";
        InfinispanCorrelationRepository repository = new InfinispanCorrelationRepository(remoteCacheManager, null);
        InfinispanCorrelationService sut = new InfinispanCorrelationService(repository);
        sut.create(correlation, correlatedId);

        // act
        Optional<CorrelationInstance> correlationInstance = sut.find(correlation);

        // assert
        assertThat(correlationInstance.isPresent()).isTrue();


    }

}
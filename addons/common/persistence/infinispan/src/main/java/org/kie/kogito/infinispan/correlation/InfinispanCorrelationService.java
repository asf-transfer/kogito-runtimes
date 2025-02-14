package org.kie.kogito.infinispan.correlation;

import java.util.Optional;

import org.kie.kogito.correlation.Correlation;
import org.kie.kogito.correlation.CorrelationEncoder;
import org.kie.kogito.correlation.CorrelationInstance;
import org.kie.kogito.correlation.CorrelationService;
import org.kie.kogito.event.correlation.MD5CorrelationEncoder;

public class InfinispanCorrelationService implements CorrelationService {

    private final CorrelationEncoder encoder;

    private final InfinispanCorrelationRepository correlationRepository;

    public InfinispanCorrelationService(InfinispanCorrelationRepository correlationRepository) {
        this.correlationRepository = correlationRepository;
        this.encoder = new MD5CorrelationEncoder();

    }

    @Override
    public CorrelationInstance create(Correlation correlation, String correlatedId) {
        String encodedCorrelationId = this.encoder.encode(correlation);
        return this.correlationRepository.insert(
                encodedCorrelationId, correlatedId, correlation);
    }

    @Override
    public Optional<CorrelationInstance> find(Correlation correlation) {
        String encodedCorrelationId = this.encoder.encode(correlation);
        return Optional.ofNullable(this.correlationRepository.findByEncodedCorrelationId(encodedCorrelationId));
    }

    @Override
    public Optional<CorrelationInstance> findByCorrelatedId(String correlatedId) {
        return Optional.ofNullable(this.correlationRepository.findByCorrelatedId(correlatedId));
    }

    @Override
    public void delete(Correlation correlation) {
        String encodedCorrelationId = this.encoder.encode(correlation);
        this.correlationRepository.delete(encodedCorrelationId);
    }
}

package org.kie.kogito.infinispan.correlation;

public record CorrelationType(String encodedCorrelationId, String correlatedId,
                              String correlation) {

    @Override
    public String toString() {
        return "CorrelationType[" +
                "encodedCorrelationId=" + encodedCorrelationId + ", " +
                "correlatedId=" + correlatedId + ", " +
                "correlation=" + correlation + ']';
    }

}

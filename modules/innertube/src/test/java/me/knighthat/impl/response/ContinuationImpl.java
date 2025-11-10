package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.innertube.response.Continuation;

@Data
public class ContinuationImpl implements Continuation {

    private final DataImpl nextContinuationData;
    private final DataImpl nextRadioContinuationData;

    @lombok.Data
    public static class DataImpl implements Data {

        private final String continuation;
        private final String clickTrackingParams;
    }
}

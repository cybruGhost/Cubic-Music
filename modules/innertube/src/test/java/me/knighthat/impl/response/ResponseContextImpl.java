package me.knighthat.impl.response;

import java.util.List;

import lombok.Data;
import me.knighthat.innertube.response.ResponseContext;

@Data
public class ResponseContextImpl implements ResponseContext {

    private final String visitorData;
    private final List<ServiceTrackingParamImpl> serviceTrackingParams;
    private final Integer maxAgeSeconds;

    @Data
    public static class ServiceTrackingParamImpl implements ServiceTrackingParam {

        private final String service;
        private final List<ParamImpl> params;

        @Data
        public static class ParamImpl implements Param {

            private final String key;
            private final String value;
        }
    }
}
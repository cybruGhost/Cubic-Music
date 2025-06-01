package me.knighthat.innertube.response;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ResponseContext {

    @NotNull
    String getVisitorData();

    @NotNull
    List<? extends ServiceTrackingParam> getServiceTrackingParams();

    Integer getMaxAgeSeconds();

    interface ServiceTrackingParam {

        @NotNull
        String getService();

        @NotNull
        List<? extends Param> getParams();

        interface Param {

            @NotNull
            String getKey();

            @NotNull
            String getValue();
        }
    }
}
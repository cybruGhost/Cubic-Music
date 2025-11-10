package me.knighthat.innertube.response;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Trackable {

    @NotNull
    String getTrackingParams();
}

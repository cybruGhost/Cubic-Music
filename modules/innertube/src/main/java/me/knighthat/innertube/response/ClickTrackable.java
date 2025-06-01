package me.knighthat.innertube.response;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ClickTrackable {

    @NotNull
    String getClickTrackingParams();
}

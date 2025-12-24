package me.knighthat.innertube.response;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Runs {

    @NotNull
    List<? extends Run> getRuns();

    Accessibility getAccessibility();

    interface Run {

        Boolean getBold();

        String getText();

        Endpoint getNavigationEndpoint();
    }
}
package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.innertube.response.Runs;

import java.util.List;

@Data
public class RunsImpl implements Runs {

    private final List<RunImpl> runs;
    private final AccessibilityImpl accessibility;

    @Data
    public static class RunImpl implements Run {

        private final Boolean bold;
        private final String text;
        private final EndpointImpl navigationEndpoint;
    }
}
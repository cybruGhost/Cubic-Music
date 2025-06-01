package me.knighthat.impl.response.renderer;

import lombok.Data;
import me.knighthat.impl.response.AccessibilityImpl;
import me.knighthat.impl.response.EndpointImpl;
import me.knighthat.impl.response.IconImpl;
import me.knighthat.impl.response.RunsImpl;
import me.knighthat.innertube.response.renderer.ChipCloud;

import java.util.List;

@Data
public class ChipCloudImpl implements ChipCloud {

    private final List<ChipImpl> chips;
    private final String            trackingParams;
    private final Boolean horizontalScrollable;

    @Data
    public static class ChipImpl implements Chip {

        private final RendererImpl chipCloudChipRenderer;

        @Data
        public static class RendererImpl implements Renderer {

            private final StyleImpl         style;
            private final RunsImpl          text;
            private final EndpointImpl      navigationEndpoint;
            private final String            trackingParams;
            private final IconImpl          icon;
            private final AccessibilityImpl accessibilityData;
            private final Boolean           isSelected;
            private final EndpointImpl          onDeselectedCommand;
            private final String            uniqueId;

            @Data
            public static class StyleImpl implements Style {

                private final String styleType;
            }
        }
    }
}
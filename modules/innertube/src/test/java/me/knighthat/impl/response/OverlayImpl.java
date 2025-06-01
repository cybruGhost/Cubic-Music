package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.innertube.response.Overlay;

import java.util.List;

@Data
public class OverlayImpl implements Overlay {

    private final MusicItemThumbnailOverlayRendererImpl musicItemThumbnailOverlayRenderer;

    @Data
    public static class MusicItemThumbnailOverlayRendererImpl implements MusicItemThumbnailOverlayRenderer {

        private final BackgroundImpl background;
        private final ContentImpl    content;
        private final String         contentPosition;
        private final String displayStyle;

        @Data
        public static class BackgroundImpl implements Background {

            private final VerticalGradientImpl verticalGradient;

            @Data
            public static class VerticalGradientImpl implements VerticalGradient {

                private final List<String> gradientLayerColors;
            }
        }

        @Data
        public static class ContentImpl implements Content {

            private final ButtonImpl.MusicPlayImpl musicPlayButtonRenderer;
        }
    }
}
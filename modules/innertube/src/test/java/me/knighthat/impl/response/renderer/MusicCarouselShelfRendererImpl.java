package me.knighthat.impl.response.renderer;

import lombok.Data;
import me.knighthat.impl.response.AccessibilityImpl;
import me.knighthat.impl.response.ButtonImpl;
import me.knighthat.impl.response.RunsImpl;
import me.knighthat.innertube.response.renderer.MusicCarouselShelfRenderer;

import java.util.List;

@Data
public class MusicCarouselShelfRendererImpl implements MusicCarouselShelfRenderer {

    private final HeaderImpl        header;
    private final List<ContentImpl> contents;
    private final String            trackingParams;
    private final String itemSize;
    private final String numItemsPerColumn;

    @Data
    public static class HeaderImpl implements Header {

        private final MusicCarouselShelfBasicHeaderRendererImpl musicCarouselShelfBasicHeaderRenderer;

        @Data
        public static class MusicCarouselShelfBasicHeaderRendererImpl implements MusicCarouselShelfBasicHeaderRenderer {

            private final RunsImpl          title;
            private final RunsImpl strapline;
            private final AccessibilityImpl accessibilityData;
            private final String     headerStyle;
            private final ButtonImpl moreContentButton;
            private final String     trackingParams;
        }
    }

    @Data
    public static class ContentImpl implements Content {

        private final MusicResponsiveListItemRendererImpl musicResponsiveListItemRenderer;
        private final MusicTwoRowItemRendererImpl         musicTwoRowItemRenderer;
    }
}
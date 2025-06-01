package me.knighthat.impl.response.renderer;

import lombok.Data;
import me.knighthat.impl.response.EndpointImpl;
import me.knighthat.impl.response.RunsImpl;
import me.knighthat.innertube.response.renderer.MusicShelfRenderer;

import java.util.List;

@Data
public class MusicShelfRendererImpl implements MusicShelfRenderer {

    private final RunsImpl          title;
    private final List<ContentImpl> contents;
    private final String            trackingParams;
    private final RunsImpl         bottomText;
    private final EndpointImpl     bottomEndpoint;
    private final ShelfDividerImpl shelfDivider;
    private final Boolean          contentsMultiSelectable;

    @Data
    public static class ContentImpl implements Content {

        private final MusicResponsiveListItemRendererImpl musicResponsiveListItemRenderer;
    }

    @Data
    public static class ShelfDividerImpl implements ShelfDivider {

        private final RendererImpl musicShelfDividerRenderer;

        @Data
        public static class RendererImpl implements Renderer {

            private final Boolean hidden;
        }
    }
}
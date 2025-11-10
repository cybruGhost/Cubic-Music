package me.knighthat.impl.response.renderer;

import lombok.Data;
import me.knighthat.impl.response.RunsImpl;
import me.knighthat.impl.response.thumbnail.ThumbnailImpl;
import me.knighthat.innertube.response.renderer.MusicCardShelfRenderer;

import java.util.List;

@Data
public class MusicCardShelfRendererImpl implements MusicCardShelfRenderer {

    private final String        trackingParams;
    private final ThumbnailImpl thumbnail;
    private final RunsImpl      title;
    private final RunsImpl      subtitle;
    private final List<ContentImpl> contents;

    @Data
    public static class ContentImpl implements Content {

        private final MessageRendererImpl                 messageRenderer;
        private final MusicResponsiveListItemRendererImpl musicResponsiveListItemRenderer;

        @Data
        public static class MessageRendererImpl implements MessageRenderer {

            private final RunsImpl text;
            private final String    trackingParams;
            private final StyleImpl style;

            @Data
            public static class StyleImpl implements Style {

                private final String value;
            }
        }
    }
}
package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.impl.response.thumbnail.ThumbnailsImpl;
import me.knighthat.innertube.response.NextResponse;

@Data
public class NextResponseImpl implements NextResponse {

    private final ResponseContextImpl responseContext;
    private final ContentsImpl        contents;
    private final EndpointImpl        currentVideoEndpoint;
    private final String             trackingParams;
    private final PlayerOverlaysImpl playerOverlays;

    @Data
    public static class ContentsImpl implements Contents {

        private final SingleColumnMusicWatchNextResultsRendererImpl singleColumnMusicWatchNextResultsRenderer;

        @Data
        public static class SingleColumnMusicWatchNextResultsRendererImpl implements SingleColumnMusicWatchNextResultsRenderer {

            private final TabbedRendererImpl tabbedRenderer;

            @Data
            public static class TabbedRendererImpl implements TabbedRenderer {

                private final TabsImpl watchNextTabbedResultsRenderer;
            }
        }
    }

    @Data
    public static class PlayerOverlaysImpl implements PlayerOverlays {

        private final PlayerOverlayRendererImpl playerOverlayRenderer;

        @Data
        public static class PlayerOverlayRendererImpl implements PlayerOverlayRenderer {

            private final BrowserMediaSessionImpl browserMediaSession;

            @Data
            public static class BrowserMediaSessionImpl implements BrowserMediaSession {

                private final BrowserMediaSessionRendererImpl browserMediaSessionRenderer;

                @Data
                public static class BrowserMediaSessionRendererImpl implements BrowserMediaSessionRenderer {

                    private final RunsImpl       album;
                    private final ThumbnailsImpl thumbnailDetails;
                }
            }
        }
    }
}
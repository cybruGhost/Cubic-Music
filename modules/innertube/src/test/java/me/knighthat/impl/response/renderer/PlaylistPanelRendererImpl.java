package me.knighthat.impl.response.renderer;

import lombok.Data;
import me.knighthat.impl.response.*;
import me.knighthat.impl.response.thumbnail.ThumbnailsImpl;
import me.knighthat.innertube.response.renderer.PlaylistPanelRenderer;

import java.util.List;

@Data
public class PlaylistPanelRendererImpl implements PlaylistPanelRenderer {

    private final List<ContentImpl> contents;
    private final String            playlistId;
    private final Boolean isInfinite;
    private final List<ContinuationImpl> continuations;
    private final String trackingParams;
    private final Short      numItemsToShow;
    private final ButtonImpl shuffleToggleButton;

    @Data
    public static class ContentImpl implements Content {

        private final VideoRendererImpl           playlistPanelVideoRenderer;
        private final AutomixPreviewVideoRendererImpl automixPreviewVideoRenderer;

        @Data
        public static class VideoRendererImpl implements VideoRenderer {

            private final RunsImpl title;
            private final RunsImpl       longBylineText;
            private final ThumbnailsImpl thumbnail;
            private final RunsImpl       lengthText;
            private final Boolean      selected;
            private final EndpointImpl navigationEndpoint;
            private final String       videoId;
            private final RunsImpl        shortBylineText;
            private final List<BadgeImpl> badges;
            private final String          trackingParams;
            private final MenuImpl menu;
            private final String   playlistSetVideoId;
            private final Boolean canReorder;
            private final EndpointImpl queueNavigationEndpoint;
        }
    }
}
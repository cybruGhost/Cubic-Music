package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.impl.response.renderer.SectionListRendererImpl;
import me.knighthat.impl.response.thumbnail.ThumbnailImpl;
import me.knighthat.innertube.response.BrowseResponse;

import java.util.List;

@Data
public class BrowseResponseImpl implements BrowseResponse {

    private final ResponseContextImpl responseContext;
    private final ContentsImpl        contents;
    private final HeaderImpl   header;
    private final String       trackingParams;
    private final Integer maxAgeStoreSeconds;
    private final MicroformatImpl   microformat;
    private final ThumbnailImpl background;

    @Data
    public static class ContentsImpl implements Contents {

        private final TabsImpl                           singleColumnBrowseResultsRenderer;
        private final TwoColumnBrowseResultsRendererImpl twoColumnBrowseResultsRenderer;
        private final SectionListRendererImpl            sectionListRenderer;

        @Data
        public static class TwoColumnBrowseResultsRendererImpl implements TwoColumnBrowseResultsRenderer {

            private final List<TabsImpl.TabImpl> tabs;
            private final SecondaryContentsImpl  secondaryContents;

            @Data
            public static class SecondaryContentsImpl implements SecondaryContents {

                private final SectionListRendererImpl sectionListRenderer;
            }
        }
    }

    @Data
    public static class HeaderImpl implements Header {

        private final MusicImmersiveHeaderRendererImpl musicImmersiveHeaderRenderer;
        private final MusicHeaderRendererImpl          musicHeaderRenderer;

        @Data
        public static class MusicImmersiveHeaderRendererImpl implements MusicImmersiveHeaderRenderer {

            private final RunsImpl     title;
            private final ButtonImpl subscriptionButton;
            private final RunsImpl     description;
            private final ButtonImpl    moreButton;
            private final MenuImpl      menu;
            private final ThumbnailImpl thumbnail;
            private final String        trackingParams;
            private final ButtonImpl playButton;
            private final ButtonImpl startRadioButton;
            private final EndpointImpl shareEndpoint;
            private final RunsImpl     monthlyListenerCount;
        }

        @Data
        public static class MusicHeaderRendererImpl implements MusicHeaderRenderer {

            private final RunsImpl title;
            private final String   trackingParams;
        }
    }
}
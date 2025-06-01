package me.knighthat.impl.response.renderer;

import lombok.Data;
import me.knighthat.impl.response.*;
import me.knighthat.impl.response.thumbnail.ThumbnailImpl;
import me.knighthat.impl.response.thumbnail.ThumbnailsImpl;
import me.knighthat.innertube.response.renderer.SectionListRenderer;

import java.util.List;

@Data
public class SectionListRendererImpl implements SectionListRenderer {

    private final List<ContentImpl>      contents;
    private final List<ContinuationImpl> continuations;
    private final String                 trackingParams;
    private final HeaderImpl header;

    @Data
    public static class ContentImpl implements Content {

        private final MusicDescriptionShelfRendererImpl  musicDescriptionShelfRenderer;
        private final MusicTastebuilderShelfRendererImpl musicTastebuilderShelfRenderer;
        private final MusicResponsiveHeaderRendererImpl musicResponsiveHeaderRenderer;
        private final MusicShelfRendererImpl            musicShelfRenderer;
        private final MusicCarouselShelfRendererImpl    musicCarouselShelfRenderer;
        private final GridRendererImpl               gridRenderer;
        private final MusicPlaylistShelfRendererImpl musicPlaylistShelfRenderer;
        private final MusicCardShelfRendererImpl musicCardShelfRenderer;

        @Data
        public static class MusicDescriptionShelfRendererImpl implements MusicDescriptionShelfRenderer {

            private final RunsImpl   description;
            private final ButtonImpl moreButton;
            private final String     trackingParams;
            private final String shelfStyle;
            private final Integer maxCollapsedLines;
            private final Integer           maxExpandedLines;
            private final List<CommandImpl> onShowCommands;
            private final RunsImpl              footer;

            @Data
            public static class CommandImpl implements Command {

                private final String                   clickTrackingParams;
                private final LogLyricEventCommandImpl logLyricEventCommand;

                @Data
                public static class LogLyricEventCommandImpl implements LogLyricEventCommand {

                    private final String serializedLyricInfo;
                }
            }
        }

        @Data
        public static class MusicTastebuilderShelfRendererImpl implements MusicTastebuilderShelfRenderer {

            private final ThumbnailImpl thumbnail;
            private final RunsImpl          primaryText;
            private final RunsImpl secondaryText;
            private final ButtonImpl actionButton;
            private final Boolean isVisible;
            private final String trackingParams;

            @Data
            public static class ThumbnailImpl implements Thumbnail {

                private final MusicTastebuilderShelfThumbnailRendererImpl musicTastebuilderShelfThumbnailRenderer;

                @Data
                public static class MusicTastebuilderShelfThumbnailRendererImpl implements MusicTastebuilderShelfThumbnailRenderer {

                    private final ThumbnailsImpl thumbnail;
                }
            }
        }

        @Data
        public static class MusicResponsiveHeaderRendererImpl implements MusicResponsiveHeaderRenderer {

            private final ThumbnailImpl    thumbnail;
            private final List<ButtonImpl> buttons;
            private final RunsImpl             title;
            private final RunsImpl subtitle;
            private final String          trackingParams;
            private final DescriptionImpl description;
            private final RunsImpl             straplineTextOne;
            private final ThumbnailImpl   straplineThumbnail;
            private final List<BadgeImpl> subtitleBadge;
            private final RunsImpl        secondSubtitle;

            @Data
            public static class ButtonImpl implements Button {

                private final me.knighthat.impl.response.ButtonImpl.ToggleImpl    toggleButtonRenderer;
                private final me.knighthat.impl.response.ButtonImpl.MusicPlayImpl musicPlayButtonRenderer;
                private final MenuImpl.RendererImpl                                   menuRenderer;
            }

            @Data
            public static class DescriptionImpl implements Description {

                private final MusicDescriptionShelfRendererImpl musicDescriptionShelfRenderer;
            }
        }

        @Data
        public static class GridRendererImpl implements GridRenderer {

            private final List<ItemImpl> items;
            private final String         trackingParams;

            @Data
            public static class ItemImpl implements Item {

                private final MusicTwoRowItemRendererImpl musicTwoRowItemRenderer;
            }
        }
    }

    @Data
    public static class HeaderImpl implements Header {

        private final ChipCloudImpl chipCloudRenderer;
    }
}
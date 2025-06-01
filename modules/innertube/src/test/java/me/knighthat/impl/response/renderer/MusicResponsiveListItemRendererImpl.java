package me.knighthat.impl.response.renderer;

import lombok.Data;
import me.knighthat.impl.response.*;
import me.knighthat.impl.response.thumbnail.ThumbnailImpl;
import me.knighthat.innertube.response.renderer.MusicResponsiveListItemRenderer;

import java.util.List;

@Data
public class MusicResponsiveListItemRendererImpl implements MusicResponsiveListItemRenderer {

    private final String                  trackingParams;
    private final ThumbnailImpl           thumbnail;
    private final OverlayImpl             overlay;
    private final List<FlexColumnImpl>    flexColumns;
    private final List<FixedColumnImpl>   fixedColumns;
    private final MenuImpl                menu;
    private final List<BadgeImpl>         badges;
    private final PlaylistItemDataImpl    playlistItemData;
    private final String                  flexColumnDisplayStyle;
    private final EndpointImpl            navigationEndpoint;
    private final String                  itemHeight;
    private final RunsImpl                index;
    private final MultiSelectCheckboxImpl multiSelectCheckbox;


    @Data
    public static class FlexColumnImpl implements FlexColumn {

        private final RendererImpl musicResponsiveListItemFlexColumnRenderer;

        @Data
        public static class RendererImpl implements Renderer {

            private final RunsImpl text;
            private final String   displayPriority;
        }
    }

    @Data
    public static class FixedColumnImpl implements FixedColumn {

        private final RendererImpl musicResponsiveListItemFixedColumnRenderer;

        @Data
        public static class RendererImpl implements Renderer {

            private final RunsImpl text;
            private final String   displayPriority;
            private final String   size;
        }
    }

    @Data
    public static class PlaylistItemDataImpl implements PlaylistItemData {

        private final String playlistSetVideoId;
        private final String videoId;
    }

    @Data
    public static class MultiSelectCheckboxImpl implements MultiSelectCheckbox {

        private final RendererImpl checkboxRenderer;

        @Data
        public static class RendererImpl implements Renderer {

            private final ChangeCommandImpl onSelectionChangeCommand;
            private final String            checkedState;
            private final String            trackingParams;

            @Data
            public static class ChangeCommandImpl implements ChangeCommand {

                private final String            clickTrackingParams;
                private final UpdateCommandImpl updateMultiSelectStateCommand;

                @Data
                public static class UpdateCommandImpl implements UpdateCommand {

                    private final String multiSelectParams;
                    private final String multiSelectItem;
                }
            }
        }
    }
}
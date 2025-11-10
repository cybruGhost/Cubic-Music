package me.knighthat.impl.response.renderer;

import lombok.Data;
import me.knighthat.impl.response.ButtonImpl;
import me.knighthat.impl.response.RunsImpl;
import me.knighthat.innertube.response.renderer.MusicQueueRenderer;

import java.util.List;

@Data
public class MusicQueueRendererImpl implements MusicQueueRenderer {

    private final ContentImpl content;
    private final Boolean            hack;
    private final HeaderImpl             header;
    private final SubHeaderChipCloudImpl subHeaderChipCloud;

    @Data
    public static class ContentImpl implements Content {

        private final PlaylistPanelRendererImpl playlistPanelRenderer;
    }

    @Data
    public static class HeaderImpl implements Header {

        private final MusicQueueHeaderRendererImpl musicQueueHeaderRenderer;

        @Data
        public static class MusicQueueHeaderRendererImpl implements MusicQueueHeaderRenderer {

            private final RunsImpl title;
            private final RunsImpl         subtitle;
            private final List<ButtonImpl> buttons;
            private final String           trackingParams;
        }
    }

    @Data
    public static class SubHeaderChipCloudImpl implements SubHeaderChipCloud {

        private final ChipCloudImpl chipCloudRenderer;
    }
}
package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.impl.response.renderer.MusicQueueRendererImpl;
import me.knighthat.impl.response.renderer.SectionListRendererImpl;
import me.knighthat.innertube.response.Tabs;

import java.util.List;

@Data
public class TabsImpl implements Tabs {

    private final List<TabImpl> tabs;

    @Data
    public static class TabImpl implements Tab {

        private final RendererImpl tabRenderer;

        @Data
        public static class RendererImpl implements Renderer {

            private final EndpointImpl endpoint;
            private final String title;
            private final Boolean     selected;
            private final ContentImpl content;
            private final IconImpl    icon;
            private final String tabIdentifier;
            private final String trackingParams;

            @Data
            public static class ContentImpl implements Content {

                private final SectionListRendererImpl sectionListRenderer;
                private final MusicQueueRendererImpl  musicQueueRenderer;
            }
        }
    }
}
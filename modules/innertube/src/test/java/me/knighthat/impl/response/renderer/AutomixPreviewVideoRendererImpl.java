package me.knighthat.impl.response.renderer;

import lombok.Data;
import me.knighthat.innertube.response.Endpoint;
import me.knighthat.innertube.response.renderer.AutomixPreviewVideoRenderer;

@Data
public class AutomixPreviewVideoRendererImpl implements AutomixPreviewVideoRenderer {

    Content Content;

    @Data
    public static class ContentImpl implements AutomixPreviewVideoRenderer.Content {

        Renderer AutomixPlaylistVideoRenderer;

        @Data
        public static class Renderer implements AutomixPreviewVideoRenderer.Content.Renderer {

            Endpoint NavigationEndpoint;

            String TrackingParams;

            String AutomixMode;
        }
    }
}

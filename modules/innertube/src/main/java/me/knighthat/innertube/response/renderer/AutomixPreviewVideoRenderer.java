package me.knighthat.innertube.response.renderer;

import me.knighthat.innertube.response.Endpoint;
import me.knighthat.innertube.response.Trackable;

public interface AutomixPreviewVideoRenderer {

    Content getContent();

    interface Content {

        Renderer getAutomixPlaylistVideoRenderer();

        interface Renderer extends Trackable {

            Endpoint getNavigationEndpoint();

            String getAutomixMode();
        }
    }
}

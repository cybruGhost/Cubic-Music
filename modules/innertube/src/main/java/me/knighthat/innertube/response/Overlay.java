package me.knighthat.innertube.response;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Overlay {

    MusicItemThumbnailOverlayRenderer getMusicItemThumbnailOverlayRenderer();

    interface MusicItemThumbnailOverlayRenderer {

        Background getBackground();

        Content getContent();

        String getContentPosition();

        String getDisplayStyle();

        interface Background {

            VerticalGradient getVerticalGradient();

            interface VerticalGradient {

                @NotNull
                List<String> getGradientLayerColors();
            }
        }

        interface Content {

            Button.MusicPlay getMusicPlayButtonRenderer();
        }
    }
}
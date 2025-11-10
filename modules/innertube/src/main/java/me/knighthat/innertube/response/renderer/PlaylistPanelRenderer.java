package me.knighthat.innertube.response.renderer;

import org.jetbrains.annotations.NotNull;

import me.knighthat.innertube.response.*;
import me.knighthat.innertube.response.thumbnail.Thumbnails;

import java.util.List;

public interface PlaylistPanelRenderer extends Trackable {

    @NotNull
    List<? extends Content> getContents();

    String getPlaylistId();

    Boolean getIsInfinite();

    @NotNull
    List<? extends Continuation> getContinuations();

    Short getNumItemsToShow();

    Button getShuffleToggleButton();

    interface Content {

        VideoRenderer getPlaylistPanelVideoRenderer();

        AutomixPreviewVideoRenderer getAutomixPreviewVideoRenderer();

        interface VideoRenderer extends Trackable {

            Runs getTitle();

            Runs getLongBylineText();

            Thumbnails getThumbnail();

            Runs getLengthText();

            Boolean getSelected();

            Endpoint getNavigationEndpoint();

            String getVideoId();

            Runs getShortBylineText();

            @NotNull
            List<? extends Badge> getBadges();

            Menu getMenu();

            String getPlaylistSetVideoId();

            Boolean getCanReorder();

            Endpoint getQueueNavigationEndpoint();

        }
    }
}
package me.knighthat.innertube.response.renderer;

import org.jetbrains.annotations.NotNull;

import me.knighthat.innertube.response.*;
import me.knighthat.innertube.response.thumbnail.Thumbnail;

import java.util.List;

public interface MusicTwoRowItemRenderer extends Trackable {

    Thumbnail getThumbnailRenderer();

    String getAspectRatio();

    Runs getTitle();

    Runs getSubtitle();

    Endpoint getNavigationEndpoint();

    Menu getMenu();

    Overlay getThumbnailOverlay();

    @NotNull
    List<? extends Badge> getSubtitleBadges();
}
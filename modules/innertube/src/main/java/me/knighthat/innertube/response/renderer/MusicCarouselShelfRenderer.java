package me.knighthat.innertube.response.renderer;

import org.jetbrains.annotations.NotNull;

import me.knighthat.innertube.response.Accessibility;
import me.knighthat.innertube.response.Button;
import me.knighthat.innertube.response.Runs;
import me.knighthat.innertube.response.Trackable;

import java.util.List;

public interface MusicCarouselShelfRenderer extends Trackable {

    Header getHeader();

    @NotNull
    List<? extends Content> getContents();

    String getItemSize();

    String getNumItemsPerColumn();

    interface Header {

        MusicCarouselShelfBasicHeaderRenderer getMusicCarouselShelfBasicHeaderRenderer();

        interface MusicCarouselShelfBasicHeaderRenderer extends Trackable {

            Runs getTitle();

            Runs getStrapline();

            Accessibility getAccessibilityData();

            String getHeaderStyle();

            Button getMoreContentButton();
        }
    }

    interface Content {

        MusicResponsiveListItemRenderer getMusicResponsiveListItemRenderer();

        MusicTwoRowItemRenderer getMusicTwoRowItemRenderer();
    }
}
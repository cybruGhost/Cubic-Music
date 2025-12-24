package me.knighthat.innertube.response.renderer;

import org.jetbrains.annotations.NotNull;

import me.knighthat.innertube.response.Endpoint;
import me.knighthat.innertube.response.Runs;
import me.knighthat.innertube.response.Trackable;

import java.util.List;

public interface MusicShelfRenderer extends Trackable {

    Runs getTitle();

    @NotNull
    List<? extends Content> getContents();

    Runs getBottomText();

    Endpoint getBottomEndpoint();

    ShelfDivider getShelfDivider();

    Boolean getContentsMultiSelectable();

    interface Content {

        MusicResponsiveListItemRenderer getMusicResponsiveListItemRenderer();
    }

    interface ShelfDivider {

        Renderer getMusicShelfDividerRenderer();

        interface Renderer {

            Boolean getHidden();
        }
    }
}
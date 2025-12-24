package me.knighthat.innertube.response.renderer;

import org.jetbrains.annotations.NotNull;

import me.knighthat.innertube.response.Runs;
import me.knighthat.innertube.response.Trackable;
import me.knighthat.innertube.response.thumbnail.Thumbnail;

import java.util.List;

public interface MusicCardShelfRenderer extends Trackable {

    Thumbnail getThumbnail();

    Runs getTitle();

    Runs getSubtitle();

    @NotNull
    List<? extends Content> getContents();

    interface Content {

        MessageRenderer getMessageRenderer();

        MusicResponsiveListItemRenderer getMusicResponsiveListItemRenderer();

        interface MessageRenderer extends Trackable {

            Runs getText();

            Style getStyle();

            interface Style {

                String getValue();
            }
        }
    }
}
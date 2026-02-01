package me.knighthat.innertube.response.renderer;

import org.jetbrains.annotations.NotNull;

import me.knighthat.innertube.response.Button;
import me.knighthat.innertube.response.Runs;
import me.knighthat.innertube.response.Trackable;

import java.util.List;

public interface MusicQueueRenderer {

    Content getContent();

    Boolean getHack();

    Header getHeader();

    SubHeaderChipCloud getSubHeaderChipCloud();

    interface Content {

        PlaylistPanelRenderer getPlaylistPanelRenderer();
    }

    interface Header {

        MusicQueueHeaderRenderer getMusicQueueHeaderRenderer();

        interface MusicQueueHeaderRenderer extends Trackable {

            Runs getTitle();

            Runs getSubtitle();

            @NotNull
            List<? extends Button> getButtons();
        }
    }

    interface SubHeaderChipCloud {

        ChipCloud getChipCloudRenderer();
    }
}
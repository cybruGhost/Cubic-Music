package me.knighthat.innertube.response.renderer;

import org.jetbrains.annotations.NotNull;

import me.knighthat.innertube.response.Endpoint;
import me.knighthat.innertube.response.Icon;
import me.knighthat.innertube.response.Runs;
import me.knighthat.innertube.response.Trackable;

import java.util.List;

public interface SearchSuggestionsSectionRenderer {

    @NotNull
    List<? extends Content> getContents();

    interface Content {

        MusicResponsiveListItemRenderer getMusicResponsiveListItemRenderer();

        Renderer getSearchSuggestionRenderer();

        interface Renderer extends Trackable {

            Runs getSuggestion();

            Endpoint getNavigationEndpoint();

            Icon getIcon();
        }
    }
}
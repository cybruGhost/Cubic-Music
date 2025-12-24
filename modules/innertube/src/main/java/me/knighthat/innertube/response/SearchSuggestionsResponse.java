package me.knighthat.innertube.response;

import org.jetbrains.annotations.NotNull;

import me.knighthat.innertube.response.renderer.SearchSuggestionsSectionRenderer;

import java.util.List;

public interface SearchSuggestionsResponse extends Trackable {

    ResponseContext getResponseContext();

    @NotNull
    List<? extends Content> getContents();

    interface Content {

        SearchSuggestionsSectionRenderer getSearchSuggestionsSectionRenderer();
    }
}
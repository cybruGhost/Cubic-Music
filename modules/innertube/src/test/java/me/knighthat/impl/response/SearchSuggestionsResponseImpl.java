package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.impl.response.renderer.SearchSuggestionsSectionRendererImpl;
import me.knighthat.innertube.response.SearchSuggestionsResponse;

import java.util.List;

@Data
public class SearchSuggestionsResponseImpl implements SearchSuggestionsResponse {

    private final ResponseContextImpl   responseContext;
    private final List<ContentImpl> contents;
    private final String            trackingParams;

    @Data
    public static class ContentImpl implements Content {

        private final SearchSuggestionsSectionRendererImpl searchSuggestionsSectionRenderer;
    }
}
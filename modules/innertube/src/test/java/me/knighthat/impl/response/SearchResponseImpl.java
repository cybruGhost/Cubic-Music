package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.innertube.response.SearchResponse;

@Data
public class SearchResponseImpl implements SearchResponse {

    private final ResponseContextImpl responseContext;
    private final ContentsImpl    contents;
    private final String          trackingParams;

    @Data
    public static class ContentsImpl implements Contents {

        private final TabsImpl tabbedSearchResultsRenderer;
    }
}
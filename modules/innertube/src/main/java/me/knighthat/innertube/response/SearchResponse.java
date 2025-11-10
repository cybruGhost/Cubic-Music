package me.knighthat.innertube.response;

public interface SearchResponse extends Trackable {

    ResponseContext getResponseContext();

    Contents getContents();

    interface Contents {

        Tabs getTabbedSearchResultsRenderer();
    }
}
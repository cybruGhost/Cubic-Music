package me.knighthat.innertube.response;

import me.knighthat.impl.response.SearchSuggestionsResponseImpl;
import me.knighthat.innertube.Endpoints;
import me.knighthat.innertube.HttpClient;
import me.knighthat.innertube.request.body.Context;
import me.knighthat.innertube.request.body.SearchSuggestionsBody;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchSuggestionsResponseTest implements HttpClient {

    /**
     * Fetch data from "music/get_search_suggestions" endpoint and convert it into {@link SearchSuggestionsResponse}
     */
    @Test
    void testFetchSearchSuggestions() {
        SearchSuggestionsBody searchBody = new SearchSuggestionsBody( Context.WEB_DEFAULT, "take me to church" );
        Request request = makeRequest( Endpoints.SEARCH_SUGGESTIONS, searchBody );

        try (
                Response response = CLIENT.newCall( request ).execute() ;
                InputStream inStream = response.body().byteStream() ;
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            SearchSuggestionsResponse converted = JSON.fromJson( reader, SearchSuggestionsResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }
}
package me.knighthat.innertube.response;

import me.knighthat.impl.response.SearchResponseImpl;
import me.knighthat.innertube.Endpoints;
import me.knighthat.innertube.HttpClient;
import me.knighthat.innertube.request.body.Context;
import me.knighthat.innertube.request.body.SearchBody;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchResponseTest implements HttpClient {

    /**
     * Fetch data from "search" endpoint and convert it into {@link SearchResponse}
     */
    @Test
    void testFetchSearch() {
        SearchBody searchBody = new SearchBody( Context.WEB_DEFAULT, "take me to church", null );
        Request request = makeRequest( Endpoints.SEARCH, searchBody );

        try (
                Response response = CLIENT.newCall( request ).execute() ;
                InputStream inStream = response.body().byteStream() ;
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            SearchResponse converted = JSON.fromJson( reader, SearchResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }
}
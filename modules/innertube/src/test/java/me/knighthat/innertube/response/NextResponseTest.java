package me.knighthat.innertube.response;

import me.knighthat.impl.response.NextResponseImpl;
import me.knighthat.innertube.Endpoints;
import me.knighthat.innertube.HttpClient;
import me.knighthat.innertube.request.body.Context;
import me.knighthat.innertube.request.body.NextBody;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NextResponseTest implements HttpClient {

    /**
     * Fetch data from "next" endpoint and convert it into {@link NextResponse}
     */
    @Test
    void testFetchNextSong() {
        NextBody nextBody = new NextBody( Context.WEB_DEFAULT, "nZjTtuNR3Og", "", "wAEB" );
        Request request = makeRequest( Endpoints.NEXT, nextBody );

        try (
                Response response = CLIENT.newCall( request ).execute();
                InputStream inStream = response.body().byteStream();
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            NextResponse converted = JSON.fromJson( reader, NextResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }

    /**
     * Fetch data from "next" endpoint and convert it into {@link NextResponse}
     */
    @Test
    void testFetchNextPlaylist() {
        NextBody nextBody = new NextBody( Context.WEB_DEFAULT, "", "RDAMVM10H_LmJsb4M", "" );
        Request request = makeRequest( Endpoints.NEXT, nextBody );

        try (
                Response response = CLIENT.newCall( request ).execute();
                InputStream inStream = response.body().byteStream();
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            NextResponse converted = JSON.fromJson( reader, NextResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }
}
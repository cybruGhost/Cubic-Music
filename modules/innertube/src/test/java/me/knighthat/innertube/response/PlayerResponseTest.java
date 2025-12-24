package me.knighthat.innertube.response;

import me.knighthat.impl.response.PlayerResponseImpl;
import me.knighthat.innertube.Endpoints;
import me.knighthat.innertube.HttpClient;
import me.knighthat.innertube.request.body.Context;
import me.knighthat.innertube.request.body.PlayerBody;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerResponseTest implements HttpClient {

    /**
     * Fetch data from "player" endpoint and convert it into {@link PlayerResponse}
     */
    @Test
    void testFetchPlayer() {
        PlayerBody playerBody = new PlayerBody( Context.WEB_DEFAULT, "phLb_SoPBlA", null );
        Request request = makeRequest( Endpoints.PLAYER, playerBody );

        try (
                Response response = CLIENT.newCall( request ).execute() ;
                InputStream inStream = response.body().byteStream() ;
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            PlayerResponse converted = JSON.fromJson( reader, PlayerResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }
}
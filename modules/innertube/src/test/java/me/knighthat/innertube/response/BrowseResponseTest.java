package me.knighthat.innertube.response;

import me.knighthat.impl.response.BrowseResponseImpl;
import me.knighthat.innertube.Endpoints;
import me.knighthat.innertube.HttpClient;
import me.knighthat.innertube.request.body.BrowseBody;
import me.knighthat.innertube.request.body.Context;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowseResponseTest implements HttpClient {

    /**
     * Fetch data from "browse" endpoint and convert it into {@link BrowseResponse}
     */
    @Test
    void testFetchMusicHome() {
        BrowseBody searchBody = new BrowseBody( Context.WEB_DEFAULT, "FEmusic_home", "", null );
        Request request = makeRequest( Endpoints.BROWSE, searchBody );

        try (
                Response response = CLIENT.newCall( request ).execute() ;
                InputStream inStream = response.body().byteStream() ;
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            BrowseResponse converted = JSON.fromJson( reader, BrowseResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }

    /**
     * Fetch data from "browse" endpoint and convert it into {@link BrowseResponse}
     */
    @Test
    void testFetchRadio() {
        BrowseBody searchBody = new BrowseBody( Context.WEB_DEFAULT, "", "RDAMVMphLb_SoPBlA", "wAEB" );
        Request request = makeRequest( Endpoints.BROWSE, searchBody );

        try (
                Response response = CLIENT.newCall( request ).execute() ;
                InputStream inStream = response.body().byteStream() ;
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            BrowseResponse converted = JSON.fromJson( reader, BrowseResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }

    /**
     * Fetch data from "browse" endpoint and convert it into {@link BrowseResponse}
     */
    @Test
    void testFetchAlbum() {
        BrowseBody searchBody = new BrowseBody( Context.WEB_DEFAULT, "MPREb_rGUlsaCTNv7", "", null );
        Request request = makeRequest( Endpoints.BROWSE, searchBody );

        try (
                Response response = CLIENT.newCall( request ).execute() ;
                InputStream inStream = response.body().byteStream() ;
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            BrowseResponse converted = JSON.fromJson( reader, BrowseResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }

    /**
     * Fetch data from "browse" endpoint and convert it into {@link BrowseResponse}
     */
    @Test
    void testFetchArtist() {
        BrowseBody searchBody = new BrowseBody( Context.WEB_DEFAULT, "UCprAFmT0C6O4X0ToEXpeFTQ", "", null );
        Request request = makeRequest( Endpoints.BROWSE, searchBody );

        try (
                Response response = CLIENT.newCall( request ).execute() ;
                InputStream inStream = response.body().byteStream() ;
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            BrowseResponse converted = JSON.fromJson( reader, BrowseResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }

    /**
     * Fetch data from "browse" endpoint and convert it into {@link BrowseResponse}
     */
    @Test
    void testFetchArtistAlbum() {
        BrowseBody searchBody = new BrowseBody( Context.WEB_DEFAULT, "MPADUCprAFmT0C6O4X0ToEXpeFTQ", "", "ggMIegYIAhoCAQI%3D" );
        Request request = makeRequest( Endpoints.BROWSE, searchBody );

        try (
                Response response = CLIENT.newCall( request ).execute() ;
                InputStream inStream = response.body().byteStream() ;
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            BrowseResponse converted = JSON.fromJson( reader, BrowseResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }

    /**
     * Fetch data from "browse" endpoint and convert it into {@link BrowseResponse}
     */
    @Test
    void testFetchArtistSongs() {
        BrowseBody searchBody = new BrowseBody( Context.WEB_DEFAULT, "", "VLOLAK5uy_lcvoZKgUhdnEreHlWY_RaEwR47D-vwdTI", "ggMCCAI%3D" );
        Request request = makeRequest( Endpoints.BROWSE, searchBody );

        try (
                Response response = CLIENT.newCall( request ).execute() ;
                InputStream inStream = response.body().byteStream() ;
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            BrowseResponse converted = JSON.fromJson( reader, BrowseResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }

    /**
     * Fetch data from "browse" endpoint and convert it into {@link BrowseResponse}
     */
    @Test
    void testFetchLyrics() {
        BrowseBody searchBody = new BrowseBody( Context.WEB_DEFAULT, "MPLYt_4sk9YQcgV3s-1", "", null );
        Request request = makeRequest( Endpoints.BROWSE, searchBody );

        try (
                Response response = CLIENT.newCall( request ).execute() ;
                InputStream inStream = response.body().byteStream() ;
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            BrowseResponse converted = JSON.fromJson( reader, BrowseResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }

    /**
     * Fetch data from "browse" endpoint and convert it into {@link BrowseResponse}
     */
    @Test
    void testFetchPlaylist() {
        BrowseBody searchBody = new BrowseBody( Context.WEB_DEFAULT, "", "VLRDCLAK5uy_lc4yBYpywmmgjplkuzYxzUgLJ8UvDKUUk", null );
        Request request = makeRequest( Endpoints.BROWSE, searchBody );

        try (
                Response response = CLIENT.newCall( request ).execute() ;
                InputStream inStream = response.body().byteStream() ;
                InputStreamReader reader = new InputStreamReader( inStream )
        ) {
            assertTrue( response.isSuccessful() );

            BrowseResponse converted = JSON.fromJson( reader, BrowseResponseImpl.class );
            System.out.println( JSON.toJson( converted ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            Assertions.fail( e.getMessage() );
        }
    }
}
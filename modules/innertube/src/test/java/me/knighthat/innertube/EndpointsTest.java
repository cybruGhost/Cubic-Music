package me.knighthat.innertube;

import me.knighthat.innertube.request.body.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests designed to make sure that all endpoints are
 * reachable given provided data is correct and adhere
 * to YTM standard.
 */
class EndpointsTest implements HttpClient {

    @Test
    void testBrowseArtist() {
        BrowseBody browseBody = new BrowseBody( Context.WEB_DEFAULT, "UCprAFmT0C6O4X0ToEXpeFTQ", "", null );
        Assertions.assertTrue( post( Endpoints.BROWSE, browseBody ) );
    }

    @Test
    void testBrowseAlbum() {
        BrowseBody browseBody = new BrowseBody( Context.WEB_DEFAULT, "MPREb_rGUlsaCTNv7", "", null );
        Assertions.assertTrue( post( Endpoints.BROWSE, browseBody ) );
    }

    @Test
    void testBrowseRadio() {
        BrowseBody browseBody = new BrowseBody( Context.WEB_DEFAULT, "", "RDAMVMphLb_SoPBlA", "wAEB" );
        Assertions.assertTrue( post( Endpoints.BROWSE, browseBody ) );
    }

    @Test
    void testBrowseLyrics() {
        BrowseBody browseBody = new BrowseBody( Context.WEB_DEFAULT, "MPLYt_4sk9YQcgV3s-1", "", null );
        Assertions.assertTrue( post( Endpoints.BROWSE, browseBody ) );
    }

    @Test
    void testBrowseMusicHome() {
        BrowseBody browseBody = new BrowseBody( Context.WEB_DEFAULT, "FEmusic_home", "", null );
        Assertions.assertTrue( post( Endpoints.BROWSE, browseBody ) );
    }

    @Test
    void testBrowsePlaylist() {
        BrowseBody browseBody = new BrowseBody( Context.WEB_DEFAULT, "VLRDCLAK5uy_k6PkYWus1Mt-aKrbb0Ne8SkA2BgAk1Yy4", "", null );
        Assertions.assertTrue( post( Endpoints.BROWSE, browseBody ) );
    }

    @Test
    void testNextSong() {
        NextBody nextBody = new NextBody( Context.WEB_DEFAULT, "nZjTtuNR3Og", "", null );
        Assertions.assertTrue( post( Endpoints.BROWSE, nextBody ) );
    }

    @Test
    void testNextPlaylist() {
        NextBody nextBody = new NextBody( Context.WEB_DEFAULT, "", "RDAMVMnZjTtuNR3Og", "wAEB" );
        Assertions.assertTrue( post( Endpoints.BROWSE, nextBody ) );
    }

    @Test
    void testPlayer() {
        PlayerBody playerBody = new PlayerBody( Context.WEB_DEFAULT, "phLb_SoPBlA", null );
        Assertions.assertTrue( post( Endpoints.BROWSE, playerBody ) );
    }

    @Test
    void testSearch() {
        SearchBody searchBody = new SearchBody( Context.WEB_DEFAULT, "take me to church", null );
        Assertions.assertTrue( post( Endpoints.BROWSE, searchBody ) );
    }

    @Test
    void testSearchSuggestion() {
        SearchSuggestionsBody searchSuggestionsBody = new SearchSuggestionsBody( Context.WEB_DEFAULT, "figure 09" );
        Assertions.assertTrue( post( Endpoints.BROWSE, searchSuggestionsBody ) );
    }
}
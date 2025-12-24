package me.knighthat.innertube.response;

import org.jetbrains.annotations.NotNull;

import me.knighthat.innertube.response.renderer.SectionListRenderer;
import me.knighthat.innertube.response.thumbnail.Thumbnail;

import java.util.List;

public interface BrowseResponse extends Trackable {

    ResponseContext getResponseContext();

    Contents getContents();

    Header getHeader();

    // Missing "frameworkUpdates"

    Integer getMaxAgeStoreSeconds();

    Microformat getMicroformat();

    Thumbnail getBackground();

    interface Contents {

        Tabs getSingleColumnBrowseResultsRenderer();

        TwoColumnBrowseResultsRenderer getTwoColumnBrowseResultsRenderer();

        SectionListRenderer getSectionListRenderer();

        interface TwoColumnBrowseResultsRenderer {

            @NotNull
            List<? extends Tabs.Tab> getTabs();

            SecondaryContents getSecondaryContents();

            interface SecondaryContents {

                SectionListRenderer getSectionListRenderer();
            }
        }
    }

    interface Header {

        MusicImmersiveHeaderRenderer getMusicImmersiveHeaderRenderer();

        MusicHeaderRenderer getMusicHeaderRenderer();

        interface MusicImmersiveHeaderRenderer extends Trackable {

            Runs getTitle();

            Button getSubscriptionButton();

            Runs getDescription();

            Button getMoreButton();

            Menu getMenu();

            Thumbnail getThumbnail();

            Button getPlayButton();

            Button getStartRadioButton();

            Endpoint getShareEndpoint();

            Runs getMonthlyListenerCount();
        }

        interface MusicHeaderRenderer extends Trackable {

            Runs getTitle();
        }
    }
}
package me.knighthat.innertube.response;

import org.jetbrains.annotations.NotNull;

import me.knighthat.innertube.response.thumbnail.Thumbnails;

import java.util.List;

public interface Microformat {

    Renderer getMicroformatDataRenderer();

    interface Renderer {

        String getUrlCanonical();

        String getTitle();

        String getDescription();

        Thumbnails getThumbnail();

        String getSiteName();

        String getAppName();

        String getAndroidPackage();

        String getIosAppStoreId();

        String getIosAppArguments();

        String getOgType();

        String getUrlApplinksIos();

        String getUrlApplinksAndroid();

        String getUrlTwitterIos();

        String getUrlTwitterAndroid();

        String getTwitterCardType();

        String getTwitterSiteHandle();

        String getSchemaDotOrgType();

        Boolean getNoindex();

        Boolean getUnlisted();

        Boolean getPaid();

        Boolean getFamilySafe();

        @NotNull
        List<? extends String> getTags();

        @NotNull
        List<? extends String> getAvailableCountries();

        PageOwnerDetails getPageOwnerDetails();

        VideoDetails getVideoDetails();

        @NotNull
        List<? extends Link> getLinkAlternates();

        String getViewCount();

        String getPublishDate();

        String getCategory();

        String getUploadDate();

        interface PageOwnerDetails {

            String getName();

            String getExternalChannelId();

            String getYoutubeProfileUrl();
        }

        interface VideoDetails {

            String getExternalVideoId();

            String getDurationSeconds();

            String getDurationIso8601();
        }

        interface Link {

            String getHrefUrl();

            String getTitle();

            String getAlternateType();
        }
    }
}

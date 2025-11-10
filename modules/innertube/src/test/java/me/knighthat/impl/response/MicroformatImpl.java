package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.impl.response.thumbnail.ThumbnailsImpl;
import me.knighthat.innertube.response.Microformat;

import java.util.List;

@Data
public class MicroformatImpl implements Microformat {

    private final RendererImpl microformatDataRenderer;

    @Data
    public static class RendererImpl implements Renderer {

        private final String urlCanonical;
        private final String title;
        private final String         description;
        private final ThumbnailsImpl thumbnail;
        private final String         siteName;
        private final String appName;
        private final String androidPackage;
        private final String iosAppStoreId;
        private final String iosAppArguments;
        private final String ogType;
        private final String urlApplinksIos;
        private final String urlApplinksAndroid;
        private final String urlTwitterIos;
        private final String urlTwitterAndroid;
        private final String twitterCardType;
        private final String twitterSiteHandle;
        private final String schemaDotOrgType;
        private final Boolean noindex;
        private final Boolean unlisted;
        private final Boolean paid;
        private final Boolean familySafe;
        private final List<String> tags;
        private final List<String> availableCountries;
        private final PageOwnerDetailsImpl pageOwnerDetails;
        private final VideoDetailsImpl videoDetails;
        private final List<LinkImpl> linkAlternates;
        private final String viewCount;
        private final String publishDate;
        private final String category;
        private final String uploadDate;

        @Data
        public static class PageOwnerDetailsImpl implements PageOwnerDetails {

            private final String name;
            private final String externalChannelId;
            private final String youtubeProfileUrl;
        }

        @Data
        public static class VideoDetailsImpl implements VideoDetails {

            private final String externalVideoId;
            private final String durationSeconds;
            private final String durationIso8601;
        }

        @Data
        public static class LinkImpl implements Link {

            private final String hrefUrl;
            private final String title;
            private final String alternateType;
        }
    }
}

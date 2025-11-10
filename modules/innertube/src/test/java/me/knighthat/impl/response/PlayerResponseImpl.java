package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.impl.response.thumbnail.ThumbnailsImpl;
import me.knighthat.innertube.response.PlayerResponse;

import java.util.List;

@Data
public class PlayerResponseImpl implements PlayerResponse {

    private final ResponseContextImpl   responseContext;
    private final PlayabilityStatusImpl playabilityStatus;
    private final StreamingDataImpl streamingData;
    private final VideoDetailsImpl videoDetails;
    private final PlayerConfigImpl playerConfig;
    private final MicroformatImpl      microformat;
    private final String trackingParams;

    @Data
    public static class PlayabilityStatusImpl implements PlayabilityStatus {

        private final String status;
        private final String reason;
        private final Boolean                  playableInEmbed;
        private final AudioOnlyPlayabilityImpl audioOnlyPlayability;
        private final MiniplayerImpl           miniplayer;
        private final String                   contextParams;

        @Data
        public static class AudioOnlyPlayabilityImpl implements AudioOnlyPlayability {

            private final AudioOnlyPlayabilityRendererImpl audioOnlyPlayabilityRenderer;

            @Data
            public static class AudioOnlyPlayabilityRendererImpl implements AudioOnlyPlayabilityRenderer {

                private final String trackingParams;
                private final String audioOnlyAvailability;
            }
        }

        @Data
        public static class MiniplayerImpl implements Miniplayer {

            private final MiniplayerRendererImpl miniplayerRenderer;

            @Data
            public static class MiniplayerRendererImpl implements MiniplayerRenderer {

                private final String playbackMode;
            }
        }
    }

    @Data
    public static class StreamingDataImpl implements StreamingData {

        private final String           expiresInSeconds;
        private final List<FormatImpl> formats;
        private final List<FormatImpl> adaptiveFormats;
        private final String           serverAbrStreamingUrl;

        @Data
        public static class FormatImpl implements Format {

            private final Short itag;
            private final String url;
            private final String mimeType;
            private final Integer bitrate;
            private final Short width;
            private final Short     height;
            private final RangeImpl initRange;
            private final RangeImpl indexRange;
            private final String    lastModified;
            private final String contentLength;
            private final String quality;
            private final Byte fps;
            private final String qualityLabel;
            private final String projectionType;
            private final Integer       averageBitrate;
            private final ColorInfoImpl colorInfo;
            private final Boolean       highReplication;
            private final String audioQuality;
            private final String approxDurationMs;
            private final String audioSampleRate;
            private final Byte audioChannels;
            private final Float loudnessDb;
            private final String signatureCipher;

            @Data
            public static class RangeImpl implements Range {

                private final String start;
                private final String end;
            }

            @Data
            public static class ColorInfoImpl implements ColorInfo {

                private final String primaries;
                private final String transferCharacteristics;
                private final String matrixCoefficients;
            }
        }
    }

    @Data
    public static class VideoDetailsImpl implements VideoDetails {

        private final String videoId;
        private final String title;
        private final String lengthSeconds;
        private final String channelId;
        private final Boolean isOwnerViewing;
        private final Boolean        isCrawlable;
        private final ThumbnailsImpl thumbnail;
        private final Boolean        allowRatings;
        private final String viewCount;
        private final String author;
        private final Boolean isPrivate;
        private final Boolean isUnpluggedCorpus;
        private final String musicVideoType;
        private final Boolean isLiveContent;
    }

    @Data
    public static class PlayerConfigImpl implements PlayerConfig {

        private final AudioConfigImpl audioConfig;

        @Data
        public static class AudioConfigImpl implements AudioConfig {

            private final Float loudnessDb;
            private final Float perceptualLoudnessDb;
            private final Boolean enablePerFormatLoudness;
        }
    }
}
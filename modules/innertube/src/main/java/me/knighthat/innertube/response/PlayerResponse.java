package me.knighthat.innertube.response;

import org.jetbrains.annotations.NotNull;

import me.knighthat.innertube.response.thumbnail.Thumbnails;

import java.util.List;

public interface PlayerResponse extends Trackable {

    ResponseContext getResponseContext();

    PlayabilityStatus getPlayabilityStatus();

    StreamingData getStreamingData();

    VideoDetails getVideoDetails();

    PlayerConfig getPlayerConfig();

    Microformat getMicroformat();

    interface PlayabilityStatus {

        String getStatus();

        String getReason();

        Boolean getPlayableInEmbed();

        AudioOnlyPlayability getAudioOnlyPlayability();

        Miniplayer getMiniplayer();

        String getContextParams();

        interface AudioOnlyPlayability {

            AudioOnlyPlayabilityRenderer getAudioOnlyPlayabilityRenderer();

            interface AudioOnlyPlayabilityRenderer extends Trackable {

                String getAudioOnlyAvailability();
            }
        }

        interface Miniplayer {

            MiniplayerRenderer getMiniplayerRenderer();

            interface MiniplayerRenderer {

                String getPlaybackMode();
            }
        }
    }

    interface StreamingData {

        String getExpiresInSeconds();

        @NotNull
        List<? extends Format> getFormats();

        @NotNull
        List<? extends Format> getAdaptiveFormats();

        String getServerAbrStreamingUrl();

        interface Format {

            Short getItag();

            String getUrl();

            String getMimeType();

            Integer getBitrate();

            Short getWidth();

            Short getHeight();

            Range getInitRange();

            Range getIndexRange();

            String getLastModified();

            String getContentLength();

            String getQuality();

            Byte getFps();

            String getQualityLabel();

            String getProjectionType();

            Integer getAverageBitrate();

            ColorInfo getColorInfo();

            Boolean getHighReplication();

            String getAudioQuality();

            String getApproxDurationMs();

            String getAudioSampleRate();

            Byte getAudioChannels();

            Float getLoudnessDb();

            String getSignatureCipher();

            interface Range {

                String getStart();

                String getEnd();
            }

            interface ColorInfo {

                String getPrimaries();

                String getTransferCharacteristics();

                String getMatrixCoefficients();
            }
        }
    }

    interface VideoDetails {

        String getVideoId();

        String getTitle();

        String getLengthSeconds();

        String getChannelId();

        Boolean getIsOwnerViewing();

        Boolean getIsCrawlable();

        Thumbnails getThumbnail();

        Boolean getAllowRatings();

        String getViewCount();

        String getAuthor();

        Boolean getIsPrivate();

        Boolean getIsUnpluggedCorpus();

        String getMusicVideoType();

        Boolean getIsLiveContent();
    }

    interface PlayerConfig {

        AudioConfig getAudioConfig();

        interface AudioConfig {

            Float getLoudnessDb();

            Float getPerceptualLoudnessDb();

            Boolean getEnablePerFormatLoudness();
        }
    }
}
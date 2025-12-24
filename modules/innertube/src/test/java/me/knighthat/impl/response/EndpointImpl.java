package me.knighthat.impl.response;

import java.util.List;

import lombok.Data;
import me.knighthat.innertube.response.Endpoint;

@Data
public class EndpointImpl implements Endpoint {

    private final String clickTrackingParams;
    private final BrowseImpl browseEndpoint;
    private final WatchImpl watchEndpoint;
    private final ModalImpl modalEndpoint;
    private final WatchPlaylistImpl watchPlaylistEndpoint;
    private final QueueImpl.QueueAddImpl queueAddEndpoint;
    private final QueueImpl.QueueRemoveImpl removeFromQueueEndpoint;
    private final LikeImpl likeEndpoint;
    private final ShareEntityImpl shareEntityEndpoint;
    private final SearchImpl searchEndpoint;
    private final SignInImpl signInEndpoint;
    private final FeedbackImpl feedbackEndpoint;
    private final PlaylistEditImpl playlistEditEndpoint;
    private final QueueUpdateImpl queueUpdateCommand;
    private final SubscribeImpl subscribeEndpoint;
    private final SubscribeImpl unsubscribeEndpoint;
    private final SignalServiceImpl signalServiceEndpoint;

    @Data
    public static class BrowseImpl implements Browse {

        private final String browseId;
        private final String params;
        private final BrowseEndpointContextSupportedConfigsImpl browseEndpointContextSupportedConfigs;

        @Data
        public static class BrowseEndpointContextSupportedConfigsImpl implements BrowseEndpointContextSupportedConfigs {

            private final BrowseEndpointContextMusicConfigImpl browseEndpointContextMusicConfig;

            @Data
            public static class BrowseEndpointContextMusicConfigImpl implements BrowseEndpointContextMusicConfig {

                private final String pageType;
            }
        }
    }

    @Data
    public static class FeedbackImpl implements Feedback {

        private final String feedbackToken;
    }

    @Data
    public static class LikeImpl implements Like {

        private final String status;
        private final TargetImpl target;

        @Data
        public static class TargetImpl implements Target {

            private final String playlistId;
        }
    }

    @Data
    public static class ModalImpl implements Modal {

        private final RendererImpl modal;

        @Data
        public static class RendererImpl implements Renderer {

            private final TitleAndButtonRendererImpl modalWithTitleAndButtonRenderer;

            @Data
            public static class TitleAndButtonRendererImpl implements TitleAndButtonRenderer {

                private final RunsImpl title;
                private final RunsImpl content;
                private final ButtonImpl button;
            }
        }
    }

    @Data
    public static class PlaylistEditImpl implements PlaylistEdit {

        private final String playlistId;
        private final List<ActionImpl> actions;

        @Data
        public static class ActionImpl implements Action {

            private final String setVideoId;
            private final String action;
            private final String removedVideoId;
        }
    }

    @Data
    public static class QueueImpl implements Queue {

        @Data
        public static class QueueAddImpl implements QueueAdd {

            private final QueueTargetImpl queueTarget;
            private final String queueInsertPosition;
            private final List<CommandImpl> commands;

            @Data
            public static class QueueTargetImpl implements QueueTarget {

                private final String videoId;
                private final String playlistId;
                private final EndpointImpl onEmptyQueue;
            }
        }

        @Data
        public static class QueueRemoveImpl implements QueueRemove {

            private final String videoId;
            private final List<CommandImpl> commands;
        }

        @Data
        public static class CommandImpl implements Command {

            private final String clickTrackingParams;
            private final AddToToastActionImpl addToToastAction;

            @Data
            public static class AddToToastActionImpl implements AddToToastAction {

                private final ItemImpl item;

                @Data
                public static class ItemImpl implements Item {

                    private final NotificationTextRendererImpl notificationTextRenderer;

                    @Data
                    public static class NotificationTextRendererImpl implements NotificationTextRenderer {

                        private final RunsImpl successResponseText;
                        private final String trackingParams;
                    }
                }
            }
        }
    }

    @Data
    public static class QueueUpdateImpl implements QueueUpdate {

        private final String queueUpdateSection;
        private final FetchContentsCommandImpl fetchContentsCommand;
        private final Boolean dedupeAgainstLocalQueue;

        @Data
        public static class FetchContentsCommandImpl implements FetchContentsCommand {

            private final String clickTrackingParams;
            private final WatchImpl watchEndpoint;
        }
    }

    @Data
    public static class SearchImpl implements Search {

        private final String query;
        private final String params;
    }

    @Data
    public static class ShareEntityImpl implements ShareEntity {

        private final String serializedShareEntity;
        private final String sharePanelType;
    }

    @Data
    public static class SignInImpl implements SignIn {

        private final Boolean hack;
    }

    @Data
    public static class WatchImpl implements Watch {

        private final String videoId;
        private final String playlistId;
        private final Integer index;
        private final String params;
        private final String playerParams;
        private final String ustreamerConfig;
        private final String playlistSetVideoId;
        private final LoggingContextImpl loggingContext;
        private final WatchEndpointMusicSupportedConfigsImpl watchEndpointMusicSupportedConfigs;

        @Data
        public static class LoggingContextImpl implements LoggingContext {

            private final VssLoggingContextImpl vssLoggingContext;

            @Data
            public static class VssLoggingContextImpl implements VssLoggingContext {

                private final String serializedContextData;
            }
        }

        @Data
        public static class WatchEndpointMusicSupportedConfigsImpl implements WatchEndpointMusicSupportedConfigs {

            private final WatchEndpointMusicConfigImpl watchEndpointMusicConfig;

            @Data
            public static class WatchEndpointMusicConfigImpl implements WatchEndpointMusicConfig {

                private final Boolean hasPersistentPlaylistPanel;
                private final String musicVideoType;
            }
        }
    }

    @Data
    public static class WatchPlaylistImpl implements WatchPlaylist {

        private final String playlistId;
        private final String params;
    }

    @Data
    public static class SubscribeImpl implements Subscribe {

        private final List<String> channelIds;
        private final String params;
    }

    @Data
    public static class SignalServiceImpl implements SignalService {

        private final String signal;
        private final List<ActionImpl> actions;

        @Data
        public static class ActionImpl implements Action {

            private final String clickTrackingParams;
            private final OpenPopupImpl openPopupAction;

            @Data
            public static class OpenPopupImpl implements OpenPopup {

                private final PopupImpl popup;
                private final String popupType;

                @Data
                public static class PopupImpl implements Popup {

                    private final RendererImpl confirmDialogRenderer;

                    @Data
                    public static class RendererImpl implements Renderer {

                        private final String trackingParams;
                        private final List<RunsImpl> dialogMessages;
                        private final ButtonImpl confirmButton;
                        private final ButtonImpl cancelButton;
                    }
                }
            }
        }
    }
}
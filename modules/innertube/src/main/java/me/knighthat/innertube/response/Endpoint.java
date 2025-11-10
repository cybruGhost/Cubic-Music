package me.knighthat.innertube.response;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Endpoint extends ClickTrackable{

    Browse getBrowseEndpoint();

    Watch getWatchEndpoint();

    Modal getModalEndpoint();

    WatchPlaylist getWatchPlaylistEndpoint();

    Queue.QueueAdd getQueueAddEndpoint();

    Queue.QueueRemove getRemoveFromQueueEndpoint();

    Like getLikeEndpoint();

    ShareEntity getShareEntityEndpoint();

    Search getSearchEndpoint();

    SignIn getSignInEndpoint();

    Feedback getFeedbackEndpoint();

    PlaylistEdit getPlaylistEditEndpoint();

    QueueUpdate getQueueUpdateCommand();

    Subscribe getSubscribeEndpoint();

    Subscribe getUnsubscribeEndpoint();

    SignalService getSignalServiceEndpoint();

    interface Browse {

        String getBrowseId();

        String getParams();

        BrowseEndpointContextSupportedConfigs getBrowseEndpointContextSupportedConfigs();

        interface BrowseEndpointContextSupportedConfigs {

            BrowseEndpointContextMusicConfig getBrowseEndpointContextMusicConfig();

            interface BrowseEndpointContextMusicConfig {

                String getPageType();
            }
        }
    }

    interface Feedback {

        String getFeedbackToken();
    }

    interface Like {

        String getStatus();

        Target getTarget();

        interface Target {

            String getPlaylistId();
        }
    }

    interface Modal {

        Renderer getModal();

        interface Renderer {

            TitleAndButtonRenderer getModalWithTitleAndButtonRenderer();

            interface TitleAndButtonRenderer {

                Runs getTitle();

                Runs getContent();

                Button getButton();
            }
        }
    }

    interface PlaylistEdit {

        String getPlaylistId();

        @NotNull
        List<? extends PlaylistEdit.Action> getActions();

        interface Action {

            String getSetVideoId();

            String getAction();

            String getRemovedVideoId();
        }
    }

    interface Queue {

        interface QueueAdd {

            QueueTarget getQueueTarget();

            String getQueueInsertPosition();

            @NotNull
            List<? extends Command> getCommands();

            interface QueueTarget {

                String getVideoId();

                String getPlaylistId();

                Endpoint getOnEmptyQueue();
            }
        }

        interface QueueRemove {

            String getVideoId();

            @NotNull
            List<? extends Command> getCommands();
        }

        interface Command extends ClickTrackable {

            AddToToastAction getAddToToastAction();

            interface AddToToastAction {

                Item getItem();

                interface Item {

                    Item.NotificationTextRenderer getNotificationTextRenderer();

                    interface NotificationTextRenderer extends Trackable{

                        Runs getSuccessResponseText();
                    }
                }
            }
        }
    }

    interface QueueUpdate {

        String getQueueUpdateSection();

        FetchContentsCommand getFetchContentsCommand();

        Boolean getDedupeAgainstLocalQueue();

        interface FetchContentsCommand extends ClickTrackable {

            Watch getWatchEndpoint();
        }
    }

    interface Search {

        String getQuery();

        String getParams();
    }

    interface ShareEntity {

        String getSerializedShareEntity();

        String getSharePanelType();
    }

    interface SignIn {

        Boolean getHack();
    }

    interface Watch {

        String getVideoId();

        String getPlaylistId();

        Integer getIndex();

        String getParams();

        String getPlayerParams();

        String getUstreamerConfig();

        String getPlaylistSetVideoId();

        LoggingContext getLoggingContext();

        WatchEndpointMusicSupportedConfigs getWatchEndpointMusicSupportedConfigs();

        interface LoggingContext {

            LoggingContext.VssLoggingContext getVssLoggingContext();

            interface VssLoggingContext {

                String getSerializedContextData();
            }
        }

        interface WatchEndpointMusicSupportedConfigs {

            WatchEndpointMusicConfig getWatchEndpointMusicConfig();

            interface WatchEndpointMusicConfig {

                Boolean getHasPersistentPlaylistPanel();

                String getMusicVideoType();
            }
        }
    }

    interface WatchPlaylist {

        String getPlaylistId();

        String getParams();
    }

    interface Subscribe {

        @NotNull
        List<String> getChannelIds();

        @Nullable
        String getParams();
    }

    interface SignalService {

        String getSignal();

        @NotNull
        List<? extends Action> getActions();

        interface Action extends ClickTrackable {

            OpenPopup getOpenPopupAction();

            interface OpenPopup {

                Popup getPopup();

                String getPopupType();

                interface Popup {

                    Renderer getConfirmDialogRenderer();

                    interface Renderer extends Trackable {

                        @NotNull
                        List<? extends Runs> getDialogMessages();

                        Button getConfirmButton();

                        Button getCancelButton();
                    }
                }
            }
        }
    }
}
package me.knighthat.innertube.response;

import java.util.List;

import me.knighthat.innertube.response.renderer.ChipCloud;

public interface Button {

    Renderer getButtonRenderer();

    Toggle getToggleButtonRenderer();

    ChipCloud.Chip.Renderer getChipCloudChipRenderer();

    Like getLikeButtonRenderer();

    Subscribe getSubscribeButtonRenderer();

    interface Renderer extends Trackable {

        String getStyle();

        Boolean getIsDisabled();

        String getSize();

        Runs getText();

        Icon getIcon();

        Endpoint getNavigationEndpoint();

        Endpoint getServiceEndpoint();

        Accessibility.Data getAccessibility();

        Accessibility getAccessibilityData();
    }

    interface Like extends Trackable {

        Target getTarget();

        String getLikeStatus();

        Boolean getLikesAllowed();

        Endpoint getDislikeNavigationEndpoint();

        Endpoint getLikeCommand();

        interface Target {

            String getVideoId();
        }
    }

    interface MusicPlay extends Trackable {

        Endpoint getPlayNavigationEndpoint();

        Icon getPlayIcon();

        Icon getPauseIcon();

        Long getIconColor();

        Long getBackgroundColor();

        Long getActiveBackgroundColor();

        Long getLoadingIndicatorColor();

        Icon getPlayingIcon();

        Long getIconLoadingColor();

        Float getActiveScaleFactor();

        String getButtonSize();

        String getRippleTarget();

        Accessibility getAccessibilityPlayData();

        Accessibility getAccessibilityPauseData();
    }

    interface Toggle extends Trackable {

        Boolean getIsToggled();

        Boolean getIsDisabled();

        Icon getDefaultIcon();

        Endpoint getDefaultServiceEndpoint();

        Runs getDefaultText();

        Icon getToggledIcon();

        Endpoint getToggledServiceEndpoint();

        Runs getToggledText();

        Endpoint getDefaultNavigationEndpoint();

        Accessibility getAccessibilityData();

        Accessibility getToggledAccessibilityData();
    }

    interface Subscribe extends Trackable {

        Runs getSubscriberCountText();

        Boolean getSubscribed();

        Boolean getEnabled();

        String getType();

        String getChannelId();

        Boolean getShowPreference();

        Runs getSubscriberCountWithSubscribeText();

        Runs getSubscribedButtonText();

        Runs getUnsubscribedButtonText();

        Runs getUnsubscribeButtonText();

        List<? extends Endpoint> getServiceEndpoints();

        Runs getLongSubscriberCountText();

        Runs getShortSubscriberCountText();

        Accessibility getSubscribeAccessibility();

        Accessibility getUnsubscribeAccessibility();

        Endpoint getSignInEndpoint();
    }
}
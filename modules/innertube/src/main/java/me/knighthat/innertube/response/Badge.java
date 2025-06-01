package me.knighthat.innertube.response;

public interface Badge {

    Renderer getMusicInlineBadgeRenderer();

    interface Renderer extends Trackable {

        Icon getIcon();

        Accessibility getAccessibilityData();
    }
}

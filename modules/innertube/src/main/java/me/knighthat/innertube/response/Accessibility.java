package me.knighthat.innertube.response;

public interface Accessibility {

    Data getAccessibilityData();

    interface Data {

        String getLabel();
    }
}

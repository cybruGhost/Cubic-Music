package me.knighthat.innertube.response;

public interface Continuation {

    Data getNextContinuationData();

    Data getNextRadioContinuationData();

    interface Data extends ClickTrackable {

        String getContinuation();
    }
}

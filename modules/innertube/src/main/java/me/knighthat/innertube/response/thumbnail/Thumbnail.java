package me.knighthat.innertube.response.thumbnail;

import me.knighthat.innertube.response.Trackable;

public interface Thumbnail {

    Renderer getMusicThumbnailRenderer();

    interface Renderer extends Trackable {

        Thumbnails getThumbnail();

        String getThumbnailCrop();

        String getThumbnailScale();
    }
}
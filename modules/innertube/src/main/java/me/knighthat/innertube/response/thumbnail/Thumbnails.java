package me.knighthat.innertube.response.thumbnail;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Thumbnails {

    @NotNull
    List<? extends Thumbnail> getThumbnails();

    interface Thumbnail {

        String getUrl();

        Short getWidth();

        Short getHeight();
    }
}
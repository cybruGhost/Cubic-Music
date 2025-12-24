package me.knighthat.impl.response.thumbnail;

import lombok.Data;
import me.knighthat.innertube.response.thumbnail.Thumbnails;

import java.util.List;

@Data
public class ThumbnailsImpl implements Thumbnails {

    private final List<ThumbnailImpl> thumbnails;

    @Data
    public static class ThumbnailImpl implements Thumbnail {

        private final String url;
        private final Short width;
        private final Short height;
    }
}
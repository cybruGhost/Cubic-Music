package me.knighthat.impl.response.thumbnail;

import lombok.Data;
import me.knighthat.innertube.response.thumbnail.Thumbnail;

@Data
public class ThumbnailImpl implements Thumbnail {

    private final RendererImpl musicThumbnailRenderer;

    @Data
    public static class RendererImpl implements Renderer {

        private final ThumbnailsImpl thumbnail;
        private final String         thumbnailCrop;
        private final String thumbnailScale;
        private final String trackingParams;
    }
}
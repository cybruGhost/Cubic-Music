package me.knighthat.impl.response.renderer;

import lombok.Data;
import me.knighthat.innertube.response.renderer.MusicPlaylistShelfRenderer;

import java.util.List;

@Data
public class MusicPlaylistShelfRendererImpl implements MusicPlaylistShelfRenderer {

    private final String            playlistId;
    private final List<ContentImpl> contents;
    private final Integer           collapsedItemCount;
    private final String trackingParams;
    private final Boolean contentsMultiSelectable;
    private final String targetId;

    @Data
    public static class ContentImpl implements Content {

        private final MusicResponsiveListItemRendererImpl musicResponsiveListItemRenderer;
    }
}
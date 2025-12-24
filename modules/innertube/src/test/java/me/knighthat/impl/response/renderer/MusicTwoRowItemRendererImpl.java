package me.knighthat.impl.response.renderer;

import lombok.Data;
import me.knighthat.impl.response.*;
import me.knighthat.impl.response.thumbnail.ThumbnailImpl;
import me.knighthat.innertube.response.renderer.MusicTwoRowItemRenderer;

import java.util.List;

@Data
public class MusicTwoRowItemRendererImpl implements MusicTwoRowItemRenderer {

    private final ThumbnailImpl thumbnailRenderer;
    private final String        aspectRatio;
    private final RunsImpl title;
    private final RunsImpl     subtitle;
    private final EndpointImpl navigationEndpoint;
    private final String      trackingParams;
    private final MenuImpl    menu;
    private final OverlayImpl thumbnailOverlay;
    private final List<BadgeImpl> subtitleBadges;
}
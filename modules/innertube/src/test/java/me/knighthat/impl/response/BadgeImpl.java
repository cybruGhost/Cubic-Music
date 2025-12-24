package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.innertube.response.Badge;

@Data
public class BadgeImpl implements Badge {

    private final RendererImpl musicInlineBadgeRenderer;

    @Data
    public static class RendererImpl implements Renderer {

        private final String trackingParams;
        private final IconImpl icon;
        private final AccessibilityImpl accessibilityData;
    }
}

package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.innertube.response.Menu;

import java.util.List;

@Data
public class MenuImpl implements Menu {

    private final RendererImpl menuRenderer;

    @Data
    public static class RendererImpl implements Renderer {

        private final List<ItemImpl> items;
        private final String         trackingParams;
        private final List<ButtonImpl> topLevelButtons;
        private final AccessibilityImpl accessibility;

        @Data
        public static class ItemImpl implements Item {

            private final NavigationImpl menuNavigationItemRenderer;
            private final ServiceImpl menuServiceItemRenderer;
            private final ToggleImpl  toggleMenuServiceItemRenderer;

            @Data
            public static class NavigationImpl implements Navigation {

                private final RunsImpl text;
                private final IconImpl icon;
                private final EndpointImpl navigationEndpoint;
                private final String trackingParams;
            }

            @Data
            public static class ServiceImpl implements Service {

                private final RunsImpl text;
                private final IconImpl icon;
                private final EndpointImpl serviceEndpoint;
                private final String trackingParams;
            }

            @Data
            public static class ToggleImpl implements Toggle {

                private final RunsImpl defaultText;
                private final IconImpl defaultIcon;
                private final EndpointImpl defaultServiceEndpoint;
                private final RunsImpl toggledText;
                private final IconImpl toggledIcon;
                private final EndpointImpl toggledServiceEndpoint;
                private final String trackingParams;
            }
        }
    }
}
package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.innertube.response.Accessibility;

@Data
public class AccessibilityImpl implements Accessibility {

    private final DataImpl accessibilityData;

    @lombok.Data
    public static class DataImpl implements Data {

        private final String label;
    }
}

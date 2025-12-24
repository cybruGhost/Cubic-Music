package me.knighthat.impl.response;

import lombok.Data;
import me.knighthat.innertube.response.Icon;

@Data
public class IconImpl implements Icon {

    private final String iconType;
}

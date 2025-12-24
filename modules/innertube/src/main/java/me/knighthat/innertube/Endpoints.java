package me.knighthat.innertube;

import org.jetbrains.annotations.NotNull;

public class Endpoints {

    private Endpoints() {}

    @NotNull
    public static final String BROWSE = "youtubei/v1/browse";
    @NotNull
    public static final String NEXT = "youtubei/v1/next";
    @NotNull
    public static final String PLAYER = "youtubei/v1/player";
    @NotNull
    public static final String SEARCH = "youtubei/v1/search";
    @NotNull
    public static final String SEARCH_SUGGESTIONS = "youtubei/v1/music/get_search_suggestions";
}

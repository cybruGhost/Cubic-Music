package me.knighthat.innertube.request.body;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
public class BrowseBody implements RequestBody {

    @NotNull
    private final Context context;
    @NotNull
    private final String browseId;
    @NotNull
    private final String playlistId;
    @Nullable
    private final String params;
}

package me.knighthat.innertube.request.body;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
public class PlayerBody implements RequestBody {

    @NotNull
    private final Context context;
    @NotNull
    private final String videoId;
    @Nullable
    private final String params;
}

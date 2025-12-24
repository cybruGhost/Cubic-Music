package me.knighthat.innertube.request.body;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class SearchSuggestionsBody implements RequestBody {

    @NotNull
    private final Context context;
    @NotNull
    private final String input;
}

package me.knighthat.innertube.request.body;

import lombok.Data;
import me.knighthat.innertube.SearchFilter;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
public class SearchBody implements RequestBody {

    @NotNull
    private final Context context;
    @NotNull
    private final String  query;
    @Nullable
    @MagicConstant(valuesFromClass = SearchFilter.class)
    private final String params;
}

package app.it.fast4x.rimusic.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.layout.onSizeChanged
import app.it.fast4x.rimusic.appRunningInBackground
import app.it.fast4x.rimusic.colorPalette
import app.it.fast4x.rimusic.enums.AnimatedGradient
import app.it.fast4x.rimusic.enums.BackgroundProgress
import app.it.fast4x.rimusic.enums.ColorPaletteMode
import app.it.fast4x.rimusic.enums.PlayerBackgroundColors
import app.it.fast4x.rimusic.enums.PlayerType
import app.it.fast4x.rimusic.ui.components.themed.animateBrushRotation
import app.it.fast4x.rimusic.ui.styling.ColorPalette
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.BlackCherryCosmos
import com.mikepenz.hypnoticcanvas.shaders.GlossyGradients
import com.mikepenz.hypnoticcanvas.shaders.GoldenMagma
import com.mikepenz.hypnoticcanvas.shaders.GradientFlow
import com.mikepenz.hypnoticcanvas.shaders.IceReflection
import com.mikepenz.hypnoticcanvas.shaders.InkFlow
import com.mikepenz.hypnoticcanvas.shaders.MeshGradient
import com.mikepenz.hypnoticcanvas.shaders.MesmerizingLens
import com.mikepenz.hypnoticcanvas.shaders.OilFlow
import com.mikepenz.hypnoticcanvas.shaders.PurpleLiquid
import com.mikepenz.hypnoticcanvas.shaders.Shader
import com.mikepenz.hypnoticcanvas.shaders.Stage
import kotlin.Float.Companion.POSITIVE_INFINITY

@Composable
internal fun rememberPlayerBackgroundModifier(
    isGradientBackgroundEnabled: Boolean,
    playerBackgroundColors: PlayerBackgroundColors,
    playerType: PlayerType,
    showthumbnail: Boolean,
    albumCoverRotation: Boolean,
    bottomgradient: Boolean,
    colorPaletteMode: ColorPaletteMode,
    expandedplayer: Boolean,
    isLandscape: Boolean,
    dynamicColorPalette: ColorPalette,
    basePalette: ColorPalette,
    blackgradient: Boolean,
    lightTheme: Boolean,
    animatedGradient: AnimatedGradient,
    tempGradient: AnimatedGradient,
    dominant: Int,
    vibrant: Int,
    lightVibrant: Int,
    darkVibrant: Int,
    muted: Int,
    lightMuted: Int,
    darkMuted: Int,
    isPlaying: Boolean,
    saturate: @Composable (Int) -> Color,
    darkenBy: (Color) -> Color,
): Modifier {
    var backgroundModifier: Modifier = Modifier
    var sizeShader by remember { mutableStateOf(Size.Zero) }

    if (!isGradientBackgroundEnabled) {
        backgroundModifier = when {
            playerBackgroundColors == PlayerBackgroundColors.BlurredCoverColor &&
                (playerType == PlayerType.Essential || (showthumbnail && !albumCoverRotation)) -> {
                backgroundModifier
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            1.0f to if (bottomgradient) {
                                if (colorPaletteMode == ColorPaletteMode.Light) {
                                    Color.White.copy(if (isLandscape) 0.8f else 0.75f)
                                } else {
                                    Color.Black.copy(if (isLandscape) 0.8f else 0.75f)
                                }
                            } else {
                                Color.Transparent
                            },
                            startY = if (isLandscape) 600f else if (expandedplayer) 1300f else 950f,
                            endY = POSITIVE_INFINITY
                        )
                    )
                    .background(
                        if (bottomgradient) {
                            if (isLandscape) {
                                if (colorPaletteMode == ColorPaletteMode.Light) Color.White.copy(0.25f)
                                else Color.Black.copy(0.25f)
                            } else {
                                Color.Transparent
                            }
                        } else {
                            Color.Transparent
                        }
                    )
            }

            playerBackgroundColors == PlayerBackgroundColors.ColorPalette -> {
                backgroundModifier.drawBehind {
                    val colors = listOf(
                        Color(dominant),
                        Color(vibrant),
                        Color(lightVibrant),
                        Color(darkVibrant),
                        Color(muted),
                        Color(lightMuted),
                        Color(darkMuted)
                    )
                    val stripeHeight = size.height / colors.size
                    colors.forEachIndexed { index, color ->
                        drawRect(
                            color = color,
                            topLeft = Offset(0f, index * stripeHeight),
                            size = Size(size.width, stripeHeight)
                        )
                    }
                }
            }

            playerBackgroundColors == PlayerBackgroundColors.CoverColor -> {
                backgroundModifier.background(dynamicColorPalette.background1)
            }

            playerBackgroundColors == PlayerBackgroundColors.ThemeColor -> {
                backgroundModifier.background(basePalette.background1)
            }

            else -> backgroundModifier
        }
    } else {
        backgroundModifier = when (playerBackgroundColors) {
            PlayerBackgroundColors.AnimatedGradient -> {
                var background by remember { mutableStateOf(Color.Transparent) }
                var shaderCondition by remember { mutableStateOf(true) }
                var shader: Shader? by remember { mutableStateOf(null) }
                val type = remember(animatedGradient, tempGradient) {
                    if (animatedGradient == AnimatedGradient.Random) tempGradient else animatedGradient
                }

                when (type) {
                    AnimatedGradient.FluidThemeColorGradient, AnimatedGradient.FluidCoverColorGradient -> {
                        val shaderA = LinearGradientShader(
                            Offset(sizeShader.width / 2f, 0f),
                            Offset(sizeShader.width / 2f, sizeShader.height),
                            listOf(dynamicColorPalette.background2, colorPalette().background2),
                            listOf(0f, 1f)
                        )
                        val brushA by animateBrushRotation(shaderA, sizeShader, 20_000, true)

                        val shaderB = LinearGradientShader(
                            Offset(sizeShader.width / 2f, 0f),
                            Offset(sizeShader.width / 2f, sizeShader.height),
                            listOf(colorPalette().background1, dynamicColorPalette.accent),
                            listOf(0f, 1f)
                        )
                        val brushB by animateBrushRotation(shaderB, sizeShader, 12_000, false)

                        val shaderMask = LinearGradientShader(
                            Offset(sizeShader.width / 2f, 0f),
                            Offset(sizeShader.width / 2f, sizeShader.height),
                            listOf(colorPalette().background2, Color.Transparent),
                            listOf(0f, 1f)
                        )
                        val brushMask by animateBrushRotation(shaderMask, sizeShader, 15_000, true)

                        backgroundModifier.drawBehind {
                            drawRect(brush = brushA)
                            drawRect(brush = brushMask, blendMode = BlendMode.DstOut)
                            drawRect(brush = brushB, blendMode = BlendMode.DstAtop)
                        }
                    }

                    AnimatedGradient.Linear -> {
                        backgroundModifier.animatedGradient(
                            isPlaying,
                            darkenBy(saturate(dominant)),
                            darkenBy(saturate(vibrant)),
                            darkenBy(saturate(lightVibrant)),
                            darkenBy(saturate(darkVibrant)),
                            darkenBy(saturate(muted)),
                            darkenBy(saturate(lightMuted)),
                            darkenBy(saturate(darkMuted))
                        )
                    }

                    AnimatedGradient.Mesh -> {
                        shaderCondition = !appRunningInBackground
                        shader = MeshGradient(
                            colors = arrayOf(
                                darkenBy(saturate(vibrant)),
                                darkenBy(saturate(lightVibrant)),
                                darkenBy(saturate(darkVibrant)),
                                darkenBy(saturate(muted)),
                                darkenBy(saturate(lightMuted)),
                                darkenBy(saturate(darkMuted)),
                                darkenBy(saturate(dominant))
                            ),
                            scale = 1f
                        )
                        backgroundModifier
                    }

                    AnimatedGradient.MesmerizingLens -> {
                        shader = MesmerizingLens
                        backgroundModifier
                    }

                    AnimatedGradient.GlossyGradients -> {
                        if (!lightTheme) background = Color.Black.copy(.2f)
                        shader = GlossyGradients
                        backgroundModifier
                    }

                    AnimatedGradient.GradientFlow -> {
                        if (!lightTheme) background = Color.Black.copy(.2f)
                        shader = GradientFlow
                        backgroundModifier
                    }

                    AnimatedGradient.PurpleLiquid -> {
                        shader = PurpleLiquid
                        backgroundModifier
                    }

                    AnimatedGradient.InkFlow -> {
                        if (lightTheme) background = Color.White.copy(.4f)
                        shader = InkFlow
                        backgroundModifier
                    }

                    AnimatedGradient.OilFlow -> {
                        if (lightTheme) background = Color.White.copy(.4f)
                        shader = OilFlow
                        backgroundModifier
                    }

                    AnimatedGradient.IceReflection -> {
                        background = if (!lightTheme) Color.Black.copy(.3f) else Color.White.copy(.4f)
                        shader = IceReflection
                        backgroundModifier
                    }

                    AnimatedGradient.Stage -> {
                        if (!lightTheme) background = Color.Black.copy(.3f)
                        shader = Stage
                        backgroundModifier
                    }

                    AnimatedGradient.GoldenMagma -> {
                        background = if (!lightTheme) Color.Black.copy(.2f) else Color.White.copy(.3f)
                        shader = GoldenMagma
                        backgroundModifier
                    }

                    AnimatedGradient.BlackCherryCosmos -> {
                        if (lightTheme) background = Color.White.copy(.35f)
                        shader = BlackCherryCosmos
                        backgroundModifier
                    }

                    AnimatedGradient.Random -> error("AnimatedGradient.Random should be resolved earlier")
                }
                    .let { modifier ->
                        if (shaderCondition && shader != null) modifier.shaderBackground(shader!!) else modifier
                    }
                    .background(background)
                    .onSizeChanged {
                        sizeShader = Size(it.width.toFloat(), it.height.toFloat())
                    }
            }

            else -> {
                backgroundModifier.background(
                    Brush.verticalGradient(
                        0.5f to if (playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient) {
                            dynamicColorPalette.background1
                        } else {
                            basePalette.background1
                        },
                        1.0f to if (blackgradient) {
                            Color.Black
                        } else if (playerBackgroundColors == PlayerBackgroundColors.CoverColorGradient) {
                            dynamicColorPalette.background2
                        } else {
                            basePalette.background2
                        },
                        startY = 0.0f,
                        endY = 1500.0f
                    )
                )
            }
        }
    }

    return backgroundModifier
}

internal fun Modifier.playerProgressOverlay(
    shouldShowCanvas: Boolean,
    containerModifier: Modifier,
    backgroundProgress: BackgroundProgress,
    positionMs: Long,
    durationMs: Long,
    progressOverlayBrush: Brush?,
    favoritesOverlay: Color,
): Modifier {
    val baseModifier = if (!shouldShowCanvas) containerModifier else Modifier
    return baseModifier.drawBehind {
        if (backgroundProgress == BackgroundProgress.Both || backgroundProgress == BackgroundProgress.Player) {
            val safeDuration = durationMs.coerceAtLeast(1L).toFloat()
            val progressSize = Size(
                width = (positionMs.coerceAtLeast(0L).toFloat() / safeDuration) * size.width,
                height = size.maxDimension
            )
            if (progressOverlayBrush != null) {
                drawRect(
                    brush = progressOverlayBrush,
                    topLeft = Offset.Zero,
                    size = progressSize
                )
            } else {
                drawRect(
                    color = favoritesOverlay,
                    topLeft = Offset.Zero,
                    size = progressSize
                )
            }
        }
    }
}

package app.it.fast4x.rimusic.extensions.nextvisualizer.utils

import android.graphics.*
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.Painter
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.fft.FftBar
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.fft.FftCLine
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.fft.FftCWaveRgb
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.misc.Background
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.misc.Icon
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.modifier.Compose
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.modifier.Rotate
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.modifier.Scale
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.modifier.Shake

class Preset {
    companion object {

        /**
         * Feel free to add your awesome preset here ;)
         * Hint: You can use `Compose` painter to group multiple painters together as a single painter
         */
        fun getPreset(name: String): Painter {
            return when (name) {
                "debug" -> FftBar()
                else -> FftBar()
            }
        }

        fun getPresetWithBitmap(name: String, bitmap: Bitmap): Painter {
            return when (name) {
                "cIcon" -> Compose(Rotate(FftCLine()), Icon(Icon.getCircledBitmap(bitmap)))
                "cWaveRgbIcon" -> Compose(
                    Rotate(FftCWaveRgb()),
                    Icon(Icon.getCircledBitmap(bitmap))
                )
                "liveBg" -> Scale(Shake(Background(bitmap)), scaleX = 1.02f, scaleY = 1.02f)
                "debug" -> Icon(bitmap)
                else -> Icon(bitmap)
            }
        }
    }
}
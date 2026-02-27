package app.it.fast4x.rimusic.extensions.nextvisualizer.views

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.Painter
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.misc.SimpleText
import app.it.fast4x.rimusic.extensions.nextvisualizer.painters.modifier.Compose
import app.it.fast4x.rimusic.extensions.nextvisualizer.utils.FrameManager
import app.it.fast4x.rimusic.extensions.nextvisualizer.utils.VisualizerHelper

class VisualizerView : View {

    private val frameManager = FrameManager()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val simpleText: SimpleText = SimpleText().apply { paint.textSize = dp2px(resources, 12f) }
    private lateinit var painter: Painter
    private lateinit var visualizerHelper: VisualizerHelper

    var anim = true
    var fps = false

    companion object {
        private fun dp2px(resources: Resources, dp: Float): Float {
            return dp * resources.displayMetrics.density
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setup(visualizerHelper: VisualizerHelper, painter: Painter) {
        this.visualizerHelper = visualizerHelper
        this.painter = Compose(painter, simpleText)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (this::painter.isInitialized && this::visualizerHelper.isInitialized) {
            setLayerType(LAYER_TYPE_HARDWARE, paint)
            canvas?.apply {
                simpleText.text = if (fps) "FPS: ${frameManager.fps()}" else ""
                painter.calc(visualizerHelper)
                painter.draw(canvas, visualizerHelper)
            }
            frameManager.tick()
            if (anim) invalidate()
        }
    }
}
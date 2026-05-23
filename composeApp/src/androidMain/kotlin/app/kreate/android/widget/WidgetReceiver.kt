package app.kreate.android.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class VerticalWidgetReceiver(
    override val glanceAppWidget: GlanceAppWidget = Widget.Vertical
): GlanceAppWidgetReceiver()

class HorizontalWidgetReceiver(
    override val glanceAppWidget: GlanceAppWidget = Widget.Horizontal
): GlanceAppWidgetReceiver()

class RescueWidgetReceiver(
    override val glanceAppWidget: GlanceAppWidget = Widget.Rescue
): GlanceAppWidgetReceiver()

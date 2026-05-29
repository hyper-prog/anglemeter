package com.anglemeter.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.anglemeter.app.OverlayColorScheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun AngleOverlayCanvas(
    angleDeg: Float,
    isCoincident: Boolean,
    colorScheme: OverlayColorScheme,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val halfLength = hypot(size.width, size.height)
        val baseStrokeWidth = 3.dp.toPx()

        val fixedColor = if (colorScheme == OverlayColorScheme.BlackBrown) {
            Color.Black
        } else {
            Color.White
        }

        val movingColor = if (colorScheme == OverlayColorScheme.BlackBrown) {
            Color(0xFF7A4A20)
        } else {
            Color(0xFFFFB6C1)
        }

        fun drawExtendedLine(angle: Float, color: Color, strokeWidth: Float) {
            val radians = angle * PI.toFloat() / 180f
            val direction = Offset(cos(radians), sin(radians))
            val start = Offset(
                x = center.x - direction.x * halfLength,
                y = center.y - direction.y * halfLength
            )
            val end = Offset(
                x = center.x + direction.x * halfLength,
                y = center.y + direction.y * halfLength
            )

            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = strokeWidth
            )
        }

        fun drawCross(angle: Float, color: Color, strokeWidth: Float) {
            drawExtendedLine(angle = angle, color = color, strokeWidth = strokeWidth)
            drawExtendedLine(angle = angle + 90f, color = color, strokeWidth = strokeWidth)
        }

        drawCross(angle = 0f, color = fixedColor, strokeWidth = baseStrokeWidth)
        drawCross(angle = angleDeg, color = movingColor, strokeWidth = baseStrokeWidth)

        if (isCoincident) {
            val outlineStrokeWidth = 8.dp.toPx()
            val centerStrokeWidth = 5.dp.toPx()

            drawCross(
                angle = angleDeg,
                color = Color.Black,
                strokeWidth = outlineStrokeWidth
            )
            drawCross(
                angle = angleDeg,
                color = Color(0xFFD40000),
                strokeWidth = centerStrokeWidth
            )
        }
    }
}

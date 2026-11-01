package com.ramesh.sms.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * A chat bubble with a small pointed tail in the bottom corner - the default
 * "speech bubble" look.
 *
 * The bubble body stops [tail] short of the bottom so the tail can be drawn
 * inside the layout bounds (nothing is clipped), which is why callers add the
 * same amount of bottom padding to the bubble content.
 */
class TailBubbleShape(
    private val corner: Dp,
    private val tail: Dp,
    private val tailOnRight: Boolean
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return Outline.Generic(Path())

        val tailPx = with(density) { tail.toPx() }
            .coerceAtMost(width / 2f)
            .coerceAtMost(height / 2f)
        val bodyHeight = (height - tailPx).coerceAtLeast(1f)
        val radius = with(density) { corner.toPx() }
            .coerceAtMost(min(width, bodyHeight) / 2f)

        val path = Path()
        path.moveTo(radius, 0f)
        path.lineTo(width - radius, 0f)
        // Top-right corner.
        path.arcTo(Rect(width - 2 * radius, 0f, width, 2 * radius), 270f, 90f, false)

        if (tailOnRight) {
            // Right edge, then the tail sweeping down to the bottom-right point.
            path.lineTo(width, height)
            path.lineTo(width - tailPx, bodyHeight)
            path.lineTo(radius, bodyHeight)
            // Bottom-left corner.
            path.arcTo(
                Rect(0f, bodyHeight - 2 * radius, 2 * radius, bodyHeight),
                90f,
                90f,
                false
            )
        } else {
            path.lineTo(width, bodyHeight - radius)
            // Bottom-right corner.
            path.arcTo(
                Rect(width - 2 * radius, bodyHeight - 2 * radius, width, bodyHeight),
                0f,
                90f,
                false
            )
            path.lineTo(tailPx, bodyHeight)
            // Tail sweeping down to the bottom-left point.
            path.lineTo(0f, height)
        }

        path.lineTo(0f, radius)
        // Top-left corner.
        path.arcTo(Rect(0f, 0f, 2 * radius, 2 * radius), 180f, 90f, false)
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Builds the bubble shape for the user's chosen style.
 */
fun chatBubbleShape(outgoing: Boolean, style: BubbleStyle, tail: Boolean): Shape {
    val needsTail = tail && style.tailDp > 0f
    if (needsTail) {
        return TailBubbleShape(
            corner = style.cornerDp.dp,
            tail = style.tailDp.dp,
            tailOnRight = outgoing
        )
    }
    val corner = style.cornerDp.dp
    // Pill bubbles keep identical corners everywhere; the rest get the classic
    // small "tail side" corner used by other messengers.
    if (style == BubbleStyle.PILL) return RoundedCornerShape(corner)
    val small = 4.dp
    return if (outgoing) {
        RoundedCornerShape(
            topStart = corner,
            topEnd = corner,
            bottomEnd = small,
            bottomStart = corner
        )
    } else {
        RoundedCornerShape(
            topStart = corner,
            topEnd = corner,
            bottomEnd = corner,
            bottomStart = small
        )
    }
}

/**
 * Extra inner padding the bubble content needs so text never runs under the tail.
 */
fun bubbleContentBottomPadding(style: BubbleStyle, tail: Boolean): Dp =
    if (tail && style.tailDp > 0f) style.tailDp.dp else 0.dp

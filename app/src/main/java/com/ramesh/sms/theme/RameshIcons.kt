package com.ramesh.sms.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn icons for the few glyphs Material does not ship.
 *
 * The action strip follows the reference layout: share is Material's own share glyph (three nodes
 * and two plain lines, no arrowhead), forward is a semicircular arrow that starts at the bottom and
 * curves to the left, and select is the "list with a double check" mark rather than a bare double
 * check - a bare check is what a "mark as read" button looks like, and this one selects messages.
 */
object RameshIcons {

    /**
     * Forward: a semicircular arrow that leaves the message from the bottom and points to the left.
     *
     * Drawn with a stroke instead of a filled block so its weight matches the neighbouring Material
     * icons at 21 dp.
     */
    val Forward: ImageVector by lazy {
        ImageVector.Builder(
            name = "RameshForward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .addPath(
                pathData = nodes("M19.2,20.5 C19.2,12.6 15.1,9.6 8.6,9.6"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            )
            .addPath(
                pathData = nodes("M12.6,4.6 L7.2,9.6 L12.6,14.6"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            .build()
    }

    /**
     * Select: a short rule above a double check, the mark that means "act on this message" in the
     * strip. The rule is what separates it at a glance from the read/unread double check.
     */
    val SelectList: ImageVector by lazy {
        ImageVector.Builder(
            name = "RameshSelectList",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .addPath(
                pathData = nodes("M5,5.2 L19,5.2"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            )
            .addPath(
                pathData = nodes("M2.6,12.6 L6.6,16.6 L13.2,9.2"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            .addPath(
                pathData = nodes("M11.4,17.6 L15.4,21.6 L22,14.2"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            .build()
    }

    private fun nodes(pathData: String) = PathParser().parsePathString(pathData).toNodes()
}

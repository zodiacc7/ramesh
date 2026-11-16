package com.ramesh.sms.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn icons for the glyphs Material does not ship in the shape the reference strip uses.
 *
 * Every icon here is drawn on the same 24x24 grid and at the same visual weight as the Material
 * icons next to it, because a strip that mixes two icon families looks like a mistake:
 *
 * - **Share** - three nodes joined by two lines, with an arrowhead on the upper node. Material's
 *   share glyph has no arrowhead.
 * - **Forward** - a filled arrow whose tail swoops up from the bottom left to a head pointing up
 *   and to the right, exactly as in the reference.
 * - **Trash** - a filled can with a separate lid bar, not Material's outlined bin.
 * - **Info** - a solid disc with the "i" knocked out of it.
 * - **Select** - a check followed by two rules, not a bare double check: a bare check reads as
 *   "mark as read", and this button selects messages.
 */
object RameshIcons {

    /** Share: two dots, the connecting lines, and an arrow leaving towards the top right. */
    val Share: ImageVector by lazy {
        ImageVector.Builder(
            name = "RameshShare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .addPath(
                pathData = nodes(
                    "M6,16.2 A2.7,2.7 0 1,1 6,10.8 A2.7,2.7 0 1,1 6,16.2 Z " +
                        "M18.2,22.1 A2.7,2.7 0 1,1 18.2,16.7 A2.7,2.7 0 1,1 18.2,22.1 Z"
                ),
                fill = SolidColor(Color.Black)
            )
            .addPath(
                pathData = nodes("M7.9,14.8 L16.4,18.5 M7.9,12.2 L14.4,8.5"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            )
            .addPath(
                pathData = nodes("M21.7,2.3 L20.7,10.4 L13.5,6.1 Z"),
                fill = SolidColor(Color.Black)
            )
            .build()
    }

    /**
     * Forward: a filled arrow whose tail curves up from the bottom left and whose head points up
     * and to the right.
     *
     * The curve is stroked and the head filled: the stroke keeps the tail at the same weight as the
     * neighbouring icons, which a single filled blob does not.
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
                pathData = nodes("M3.6,20.4 C4.5,13.6 8.8,9.3 15.0,8.4"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round
            )
            .addPath(
                pathData = nodes("M21.9,2.6 L20.3,11.6 L11.9,5.1 Z"),
                fill = SolidColor(Color.Black)
            )
            .build()
    }

    /** Trash: a filled can with a separate lid bar. */
    val Trash: ImageVector by lazy {
        ImageVector.Builder(
            name = "RameshTrash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            // Lid: a flat bar with rounded ends, separated from the body by a gap.
            .addPath(
                pathData = nodes(
                    "M4.1,6.2 A1.1,1.1 0 0,1 5.2,5.1 L18.8,5.1 A1.1,1.1 0 0,1 19.9,6.2 " +
                        "L19.9,7.4 A0.9,0.9 0 0,1 19.0,8.3 L5.0,8.3 A0.9,0.9 0 0,1 4.1,7.4 Z"
                ),
                fill = SolidColor(Color.Black)
            )
            // Body: slightly tapered with a rounded bottom.
            .addPath(
                pathData = nodes(
                    "M6.5,10.1 L17.5,10.1 L16.7,19.9 " +
                        "A2.0,2.0 0 0,1 14.7,21.7 L9.3,21.7 " +
                        "A2.0,2.0 0 0,1 7.3,19.9 Z"
                ),
                fill = SolidColor(Color.Black)
            )
            .build()
    }

    /**
     * Info: a solid disc with the "i" cut out.
     *
     * The cut-out is a hole in the same path (the disc runs clockwise, the letter runs
     * counter-clockwise), so it takes the colour of whatever is behind the icon instead of needing a
     * hard-coded background.
     */
    val InfoFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "RameshInfoFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .addPath(
                pathData = nodes(
                    // Disc, clockwise.
                    "M12,2 A10,10 0 1,1 12,22 A10,10 0 1,1 12,2 Z " +
                        // Dot of the "i", counter-clockwise.
                        "M12,6.0 A1.8,1.8 0 1,0 12,9.6 A1.8,1.8 0 1,0 12,6.0 Z " +
                        // Stem, counter-clockwise.
                        "M10.9,11.2 L10.9,18.2 L13.1,18.2 L13.1,11.2 Z"
                ),
                fill = SolidColor(Color.Black)
            )
            .build()
    }

    /** Select: a check followed by two rules. */
    val SelectList: ImageVector by lazy {
        ImageVector.Builder(
            name = "RameshSelectList",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .addPath(
                pathData = nodes("M2.8,12.6 L7.0,16.8 L13.4,8.6"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            .addPath(
                pathData = nodes("M15.2,8.7 L21.6,8.7 M15.2,15.4 L21.6,15.4"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            )
            .build()
    }

    private fun nodes(pathData: String) = PathParser().parsePathString(pathData).toNodes()
}

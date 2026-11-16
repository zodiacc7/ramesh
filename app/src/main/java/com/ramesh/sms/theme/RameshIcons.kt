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
 * - **Forward** - a filled arrow whose head points right and whose tail sweeps down to the bottom
 *   left, exactly as in the reference.
 * - **Trash** - a filled can with a separate lid bar, not Material's outlined bin.
 * - **Info** - a solid disc with the "i" knocked out of it.
 * - **Select** - a rounded square bracket with a check through its open right side, not a bare
 *   double check: a bare check reads as "mark as read", and this button selects messages.
 */
object RameshIcons {

    /**
     * Forward: a filled arrow whose head points right and whose tail sweeps down to the bottom left.
     *
     * The head is a triangle on a vertical base, tall enough to span the icon the way the sample
     * does; the tail is a round-capped stroke leaving the middle of that base, so the two read as one
     * solid arrow instead of a small head on a thin tail.
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
                pathData = nodes("M12.9,0.4 L23.8,11.5 L12.9,22.6 Z"),
                fill = SolidColor(Color.Black)
            )
            .addPath(
                pathData = nodes("M12.4,6.4 C7.2,7.4 3.6,12.8 2.3,21.4"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 4.6f,
                strokeLineCap = StrokeCap.Round
            )
            .build()
    }

    /**
     * Select: a rounded square bracket, open on the right, with a check inside and through the gap -
     * the sample the user sent. A plain double check reads as "mark as read" instead.
     */
    val SelectCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "RameshSelectCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .addPath(
                pathData = nodes(
                    // Top edge from the right stub, around the left side, along the bottom to the
                    // lower right stub. The two ends are the rounded caps of the open right side.
                    "M17.0,4.6 L8.6,4.6 " +
                        "A3.4,3.4 0 0,0 5.2,8.0 L5.2,16.0 " +
                        "A3.4,3.4 0 0,0 8.6,19.4 L17.0,19.4"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.3f,
                strokeLineCap = StrokeCap.Round
            )
            .addPath(
                pathData = nodes("M8.4,12.0 L11.6,15.6 L18.6,6.9"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.3f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
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
            .addPath(
                pathData = nodes(
                    "M4.1,6.2 A1.1,1.1 0 0,1 5.2,5.1 L18.8,5.1 A1.1,1.1 0 0,1 19.9,6.2 " +
                        "L19.9,7.4 A0.9,0.9 0 0,1 19.0,8.3 L5.0,8.3 A0.9,0.9 0 0,1 4.1,7.4 Z"
                ),
                fill = SolidColor(Color.Black)
            )
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

    /** Info: a solid disc with the "i" cut out of it. */
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
                    "M12,2 A10,10 0 1,1 12,22 A10,10 0 1,1 12,2 Z " +
                        "M12,6.0 A1.8,1.8 0 1,0 12,9.6 A1.8,1.8 0 1,0 12,6.0 Z " +
                        "M10.9,11.2 L10.9,18.2 L13.1,18.2 L13.1,11.2 Z"
                ),
                fill = SolidColor(Color.Black)
            )
            .build()
    }

    private fun nodes(pathData: String) = PathParser().parsePathString(pathData).toNodes()
}

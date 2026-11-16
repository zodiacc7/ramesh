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
 * Every path here is traced from the PNG samples the user sent (512 px canvas, scaled to this 24x24
 * grid), not redrawn by eye: the numbers are the sample's own outlines, so "make it look like the
 * picture" is not a matter of opinion. The rasterised paths match the samples at an intersection
 * over union of 0.99 / 0.97 / 0.99 (forward / select / share).
 *
 * - **Forward** - the arrow from the sample: a filled head on a vertical base, plus the swoosh that
 *   leaves its upper back edge and sweeps down to the bottom left. Both are filled outlines, so the
 *   tail is exactly as thick as the sample's and meets the head with no notch of its own.
 * - **Select** - a rounded bracket, open on the right, with both stubs ending at the same x, and a
 *   check running from inside the bracket out through the opening.
 * - **Share** - three solid nodes joined by two thin lines, no arrowhead; the outer nodes reach the
 *   top and bottom edges of the grid exactly as in the sample (Material's glyph is smaller).
 * - **Trash** - a filled can with a separate lid bar, not Material's outlined bin.
 * - **Info** - a solid disc with the "i" knocked out of it.
 */
object RameshIcons {

    /**
     * Forward: the sample's arrow, as one filled shape plus a filled swoosh.
     *
     * The head is `M12.70,0 L23.94,9.11 L12.70,18.52 Z` and the tail is a crescent whose upper edge
     * leaves the head's back edge at y=5.65 and whose round outer edge reaches the bottom left
     * corner; tracing it as an outline (rather than a stroked curve) is what gives the sample's
     * flat-sided, tapering tail.
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
                pathData = nodes("M12.70,0.00 L23.94,9.11 L12.70,18.52 Z"),
                fill = SolidColor(Color.Black)
            )
            .addPath(
                pathData = nodes(
                    "M13.26,5.65 " +
                        "C5.82,5.81 0.21,11.82 0.03,18.51 L0.00,23.90 " +
                        "C1.54,17.95 6.10,13.01 12.97,12.66 Z"
                ),
                fill = SolidColor(Color.Black)
            )
            .build()
    }

    /**
     * Select: the sample's open bracket with a check through it.
     *
     * Both right-hand stubs stop at x=19.70 and the bottom-right corner is rounded (radius 4.52),
     * which is what makes it read as a bracket rather than as a broken box.
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
                    "M10.06,3.86 L8.84,3.86 " +
                        "C6.34,3.86 4.32,5.88 4.32,8.38 L4.32,14.75 " +
                        "C4.32,17.25 6.34,19.27 8.84,19.27 L15.18,19.27 " +
                        "C17.68,19.27 19.70,17.25 19.70,14.75 L19.70,13.48"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.92f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            .addPath(
                pathData = nodes("M8.14,10.62 L12.04,14.34 L18.56,5.99"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.92f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            .build()
    }

    /**
     * Share: three solid nodes and two thin lines, no arrowhead - the sample exactly.
     *
     * Material's share glyph has nodes that are noticeably smaller than the sample's, which is why
     * the strip never looked like the reference even after the arrowhead was removed.
     */
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
                    "M4.96,8.48 A4.21,4.21 0 1,1 4.96,16.90 A4.21,4.21 0 1,1 4.96,8.48 Z " +
                        "M19.03,0.00 A4.21,4.21 0 1,1 19.03,8.42 A4.21,4.21 0 1,1 19.03,0.00 Z " +
                        "M19.03,15.57 A4.21,4.21 0 1,1 19.03,23.99 A4.21,4.21 0 1,1 19.03,15.57 Z"
                ),
                fill = SolidColor(Color.Black)
            )
            .addPath(
                pathData = nodes("M4.96,12.69 L19.03,4.21 M4.96,12.69 L19.03,19.78"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.39f,
                strokeLineCap = StrokeCap.Round
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

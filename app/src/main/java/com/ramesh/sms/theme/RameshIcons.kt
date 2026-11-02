package com.ramesh.sms.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn icons for the few glyphs Material does not ship.
 *
 * Material's `Share` is three nodes joined by two plain lines. The share glyph Textra draws - and the
 * one users recognise from the Android system share sheet - has an **arrowhead** on the upper node,
 * pointing away from the group. That difference is why the Material version looked like the wrong
 * icon in the action strip, so the shape is built here: two dots, two connecting lines, one arrow.
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

    private fun nodes(pathData: String) = PathParser().parsePathString(pathData).toNodes()
}

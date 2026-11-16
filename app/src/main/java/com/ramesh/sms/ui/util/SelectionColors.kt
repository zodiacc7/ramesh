package com.ramesh.sms.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * A selection highlight that is guaranteed to be visible on [background].
 *
 * The default selection colour is the theme's primary, and on a bubble (or a row) painted in that
 * same accent the highlight simply vanishes. Deriving it from the *background's own brightness*
 * instead - a light veil on dark surfaces, a dark veil on light ones - keeps it visible whatever
 * theme, bubble tone or wallpaper is in use.
 *
 * The alpha is deliberately high enough to be obvious. A "subtle" 10-20% tint is exactly what users
 * report as "the selection has the same colour as the background".
 */
fun selectionHighlight(background: Color): Color {
    val dark = background.luminance() < 0.5f
    return if (dark) {
        // Light veil over a dark surface.
        Color.White.copy(alpha = 0.42f)
    } else {
        // Dark veil over a light surface. Black at this alpha is clearly darker than any light tone
        // while still letting the text underneath stay readable.
        Color.Black.copy(alpha = 0.26f)
    }
}

/**
 * The colour of the selection handles that go with [selectionHighlight].
 *
 * Handles are drawn as small filled shapes, so they need full opacity; they take the same side as
 * the veil (light handles on a dark surface, dark handles on a light one).
 */
fun selectionHandleColor(background: Color): Color {
    val dark = background.luminance() < 0.5f
    return if (dark) Color.White else Color.Black
}

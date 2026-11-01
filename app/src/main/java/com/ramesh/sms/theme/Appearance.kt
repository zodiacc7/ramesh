package com.ramesh.sms.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Textra-style customisation model.
 *
 * Everything the user can tweak about the look of Ramesh lives here so it can be
 * persisted in one place and read from any composable through [AppearanceStore].
 */
@Immutable
data class Appearance(
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val accent: AccentColor = AccentColor.TEAL,
    val bubbleStyle: BubbleStyle = BubbleStyle.TEXTRA,
    val bubbleTail: Boolean = true,
    val sentTone: BubbleTone = BubbleTone.ACCENT,
    val receivedTone: BubbleTone = BubbleTone.SURFACE,
    val backdrop: ChatBackdrop = ChatBackdrop.DEFAULT,
    val wallpaperUri: String? = null,
    val chatFontSize: ChatFontSize = ChatFontSize.DEFAULT,
    val fontFamily: ChatFontFamily = ChatFontFamily.VAZIRMATN,
    val showChatAvatars: Boolean = false,
    val showDateHeaders: Boolean = true,
    val showInboxAvatars: Boolean = true,
    val enterToSend: Boolean = false,
    val showCharacterCounter: Boolean = true,
    val otpCopyAction: Boolean = true,
    /** Drag a conversation sideways to reveal a delete button. */
    val swipeToDelete: Boolean = true,
    /** How a message bubble appears when it enters the chat. */
    val messageAnimation: MessageAnimation = MessageAnimation.FADE
)

enum class ThemeMode(val label: String) {
    AUTO("Auto"),
    LIGHT("Light"),
    DARK("Dark"),
    BLACK("Black")
}

enum class BubbleStyle(val label: String, val cornerDp: Float, val tailDp: Float) {
    TEXTRA("Textra", 18f, 11f),
    ROUNDED("Rounded", 20f, 0f),
    PILL("Pill", 24f, 0f),
    COMPACT("Compact", 10f, 7f)
}

/** Enter/exit animation used for message bubbles. */
enum class MessageAnimation(val label: String) {
    FADE("Fade"),
    SLIDE("Slide"),
    POP("Pop"),
    NONE("None")
}

enum class ChatFontSize(val label: String, val scale: Float) {
    SMALL("Small", 0.88f),
    DEFAULT("Default", 1f),
    LARGE("Large", 1.14f),
    XLARGE("Extra large", 1.3f)
}

enum class ChatFontFamily(val label: String) {
    VAZIRMATN("Vazirmatn"),
    JAKARTA("Plus Jakarta"),
    SYSTEM("System")
}

/**
 * Accent palettes. The teal entry keeps Ramesh's brand colour; the rest are
 * hand-tuned Material tones so every accent works in light, dark and black.
 */
@Immutable
data class AccentPalette(
    val swatch: Color,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color
)

enum class AccentColor(val label: String, val palette: AccentPalette?) {
    SYSTEM("System", null),
    TEAL(
        "Turquoise",
        AccentPalette(
            swatch = Color(0xFF00838F),
            lightPrimary = Color(0xFF006874),
            lightOnPrimary = Color(0xFFFFFFFF),
            lightPrimaryContainer = Color(0xFF97F0FF),
            lightOnPrimaryContainer = Color(0xFF001F24),
            darkPrimary = Color(0xFF4FD8EB),
            darkOnPrimary = Color(0xFF00363D),
            darkPrimaryContainer = Color(0xFF004F58),
            darkOnPrimaryContainer = Color(0xFF97F0FF)
        )
    ),
    BLUE(
        "Blue",
        AccentPalette(
            swatch = Color(0xFF1565C0),
            lightPrimary = Color(0xFF1565C0),
            lightOnPrimary = Color(0xFFFFFFFF),
            lightPrimaryContainer = Color(0xFFD6E3FF),
            lightOnPrimaryContainer = Color(0xFF001B3E),
            darkPrimary = Color(0xFF9FCAFF),
            darkOnPrimary = Color(0xFF003257),
            darkPrimaryContainer = Color(0xFF00497D),
            darkOnPrimaryContainer = Color(0xFFD1E4FF)
        )
    ),
    INDIGO(
        "Indigo",
        AccentPalette(
            swatch = Color(0xFF4F5BD5),
            lightPrimary = Color(0xFF4F5BD5),
            lightOnPrimary = Color(0xFFFFFFFF),
            lightPrimaryContainer = Color(0xFFE0E0FF),
            lightOnPrimaryContainer = Color(0xFF00105C),
            darkPrimary = Color(0xFFBAC3FF),
            darkOnPrimary = Color(0xFF1E2A78),
            darkPrimaryContainer = Color(0xFF36429A),
            darkOnPrimaryContainer = Color(0xFFE0E0FF)
        )
    ),
    PURPLE(
        "Purple",
        AccentPalette(
            swatch = Color(0xFF7A4BC4),
            lightPrimary = Color(0xFF7A4BC4),
            lightOnPrimary = Color(0xFFFFFFFF),
            lightPrimaryContainer = Color(0xFFEBDCFF),
            lightOnPrimaryContainer = Color(0xFF2C0E60),
            darkPrimary = Color(0xFFD3BBFF),
            darkOnPrimary = Color(0xFF3E2170),
            darkPrimaryContainer = Color(0xFF553691),
            darkOnPrimaryContainer = Color(0xFFEBDCFF)
        )
    ),
    PINK(
        "Pink",
        AccentPalette(
            swatch = Color(0xFFC2185B),
            lightPrimary = Color(0xFFC2185B),
            lightOnPrimary = Color(0xFFFFFFFF),
            lightPrimaryContainer = Color(0xFFFFD9E2),
            lightOnPrimaryContainer = Color(0xFF3E001D),
            darkPrimary = Color(0xFFFFB1C8),
            darkOnPrimary = Color(0xFF64002F),
            darkPrimaryContainer = Color(0xFF8E0B45),
            darkOnPrimaryContainer = Color(0xFFFFD9E2)
        )
    ),
    RED(
        "Red",
        AccentPalette(
            swatch = Color(0xFFB3261E),
            lightPrimary = Color(0xFFB3261E),
            lightOnPrimary = Color(0xFFFFFFFF),
            lightPrimaryContainer = Color(0xFFFFDAD6),
            lightOnPrimaryContainer = Color(0xFF410E0B),
            darkPrimary = Color(0xFFFFB4AB),
            darkOnPrimary = Color(0xFF690005),
            darkPrimaryContainer = Color(0xFF8C1D18),
            darkOnPrimaryContainer = Color(0xFFFFDAD6)
        )
    ),
    ORANGE(
        "Orange",
        AccentPalette(
            swatch = Color(0xFFC25E00),
            lightPrimary = Color(0xFFC25E00),
            lightOnPrimary = Color(0xFFFFFFFF),
            lightPrimaryContainer = Color(0xFFFFDCC2),
            lightOnPrimaryContainer = Color(0xFF3A1A00),
            darkPrimary = Color(0xFFFFB77C),
            darkOnPrimary = Color(0xFF5B2A00),
            darkPrimaryContainer = Color(0xFF8A4500),
            darkOnPrimaryContainer = Color(0xFFFFDCC2)
        )
    ),
    GREEN(
        "Green",
        AccentPalette(
            swatch = Color(0xFF146C2E),
            lightPrimary = Color(0xFF146C2E),
            lightOnPrimary = Color(0xFFFFFFFF),
            lightPrimaryContainer = Color(0xFFA6F4B5),
            lightOnPrimaryContainer = Color(0xFF002108),
            darkPrimary = Color(0xFF8BDA9B),
            darkOnPrimary = Color(0xFF003910),
            darkPrimaryContainer = Color(0xFF00531D),
            darkOnPrimaryContainer = Color(0xFFA6F4B5)
        )
    ),
    SLATE(
        "Slate",
        AccentPalette(
            swatch = Color(0xFF4A5A63),
            lightPrimary = Color(0xFF4A5A63),
            lightOnPrimary = Color(0xFFFFFFFF),
            lightPrimaryContainer = Color(0xFFDDE4E8),
            lightOnPrimaryContainer = Color(0xFF131C21),
            darkPrimary = Color(0xFFB3C8D2),
            darkOnPrimary = Color(0xFF1E2B32),
            darkPrimaryContainer = Color(0xFF3A474E),
            darkOnPrimaryContainer = Color(0xFFDDE4E8)
        )
    );

    val swatchColor: Color get() = palette?.swatch ?: Color(0xFF7A7A7E)
}

/**
 * Bubble colour presets, Textra style: pick a colour for outgoing bubbles and a
 * different one for incoming bubbles. [ACCENT] and [SURFACE] follow the theme.
 */
enum class BubbleTone(
    val label: String,
    val lightBg: Color,
    val lightFg: Color,
    val darkBg: Color,
    val darkFg: Color
) {
    ACCENT("Accent", Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified),
    SURFACE("Theme", Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified),
    WHITE("White", Color(0xFFFFFFFF), Color(0xFF12171A), Color(0xFF1B1F22), Color(0xFFF1F3F5)),
    GRAY("Gray", Color(0xFFE9EBED), Color(0xFF171C1F), Color(0xFF2A2E31), Color(0xFFEDEFF1)),
    BLUE("Blue", Color(0xFFD3E3FD), Color(0xFF041E3F), Color(0xFF1B4A7A), Color(0xFFEAF2FF)),
    GREEN("Green", Color(0xFFD7F2DC), Color(0xFF0B2E17), Color(0xFF1C5133), Color(0xFFE6FAEC)),
    PURPLE("Purple", Color(0xFFE8DDFB), Color(0xFF22103F), Color(0xFF4A2E7A), Color(0xFFF2EAFF)),
    PINK("Pink", Color(0xFFFBDDE7), Color(0xFF3D0B21), Color(0xFF76213F), Color(0xFFFDE7EF)),
    ORANGE("Orange", Color(0xFFFBE2CC), Color(0xFF3A1900), Color(0xFF7A4318), Color(0xFFFDEBD9)),
    CHARCOAL("Charcoal", Color(0xFF2C3134), Color(0xFFF0F2F4), Color(0xFF3B4145), Color(0xFFFFFFFF))
}

/**
 * Chat wallpaper tones. The dark values used to sit within a few points of the dark theme
 * background (#111111, #0B1720, #0A1811, …), so every option looked identical. They are now spaced
 * far enough apart to be visible on an OLED screen while still keeping bubbles readable.
 */
enum class ChatBackdrop(val label: String, val light: Color?, val dark: Color?) {
    DEFAULT("Default", null, null),
    PAPER("Paper", Color(0xFFF2E9DB), Color(0xFF2A2118)),
    SKY("Sky", Color(0xFFD9E9F7), Color(0xFF10293D)),
    MINT("Mint", Color(0xFFD8EEE1), Color(0xFF10301F)),
    LAVENDER("Lavender", Color(0xFFE9E1F7), Color(0xFF241A3D)),
    SAND("Sand", Color(0xFFF3E3C8), Color(0xFF322618)),
    CHARCOAL("Charcoal", Color(0xFFDDE1E6), Color(0xFF14181C))
}

/**
 * Resolves a bubble preset to real colours for the current theme.
 * ACCENT/SURFACE read the live Material colour scheme so they follow the picker.
 */
@Composable
fun BubbleTone.bubbleBackground(outgoing: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    return when (this) {
        BubbleTone.ACCENT -> if (outgoing) scheme.primary else scheme.primaryContainer
        BubbleTone.SURFACE -> scheme.surfaceContainerHigh
        else -> if (dark) darkBg else lightBg
    }
}

@Composable
fun BubbleTone.bubbleContent(outgoing: Boolean): Color {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    return when (this) {
        BubbleTone.ACCENT -> if (outgoing) scheme.onPrimary else scheme.onPrimaryContainer
        BubbleTone.SURFACE -> scheme.onSurface
        else -> if (dark) darkFg else lightFg
    }
}

@Composable
fun ChatBackdrop.backdropColorOrNull(): Color? {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) dark else light
}

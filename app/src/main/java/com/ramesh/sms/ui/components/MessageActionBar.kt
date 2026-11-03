package com.ramesh.sms.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramesh.sms.R
import com.ramesh.sms.theme.PillShape
import com.ramesh.sms.theme.RameshIcons
import kotlin.math.roundToInt

/** Duration of the strip's slide, in and out. Short enough to feel attached to the tap. */
private const val ANIMATION_MS = 150

/**
 * The action strip a single tap on a bubble opens - the Textra layout.
 *
 * The strip is drawn **over the message that was tapped**, not at the top of the screen: [anchor]
 * is the bubble's bounds in this composable's coordinate space, and the strip is placed above it,
 * dropping below when there is no room. It is an ordinary overlay, not a `Dialog`, so nothing around
 * it is dimmed; a transparent layer behind it only exists so a tap anywhere else closes the strip.
 *
 * Order: [code chip] · Copy · React · Share · Forward · Delete · Info · Select. The chip only appears
 * when the message carries a verification code, and copies the code itself. The last action is the
 * double check from Textra's strip and it selects the message; the old read/unread toggle is gone,
 * because that is not what that button does there.
 *
 * The strip is stateless. Delete is armed by the caller through [deleteArmed]: the first tap on the
 * bin opens it (red, filled), the second deletes.
 */
@Composable
fun MessageActionBar(
    anchor: Rect,
    visible: Boolean = true,
    otpCode: String?,
    canReact: Boolean,
    /** Which side the bubble sits on; the strip slides in from exactly there. */
    isSent: Boolean,
    deleteArmed: Boolean,
    onCopyCode: () -> Unit,
    onCopy: () -> Unit,
    onReact: () -> Unit,
    onShare: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onSelect: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    // A received bubble sits at the start of the chat and a sent one at the end, so the strip comes
    // from that same side: received slides left-to-right, sent right-to-left. In a right-to-left UI
    // the two swap with the layout, so the rule stays "from the bubble's own side" instead of a
    // hard-coded sign.
    val fromEnd = if (layoutDirection == LayoutDirection.Rtl) !isSent else isSent

    // One animation for every bubble: a horizontal slide. Fading one and sliding the other (what
    // this used to do) is exactly what made the two look different.
    val slide = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            slide.snapTo(if (fromEnd) 1f else -1f)
            slide.animateTo(0f, tween(ANIMATION_MS))
        } else {
            slide.animateTo(if (fromEnd) 1f else -1f, tween(ANIMATION_MS))
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val gap = with(density) { 8.dp.toPx() }
        val edge = with(density) { 8.dp.toPx() }
        val availableWidth = constraints.maxWidth

        // The bubble is on the right half of the chat when the message was sent, so mirror the strip
        // the same way: it grows to the left of a sent bubble and to the right of a received one.
        val rawX = if (barSize.width == 0 || anchor.center.x < availableWidth / 2f) {
            anchor.left
        } else {
            anchor.right - barSize.width
        }
        val x = rawX.coerceIn(edge, (availableWidth - barSize.width - edge).coerceAtLeast(edge))

        // Vertical placement, clamped into the window in both directions. A bubble that is only
        // partly on screen (mid-scroll, or a tall message under the app bar) used to hand the strip
        // a top edge outside the viewport, which is a layout the container cannot satisfy. Clamping
        // keeps the strip fully visible: above the bubble when it fits, below it otherwise, and
        // never past the bottom edge.
        val maxY = (constraints.maxHeight - barSize.height - edge).coerceAtLeast(edge)
        val above = anchor.top - barSize.height - gap
        val below = anchor.bottom + gap
        val y = when {
            barSize.height == 0 -> edge
            above >= edge -> above.coerceAtMost(maxY)
            else -> below.coerceIn(edge, maxY)
        }

        // Tap anywhere else to close. Drawn first, so the strip itself gets the taps that land on it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() }
                }
        )

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                // Positioned with an offset instead of padding: an offset never changes the
                // constraints handed to the strip, so there is no padding/measure exception to hit
                // and the strip cannot be squeezed to zero width at the edges of the screen.
                .offset {
                    val travel = barSize.width.toFloat().coerceAtLeast(1f)
                    IntOffset(
                        x.roundToInt() + (slide.value * travel).roundToInt(),
                        y.roundToInt()
                    )
                }
                .widthIn(max = 560.dp)
                // Hidden for the single frame before its size is known, so it never flickers at the
                // bubble's corner and then jumps above it.
                .alpha(if (barSize == IntSize.Zero) 0f else 1f)
                .onSizeChanged { barSize = it }
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 54.dp)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!otpCode.isNullOrBlank()) {
                    OtpChip(code = otpCode, onCopy = onCopyCode)
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Row(
                    modifier = Modifier.then(
                        if (otpCode.isNullOrBlank()) Modifier
                        else Modifier.horizontalScroll(rememberScrollState())
                    ),
                    horizontalArrangement = if (otpCode.isNullOrBlank()) {
                        Arrangement.SpaceEvenly
                    } else {
                        Arrangement.spacedBy(2.dp)
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BarAction(
                        icon = Icons.Default.ContentCopy,
                        label = stringResource(R.string.chat_copy),
                        showLabel = true,
                        onClick = onCopy
                    )
                    if (canReact) {
                        BarAction(
                            icon = Icons.Default.AddReaction,
                            label = stringResource(R.string.chat_react),
                            onClick = onReact
                        )
                    }
                    BarAction(
                        icon = Icons.Default.Share,
                        label = stringResource(R.string.chat_menu_share),
                        onClick = onShare
                    )
                    BarAction(
                        // Semicircular arrow from the bottom to the left, as in the reference.
                        icon = RameshIcons.Forward,
                        label = stringResource(R.string.chat_menu_forward),
                        onClick = onForward
                    )
                    BarAction(
                        icon = if (deleteArmed) Icons.Default.Delete else Icons.Default.DeleteOutline,
                        label = if (deleteArmed) {
                            stringResource(R.string.chat_delete_confirm)
                        } else {
                            stringResource(R.string.chat_delete)
                        },
                        tint = if (deleteArmed) Color(0xFFE5484D) else null,
                        emphasised = deleteArmed,
                        onClick = onDelete
                    )
                    BarAction(
                        icon = Icons.Default.Info,
                        label = stringResource(R.string.chat_message_info),
                        onClick = onInfo
                    )
                    BarAction(
                        // The list-with-double-check mark: it enters selection mode with this
                        // message selected. The bare double check belongs to "mark as read".
                        icon = RameshIcons.SelectList,
                        label = stringResource(R.string.chat_menu_select),
                        onClick = onSelect
                    )
                }
            }
        }
    }
}

@Composable
private fun OtpChip(code: String, onCopy: () -> Unit) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .height(40.dp)
            .clickable(onClick = onCopy)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.chat_menu_otp_copy),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.chat_menu_otp, code),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * One action in the strip. [showLabel] is only set for Copy, which is the one action Textra spells
 * out; the rest lean on their icons and on the content description read by TalkBack.
 */
@Composable
private fun BarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    showLabel: Boolean = false,
    tint: Color? = null,
    emphasised: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    val iconTint = tint ?: if (emphasised) colors.error else colors.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(42.dp)
            .widthIn(min = 42.dp)
            .semantics { contentDescription = label }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(21.dp)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

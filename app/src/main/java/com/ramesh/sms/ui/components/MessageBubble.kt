package com.ramesh.sms.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramesh.sms.data.model.SmsMessage
import com.ramesh.sms.theme.AppearanceStore
import com.ramesh.sms.theme.PillShape
import com.ramesh.sms.theme.bubbleBackground
import com.ramesh.sms.theme.bubbleContentBottomPadding
import com.ramesh.sms.theme.chatBubbleShape
import com.ramesh.sms.theme.bubbleContent
import com.ramesh.sms.data.util.AppLanguage
import com.ramesh.sms.ui.util.SmsDateFormats
import com.ramesh.sms.ui.util.contentAware
import com.ramesh.sms.ui.util.forMessageBody

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: SmsMessage,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    simSlotNumber: Int? = null,
    useShamsi: Boolean = false,
    onToggleDateFormat: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    selectionResetKey: Any = Unit,
    onBubbleTouched: () -> Unit = {},
    onOutsideTap: () -> Unit = {},
    showAvatar: Boolean = false,
    avatar: (@Composable () -> Unit)? = null,
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
) {
    val isSent = message.isSent
    val look = AppearanceStore.current
    val bubbleShape = chatBubbleShape(
        outgoing = isSent,
        style = look.bubbleStyle,
        tail = look.bubbleTail
    )
    val emphasized = isHighlighted || isSelected
    val tone = if (isSent) look.sentTone else look.receivedTone
    val containerColor = tone.bubbleBackground(isSent)
    val contentColor = tone.bubbleContent(isSent)
    val border = when {
        emphasized -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        isSent -> null
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    }
    val dateText = remember(message.date, useShamsi, appLanguage) {
        SmsDateFormats.formatDate(message.date, useShamsi, appLanguage)
    }
    val timeText = remember(message.date) { SmsDateFormats.clock(message.date) }
    // Keep long messages from touching both edges, like every modern messenger.
    val context = LocalContext.current
    val maxBubbleWidth = remember(context) {
        val metrics = context.resources.displayMetrics
        (metrics.widthPixels * 0.8f / metrics.density).dp
    }
    val tailPadding = bubbleContentBottomPadding(look.bubbleStyle, look.bubbleTail)

    val outsideModifier = Modifier
        .fillMaxSize()
        .pointerInput(onLongClick, onToggleSelect, onOutsideTap, isSelectionMode) {
            detectTapGestures(
                onTap = { if (isSelectionMode) onToggleSelect() else onOutsideTap() },
                onLongPress = { onLongClick() }
            )
        }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 14.dp, vertical = 3.dp)
    ) {
        if (isSent) {
            Spacer(modifier = Modifier.weight(1f).then(outsideModifier))
        }
        if (!isSent && showAvatar && avatar != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Bottom)
                    .padding(end = 8.dp, bottom = 2.dp)
            ) {
                avatar()
            }
        }
        if (isSelectionMode) {
            Surface(
                onClick = onToggleSelect,
                shape = bubbleShape,
                color = containerColor,
                border = border,
                shadowElevation = 0.dp,
                modifier = Modifier.widthIn(min = 80.dp, max = maxBubbleWidth)
            ) {
                BubbleContent(
                    message = message,
                    contentColor = contentColor,
                    dateText = dateText,
                    timeText = timeText,
                    isSent = isSent,
                    simSlotNumber = simSlotNumber,
                    onToggleDateFormat = onToggleDateFormat,
                    selectable = false,
                    tailPadding = tailPadding
                )
            }
        } else {
            Surface(
                shape = bubbleShape,
                color = containerColor,
                border = border,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .widthIn(min = 80.dp, max = maxBubbleWidth)
                    // The bubble itself has to take the long press: it used to be handled only by
                    // the empty space next to it (and by the text selection container), which is
                    // why selecting a message only worked by accident.
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            onBubbleTouched()
                            if (isSelectionMode) onToggleSelect()
                        },
                        onLongClick = onLongClick
                    )
            ) {
                BubbleContent(
                    message = message,
                    contentColor = contentColor,
                    dateText = dateText,
                    timeText = timeText,
                    isSent = isSent,
                    simSlotNumber = simSlotNumber,
                    onToggleDateFormat = onToggleDateFormat,
                    selectable = true,
                    tailPadding = tailPadding,
                    selectionResetKey = selectionResetKey
                )
            }
        }
        if (!isSent) {
            Spacer(modifier = Modifier.weight(1f).then(outsideModifier))
        }
    }
}

@Composable
private fun BubbleContent(
    message: SmsMessage,
    contentColor: Color,
    dateText: String,
    timeText: String,
    isSent: Boolean,
    simSlotNumber: Int?,
    onToggleDateFormat: (() -> Unit)?,
    selectable: Boolean,
    tailPadding: androidx.compose.ui.unit.Dp,
    selectionResetKey: Any = Unit
) {
    val look = AppearanceStore.current
    val scale = look.chatFontSize.scale
    val bodyStyle = MaterialTheme.typography.bodyLarge
        .contentAware()
        .forMessageBody(message.body)
        .copy(fontSize = (16f * scale).sp, lineHeight = (23f * scale).sp)

    Column(
        modifier = Modifier.padding(
            start = 14.dp,
            end = 14.dp,
            top = 9.dp,
            bottom = 9.dp + tailPadding
        )
    ) {
        val bodyText = @Composable {
            Text(
                text = message.body,
                style = bodyStyle,
                color = contentColor,
                letterSpacing = 0.2.sp
            )
        }
        bodyText()
        val reaction = message.reaction
        if (!reaction.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Surface(
                shape = PillShape,
                color = contentColor.copy(alpha = 0.14f)
            ) {
                Text(
                    text = reaction,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.align(Alignment.End),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (simSlotNumber != null) {
                SimBadge(
                    slotNumber = simSlotNumber,
                    isSent = isSent,
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .pointerInput(onToggleDateFormat) {
                        detectTapGestures(
                            onTap = if (onToggleDateFormat != null) { { onToggleDateFormat() } } else null
                        )
                    }
                    .padding(horizontal = 2.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            if (isSent) {
                Spacer(modifier = Modifier.width(4.dp))
                val (icon, desc, tint) = when {
                    message.isFailed -> Triple(
                        Icons.Default.ErrorOutline,
                        "Failed",
                        MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    )
                    message.isDelivered -> Triple(
                        Icons.Default.DoneAll,
                        "Delivered",
                        contentColor.copy(alpha = 0.7f)
                    )
                    else -> Triple(
                        Icons.Default.Done,
                        "Sent",
                        contentColor.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = desc,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/** Textra-style day divider shown between message groups. */
@Composable
fun ChatDateHeader(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            shadowElevation = 0.dp
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

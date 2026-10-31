package com.ramesh.sms.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramesh.sms.R
import com.ramesh.sms.data.model.SmsMessage
import com.ramesh.sms.data.util.AppLanguage
import com.ramesh.sms.theme.PillShape
import com.ramesh.sms.theme.SpamCardShape
import com.ramesh.sms.theme.SpamWarningBorderDark
import com.ramesh.sms.theme.SpamWarningBorderLight
import com.ramesh.sms.theme.SpamWarningDark
import com.ramesh.sms.theme.SpamWarningLight
import com.ramesh.sms.theme.SpamWarningOnDark
import com.ramesh.sms.theme.SpamWarningOnLight
import com.ramesh.sms.theme.TagShape
import com.ramesh.sms.ui.util.SmsDateFormats
import com.ramesh.sms.ui.util.contentAware
import com.ramesh.sms.ui.util.forMessageBody

@Composable
fun SpamMessagePill(
    message: SmsMessage,
    onRevealToggle: (Boolean) -> Unit,
    onMarkNotSpam: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    simSlotNumber: Int? = null,
    useShamsi: Boolean = false,
    onToggleDateFormat: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    appLanguage: AppLanguage = AppLanguage.SYSTEM,
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) SpamWarningDark else SpamWarningLight
    val textColor = if (isDark) SpamWarningOnDark else SpamWarningOnLight
    val borderColor = if (isDark) SpamWarningBorderDark else SpamWarningBorderLight
    val emphasized = isHighlighted || isSelected

    var isRevealed by remember(message.isRevealed, isHighlighted) {
        mutableStateOf(if (isHighlighted) true else message.isRevealed)
    }
    val dateText = remember(message.date, useShamsi, appLanguage) {
        SmsDateFormats.formatDate(message.date, useShamsi, appLanguage)
    }
    val clockText = remember(message.date) { SmsDateFormats.clock(message.date) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp),
        shape = SpamCardShape,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = if (emphasized) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Header Row: Shield Badge + Tag + Reveal Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(onLongClick, onToggleSelect, isSelectionMode) {
                            detectTapGestures(
                                onTap = { if (isSelectionMode) onToggleSelect() },
                                onLongPress = { onLongClick() }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(borderColor.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = stringResource(R.string.comp_spam_filtered),
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.comp_spam_message_filtered),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        message.matchedRuleName?.let { ruleName ->
                            Surface(
                                shape = TagShape,
                                color = borderColor.copy(alpha = 0.25f),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.comp_rule_label, ruleName),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Action Pill Button
                FilledTonalButton(
                    onClick = {
                        val next = !isRevealed
                        isRevealed = next
                        onRevealToggle(next)
                    },
                    shape = PillShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = borderColor.copy(alpha = 0.3f),
                        contentColor = textColor
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRevealed) "Hide" else "Reveal",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expandable Message Content & Actions
            AnimatedVisibility(
                visible = isRevealed,
                enter = fadeIn() + expandVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                ),
                exit = fadeOut() + shrinkVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = message.body,
                                style = MaterialTheme.typography.bodyMedium.contentAware().forMessageBody(message.body),
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 21.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.7f),
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
                                text = clockText,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                            if (simSlotNumber != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                SimBadge(slotNumber = simSlotNumber)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = onMarkNotSpam
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.comp_not_spam),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            TextButton(
                                onClick = onDelete
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.comp_delete),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


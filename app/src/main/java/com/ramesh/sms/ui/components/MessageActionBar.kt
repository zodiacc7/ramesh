package com.ramesh.sms.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ramesh.sms.R
import com.ramesh.sms.theme.PillShape

/**
 * The action strip a single tap on a bubble opens, laid out the way Textra does it: one horizontal
 * bar that floats over the top of the chat, with every action already visible and nothing hidden
 * behind a "More" submenu.
 *
 * Order: [code chip] · Copy · React · Share · Forward · Delete · Info · Mark as read. The chip only
 * appears when the message carries a verification code, and copies the code itself.
 *
 * The bar is stateless: every action arrives as a lambda, so it holds no view-model or repository
 * reference. [onDismiss] is wired to the space around the bar, so a tap anywhere else closes it the
 * way a scrim would.
 */
@Composable
fun MessageActionBar(
    otpCode: String?,
    onCopyCode: () -> Unit,
    onCopy: () -> Unit,
    onReact: () -> Unit,
    onShare: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onMarkRead: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // No ripple on the backdrop: it is not a button, it is the "tap elsewhere to close" area.
    val backdropInteraction = remember { MutableInteractionSource() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = backdropInteraction,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .padding(top = 12.dp)
                    // Cap the width on tablets, then fill it: order matters, because fillMaxWidth
                    // would otherwise make the constraint exact and ignore the cap.
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!otpCode.isNullOrBlank()) {
                        OtpChip(code = otpCode, onCopy = onCopyCode)
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .then(
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
                        BarAction(
                            icon = Icons.Default.AddReaction,
                            label = stringResource(R.string.chat_react),
                            onClick = onReact
                        )
                        BarAction(
                            icon = Icons.Default.Share,
                            label = stringResource(R.string.chat_menu_share),
                            onClick = onShare
                        )
                        BarAction(
                            icon = Icons.AutoMirrored.Filled.Forward,
                            label = stringResource(R.string.chat_menu_forward),
                            onClick = onForward
                        )
                        BarAction(
                            icon = Icons.Default.DeleteOutline,
                            label = stringResource(R.string.chat_delete),
                            onClick = onDelete
                        )
                        BarAction(
                            icon = Icons.Default.Info,
                            label = stringResource(R.string.chat_message_info),
                            onClick = onInfo
                        )
                        BarAction(
                            icon = Icons.Default.MarkChatRead,
                            label = stringResource(R.string.chat_mark_read),
                            onClick = onMarkRead
                        )
                    }
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
    showLabel: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp)
        )
        if (showLabel) {
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

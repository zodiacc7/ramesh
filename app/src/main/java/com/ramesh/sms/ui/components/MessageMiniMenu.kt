package com.ramesh.sms.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ramesh.sms.R
import com.ramesh.sms.data.model.SmsMessage
import com.ramesh.sms.data.util.AppLanguage
import com.ramesh.sms.data.util.MessageLinkScanner
import com.ramesh.sms.data.util.MessageSpan
import com.ramesh.sms.data.util.findOtpHighlight
import com.ramesh.sms.theme.PillShape
import com.ramesh.sms.theme.SquircleCardShape
import com.ramesh.sms.ui.util.ContactUtils
import com.ramesh.sms.ui.util.contentAware
import com.ramesh.sms.ui.util.forMessageBody

/**
 * The small menu a single tap on a bubble opens.
 *
 * Textra answers a tap with `Copy | Forward | More`, and that is what this does. The message text
 * stays selectable inside the sheet, so copying *part* of a long message is possible without the
 * selection gesture fighting the list. Links, phone numbers and dates in the body are tappable: a
 * link opens in the browser, a number in the dialer, and a date switches the calendar the same way
 * tapping the timestamp in a bubble does.
 *
 * Everything the menu needs arrives as a lambda, so it holds no view-model or repository reference
 * and can be reasoned about on its own.
 */
@Composable
fun MessageMiniMenu(
    message: SmsMessage,
    useShamsi: Boolean,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit,
    onForward: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onReact: () -> Unit,
    onSelect: () -> Unit,
    onReportSpam: () -> Unit,
    onToggleDateFormat: (() -> Unit)?
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var expanded by remember(message.id) { mutableStateOf(false) }
    val otp = remember(message.body) { findOtpHighlight(message.body) }

    val linkColor = MaterialTheme.colorScheme.primary
    val bodyText = remember(message.body, linkColor, useShamsi, appLanguage, uriHandler) {
        annotateMessageBody(
            body = message.body,
            linkStyle = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
            onOpenUrl = { url -> runCatching { uriHandler.openUri(url) } },
            onDial = { number -> ContactUtils.openDialer(context, number) },
            onDateTap = { onToggleDateFormat?.invoke() }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = SquircleCardShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SelectionContainer {
                    Text(
                        text = bodyText,
                        style = MaterialTheme.typography.bodyMedium
                            .contentAware()
                            .forMessageBody(message.body),
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }

                if (otp != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OtpChip(
                        code = otp.code,
                        onCopy = {
                            onCopy(otp.code)
                            onDismiss()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniMenuAction(
                        icon = Icons.Default.ContentCopy,
                        label = stringResource(R.string.chat_copy),
                        onClick = {
                            onCopy(message.body)
                            onDismiss()
                        }
                    )
                    MiniMenuAction(
                        icon = Icons.AutoMirrored.Filled.Forward,
                        label = stringResource(R.string.chat_menu_forward),
                        onClick = {
                            onForward()
                            onDismiss()
                        }
                    )
                    MiniMenuAction(
                        icon = Icons.Default.MoreHoriz,
                        label = stringResource(R.string.chat_menu_more),
                        expanded = expanded,
                        onClick = { expanded = !expanded }
                    )
                }

                if (expanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniMenuAction(
                            icon = Icons.Default.Share,
                            label = stringResource(R.string.chat_menu_share),
                            onClick = {
                                onShare()
                                onDismiss()
                            }
                        )
                        MiniMenuAction(
                            icon = Icons.Default.Face,
                            label = stringResource(R.string.chat_react),
                            onClick = {
                                onReact()
                                onDismiss()
                            }
                        )
                        MiniMenuAction(
                            icon = Icons.Default.Info,
                            label = stringResource(R.string.chat_message_info),
                            onClick = {
                                onInfo()
                                onDismiss()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniMenuAction(
                            icon = Icons.Default.CheckCircle,
                            label = stringResource(R.string.chat_menu_select),
                            onClick = {
                                onSelect()
                                onDismiss()
                            }
                        )
                        MiniMenuAction(
                            icon = Icons.Default.Report,
                            label = stringResource(R.string.chat_menu_report_spam),
                            onClick = {
                                onReportSpam()
                                onDismiss()
                            }
                        )
                        // Keeps the second row visually aligned with the first.
                        Spacer(modifier = Modifier.width(76.dp))
                    }
                }

                if (onToggleDateFormat != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.chat_menu_date_toggle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        modifier = Modifier.clickable(onClick = onCopy)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.chat_menu_otp_copy),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.chat_menu_otp, code),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MiniMenuAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    expanded: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Surface(
            shape = PillShape,
            color = if (expanded) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (expanded) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.padding(11.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * The message body with its links, numbers and dates turned into tap targets.
 *
 * The spans come from [MessageLinkScanner], a pure JVM function with its own tests, so the "what is
 * tappable" decision is not buried in a composable.
 */
private fun annotateMessageBody(
    body: String,
    linkStyle: SpanStyle,
    onOpenUrl: (String) -> Unit,
    onDial: (String) -> Unit,
    onDateTap: () -> Unit
) = buildAnnotatedString {
    append(body)
    val styles = TextLinkStyles(style = linkStyle)
    for (span in MessageLinkScanner.scan(body)) {
        when (span.kind) {
            MessageSpan.Kind.URL -> addLink(
                LinkAnnotation.Url(
                    url = span.value,
                    styles = styles,
                    linkInteractionListener = { onOpenUrl(span.value) }
                ),
                start = span.start,
                end = span.end
            )

            MessageSpan.Kind.PHONE -> addLink(
                LinkAnnotation.Clickable(
                    tag = "tel:${span.value}",
                    styles = styles,
                    linkInteractionListener = { onDial(span.value) }
                ),
                start = span.start,
                end = span.end
            )

            MessageSpan.Kind.DATE -> addLink(
                LinkAnnotation.Clickable(
                    tag = "date:${span.value}",
                    styles = styles,
                    linkInteractionListener = { onDateTap() }
                ),
                start = span.start,
                end = span.end
            )
        }
    }
}

package com.ramesh.sms.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ramesh.sms.R
import com.ramesh.sms.data.model.ConversationThread
import com.ramesh.sms.data.model.SenderTab
import com.ramesh.sms.data.util.threadMatchesTab
import com.ramesh.sms.data.util.badgeCountForTab
import com.ramesh.sms.data.util.visibleThreadsForTab
import com.ramesh.sms.theme.PillShape
import kotlinx.coroutines.launch
import kotlin.math.abs

fun isThreadInTab(thread: ConversationThread, tab: SenderTab): Boolean = threadMatchesTab(thread, tab)

/** Id of the "All" pseudo tab while the strip is being reordered (matches ALL_TAB_SENTINEL). */
const val ALL_TAB_ID = -1L

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SenderTabRow(
    tabs: List<SenderTab>,
    selectedTabId: Long?,
    threads: List<ConversationThread>,
    allTabIndex: Int = 0,
    onSelectTab: (Long?) -> Unit,
    onCreateTabClick: () -> Unit,
    onTabOptionsClick: (SenderTab) -> Unit,
    onReorderTabs: (List<Long>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val touchSlop = LocalViewConfiguration.current.touchSlop

    val allEntry = remember { SenderTab(id = ALL_TAB_ID, name = "") }
    val entries = remember(tabs, allTabIndex) {
        val list = tabs.toMutableList()
        list.add(allTabIndex.coerceIn(0, list.size), allEntry)
        list
    }

    val currentTabs = remember { mutableStateListOf<SenderTab>() }
    var draggedTabId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var hasReordered by remember { mutableStateOf(false) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    // Keep the strip in sync with the database: comparing whole entries (not just ids) is what
    // makes a renamed tab show its new name without reloading the screen.
    if (draggedTabId == null && currentTabs != entries) {
        currentTabs.clear()
        currentTabs.addAll(entries)
    }

    // Bring the selected tab into view: after a restart (or after "All" was moved) the strip
    // could open scrolled to the far side and the active tab was simply not visible.
    LaunchedEffect(selectedTabId, currentTabs.size, allTabIndex) {
        if (currentTabs.isEmpty()) return@LaunchedEffect
        val index = currentTabs.indexOfFirst { entry ->
            if (selectedTabId == null) entry.id == ALL_TAB_ID else entry.id == selectedTabId
        }
        if (index < 0) return@LaunchedEffect
        val info = lazyListState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
        val fullyVisible = item != null && item.offset >= info.viewportStartOffset &&
            item.offset + item.size <= info.viewportEndOffset
        if (!fullyVisible) {
            runCatching { lazyListState.animateScrollToItem(index) }
        }
    }

    LazyRow(
        state = lazyListState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(currentTabs, key = { "tab_${it.id}" }) { tab ->
            val isAll = tab.id == ALL_TAB_ID
            val isSelected = if (isAll) selectedTabId == null else selectedTabId == tab.id
            val isDraggingThis = draggedTabId == tab.id
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "tabBg_${tab.id}"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tabText_${tab.id}"
            )
            // The badge means one thing only: unread messages first. When a tab has nothing
            // unread it falls back to the muted conversation count, so the two can never be
            // confused with each other.
            val tabThreads = visibleThreadsForTab(threads, if (isAll) null else tab)
            // Unread messages *and* spam that arrived silently. Spam is stored read and never
            // notifies, so without the second term a spam-only arrival would leave the badge at
            // zero and the user would never know it happened.
            val unreadCount = badgeCountForTab(threads, if (isAll) null else tab)
            val hasUnread = unreadCount > 0
            val count = if (hasUnread) unreadCount else tabThreads.size
            val badgeColor = when {
                hasUnread -> MaterialTheme.colorScheme.error
                isSelected -> contentColor.copy(alpha = 0.22f)
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            }
            val badgeTextColor = if (hasUnread) MaterialTheme.colorScheme.onError else contentColor

            Surface(
                shape = PillShape,
                color = containerColor,
                contentColor = contentColor,
                shadowElevation = if (isDraggingThis) 6.dp else 0.dp,
                modifier = Modifier
                    .zIndex(if (isDraggingThis) 2f else 0f)
                    .graphicsLayer {
                        if (isDraggingThis) {
                            translationX = dragOffset
                            scaleX = 1.05f
                            scaleY = 1.05f
                        }
                    }
                    .then(if (isDraggingThis) Modifier else Modifier.animateItem())
                    .clip(PillShape)
                    .combinedClickable(
                        onClick = {
                            if (draggedTabId == null) {
                                if (isAll) onSelectTab(null) else onSelectTab(tab.id)
                            }
                        }
                    )
                    .pointerInput(tab.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedTabId = tab.id
                                dragOffset = 0f
                                hasReordered = false
                                totalDragDistance = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragDistance += abs(dragAmount.x)
                                dragOffset += dragAmount.x

                                // Auto scroll near the edges of the strip
                                val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                                val draggedInfo = visibleItems.find { it.key == "tab_${tab.id}" }
                                if (draggedInfo != null) {
                                    val viewportStart = lazyListState.layoutInfo.viewportStartOffset
                                    val viewportEnd = lazyListState.layoutInfo.viewportEndOffset
                                    val edgeThreshold = with(density) { 48.dp.toPx() }
                                    val currentPos = draggedInfo.offset + dragOffset
                                    if (currentPos < viewportStart + edgeThreshold) {
                                        coroutineScope.launch { lazyListState.scrollBy(-15f) }
                                    } else if (currentPos + draggedInfo.size > viewportEnd - edgeThreshold) {
                                        coroutineScope.launch { lazyListState.scrollBy(15f) }
                                    }
                                }

                                // One swap per drag event so the layout can settle
                                val draggedIndex = currentTabs.indexOfFirst { it.id == tab.id }
                                if (draggedIndex != -1) {
                                    val items = lazyListState.layoutInfo.visibleItemsInfo
                                    val dInfo = items.find { it.key == "tab_${tab.id}" }
                                    if (dInfo != null) {
                                        val draggedCenter = dInfo.offset + dInfo.size / 2f + dragOffset

                                        if (draggedIndex < currentTabs.size - 1) {
                                            val nextTab = currentTabs[draggedIndex + 1]
                                            val nextInfo = items.find { it.key == "tab_${nextTab.id}" }
                                            if (nextInfo != null && draggedCenter > nextInfo.offset + nextInfo.size / 2f) {
                                                val spacing = nextInfo.offset - (dInfo.offset + dInfo.size)
                                                val slotShift = nextInfo.size + spacing
                                                val temp = currentTabs[draggedIndex]
                                                currentTabs[draggedIndex] = currentTabs[draggedIndex + 1]
                                                currentTabs[draggedIndex + 1] = temp
                                                dragOffset -= slotShift
                                                hasReordered = true
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        } else if (draggedIndex > 0) {
                                            val prevTab = currentTabs[draggedIndex - 1]
                                            val prevInfo = items.find { it.key == "tab_${prevTab.id}" }
                                            if (prevInfo != null && draggedCenter < prevInfo.offset + prevInfo.size / 2f) {
                                                val spacing = dInfo.offset - (prevInfo.offset + prevInfo.size)
                                                val slotShift = prevInfo.size + spacing
                                                val temp = currentTabs[draggedIndex]
                                                currentTabs[draggedIndex] = currentTabs[draggedIndex - 1]
                                                currentTabs[draggedIndex - 1] = temp
                                                dragOffset += slotShift
                                                hasReordered = true
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                val finalReordered = hasReordered
                                val finalOrder = currentTabs.map { it.id }
                                if (totalDragDistance < touchSlop && !finalReordered) {
                                    if (!isAll) onTabOptionsClick(tab)
                                } else if (finalReordered) {
                                    onReorderTabs(finalOrder)
                                }
                                draggedTabId = null
                                dragOffset = 0f
                                hasReordered = false
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                draggedTabId = null
                                dragOffset = 0f
                                hasReordered = false
                                totalDragDistance = 0f
                                currentTabs.clear()
                                currentTabs.addAll(entries)
                            }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 14.dp,
                        end = if (isSelected && !isAll) 6.dp else 14.dp,
                        top = 5.dp,
                        bottom = 5.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAll) stringResource(R.string.comp_all) else tab.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = PillShape,
                        color = badgeColor
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            fontSize = 11.sp
                        )
                    }

                    if (isSelected && !isAll) {
                        Spacer(modifier = Modifier.width(2.dp))
                        IconButton(
                            onClick = { onTabOptionsClick(tab) },
                            enabled = draggedTabId == null,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.comp_tab_options),
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // "+ New Tab" button
        item(key = "tab_add_button") {
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(PillShape)
                    .combinedClickable(onClick = onCreateTabClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.comp_new_tab),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.comp_new_tab),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

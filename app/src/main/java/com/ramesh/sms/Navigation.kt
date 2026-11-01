package com.ramesh.sms

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.ramesh.sms.ui.screens.ChatScreen
import com.ramesh.sms.ui.screens.ComposeScreen
import com.ramesh.sms.ui.screens.ConversationsScreen
import com.ramesh.sms.ui.screens.FilterStudioScreen
import com.ramesh.sms.ui.screens.SettingsScreen
import com.ramesh.sms.ui.viewmodel.ConversationsViewModel

@Composable
fun MainNavigation(
    pendingChatNav: ChatNav? = null,
    onChatNavHandled: () -> Unit = {},
    pendingComposeNav: ComposeNav? = null,
    onComposeNavHandled: () -> Unit = {}
) {
    val backStack = rememberNavBackStack(ConversationsNav)
    val conversationsViewModel: ConversationsViewModel = viewModel()
    // Forward means "towards the end of the line", which is the left in RTL layouts.
    val forwardSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f

    LaunchedEffect(pendingChatNav) {
        if (pendingChatNav != null) {
            conversationsViewModel.clearUnreadForThread(pendingChatNav.threadId)
            val current = backStack.lastOrNull()
            if (current !is ChatNav || current.threadId != pendingChatNav.threadId) {
                backStack.add(pendingChatNav)
            }
            onChatNavHandled()
        }
    }

    LaunchedEffect(pendingComposeNav) {
        if (pendingComposeNav != null) {
            val current = backStack.lastOrNull()
            if (current !is ComposeNav || current != pendingComposeNav) {
                backStack.add(pendingComposeNav)
            }
            onComposeNavHandled()
        }
    }

    NavDisplay(
        backStack = backStack,
        // Both directions are exact mirrors: slide-in + fade-in for incoming, slide-out + fade-out
        // for outgoing. Same duration both directions so entering and exiting feel identical.
        transitionSpec = {
            (slideInHorizontally(animationSpec = tween(PageTransitionMillis)) { full ->
                (full * forwardSign).toInt()
            } + fadeIn(animationSpec = tween(PageTransitionMillis)))
                .togetherWith(
                    slideOutHorizontally(animationSpec = tween(PageTransitionMillis)) { full ->
                        (-full * forwardSign).toInt()
                    } + fadeOut(animationSpec = tween(PageTransitionMillis))
                )
        },
        popTransitionSpec = {
            // Mirror of transitionSpec: when popping (back), incoming comes from the opposite side.
            (slideInHorizontally(animationSpec = tween(PageTransitionMillis)) { full ->
                (-full * forwardSign).toInt()
            } + fadeIn(animationSpec = tween(PageTransitionMillis)))
                .togetherWith(
                    slideOutHorizontally(animationSpec = tween(PageTransitionMillis)) { full ->
                        (full * forwardSign).toInt()
                    } + fadeOut(animationSpec = tween(PageTransitionMillis))
                )
        },
        entryProvider = entryProvider {
            entry<ConversationsNav> {
                ConversationsScreen(
                    onNavigateToChat = { chatNav -> backStack.add(chatNav) },
                    onNavigateToFilterStudio = { backStack.add(FilterStudioNav) },
                    onNavigateToCompose = { backStack.add(ComposeNav()) },
                    onNavigateToTestLab = { backStack.add(TestLabNav) },
                    onNavigateToSettings = { backStack.add(SettingsNav) },
                    viewModel = conversationsViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<ChatNav> { nav ->
                ChatScreen(
                    nav = nav,
                    onBackClick = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<SettingsNav> {
                SettingsScreen(
                    onBackClick = { backStack.removeLastOrNull() },
                    onOpenFilterStudio = { backStack.add(FilterStudioNav) },
                    inboxViewModel = conversationsViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<FilterStudioNav> {
                FilterStudioScreen(
                    onBackClick = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<ComposeNav> { nav ->
                ComposeScreen(
                    onBackClick = { backStack.removeLastOrNull() },
                    onNavigateToChat = { chatNav ->
                        backStack.removeLastOrNull()
                        backStack.add(chatNav)
                    },
                    initialAddress = nav.address,
                    initialBody = nav.body,
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<TestLabNav> {
                com.ramesh.sms.testing.TestLabScreen(
                    onBackClick = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )

    // Handle system back button so popTransitionSpec is used (same animation as app's back button).
    BackHandler(enabled = true) {
        backStack.removeLastOrNull()
    }
}

/** Duration of the screen change animation, kept short so it never feels sluggish. */
private const val PageTransitionMillis = 240

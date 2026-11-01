package com.ramesh.sms.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

/**
 * Enter animation for a bubble that arrives while its conversation is already open.
 *
 * This is not what the "message animation" setting controls - that one animates opening and closing
 * a conversation (see [conversationEnterTransition]). A bubble keeps a short, fixed entrance so an
 * incoming message does not simply pop into place.
 */
fun messageEnterTransition(style: MessageAnimation): EnterTransition = when (style) {
    MessageAnimation.FADE -> fadeIn(tween(220))
    MessageAnimation.SLIDE ->
        slideInVertically(animationSpec = tween(300)) { height -> height } + fadeIn(tween(180))
    MessageAnimation.POP ->
        scaleIn(animationSpec = tween(280), initialScale = 0.55f) + fadeIn(tween(160))
    MessageAnimation.NONE -> EnterTransition.None
}

/** Exit animation, used when a bubble leaves the list. */
fun messageExitTransition(style: MessageAnimation): ExitTransition = when (style) {
    MessageAnimation.FADE -> fadeOut(tween(120))
    MessageAnimation.SLIDE ->
        slideOutVertically(animationSpec = tween(160)) { height -> height / 3 } + fadeOut(tween(120))
    MessageAnimation.POP ->
        scaleOut(animationSpec = tween(150), targetScale = 0.85f) + fadeOut(tween(120))
    MessageAnimation.NONE -> ExitTransition.None
}

/**
 * Transition used when a screen (a conversation, settings, the composer) enters or leaves.
 *
 * This is what the "message animation" setting actually controls: how opening and closing a
 * conversation looks. `forward = false` mirrors the animation for a back navigation, and it is
 * also flipped for right-to-left layouts by the caller.
 */
fun conversationEnterTransition(
    style: MessageAnimation,
    forward: Boolean,
    rtl: Boolean = false
): EnterTransition {
    val sign = if (forward != rtl) 1f else -1f
    return when (style) {
        MessageAnimation.FADE -> fadeIn(tween(PageTransitionMillis))
        MessageAnimation.SLIDE -> slideInHorizontally(tween(PageTransitionMillis)) { full ->
            (full * sign).toInt()
        } + fadeIn(tween(PageTransitionMillis))
        MessageAnimation.POP -> scaleIn(tween(PageTransitionMillis), initialScale = 0.92f) +
            fadeIn(tween(PageTransitionMillis))
        MessageAnimation.NONE -> EnterTransition.None
    }
}

/** Mirror of [conversationEnterTransition]: the outgoing screen leaves on the other side. */
fun conversationExitTransition(
    style: MessageAnimation,
    forward: Boolean,
    rtl: Boolean = false
): ExitTransition {
    val sign = if (forward != rtl) -1f else 1f
    return when (style) {
        MessageAnimation.FADE -> fadeOut(tween(PageTransitionMillis))
        MessageAnimation.SLIDE -> slideOutHorizontally(tween(PageTransitionMillis)) { full ->
            (full * sign).toInt()
        } + fadeOut(tween(PageTransitionMillis))
        MessageAnimation.POP -> scaleOut(tween(PageTransitionMillis), targetScale = 0.94f) +
            fadeOut(tween(PageTransitionMillis))
        MessageAnimation.NONE -> ExitTransition.None
    }
}

/** Duration of a screen transition, shared by enter and exit so both directions feel identical. */
const val PageTransitionMillis = 260

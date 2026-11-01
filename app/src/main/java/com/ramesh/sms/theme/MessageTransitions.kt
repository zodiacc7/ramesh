package com.ramesh.sms.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

/**
 * Enter animation for a message bubble.
 *
 * The three styles have to be told apart at a glance - an earlier version moved the bubble by a
 * third of its height in 200 ms, which is indistinguishable from a fade on a phone, so the setting
 * looked broken. Each style now has a clearly different shape and duration, and still stays short
 * enough that a burst of incoming messages never turns into a slideshow.
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

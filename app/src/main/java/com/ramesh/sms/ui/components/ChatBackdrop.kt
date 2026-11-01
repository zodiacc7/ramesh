package com.ramesh.sms.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.ramesh.sms.theme.Appearance
import com.ramesh.sms.theme.backdropColorOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ChatBackdrop"

/**
 * Paints the chat wallpaper: a Textra-style tone, an optional photo picked from the gallery, plus a
 * scrim so bubbles stay readable.
 *
 * A tone is never flat - a slight vertical gradient keeps the selected colour visible even when it
 * is close to the theme background.
 */
@Composable
fun ChatBackdropBackground(appearance: Appearance, modifier: Modifier = Modifier) {
    val baseColor = appearance.backdrop.backdropColorOrNull()
    val fallback = MaterialTheme.colorScheme.background
    val wallpaper by rememberWallpaperImage(appearance.wallpaperUri)

    Box(modifier = modifier.fillMaxSize()) {
        if (baseColor == null) {
            Box(modifier = Modifier.fillMaxSize().background(fallback))
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(baseColor, baseColor.darken(0.12f))
                        )
                    )
            )
        }

        when (val state = wallpaper) {
            is WallpaperState.Ready -> {
                Image(
                    bitmap = state.image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Scrim: keeps bubble text readable over a bright or busy photo.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fallback.copy(alpha = 0.35f))
                )
            }
            // The picker succeeded but the file cannot be read any more (deleted, revoked grant or
            // a provider that never allowed persistable access). Say so instead of silently
            // showing an empty background, which looked like "the setting does nothing".
            is WallpaperState.Failed -> {
                Log.w(TAG, "Wallpaper could not be loaded: ${state.reason}")
            }
            WallpaperState.None, WallpaperState.Loading -> Unit
        }
    }
}

private sealed interface WallpaperState {
    data object None : WallpaperState
    data object Loading : WallpaperState
    data class Ready(val image: ImageBitmap) : WallpaperState
    data class Failed(val reason: String) : WallpaperState
}

@Composable
private fun rememberWallpaperImage(uri: String?): androidx.compose.runtime.State<WallpaperState> {
    val context = LocalContext.current
    return produceState<WallpaperState>(
        initialValue = if (uri.isNullOrBlank()) WallpaperState.None else WallpaperState.Loading,
        key1 = uri
    ) {
        if (uri.isNullOrBlank()) {
            value = WallpaperState.None
            return@produceState
        }
        val parsed = runCatching { Uri.parse(uri) }.getOrNull()
        if (parsed == null) {
            value = WallpaperState.Failed("unparseable uri")
            return@produceState
        }
        val result = withContext(Dispatchers.IO) {
            runCatching { decodeSampledBitmap(context, parsed) }
        }
        val bitmap = result.getOrNull()
        value = if (bitmap != null) {
            WallpaperState.Ready(bitmap.asImageBitmap())
        } else {
            WallpaperState.Failed(
                result.exceptionOrNull()?.message
                    ?: "decode returned null (moved, deleted or permission revoked)"
            )
        }
    }
}

/**
 * True when the app still holds a readable grant for [uri].
 *
 * `OpenDocument` grants are persisted with [android.content.ContentResolver.takePersistableUriPermission];
 * they are lost when the user clears app data, uninstalls Ramesh or the provider is removed. In that
 * case the wallpaper has to be picked again - silently falling back is what made the setting look
 * broken.
 */
fun hasPersistedReadAccess(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }
    } catch (e: Exception) {
        Log.w(TAG, "Could not inspect persisted uri grants", e)
        false
    }
}

/** Takes a persistable read grant so the chosen wallpaper survives a reboot. */
fun persistWallpaperAccess(context: Context, uri: Uri): Boolean {
    return try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        true
    } catch (e: SecurityException) {
        Log.w(TAG, "Provider refused a persistable grant for the wallpaper", e)
        false
    } catch (e: Exception) {
        Log.w(TAG, "Could not persist the wallpaper grant", e)
        false
    }
}

private fun Color.darken(fraction: Float): Color = Color(
    red = (red * (1f - fraction)).coerceIn(0f, 1f),
    green = (green * (1f - fraction)).coerceIn(0f, 1f),
    blue = (blue * (1f - fraction)).coerceIn(0f, 1f),
    alpha = alpha
)

/** Decodes a downsampled copy so full-resolution photos cannot exhaust memory. */
private fun decodeSampledBitmap(context: Context, uri: Uri, maxWidth: Int = 1440): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    } ?: throw IllegalStateException("openInputStream returned null (permission revoked?)")

    var sampleSize = 1
    val width = bounds.outWidth
    if (width > 0) {
        while (width / (sampleSize * 2) >= maxWidth) {
            sampleSize *= 2
        }
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}

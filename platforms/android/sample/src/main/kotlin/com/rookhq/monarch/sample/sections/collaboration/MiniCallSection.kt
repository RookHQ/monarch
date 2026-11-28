package com.rookhq.monarch.sample.sections.collaboration

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlin.math.roundToInt

/**
 * The catalogue page of MiniCall: system picture in picture for a call. The
 * catalogue activity declares `android:supportsPictureInPicture`. While this
 * page is open, Android 12 and later enter picture in picture by themselves
 * when the person goes home (`setAutoEnterEnabled`), animating from the call
 * area (`setSourceRectHint`); earlier versions enter from the button. Leaving
 * the page switches auto-enter off again, so the rest of the catalogue does
 * not shrink.
 */
object MiniCallSection : CataloguePage {
    override val title = "MiniCall"

    private const val Wide = 16
    private const val High = 9

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()
        val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        var result by rememberSaveable { mutableStateOf("") }
        var callArea by remember { mutableStateOf<Rect?>(null) }
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DisposableEffect(activity, callArea) {
                activity.setPictureInPictureParams(pictureInPictureParams(callArea, autoEnter = true))
                onDispose { activity.setPictureInPictureParams(pictureInPictureParams(callArea, autoEnter = false)) }
            }
        }
        CatalogueCaption("System picture in picture")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(Wide.toFloat() / High)
                .background(MonarchTheme.colors.bgMuted, MonarchTheme.shapes.md)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    callArea = Rect(
                        bounds.left.roundToInt(),
                        bounds.top.roundToInt(),
                        bounds.right.roundToInt(),
                        bounds.bottom.roundToInt(),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            MonarchText(text = "Call video", color = MonarchTheme.colors.textMuted)
        }
        MonarchButton(
            text = "Continue in picture in picture",
            onClick = {
                if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val params = pictureInPictureParams(callArea, autoEnter = true)
                    result = runCatching { activity.enterPictureInPictureMode(params) }.fold(
                        onSuccess = { entered -> if (entered) "" else "The system did not enter picture in picture." },
                        onFailure = { "The system refused picture in picture." },
                    )
                }
            },
            enabled = supported && activity != null,
        )
        if (!supported) {
            MonarchText(
                text = "This device does not support picture in picture.",
                style = MonarchTextStyle.Caption,
                color = MonarchTheme.colors.textMuted,
            )
        }
        if (result.isNotEmpty()) {
            MonarchText(text = result, style = MonarchTextStyle.Caption, color = MonarchTheme.colors.textMuted)
        }
    }

    /** 16:9 parameters for the call, animating from [sourceRect]; auto-enter on Android 12 and later. */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun pictureInPictureParams(sourceRect: Rect?, autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(Wide, High))
        if (sourceRect != null) builder.setSourceRectHint(sourceRect)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(autoEnter)
        return builder.build()
    }
}

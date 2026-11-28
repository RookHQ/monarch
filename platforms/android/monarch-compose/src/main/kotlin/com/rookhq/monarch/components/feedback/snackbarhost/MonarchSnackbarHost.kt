package com.rookhq.monarch.components.feedback.snackbarhost

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.monarchShadow
import com.rookhq.monarch.internal.MonarchStrokeGlyph
import com.rookhq.monarch.internal.MonarchStrokeGlyphMark
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.drawMonarchStrokeGlyph
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The Material 3 `SnackbarHost` with Monarch snackbars, the Android form of
 * a toast.
 *
 * Put it in the `snackbarHost` slot of a `Scaffold`, which places it at the
 * bottom above the navigation bar and the keyboard, and show snackbars with
 * [showMonarchSnackbar]. A snackbar shown with plain `SnackbarVisuals` takes
 * the neutral tone.
 *
 * Monarch draws each snackbar as on the web: up to 384dp wide on the raised
 * background with a subtle hairline, 12dp corners and one soft shadow (none
 * in a dark theme), a status mark in the tone colour at the start (or the
 * snackbar's own icon), the message at 14 in the default text colour, the
 * action as a small ghost button and an optional close button.
 *
 * Material 3 keeps its behavior: one snackbar at a time, the enter and exit
 * fade, a polite live region, a dismiss action for TalkBack, and a longer
 * time on screen when an accessibility service needs it.
 *
 * @param hostState the state that shows and queues the snackbars.
 * @param modifier the modifier of the host.
 */
@Composable
public fun MonarchSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val visuals = data.visuals as? MonarchSnackbarVisuals
        MonarchSnackbarContent(
            message = data.visuals.message,
            tone = visuals?.tone ?: MonarchTone.Neutral,
            icon = visuals?.icon,
            actionLabel = data.visuals.actionLabel,
            onAction = data::performAction,
            onDismiss = if (data.visuals.withDismissAction) data::dismiss else null,
            description = visuals?.description,
        )
    }
}

/** One Monarch snackbar: the panel, the status mark, the message and the buttons. */
@Composable
@Suppress("LongParameterList")
internal fun MonarchSnackbarContent(
    message: String,
    tone: MonarchTone,
    icon: ImageVector?,
    actionLabel: String?,
    onAction: () -> Unit,
    onDismiss: (() -> Unit)?,
    description: String? = null,
) {
    val tokens = MonarchSnackbarHostTokens
    val colors = tokens.colors(tone, MonarchTheme.colors, MonarchTheme.isHighContrast)
    val shape = MonarchTheme.shapes.lg
    val shadow = tokens.shadow(MonarchTheme.elevation, MonarchTheme.isDark)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.OuterHorizontal, vertical = tokens.OuterVertical),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = tokens.MaxWidth)
                .fillMaxWidth()
                .then(if (shadow != null) Modifier.monarchShadow(shadow, shape) else Modifier)
                .monarchSurface(MonarchSurfaceColors(colors.container, colors.content, colors.border), shape)
                .padding(
                    start = tokens.PaddingStart,
                    end = if (actionLabel != null || onDismiss != null) tokens.PaddingEnd else tokens.PaddingStart,
                    top = tokens.PaddingVertical,
                    bottom = tokens.PaddingVertical,
                ),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.content) {
                SnackbarMark(tone, icon, colors.icon)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = tokens.Gap),
                ) {
                    Text(
                        text = message,
                        style = tokens.messageStyle(MonarchTheme.typography),
                        color = colors.content,
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MonarchTheme.typography.caption,
                            color = MonarchTheme.colors.textMuted,
                        )
                    }
                }
                if (actionLabel != null || onDismiss != null) {
                    // The action and the close sit together at the end with no gap of their own: each keeps
                    // its 48dp target, and the message gets every other dp of the panel.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (actionLabel != null) {
                            MonarchButton(
                                text = actionLabel,
                                onClick = onAction,
                                variant = MonarchButtonVariant.Ghost,
                                size = MonarchButtonSize.Sm,
                            )
                        }
                        if (onDismiss != null) {
                            MonarchIconButton(
                                label = stringResource(R.string.monarch_snackbarhost_dismiss),
                                onClick = onDismiss,
                                size = MonarchButtonSize.Sm,
                            ) {
                                MonarchStrokeGlyphMark(
                                    MonarchStrokeGlyph.Cross,
                                    MonarchTheme.colors.textMuted,
                                    Modifier.size(tokens.CloseSize),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The caller's icon, or the drawn status mark of the info, success, warning and danger tones. */
@Composable
private fun SnackbarMark(tone: MonarchTone, icon: ImageVector?, color: Color) {
    val tokens = MonarchSnackbarHostTokens
    if (icon != null) {
        MonarchIcon(imageVector = icon, contentDescription = null, tint = color)
        return
    }
    val glyph = when (tone) {
        MonarchTone.Success -> MonarchStrokeGlyph.Check
        MonarchTone.Info, MonarchTone.Warning, MonarchTone.Danger -> MonarchStrokeGlyph.Exclamation
        else -> return
    }
    // Info is the exclamation mark upside down: an "i".
    val turn = if (tone == MonarchTone.Info) HalfTurn else 0f
    Canvas(Modifier.size(tokens.IconSize)) {
        val stroke = size.width * MonarchSnackbarHostTokens.RING_STROKE
        drawCircle(color, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
        inset(size.width * MonarchSnackbarHostTokens.GLYPH_INSET) {
            rotate(turn) { drawMonarchStrokeGlyph(glyph, color) }
        }
    }
}

private const val HalfTurn = 180f

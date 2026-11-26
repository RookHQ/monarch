package com.rookhq.monarch.components.actions.iconbutton

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.actions.button.ButtonSpinner
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonSurface
import com.rookhq.monarch.components.actions.button.MonarchButtonTokens
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.button.buttonFocusRing
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.LocalMonarchThemeTokens
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A button that shows only an icon.
 *
 * The square of the web's icon button: 32, 36 or 40dp visible, the same variants as [MonarchButton], with the
 * 48dp touch target reserved around it. The [label] is the name TalkBack reads, so an icon-only button is
 * never unnamed; the icon itself is decoration (pass a `null` content description to it).
 *
 * @param label what the button does, for example "Close" or "More actions". Already localised. It is the
 * TalkBack name of the button.
 * @param onClick called when the user activates the button. Not called while [loading] or when not [enabled].
 * @param modifier the modifier of the button.
 * @param variant the visual weight. Ghost by default, the usual look of a toolbar button.
 * @param size the visible side: 32, 36 or 40dp.
 * @param enabled whether the user can activate the button.
 * @param loading draws a spinner in place of the icon and ignores taps; the button keeps its size.
 * @param loadingLabel the state announced while [loading]. `null` uses the built-in string
 * `monarch_button_loading`.
 * @param shape the outline of the button and its focus ring. `null` uses the medium Monarch shape.
 * @param interactionSource the source of the press, focus and hover interactions of the button.
 * @param icon the icon, for example a `MonarchIcon` with a `null` content description.
 */
@Composable
public fun MonarchIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MonarchButtonVariant = MonarchButtonVariant.Ghost,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String? = null,
    shape: Shape? = null,
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable () -> Unit,
) {
    val side = MonarchButtonTokens.iconButtonSide(size)
    MonarchButtonSurface(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
        loadingLabel = loadingLabel,
        shape = shape ?: MonarchTheme.shapes.md,
        interactionSource = interactionSource,
        touchTarget = Modifier.monarchMinimumTouchTarget(),
        minHeight = side,
        minWidth = side,
        spinnerOverContent = false,
        semanticsExtra = { contentDescription = label },
    ) {
        if (loading) ButtonSpinner() else icon()
    }
}

/**
 * An icon-only button that stays pressed: bold in an editor toolbar, mute in a call, a thumb on an answer. Off,
 * it is a ghost icon button in the muted text colour; on, it takes the accent tint with the strong accent icon,
 * like the web's `aria-pressed` tools. Hover and press are one surface step. TalkBack reads [label] and whether it
 * is checked, as for the Material icon toggle button.
 *
 * @param label what the button turns on, for example "Bold" or "Mute". Already localised. It is the TalkBack
 * name of the button.
 * @param checked whether the button is on.
 * @param onCheckedChange called with the new state when the user toggles the button.
 * @param modifier the modifier of the button.
 * @param size the visible side: 32, 36 or 40dp.
 * @param enabled whether the user can toggle the button.
 * @param tone the colour of the on state: the accent by default; [MonarchTone.Ai] for an assistant toggle, or a
 * status tone when on means that status. Neutral uses the accent.
 * @param outlined draws a 1dp outline around the button: the default border when off, the tone's border when on.
 * It is the look of a control that must read as a button on a busy surface, such as over a call.
 * @param shape the outline of the button and its focus ring. `null` uses the medium Monarch shape.
 * @param interactionSource the source of the press, focus and hover interactions of the button.
 * @param icon the icon, for example a `MonarchIcon` with a `null` content description.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchIconButton(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    enabled: Boolean = true,
    shape: Shape? = null,
    interactionSource: MutableInteractionSource? = null,
    tone: MonarchTone = MonarchTone.Accent,
    outlined: Boolean = false,
    icon: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    val outline = shape ?: MonarchTheme.shapes.md
    val colors = MonarchTheme.colors
    val look = toggleLook(colors, checked, enabled, pressed || hovered, tone, outlined)
    val ring = if (focused && enabled) {
        Modifier.buttonFocusRing(outline, colors.focusRing, LocalMonarchThemeTokens.current.focusRingWidth.dp)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .toggleable(
                value = checked,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) { contentDescription = label }
            .monarchMinimumTouchTarget(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .then(ring)
                .size(MonarchButtonTokens.iconButtonSide(size))
                .background(look.fill, outline)
                .then(
                    if (look.border != null) {
                        Modifier.border(MonarchButtonTokens.BorderWidth, look.border, outline)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides look.content, content = icon)
        }
    }
}

/** The fill, the icon colour and the optional outline of a toggle icon button. */
internal class MonarchIconToggleLook(val fill: Color, val content: Color, val border: Color?)

/**
 * The look of a toggle icon button: off is quiet (muted icon, a surface step on hover); on takes the tone's subtle
 * fill with its strong icon. [outlined] adds the hairline, in the default border when off and the tone's when on.
 */
internal fun toggleLook(
    colors: MonarchColors,
    checked: Boolean,
    enabled: Boolean,
    highlighted: Boolean,
    tone: MonarchTone = MonarchTone.Accent,
    outlined: Boolean = false,
): MonarchIconToggleLook {
    val on = onLook(colors, highlighted, tone)
    return when {
        !enabled -> MonarchIconToggleLook(
            if (checked) colors.bgMuted else Color.Transparent,
            colors.textDisabled,
            colors.borderSubtle.takeIf { outlined },
        )
        checked -> MonarchIconToggleLook(on.first, on.second, on.third.takeIf { outlined })
        highlighted -> MonarchIconToggleLook(
            colors.bgMuted,
            colors.textDefault,
            colors.borderDefault.takeIf { outlined },
        )
        else -> MonarchIconToggleLook(
            if (outlined) colors.bgSurface else Color.Transparent,
            colors.textMuted,
            colors.borderDefault.takeIf { outlined },
        )
    }
}

/** The fill, icon colour and border of the on state in [tone]; Neutral and Accent use the accent. */
private fun onLook(colors: MonarchColors, highlighted: Boolean, tone: MonarchTone): Triple<Color, Color, Color> =
    when (tone) {
        MonarchTone.Ai ->
            Triple(if (highlighted) colors.aiSubtleHover else colors.aiSubtle, colors.aiStrong, colors.aiBorder)
        MonarchTone.Neutral, MonarchTone.Accent -> Triple(
            if (highlighted) colors.accentSubtleHover else colors.accentSubtle,
            colors.accentStrong,
            colors.accentBorder,
        )
        else -> {
            val palette = MonarchTonePalette.of(tone, colors)
            Triple(palette.subtle, palette.strong, palette.border)
        }
    }

package dev.monarchui.components.navigation.link

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import dev.monarchui.internal.MonarchControlState
import dev.monarchui.theme.MonarchTheme

/** Values for links that are part of a paragraph rather than a `MonarchLink` of their own. */
public object MonarchLinkDefaults {
    /**
     * The Monarch link look for a [LinkAnnotation] inside an annotated string:
     * `LinkAnnotation.Url(url, MonarchLinkDefaults.styles())` or
     * `LinkAnnotation.Clickable(tag, MonarchLinkDefaults.styles()) { … }`.
     *
     * The styles follow the same rules as `MonarchLink`: the link colour (or
     * the muted text colour for `Subtle`), underlined when pressed, hovered or
     * focused, and always underlined in the high-contrast themes.
     *
     * @param variant the look of the link.
     */
    @Composable
    public fun styles(variant: MonarchLinkVariant = MonarchLinkVariant.Inline): TextLinkStyles {
        val colors = MonarchTheme.colors
        val highContrast = MonarchTheme.isHighContrast
        val resting = MonarchLinkTokens.appearance(variant, MonarchControlState(), highContrast, colors)
        val active = MonarchLinkTokens.appearance(variant, MonarchControlState(pressed = true), highContrast, colors)
        return TextLinkStyles(
            style = resting.toSpanStyle(),
            focusedStyle = active.toSpanStyle(),
            hoveredStyle = active.toSpanStyle(),
            pressedStyle = active.toSpanStyle(),
        )
    }
}

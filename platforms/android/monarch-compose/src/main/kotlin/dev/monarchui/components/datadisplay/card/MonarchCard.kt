package dev.monarchui.components.datadisplay.card

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchTouchTargetOverhang
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.monarchShadow
import dev.monarchui.theme.MonarchTheme

/**
 * A surface that groups related content.
 *
 * The card of the web: Monarch surface colour, a 1dp border, 12dp corners and, when [elevated], the Monarch
 * shadow. It fills the available width, like a block on the web; pass `fillWidth = false` to wrap its
 * content, for example in a row of cards. It can have a [header] and a [footer]: the header has 20dp of
 * space above and at the sides, the footer sits under a subtle divider with 12dp by 20dp of padding, and
 * [content] has [contentPadding] around it (20dp by default).
 *
 * @param modifier the modifier of the card.
 * @param elevated draws the raised surface colour and a shadow, for a card that floats above the screen.
 * @param contentPadding the space between the edge of the card and [content].
 * @param selected draws the card as chosen: the subtle accent fill and a 1dp accent edge inside the card (nothing
 * moves) and TalkBack's selected state. A card that is picked from a set, such as an issue or a file.
 * @param tone draws the border in the tone's colour, for a card that carries a status. `null` and
 * [MonarchTone.Neutral] keep the default border. The text keeps carrying the meaning; the border is a hint.
 * @param fillWidth fills the available width. `false` wraps the content.
 * @param header content above [content], for example a [MonarchCardHeader]. `null` for none.
 * @param footer content under [content], for example the actions of the card, on a divider. `null` for none.
 * @param content the content, laid out in a column.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    contentPadding: PaddingValues = MonarchCardTokens.ContentPadding,
    fillWidth: Boolean = true,
    selected: Boolean = false,
    tone: MonarchTone? = null,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.lg
    val shadow = MonarchTheme.elevation.md
    Surface(
        modifier = modifier.cardWidth(fillWidth).then(
            if (elevated) Modifier.monarchShadow(shadow, shape) else Modifier,
        ).cardSelection(selected),
        shape = shape,
        color = MonarchCardTokens.container(elevated, selected, colors),
        contentColor = colors.textDefault,
        border = MonarchCardTokens.border(elevated, selected, tone, colors),
    ) {
        CardColumn(contentPadding, header, footer, content)
    }
}

/**
 * A card that the user can tap as a whole, for example to open a detail screen.
 *
 * TalkBack reads the content as one element and announces it as actionable.
 * Do not put other tappable elements inside a tappable card.
 *
 * @param onClick called when the user taps the card.
 * @param modifier the modifier of the card.
 * @param enabled whether the user can tap the card.
 * @param elevated draws the raised surface colour and a shadow.
 * @param contentPadding the space between the edge of the card and [content].
 * @param selected draws the card as chosen: the subtle accent fill and a 1dp accent edge inside the card (nothing
 * moves) and TalkBack's selected state. A card that is picked from a set, such as an issue or a file.
 * @param tone draws the border in the tone's colour, for a card that carries a status. `null` and
 * [MonarchTone.Neutral] keep the default border. The text keeps carrying the meaning; the border is a hint.
 * @param fillWidth fills the available width. `false` wraps the content.
 * @param header content above [content], for example a [MonarchCardHeader]. `null` for none.
 * @param footer content under [content], on a divider. `null` for none.
 * @param interactionSource the source of the press, focus and hover interactions of the card.
 * @param content the content, laid out in a column.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    elevated: Boolean = false,
    contentPadding: PaddingValues = MonarchCardTokens.ContentPadding,
    fillWidth: Boolean = true,
    selected: Boolean = false,
    tone: MonarchTone? = null,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.lg
    val shadow = MonarchTheme.elevation.md
    val base = modifier.cardWidth(fillWidth).monarchFocusRing(shape)
    Surface(
        onClick = onClick,
        modifier = (if (elevated) base.monarchShadow(shadow, shape) else base).cardSelection(selected),
        enabled = enabled,
        shape = shape,
        color = MonarchCardTokens.container(elevated, selected, colors),
        contentColor = colors.textDefault,
        border = MonarchCardTokens.border(elevated, selected, tone, colors),
        interactionSource = interactionSource,
    ) {
        CardColumn(contentPadding, header, footer, content)
    }
}

/** Exposes the chosen state to TalkBack, only for a card that is selected (a plain card has no such state). */
private fun Modifier.cardSelection(selected: Boolean): Modifier =
    if (selected) semantics { this.selected = true } else this

/** The caller's modifier first, so a width the caller sets wins; then the fill of the available width. */
private fun Modifier.cardWidth(fillWidth: Boolean): Modifier = if (fillWidth) fillMaxWidth() else this

@Composable
private fun CardColumn(
    contentPadding: PaddingValues,
    header: (@Composable ColumnScope.() -> Unit)?,
    footer: (@Composable ColumnScope.() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        if (header != null) {
            Column(Modifier.padding(MonarchCardTokens.HeaderPadding), content = header)
        }
        Column(modifier = Modifier.padding(contentPadding), content = content)
        if (footer != null) {
            MonarchDivider(variant = MonarchDividerVariant.Subtle)
            // The 48dp targets of the footer's buttons overhang into its padding, so the footer is 12dp deep
            // above and under the visible buttons, as on the web, not 20.
            Column(
                Modifier
                    .padding(MonarchCardTokens.FooterPadding)
                    .monarchTouchTargetOverhang(),
                content = footer,
            )
        }
    }
}

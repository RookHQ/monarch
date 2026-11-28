package dev.monarchui.components.layout.divider

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.theme.MonarchTheme

/**
 * A 1dp line that separates content.
 *
 * A thin layer over the Material 3 `HorizontalDivider` and `VerticalDivider`
 * in the Monarch border colour. The line is decoration: TalkBack skips it.
 *
 * A vertical divider takes the height of its parent, so the parent needs a
 * bounded height, for example `Row(Modifier.height(IntrinsicSize.Min))`.
 *
 * @param modifier the modifier of the divider.
 * @param orientation horizontal or vertical.
 * @param variant the default border colour, or the subtle one between rows.
 */
@Composable
public fun MonarchDivider(
    modifier: Modifier = Modifier,
    orientation: MonarchDividerOrientation = MonarchDividerOrientation.Horizontal,
    variant: MonarchDividerVariant = MonarchDividerVariant.Default,
) {
    val color = MonarchDividerTokens.color(variant, MonarchTheme.colors)
    when (orientation) {
        MonarchDividerOrientation.Horizontal ->
            HorizontalDivider(modifier = modifier, thickness = MonarchDividerTokens.Thickness, color = color)
        MonarchDividerOrientation.Vertical ->
            VerticalDivider(modifier = modifier, thickness = MonarchDividerTokens.Thickness, color = color)
    }
}

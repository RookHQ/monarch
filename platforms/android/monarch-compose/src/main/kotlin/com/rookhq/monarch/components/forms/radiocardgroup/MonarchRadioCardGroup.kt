package com.rookhq.monarch.components.forms.radiocardgroup

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.forms.radio.MonarchRadio
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * One choice shown as selectable cards, each with a title and an optional
 * description, icon and meta text, such as a plan or a shipping speed.
 *
 * Each card is `Modifier.selectable` with `Role.RadioButton` and a `MonarchRadio`
 * as its mark, inside a `selectableGroup`, so TalkBack reads each card as a
 * radio button with its position in the group. The selected card has the
 * accent subtle fill, a 1.5dp accent edge (drawn, so nothing moves) and a filled
 * radio mark, so the choice is not colour alone. Pressing a card is one surface
 * step; keyboard and D-pad focus draw the Monarch focus ring around the card.
 *
 * @param options the cards, in order.
 * @param selected the value of the chosen card, or `null` for no choice yet.
 * @param onSelect called with the value of the card the user chooses.
 * @param modifier the modifier of the group.
 * @param label the visible label of the group.
 * @param columns cards per row, from 1 to 3, on a window at least 640dp wide. Narrower windows and font scales
 * of 1.5 and above always use one column.
 * @param enabled whether any card can be chosen. A card can also be disabled on its own.
 */
@Composable
public fun <T> MonarchRadioCardGroup(
    options: List<MonarchRadioCardOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    columns: Int = 1,
    enabled: Boolean = true,
) {
    MonarchRadioCardGroup(
        options = options,
        selected = selected,
        onSelect = onSelect,
        modifier = modifier,
        label = label,
        columns = columns,
        enabled = enabled,
        layout = MonarchRadioCardLayout.Row,
    )
}

/**
 * The radio card group with a choice of [layout]: [MonarchRadioCardLayout.Row] (what the overload above uses) or
 * [MonarchRadioCardLayout.Tile], centred tiles that keep [columns] on a phone, as the web's type chooser does.
 * Everything else is as in the overload above.
 *
 * @param layout how the cards are laid out.
 */
@Composable
public fun <T> MonarchRadioCardGroup(
    options: List<MonarchRadioCardOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    columns: Int = 1,
    enabled: Boolean = true,
    layout: MonarchRadioCardLayout,
) {
    val tokens = MonarchRadioCardGroupTokens
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.LabelGap)) {
        if (label != null) {
            MonarchText(
                text = label,
                style = MonarchTextStyle.Label,
                color = if (enabled) MonarchTheme.colors.textDefault else MonarchTheme.colors.textDisabled,
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale
            val count = if (layout == MonarchRadioCardLayout.Tile) {
                tokens.tileColumns(columns, fontScale)
            } else {
                tokens.columns(columns, maxWidth, fontScale)
            }
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(tokens.Gap),
            ) {
                options.chunked(count).forEach { row ->
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                    ) {
                        row.forEach { option ->
                            RadioCard(
                                option = option,
                                selected = option.value == selected,
                                enabled = enabled && option.enabled,
                                tile = layout == MonarchRadioCardLayout.Tile,
                                onClick = { onSelect(option.value) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        repeat(count - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> RadioCard(
    option: MonarchRadioCardOption<T>,
    selected: Boolean,
    enabled: Boolean,
    tile: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val tokens = MonarchRadioCardGroupTokens
    val colors = MonarchTheme.colors
    val motion = MonarchTheme.motion
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val card = tokens.card(selected, enabled, colors, pressed)
    val shape = MonarchTheme.shapes.lg
    val container by animateColorAsState(card.container, motion.spec(motion.fast), label = "card fill")
    val edge by animateColorAsState(card.border ?: card.container, motion.spec(motion.fast), label = "card edge")
    val inner by animateFloatAsState(if (selected) 1f else 0f, motion.spec(motion.fast), label = "card inner edge")
    val radius = MonarchTokens.Radius.lg.dp
    val surface = modifier
            .monarchFocusRing(shape)
            .selectable(
                selected = selected,
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .background(container, shape)
            .border(tokens.BorderWidth, edge, shape)
            .drawWithContent {
                drawContent()
                // The web's 1px border plus a 1px inset ring when chosen: a 2dp edge with no change of layout.
                if (inner > 0f) {
                    // The inner ring grows in with the selection instead of fading (no alpha dimming).
                    val border = tokens.BorderWidth.toPx()
                    val ringWidth = (tokens.borderWidth(true) - tokens.BorderWidth).toPx() * inner
                    val inset = border + ringWidth / 2
                    drawRoundRect(
                        color = edge,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        cornerRadius = CornerRadius(radius.toPx() - inset),
                        style = Stroke(ringWidth),
                    )
                }
            }
            .heightIn(min = MonarchTouchTarget.Minimum)
            .padding(tokens.ContentPadding)
    if (tile) {
        TileContent(option, selected, enabled, card.content, surface)
        return
    }
    Row(
        modifier = surface,
        horizontalArrangement = Arrangement.spacedBy(tokens.ContentGap),
        verticalAlignment = Alignment.Top,
    ) {
        option.icon?.let { icon ->
            Box(modifier = Modifier.clearAndSetSemantics { }) {
                CompositionLocalProvider(
                    LocalContentColor provides tokens.icon(selected, enabled, colors),
                    content = icon,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.MetaGap)) {
                Text(
                    text = option.title,
                    modifier = Modifier.weight(1f),
                    style = MonarchTheme.typography.label,
                    color = card.content,
                )
                option.meta?.let { meta ->
                    Text(
                        text = meta,
                        style = tokens.metaStyle(MonarchTheme.typography),
                        color = tokens.muted(enabled, colors),
                    )
                }
            }
            option.description?.let { description ->
                Text(
                    text = description,
                    style = MonarchTheme.typography.caption,
                    color = tokens.muted(enabled, colors),
                )
            }
        }
        // The whole card is the control; the radio mark only shows the state. It sits at the end, as on the web.
        Box(Modifier.padding(top = tokens.MarkTop)) {
            MonarchRadio(selected = selected, onClick = null, enabled = enabled)
        }
    }
}

/** The centred tile: the icon above the title, the meta and the description, all centred, with no radio mark. */
@Composable
private fun <T> TileContent(
    option: MonarchRadioCardOption<T>,
    selected: Boolean,
    enabled: Boolean,
    contentColor: Color,
    modifier: Modifier,
) {
    val tokens = MonarchRadioCardGroupTokens
    val colors = MonarchTheme.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.TextGap, Alignment.CenterVertically),
    ) {
        option.icon?.let { icon ->
            Box(modifier = Modifier.padding(bottom = tokens.TileIconGap).clearAndSetSemantics { }) {
                CompositionLocalProvider(
                    LocalContentColor provides tokens.icon(selected, enabled, colors),
                    content = icon,
                )
            }
        }
        Text(
            text = option.title,
            style = MonarchTheme.typography.label,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
        option.meta?.let { meta ->
            Text(
                text = meta,
                style = tokens.metaStyle(MonarchTheme.typography),
                color = tokens.muted(enabled, colors),
                textAlign = TextAlign.Center,
            )
        }
        option.description?.let { description ->
            Text(
                text = description,
                style = MonarchTheme.typography.caption,
                color = tokens.muted(enabled, colors),
                textAlign = TextAlign.Center,
            )
        }
    }
}

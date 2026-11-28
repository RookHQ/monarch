package dev.monarchui.components.intelligence.aimodelpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.badge.MonarchBadgeSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.MonarchAiAccent
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchGlyphs
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/** The content of the model panel: the models under their provider headings. */
@Composable
internal fun MonarchAiModelPickerList(
    selection: String?,
    models: List<MonarchAiModelOption>,
    canConfigure: Boolean,
    labels: MonarchAiModelPickerLabels,
    onChoose: (MonarchAiModelOption, MonarchAiModelPickerOptionState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MonarchAiModelPickerTokens
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.SheetGap),
    ) {
        LazyColumn(modifier = Modifier.heightIn(max = MaxListHeight)) {
            tokens.groups(models).forEach { (provider, group) ->
                item(key = "provider:$provider") {
                    MonarchText(
                        text = provider,
                        modifier = Modifier.padding(start = tokens.RowPadding, top = tokens.RowPadding),
                        style = MonarchTextStyle.Caption,
                        color = MonarchTheme.colors.textMuted,
                        heading = true,
                    )
                }
                items(group, key = { "model:${it.id}" }) { model ->
                    val state = tokens.state(model, canConfigure)
                    MonarchAiModelPickerRow(model, model.id == selection, state, labels) { onChoose(model, state) }
                }
            }
        }
    }
}

private val MaxListHeight = (MonarchTokens.Space.space16 * 8).dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonarchAiModelPickerRow(
    model: MonarchAiModelOption,
    selected: Boolean,
    state: MonarchAiModelPickerOptionState,
    labels: MonarchAiModelPickerLabels,
    onClick: () -> Unit,
) {
    val tokens = MonarchAiModelPickerTokens
    val colors = MonarchTheme.colors
    val enabled = state != MonarchAiModelPickerOptionState.Blocked
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .then(if (selected) Modifier.background(colors.aiSubtle, MonarchTheme.shapes.md) else Modifier)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = tokens.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
        verticalAlignment = Alignment.Top,
    ) {
        // As the web: the chosen model is a magenta check on a subtle AI row; the others have no mark.
        MonarchAiAccent {
            if (selected) {
                Box(Modifier.size(MonarchTokens.Space.space5.dp), contentAlignment = Alignment.Center) {
                    MonarchIcon(
                        MonarchGlyphs.Check,
                        contentDescription = null,
                        size = MonarchIconSize.Sm,
                        tint = colors.aiStrong,
                    )
                }
            } else {
                // No ring on the other rows, as on the web: the selection is told by the check and the row tint.
                Spacer(Modifier.size(MonarchTokens.Space.space5.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
                verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = model.name,
                    style = MonarchTheme.typography.bodyStrong,
                    color = tokens.nameColor(state, colors),
                )
                if (model.requiresKey) {
                    if (model.keyConfigured) {
                        MonarchBadge(
                            text = monarchString(labels.ownKey, R.string.monarch_aimodelpicker_own_key),
                            tone = MonarchTone.Success,
                            size = MonarchBadgeSize.Sm,
                        )
                    } else {
                        MonarchBadge(
                            text = monarchString(labels.keyNeeded, R.string.monarch_aimodelpicker_key_needed),
                            tone = MonarchTone.Warning,
                            leadingIcon = MonarchDefaultIcons.Lock,
                            size = MonarchBadgeSize.Sm,
                        )
                    }
                }
            }
            model.description?.let {
                Text(text = it, style = MonarchTheme.typography.caption, color = tokens.detailColor(state, colors))
            }
            if (model.capabilities.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
                    verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                ) {
                    model.capabilities.forEach { capability ->
                        MonarchBadge(
                            text = labels.capability?.invoke(capability)
                                ?: stringResource(tokens.capabilityText(capability)),
                            tone = MonarchTone.Neutral,
                            size = MonarchBadgeSize.Sm,
                        )
                    }
                }
            }
            MonarchAiModelPickerMeta(model, state, labels)
            when (state) {
                MonarchAiModelPickerOptionState.Key -> SpokenOnly(
                    monarchString(labels.addKeyHint, R.string.monarch_aimodelpicker_add_key_hint),
                )
                MonarchAiModelPickerOptionState.Blocked -> SpokenOnly(
                    monarchString(labels.unavailable, R.string.monarch_aimodelpicker_unavailable),
                )
                MonarchAiModelPickerOptionState.Ok -> Unit
            }
        }
    }
}

/** The context size, the speed and the cost in one muted line; the cost is drawn as $ and read in words. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonarchAiModelPickerMeta(
    model: MonarchAiModelOption,
    state: MonarchAiModelPickerOptionState,
    labels: MonarchAiModelPickerLabels,
) {
    val tokens = MonarchAiModelPickerTokens
    val locale = LocalConfiguration.current.locales[0]
    val parts = listOfNotNull(
        model.contextTokens?.let { count ->
            val compact = tokens.compact(count, locale)
            labels.context?.invoke(compact) ?: stringResource(R.string.monarch_aimodelpicker_context, compact)
        },
        model.speed?.let { labels.speed?.invoke(it) ?: stringResource(tokens.speedText(it)) },
    )
    val tier = tokens.tier(model)
    if (parts.isEmpty() && tier == null) return
    val color = tokens.detailColor(state, MonarchTheme.colors)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.ValueGap)) {
        if (parts.isNotEmpty()) {
            Text(text = parts.joinToString(" · "), style = MonarchTheme.typography.caption, color = color)
        }
        if (tier != null) {
            val spoken = labels.cost?.invoke(tier) ?: stringResource(tokens.costText(tier))
            Text(
                text = (if (parts.isNotEmpty()) "· " else "") + "$".repeat(tier),
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                style = MonarchTheme.typography.caption,
                color = color,
            )
        }
    }
}

/** Text that TalkBack reads with the row and that is not drawn. */
@Composable
private fun SpokenOnly(text: String) {
    Box(Modifier.semantics { contentDescription = text })
}

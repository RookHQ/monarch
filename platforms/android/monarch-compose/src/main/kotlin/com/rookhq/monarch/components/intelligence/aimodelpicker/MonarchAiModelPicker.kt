package com.rookhq.monarch.components.intelligence.aimodelpicker

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.overlays.popover.MonarchPopover
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Lets people choose which AI model answers: a button showing the current
 * model that opens a list of models grouped by provider, with capability
 * badges, context size, speed, cost and the state of bring-your-own provider
 * keys.
 *
 * Built by Monarch from a secondary `MonarchButton` that opens a
 * `MonarchPopover` under it, holding a `LazyColumn` of selectable rows with a
 * check mark, provider headings and `MonarchBadge`s. A menu cannot show
 * badges and several lines per model, so the list is a panel, as on the web.
 * Choosing a model calls [onSelectionChange] and closes the panel. A model
 * whose key is missing calls [onConfigureKey] instead; without it such
 * models are shown but cannot be chosen.
 *
 * @param selection the id of the selected model, or `null` for none. An id that matches no model shows the
 * placeholder.
 * @param models the models in display order, grouped by provider in first-seen order.
 * @param onSelectionChange called with the id of the chosen model.
 * @param modifier the modifier of the button.
 * @param size the height and padding of the button.
 * @param enabled whether the button opens the list.
 * @param trailingIcon an icon at the end of the button, for example a chevron that points down as a `MonarchIcon`
 * with a `null` content description. Monarch bundles no icons, so there is no default.
 * @param labels replacements for every built-in string.
 * @param interactionSource the source of the press, focus and hover interactions of the button.
 * @param onConfigureKey called with a model whose key is missing when it is chosen, to add the key.
 * @param initiallyOpen shows the list of models open when the picker first appears, as the web demo does.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
public fun MonarchAiModelPicker(
    selection: String?,
    models: List<MonarchAiModelOption>,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    labels: MonarchAiModelPickerLabels = MonarchAiModelPickerLabels(),
    interactionSource: MutableInteractionSource? = null,
    onConfigureKey: ((MonarchAiModelOption) -> Unit)? = null,
    initiallyOpen: Boolean = false,
) {
    val tokens = MonarchAiModelPickerTokens
    val colors = MonarchTheme.colors
    var open by rememberSaveable { mutableStateOf(initiallyOpen) }
    val selected = models.firstOrNull { it.id == selection }
    val name = monarchString(labels.label, R.string.monarch_aimodelpicker_label)
    val placeholder = monarchString(labels.placeholder, R.string.monarch_aimodelpicker_placeholder)
    val value = selected?.let { stringResource(R.string.monarch_aimodelpicker_value, it.name, it.provider) }
        ?: placeholder
    Box(modifier = modifier) {
    MonarchButton(
        onClick = { open = true },
        modifier = Modifier.semantics {
            contentDescription = name
            stateDescription = value
        },
        size = size,
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        FlowRow(
            modifier = Modifier.weight(1f, fill = false),
            horizontalArrangement = Arrangement.spacedBy(tokens.ValueGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected != null) {
                Text(text = selected.name)
                Text(text = selected.provider, color = tokens.mutedColor(enabled, colors))
            } else {
                Text(text = placeholder, color = tokens.mutedColor(enabled, colors))
            }
        }
        if (trailingIcon != null) {
            CompositionLocalProvider(
                LocalContentColor provides tokens.mutedColor(enabled, colors),
                content = trailingIcon,
            )
        }
    }
    // As on the web: the list drops down under the button, in a panel, not in a full sheet.
    MonarchPopover(expanded = open, onDismissRequest = { open = false }, label = name) {
        MonarchAiModelPickerList(
            selection = selection,
            models = models,
            canConfigure = onConfigureKey != null,
            labels = labels,
            onChoose = { model, state ->
                open = false
                if (state == MonarchAiModelPickerOptionState.Key) {
                    onConfigureKey?.invoke(model)
                } else {
                    onSelectionChange(model.id)
                }
            },
        )
    }
    }
}

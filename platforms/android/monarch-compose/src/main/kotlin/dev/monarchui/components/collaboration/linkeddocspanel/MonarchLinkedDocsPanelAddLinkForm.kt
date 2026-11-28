package dev.monarchui.components.collaboration.linkeddocspanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.internal.monarchString

/**
 * The inline form of a `MonarchLinkedDocsPanel` that adds an external link:
 * an address, an optional title, Add and Cancel. An address that is not a
 * full http or https address shows an error and is not submitted; an empty
 * title becomes the host name.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonarchLinkedDocsPanelAddLinkForm(
    labels: MonarchLinkedDocsPanelLabels,
    onAdd: (url: String, title: String) -> Unit,
    onClose: () -> Unit,
) {
    val tokens = MonarchLinkedDocsPanelTokens
    var url by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val invalidText = monarchString(labels.urlInvalid, R.string.monarch_linkeddocspanel_url_invalid)
    val submit = {
        val clean = tokens.parseLinkUrl(url)
        if (clean == null) {
            invalid = true
        } else {
            onAdd(clean, title.trim().ifEmpty { tokens.hostOf(clean) })
            onClose()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.PartGap)) {
        MonarchTextField(
            value = url,
            onValueChange = {
                url = it
                invalid = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = monarchString(labels.urlLabel, R.string.monarch_linkeddocspanel_url_label),
            errorText = invalidText.takeIf { invalid },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        )
        MonarchTextField(
            value = title,
            onValueChange = { title = it.take(MaxTitleLength) },
            modifier = Modifier.fillMaxWidth(),
            label = monarchString(labels.titleLabel, R.string.monarch_linkeddocspanel_title_label),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.PartGap)) {
            MonarchButton(
                text = monarchString(labels.add, R.string.monarch_linkeddocspanel_add),
                onClick = submit,
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
            )
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_linkeddocspanel_cancel),
                onClick = onClose,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

/** The longest title the form accepts, as on the web. */
private const val MaxTitleLength = 300

package dev.monarchui.components.technical.jsonviewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.forms.textfield.MonarchTextFieldLabelVisibility
import dev.monarchui.foundation.MonarchFieldSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.theme.MonarchTheme

/** The most lines of the selected path in the footer. */
private const val PathLines = 2

/**
 * The toolbar: the search field (a `MonarchTextField`), the match count in a polite live region while searching, and
 * Collapse all.
 */
@Composable
internal fun MonarchJsonViewerToolbar(
    searchable: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    matches: String?,
    searchIcon: (@Composable () -> Unit)?,
    clearIcon: (@Composable () -> Unit)?,
    strings: MonarchJsonViewerStrings,
    onCollapseAll: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MonarchJsonViewerTokens.Bar),
        verticalArrangement = Arrangement.spacedBy(MonarchJsonViewerTokens.Bar),
    ) {
        // As on the web: the search field and Collapse all share one row, and the match count sits below.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MonarchJsonViewerTokens.Bar),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (searchable) {
                MonarchTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    label = strings.search,
                    labelVisibility = MonarchTextFieldLabelVisibility.Hidden,
                    size = MonarchFieldSize.Small,
                    placeholder = strings.searchPrompt,
                    leadingIcon = searchIcon,
                    trailingIcon = if (clearIcon != null && query.isNotEmpty()) {
                        {
                            MonarchIconButton(
                                label = strings.clearSearch,
                                onClick = { onQueryChange("") },
                                size = MonarchButtonSize.Sm,
                            ) { clearIcon() }
                        }
                    } else {
                        null
                    },
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            MonarchButton(
                text = strings.collapseAll,
                onClick = onCollapseAll,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        if (matches != null) {
            MonarchText(
                text = matches,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTextStyle.Caption,
                color = MonarchJsonViewerTokens.muted(MonarchTheme.colors),
            )
        }
    }
}

/** The footer: the selected path, read as "Selected" and the path, with Copy path and Copy value. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonarchJsonViewerFooter(
    path: String,
    value: MonarchJsonValue,
    strings: MonarchJsonViewerStrings,
    onCopy: ((String) -> Unit)?,
) {
    val spoken = strings.selectedPath + ", " + path
    Column(
        modifier = Modifier.fillMaxWidth().padding(MonarchJsonViewerTokens.Bar),
        verticalArrangement = Arrangement.spacedBy(MonarchJsonViewerTokens.Bar),
    ) {
        MonarchText(
            text = path,
            modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
            style = MonarchTextStyle.CodeSmall,
            color = MonarchJsonViewerTokens.muted(MonarchTheme.colors),
            overflow = TextOverflow.Ellipsis,
            maxLines = PathLines,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchJsonViewerTokens.Bar)) {
            MonarchCopyButton(
                value = path,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                onCopy = onCopy,
                label = strings.copyPath,
            )
            MonarchCopyButton(
                value = MonarchJsonViewerPaths.copyText(value),
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                onCopy = onCopy,
                label = strings.copyValue,
            )
        }
    }
}

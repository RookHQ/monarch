package com.rookhq.monarch.components.collaboration.linkeddocspanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuDivider
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.search.searchbar.MonarchSearchBar
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The Link doc button of a `MonarchLinkedDocsPanel` and its menu: a search
 * field, the docs that are not linked yet and match it, and New doc.
 */
@Composable
internal fun MonarchLinkedDocsPanelDocMenu(
    options: List<MonarchLinkedDocsPanelOption>,
    linked: Set<String>,
    labels: MonarchLinkedDocsPanelLabels,
    onLinkDoc: (docId: String) -> Unit,
    onDocQueryChange: ((String) -> Unit)?,
    onCreateDoc: (() -> Unit)?,
    icon: (@Composable () -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val shown = MonarchLinkedDocsPanelTokens.options(options, linked, query)
    Box {
        MonarchButton(
            text = monarchString(labels.linkDoc, R.string.monarch_linkeddocspanel_link_doc),
            onClick = { open = true },
            size = MonarchButtonSize.Sm,
            leadingIcon = icon,
        )
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
            MonarchSearchBar(
                query = query,
                onQueryChange = {
                    query = it
                    onDocQueryChange?.invoke(it)
                },
                onSearch = {},
                modifier = Modifier.padding(horizontal = MonarchLinkedDocsPanelTokens.PartGap),
                placeholder = monarchString(labels.searchDocs, R.string.monarch_linkeddocspanel_search_docs),
            )
            if (shown.isEmpty()) {
                MonarchText(
                    text = monarchString(labels.noDocs, R.string.monarch_linkeddocspanel_no_docs),
                    modifier = Modifier.padding(MonarchLinkedDocsPanelTokens.SectionGap),
                    color = MonarchLinkedDocsPanelTokens.mutedText(MonarchTheme.colors),
                )
            }
            shown.forEach { option ->
                MonarchMenuItem(
                    onClick = {
                        open = false
                        query = ""
                        onLinkDoc(option.docId)
                    },
                ) {
                    Text(option.label)
                    option.description?.let {
                        Text(it, color = MonarchLinkedDocsPanelTokens.mutedText(MonarchTheme.colors))
                    }
                }
            }
            if (onCreateDoc != null) {
                MonarchMenuDivider()
                MonarchMenuItem(
                    text = monarchString(labels.newDoc, R.string.monarch_linkeddocspanel_new_doc),
                    onClick = {
                        open = false
                        onCreateDoc()
                    },
                )
            }
        }
    }
}

package dev.monarchui.components.files.documentrelations

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.internal.MonarchGlyphs
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.foundation.icon.MonarchIcon
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.progress.MonarchProgressVariant
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNoticeVariant
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIcon
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIconSize
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchAnnouncerNode
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchRemovalFocus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchRemovalFallback
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchAnnouncer
import dev.monarchui.internal.rememberMonarchRemovalFocus
import dev.monarchui.theme.MonarchTheme
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Typed links between documents ("Parent of", "Supersedes", "Related to"),
 * read the right way round from either side, with a sheet to add one by
 * searching and a remove button on each.
 *
 * A pattern built from Monarch parts: rows with a `MonarchFileTypeIcon`, the
 * relation in words and the document's title, each one TalkBack element that
 * opens the document through [onOpen]; a remove `MonarchIconButton` (or a text
 * `MonarchButton` without an icon) per row; and an "Add relation"
 * `MonarchButton` that opens a Material 3 `ModalBottomSheet` with a
 * `MonarchSelect` of types, a `MonarchSearchBar` and the matching documents.
 * Choosing a document adds the relation. Adding and removing are announced.
 * The rows are a plain column, so the component can sit in a scrolling
 * screen; it lists all relations at once.
 *
 * @param relations this document's relations.
 * @param modifier the modifier of the component.
 * @param types the relation types, their labels and inverses. The defaults are the web's seven types.
 * @param candidates documents that can be linked, listed in the sheet without those already related, at most 12.
 * @param defaultType the type chosen when the sheet opens; related-to, or the first type, by default.
 * @param showEmpty shows "No relations yet." when there are none.
 * @param onSearch searches on the server as the person types; update [candidates] with the results. Without it,
 * [candidates] are filtered by title.
 * @param onOpen opens a related document, usually by navigating to it. Without it, rows are not interactive.
 * @param onAdd adds a relation with the target's id and the type's value, and shows "Add relation". Throw to keep
 * the sheet open with the error.
 * @param onRemove removes a relation by its id, and shows a remove button on each row. Throw to show the error
 * under the list.
 * @param removeIcon the icon of each remove button. Monarch bundles no icons; without it the button is the text
 * "Remove".
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Composable
@Suppress("LongParameterList", "LongMethod")
public fun MonarchDocumentRelations(
    relations: List<MonarchDocumentRelation>,
    modifier: Modifier = Modifier,
    types: List<MonarchDocumentRelationType> = MonarchDocumentRelationType.defaults(),
    candidates: List<MonarchDocumentRelationTarget> = emptyList(),
    defaultType: String? = null,
    showEmpty: Boolean = false,
    onSearch: ((String) -> Unit)? = null,
    onOpen: ((MonarchDocumentRelationTarget) -> Unit)? = null,
    onAdd: (suspend (targetId: String, type: String) -> Unit)? = null,
    onRemove: (suspend (relationId: String) -> Unit)? = null,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchDocumentRelationsLabels = MonarchDocumentRelationsLabels(),
) {
    val tokens = MonarchDocumentRelationsTokens
    val locale = LocalConfiguration.current.locales[0]
    val texts = documentRelationsTexts(labels, locale)
    val scope = rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(false) }
    var removing by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val announcer = rememberMonarchAnnouncer()
    // As on the web: after a removal focus goes to the next relation's remove button, else the previous one's,
    // else Add.
    val removalFocus = rememberMonarchRemovalFocus(relations.map { it.id })
    val remove: (MonarchDocumentRelation) -> Unit = { relation ->
        val action = onRemove
        if (action != null && removing == null) {
            removing = relation.id
            error = null
            removalFocus.removing(relations.map { it.id }, relation.id)
            scope.launch {
                val failure = runCatching { action(relation.id) }.exceptionOrNull()
                if (failure is CancellationException) throw failure
                removing = null
                if (failure == null) {
                    announcer.announce(texts.removed(relation.target.title))
                } else {
                    removalFocus.cancel()
                    error = failure.message ?: texts.failed
                }
            }
        }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        if (relations.isNotEmpty()) {
            DocumentRelationsList(
                relations = relations,
                types = types,
                texts = texts,
                onOpen = onOpen,
                removing = removing,
                removeIcon = removeIcon,
                onRemove = remove.takeIf { onRemove != null },
                removalFocus = removalFocus,
            )
        } else if (showEmpty) {
            Text(
                text = monarchString(labels.none, R.string.monarch_documentrelations_none),
                style = MonarchTheme.typography.body,
                color = tokens.secondaryColor(true, MonarchTheme.colors),
            )
        }
        error?.let {
            MonarchInlineNotice(message = it, tone = MonarchTone.Danger, variant = MonarchInlineNoticeVariant.Plain)
        }
        if (onAdd != null) {
            MonarchButton(
                text = monarchString(labels.add, R.string.monarch_documentrelations_add),
                onClick = {
                    error = null
                    open = true
                },
                modifier = Modifier.monarchRemovalFallback(removalFocus),
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = removing == null,
            )
        }
        // Always present, so each addition or removal is announced as a change of this live region.
        MonarchAnnouncerNode(announcer)
    }
    if (open && onAdd != null) {
        DocumentRelationsSheet(
            types = types,
            initialType = tokens.initialType(defaultType, types),
            candidates = candidates,
            relations = relations,
            onSearch = onSearch,
            onAdd = onAdd,
            labels = labels,
            texts = texts,
            onAdded = { title ->
                open = false
                announcer.announce(texts.added(title))
            },
            onClose = { open = false },
        )
    }
}

/** The relations as rows, in a traversal group named by the list's label. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("LongParameterList")
private fun DocumentRelationsList(
    relations: List<MonarchDocumentRelation>,
    types: List<MonarchDocumentRelationType>,
    texts: DocumentRelationsTexts,
    onOpen: ((MonarchDocumentRelationTarget) -> Unit)?,
    removing: String?,
    removeIcon: (@Composable () -> Unit)?,
    onRemove: ((MonarchDocumentRelation) -> Unit)?,
    removalFocus: MonarchRemovalFocus<String>,
) {
    FlowRow(
        modifier = Modifier.semantics {
            isTraversalGroup = true
            contentDescription = texts.label
        },
        horizontalArrangement = Arrangement.spacedBy(MonarchDocumentRelationsTokens.ChipGap),
        verticalArrangement = Arrangement.spacedBy(MonarchDocumentRelationsTokens.ChipGap),
    ) {
        relations.forEach { relation ->
            DocumentRelationRow(
                relation = relation,
                typeLabel = MonarchDocumentRelationsTokens.labelOf(relation, types).second,
                typeValue = MonarchDocumentRelationsTokens.labelOf(relation, types).first,
                onOpen = onOpen,
                removal = onRemove?.let { remove ->
                    DocumentRelationRemoval(
                        removing = removing == relation.id,
                        enabled = removing == null,
                        name = texts.remove(relation.target.title),
                        text = texts.removeText,
                        working = texts.working,
                        icon = removeIcon,
                        focus = removalFocus.requester(relation.id),
                        onRemove = { remove(relation) },
                    )
                },
            )
        }
    }
}

/** The resolved built-in strings. */
internal class DocumentRelationsTexts(
    val label: String,
    val removeText: String,
    val failed: String,
    val working: String,
    val remove: (String) -> String,
    val added: (String) -> String,
    val removed: (String) -> String,
)

@Composable
private fun documentRelationsTexts(labels: MonarchDocumentRelationsLabels, locale: Locale): DocumentRelationsTexts {
    val remove = stringResource(R.string.monarch_documentrelations_remove)
    val added = stringResource(R.string.monarch_documentrelations_added)
    val removed = stringResource(R.string.monarch_documentrelations_removed)
    return DocumentRelationsTexts(
        label = monarchString(labels.label, R.string.monarch_documentrelations_label),
        removeText = monarchString(labels.removeText, R.string.monarch_documentrelations_remove_text),
        failed = monarchString(labels.failed, R.string.monarch_documentrelations_failed),
        working = monarchString(labels.working, R.string.monarch_documentrelations_working),
        remove = labels.remove ?: { String.format(locale, remove, it) },
        added = labels.added ?: { String.format(locale, added, it) },
        removed = labels.removed ?: { String.format(locale, removed, it) },
    )
}

/** The remove button of a row. */
private class DocumentRelationRemoval(
    val removing: Boolean,
    val enabled: Boolean,
    val name: String,
    val text: String,
    val working: String,
    val icon: (@Composable () -> Unit)?,
    val focus: FocusRequester,
    val onRemove: () -> Unit,
)

@Composable
private fun DocumentRelationRow(
    relation: MonarchDocumentRelation,
    typeLabel: String,
    typeValue: String,
    onOpen: ((MonarchDocumentRelationTarget) -> Unit)?,
    removal: DocumentRelationRemoval?,
) {
    val tokens = MonarchDocumentRelationsTokens
    val colors = MonarchTheme.colors
    val target = relation.target
    Row(
        modifier = Modifier
            .heightIn(min = tokens.RowMinHeight)
            .border(MonarchShapeDefaults.BorderWidth, colors.borderDefault, CircleShape)
            .padding(start = tokens.Gap),
        horizontalArrangement = Arrangement.spacedBy(tokens.TextGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val reading = if (onOpen != null) {
            Modifier.clickable(role = Role.Button) { onOpen(target) }
        } else {
            Modifier.semantics(mergeDescendants = true) { }
        }
        Row(
            modifier = Modifier
                .heightIn(min = tokens.RowMinHeight)
                .then(reading)
                .padding(vertical = tokens.RowVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.TextGap * 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            target.fileName?.let {
                MonarchFileTypeIcon(fileName = it, size = MonarchFileTypeIconSize.Sm, decorative = true)
            }
            // The direction mark of the web's type glyph: parent up, child down, implements a check,
            // else forward.
            val glyph = when (typeValue) {
                "parent-of" -> MonarchGlyphs.ArrowUpward
                "child-of" -> MonarchGlyphs.ArrowDownward
                "implements" -> MonarchGlyphs.Check
                else -> MonarchGlyphs.ArrowForward
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.TextGap * 2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides tokens.typeColor(colors)) {
                    MonarchIcon(glyph, contentDescription = null, size = MonarchIconSize.Sm)
                }
                Text(typeLabel, style = MonarchTheme.typography.caption, color = tokens.typeColor(colors))
                Text(
                    target.title,
                    style = MonarchTheme.typography.bodyStrong,
                    color = tokens.titleColor(onOpen != null, true, colors),
                )
            }
        }
        if (removal != null) DocumentRelationRemoveButton(removal)
    }
}

@Composable
private fun DocumentRelationRemoveButton(removal: DocumentRelationRemoval) {
    val tokens = MonarchDocumentRelationsTokens
    val icon = removal.icon
    when {
        removal.removing -> Box(
            modifier = Modifier.size(tokens.RowMinHeight),
            contentAlignment = Alignment.Center,
        ) {
            MonarchProgress(
                modifier = Modifier.size(tokens.ProgressSize),
                variant = MonarchProgressVariant.Circular,
                label = removal.working,
            )
        }
        icon != null -> MonarchIconButton(
            label = removal.name,
            onClick = removal.onRemove,
            modifier = Modifier.focusRequester(removal.focus),
            enabled = removal.enabled,
        ) {
            Box(Modifier.clearAndSetSemantics { }) {
                val color = if (removal.enabled) MonarchTheme.colors.textMuted else MonarchTheme.colors.textDisabled
                CompositionLocalProvider(LocalContentColor provides color, content = icon)
            }
        }
        else -> MonarchButton(
            text = removal.text,
            onClick = removal.onRemove,
            modifier = Modifier.focusRequester(removal.focus).semantics { contentDescription = removal.name },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = removal.enabled,
        )
    }
}

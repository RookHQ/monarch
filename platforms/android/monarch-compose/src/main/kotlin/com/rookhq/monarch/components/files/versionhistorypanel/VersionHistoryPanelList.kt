package com.rookhq.monarch.components.files.versionhistorypanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.listrow.MonarchListRow
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeleton
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeletonShape
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.forms.textfield.MonarchTextFieldLabelVisibility
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.launch

/** The colour roles, under a short name. */
private val Palette = MonarchVersionHistoryPanelColors

/** The list screen: the title, the save field and the versions. */
@Composable
internal fun VersionHistoryPanelList(
    title: String?,
    versions: List<MonarchVersionHistoryPanelVersion>,
    currentId: String?,
    loading: Boolean,
    onOpen: (String) -> Unit,
    onSaveVersion: (suspend (String) -> Unit)?,
    announce: (String) -> Unit,
    context: VersionHistoryPanelContext,
    modifier: Modifier,
) {
    val labels = context.labels
    val tokens = MonarchVersionHistoryPanelTokens
    Column(modifier = modifier) {
        VersionHistoryPanelHeader(
            title = title ?: stringResource(R.string.monarch_versionhistorypanel_title),
            onBack = null,
            context = context,
        )
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (onSaveVersion != null) {
                item(key = "save") { VersionHistoryPanelSave(onSaveVersion, announce, labels) }
            }
            when {
                loading -> item(key = "loading") { VersionHistoryPanelPlaceholder(labels) }
                versions.isEmpty() -> item(key = "empty") {
                    MonarchEmptyState(
                        title = monarchString(labels.empty, R.string.monarch_versionhistorypanel_empty),
                        modifier = Modifier.padding(tokens.Inset),
                        size = MonarchEmptyStateSize.Sm,
                    )
                }
                else -> itemsIndexed(versions, key = { _, version -> version.id }) { index, version ->
                    if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
                    VersionHistoryPanelRow(version, version.id == currentId, onOpen, context)
                }
            }
        }
    }
}

/** The title, the Versions button on a version's screen, and Close. */
@Composable
internal fun VersionHistoryPanelHeader(
    title: String,
    onBack: (() -> Unit)?,
    context: VersionHistoryPanelContext,
) {
    val labels = context.labels
    val tokens = MonarchVersionHistoryPanelTokens
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.SmallGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            val back = monarchString(labels.back, R.string.monarch_versionhistorypanel_back)
            MonarchButton(
                text = monarchString(labels.versions, R.string.monarch_versionhistorypanel_versions),
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = back },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = tokens.SmallGap)
                .semantics { heading() },
            style = MonarchTheme.typography.subheading,
            color = Palette.titleColor(MonarchTheme.colors),
        )
        context.onClose?.let { close ->
            val closeLabel = monarchString(labels.close, R.string.monarch_versionhistorypanel_close)
            val closeIcon = context.closeIcon
            if (closeIcon != null) {
                MonarchIconButton(label = closeLabel, onClick = close, size = MonarchButtonSize.Sm) { closeIcon() }
            } else {
                MonarchButton(
                    text = closeLabel,
                    onClick = close,
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}

/** The name field and Save version; the error shows under the field. */
@Composable
private fun VersionHistoryPanelSave(
    onSaveVersion: suspend (String) -> Unit,
    announce: (String) -> Unit,
    labels: MonarchVersionHistoryPanelLabels,
) {
    val tokens = MonarchVersionHistoryPanelTokens
    var note by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val saved = monarchString(labels.saved, R.string.monarch_versionhistorypanel_saved)
    val failed = monarchString(labels.saveFailed, R.string.monarch_versionhistorypanel_save_failed)
    Row(
        modifier = Modifier.padding(tokens.Inset),
        horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        verticalAlignment = Alignment.Top,
    ) {
        MonarchTextField(
            value = note,
            onValueChange = { note = it.take(tokens.NOTE_MAX_LENGTH) },
            modifier = Modifier.weight(1f),
            labelVisibility = MonarchTextFieldLabelVisibility.Hidden,
            label = monarchString(labels.noteLabel, R.string.monarch_versionhistorypanel_note_label),
            placeholder = monarchString(labels.notePlaceholder, R.string.monarch_versionhistorypanel_note_placeholder),
            errorText = error,
            enabled = !saving,
        )
        MonarchButton(
            text = monarchString(labels.save, R.string.monarch_versionhistorypanel_save),
            onClick = {
                if (!saving) {
                    saving = true
                    error = null
                    scope.launch {
                        val outcome = runCatching { onSaveVersion(tokens.note(note)) }
                        saving = false
                        val thrown = outcome.exceptionOrNull()
                        if (thrown == null) {
                            note = ""
                            announce(saved)
                        } else {
                            val text = tokens.failureText(thrown, failed)
                            error = text
                            announce(text)
                        }
                    }
                }
            },
            loading = saving,
        )
    }
}

/** Three placeholder rows, read once as "Loading". */
@Composable
private fun VersionHistoryPanelPlaceholder(labels: MonarchVersionHistoryPanelLabels) {
    val tokens = MonarchVersionHistoryPanelTokens
    val loading = monarchString(labels.loading, R.string.monarch_versionhistorypanel_loading)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(tokens.Inset)
            .clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        tokens.PlaceholderLines.forEach { fraction ->
            MonarchSkeleton(modifier = Modifier.fillMaxWidth(fraction), shape = MonarchSkeletonShape.Text)
        }
    }
}

/** One version: one button read as the time, the author and the name, with Current as its state. */
@Composable
private fun VersionHistoryPanelRow(
    version: MonarchVersionHistoryPanelVersion,
    current: Boolean,
    onOpen: (String) -> Unit,
    context: VersionHistoryPanelContext,
) {
    val labels = context.labels
    val tokens = MonarchVersionHistoryPanelTokens
    val colors = MonarchTheme.colors
    val time = tokens.relativeTime(version.date, context.now, context.locale)
    val name = tokens.name(version, monarchString(labels.automatic, R.string.monarch_versionhistorypanel_automatic))
    val currentText = monarchString(labels.current, R.string.monarch_versionhistorypanel_current)
    MonarchListRow(
        headlineContent = { Text(text = time, style = MonarchTheme.typography.bodyStrong) },
        modifier = Modifier
            .semantics {
                contentDescription = tokens.spoken(listOf(time, version.authorName, name))
                if (current) stateDescription = currentText
            },
        onClick = { onOpen(version.id) },
        supportingContent = {
            Text(
                text = listOfNotNull(version.authorName, name).joinToString(" · "),
                style = MonarchTheme.typography.caption,
                color = Palette.mutedColor(colors),
            )
        },
        leadingContent = {
            MonarchAvatar(
                image = version.authorImage,
                name = version.authorName,
                size = MonarchAvatarSize.Md,
                decorative = true,
            )
        },
        trailingContent = if (current) {
            { MonarchBadge(text = currentText, tone = MonarchTone.Accent) }
        } else {
            null
        },
    )
}

package com.rookhq.monarch.components.files.versionhistorypanel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeleton
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeletonShape
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewer
import com.rookhq.monarch.components.files.diffviewer.MonarchDiffViewerGranularity
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.launch

/** The colour roles, under a short name. */
private val Palette = MonarchVersionHistoryPanelColors

/** A version's screen: the byline, Changes or Full text, and Restore. Back returns to the list. */
@Composable
internal fun VersionHistoryPanelDetail(
    version: MonarchVersionHistoryPanelVersion,
    current: Boolean,
    currentLines: List<String>,
    selectedLines: List<String>?,
    onBack: () -> Unit,
    onRestore: (suspend (String) -> Unit)?,
    announce: (String) -> Unit,
    context: VersionHistoryPanelContext,
    modifier: Modifier,
) {
    val labels = context.labels
    val tokens = MonarchVersionHistoryPanelTokens
    // The system back gesture returns to the list, as in a navigation stack.
    BackHandler(onBack = onBack)
    var view by rememberSaveable(version.id) { mutableIntStateOf(0) }
    var restoreError by remember(version.id) { mutableStateOf<String?>(null) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.SmallGap)) {
        VersionHistoryPanelHeader(
            title = tokens.relativeTime(version.date, context.now, context.locale),
            onBack = onBack,
            context = context,
        )
        VersionHistoryPanelByline(version, context)
        val views = listOf(
            monarchString(labels.changes, R.string.monarch_versionhistorypanel_changes),
            monarchString(labels.fullText, R.string.monarch_versionhistorypanel_full_text),
        )
        MonarchSegmentedControl(
            options = views,
            selectedIndex = view,
            onSelect = { view = it },
            modifier = Modifier.padding(horizontal = tokens.Inset),
            fillWidth = true,
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = tokens.Inset)) {
            when {
                selectedLines == null -> VersionHistoryPanelLines(labels)
                view == 0 -> VersionHistoryPanelChanges(selectedLines, currentLines, context)
                else -> VersionHistoryPanelFullText(selectedLines)
            }
        }
        restoreError?.let { error ->
            Text(
                text = error,
                modifier = Modifier.padding(horizontal = tokens.Inset),
                style = MonarchTheme.typography.caption,
                color = Palette.errorColor(MonarchTheme.colors),
            )
        }
        if (onRestore != null && !current) {
            VersionHistoryPanelRestore(
                id = version.id,
                onRestore = onRestore,
                onDone = onBack,
                onError = { restoreError = it },
                announce = announce,
                labels = labels,
            )
        }
    }
}

/** The author, the name and the full date, read as one element. */
@Composable
private fun VersionHistoryPanelByline(version: MonarchVersionHistoryPanelVersion, context: VersionHistoryPanelContext) {
    val labels = context.labels
    val tokens = MonarchVersionHistoryPanelTokens
    val colors = MonarchTheme.colors
    val name = tokens.name(version, monarchString(labels.automatic, R.string.monarch_versionhistorypanel_automatic))
    val date = tokens.date(version.date, context.locale, context.timeZone)
    Row(
        modifier = Modifier
            .padding(horizontal = tokens.Inset)
            .clearAndSetSemantics { contentDescription = tokens.spoken(listOf(version.authorName, name, date)) },
        horizontalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchAvatar(
            image = version.authorImage,
            name = version.authorName,
            size = MonarchAvatarSize.Sm,
            decorative = true,
        )
        Column {
            Text(
                text = listOfNotNull(version.authorName, name).joinToString(" · "),
                style = MonarchTheme.typography.body,
                color = Palette.titleColor(colors),
            )
            Text(text = date, style = MonarchTheme.typography.caption, color = Palette.mutedColor(colors))
        }
    }
}

/** Placeholder lines while the version's content loads, read once as "Loading". */
@Composable
private fun VersionHistoryPanelLines(labels: MonarchVersionHistoryPanelLabels) {
    val tokens = MonarchVersionHistoryPanelTokens
    val loading = monarchString(labels.loading, R.string.monarch_versionhistorypanel_loading)
    Column(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(tokens.PlaceholderGap),
    ) {
        tokens.PlaceholderLines.forEach { fraction ->
            MonarchSkeleton(modifier = Modifier.fillMaxWidth(fraction), shape = MonarchSkeletonShape.Text)
        }
    }
}

/** The diff from the version to now, or a sentence when they are the same. */
@Composable
private fun VersionHistoryPanelChanges(
    selectedLines: List<String>,
    currentLines: List<String>,
    context: VersionHistoryPanelContext,
) {
    val labels = context.labels
    val tokens = MonarchVersionHistoryPanelTokens
    if (tokens.identical(selectedLines, currentLines)) {
        Text(
            text = monarchString(labels.identical, R.string.monarch_versionhistorypanel_identical),
            style = MonarchTheme.typography.body,
            color = Palette.mutedColor(MonarchTheme.colors),
        )
    } else {
        MonarchDiffViewer(
            before = selectedLines,
            after = currentLines,
            modifier = Modifier.fillMaxSize(),
            granularity = MonarchDiffViewerGranularity.Word,
            afterLabel = monarchString(labels.now, R.string.monarch_versionhistorypanel_now),
            showModeSwitch = false,
            labels = labels.diff,
        )
    }
}

/** The version's paragraphs, selectable. */
@Composable
private fun VersionHistoryPanelFullText(lines: List<String>) {
    val tokens = MonarchVersionHistoryPanelTokens
    SelectionContainer {
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(tokens.SmallGap)) {
            itemsIndexed(lines) { _, line ->
                Text(text = line, style = MonarchTheme.typography.body, color = Palette.titleColor(MonarchTheme.colors))
            }
        }
    }
}

/** Restore this version, behind a confirmation; progress while it runs. */
@Composable
private fun VersionHistoryPanelRestore(
    id: String,
    onRestore: suspend (String) -> Unit,
    onDone: () -> Unit,
    onError: (String?) -> Unit,
    announce: (String) -> Unit,
    labels: MonarchVersionHistoryPanelLabels,
) {
    val tokens = MonarchVersionHistoryPanelTokens
    var confirming by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val restored = monarchString(labels.restored, R.string.monarch_versionhistorypanel_restored)
    val failed = monarchString(labels.restoreFailed, R.string.monarch_versionhistorypanel_restore_failed)
    MonarchButton(
        text = monarchString(labels.restore, R.string.monarch_versionhistorypanel_restore),
        onClick = { if (!restoring) confirming = true },
        modifier = Modifier.padding(horizontal = tokens.Inset).padding(bottom = tokens.SmallGap).fillMaxWidth(),
        variant = MonarchButtonVariant.Primary,
        loading = restoring,
    )
    if (confirming) {
        MonarchDialog(
            onDismissRequest = { confirming = false },
            title = monarchString(labels.restoreTitle, R.string.monarch_versionhistorypanel_restore_title),
            description = monarchString(
                labels.restoreDescription,
                R.string.monarch_versionhistorypanel_restore_description,
            ),
            actions = {
                MonarchButton(
                    text = monarchString(labels.cancel, R.string.monarch_versionhistorypanel_cancel),
                    onClick = { confirming = false },
                    variant = MonarchButtonVariant.Ghost,
                )
                MonarchButton(
                    text = monarchString(labels.restoreConfirm, R.string.monarch_versionhistorypanel_restore_confirm),
                    onClick = {
                        confirming = false
                        restoring = true
                        onError(null)
                        scope.launch {
                            val outcome = runCatching { onRestore(id) }
                            restoring = false
                            val thrown = outcome.exceptionOrNull()
                            if (thrown == null) {
                                announce(restored)
                                onDone()
                            } else {
                                val text = tokens.failureText(thrown, failed)
                                onError(text)
                                announce(text)
                            }
                        }
                    },
                    variant = MonarchButtonVariant.Primary,
                )
            },
        )
    }
}

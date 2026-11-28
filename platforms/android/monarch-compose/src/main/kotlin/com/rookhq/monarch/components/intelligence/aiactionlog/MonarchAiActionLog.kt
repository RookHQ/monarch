package com.rookhq.monarch.components.intelligence.aiactionlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.internal.MonarchAnnouncerNode
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchAnnouncer
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * An audit list of what AI agents did: the agent, marked as an AI agent so it
 * is never taken for a person, what it did and to what, when, and Undo for
 * actions that can be reversed.
 *
 * A product pattern composed from Monarch parts: each entry is a row with a
 * decorative `MonarchAiAgentAvatar`, an "AI agent" `MonarchBadge`, the action
 * and its target (a link for an http or https URL), the relative time and a
 * ghost `MonarchButton` for Undo. Entries are a `Column`, not a lazy list, so
 * the log can sit in a screen or a panel that scrolls; long logs show
 * [pageSize] entries and a Show more button. Each entry is one TalkBack element
 * read as "Research agent (AI agent) added a summary to Quarterly report" with
 * the time, "Undone" or a failure as its state, and the target link as a named
 * action. A successful or failed undo is announced through a polite live
 * region that is always present.
 *
 * @param entries the actions, in the order to show them, usually newest first.
 * @param modifier the modifier of the log.
 * @param now the reference time for relative times, in epoch milliseconds. `null` follows the clock, refreshed
 * every minute.
 * @param pageSize how many entries show at first and how many Show more adds; at least one.
 * @param locale the locale of the relative times. `null` uses the first locale of the configuration.
 * @param onUndo reverses an action, given its entry id. Shows Undo on entries that are undoable. Throw to show
 * and announce the failure; Undo stays.
 * @param onOpenTarget opens an entry's target. `null` opens a safe URL through the system.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchAiActionLog(
    entries: List<MonarchAiActionLogEntry>,
    modifier: Modifier = Modifier,
    now: Long? = null,
    pageSize: Int = 200,
    locale: Locale? = null,
    onUndo: (suspend (id: String) -> Unit)? = null,
    onOpenTarget: ((MonarchAiActionLogEntry) -> Unit)? = null,
    labels: MonarchAiActionLogLabels = MonarchAiActionLogLabels(),
) {
    val tokens = MonarchAiActionLogTokens
    val step = tokens.step(pageSize)
    var shown by rememberSaveable(step) { mutableIntStateOf(step) }
    val undo = remember { mutableStateMapOf<String, MonarchAiActionLogUndo>() }
    val announcer = rememberMonarchAnnouncer()
    val reference = rememberMonarchRelativeClock(entries.map { it.date }, fixed = now)
    val writeIn = locale ?: LocalConfiguration.current.locales[0]
    val uriHandler = LocalUriHandler.current
    val name = monarchString(labels.label, R.string.monarch_aiactionlog_label)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        if (entries.isEmpty()) {
            MonarchEmptyState(
                title = monarchString(labels.empty, R.string.monarch_aiactionlog_empty),
                size = MonarchEmptyStateSize.Sm,
            )
        } else {
            Column(
                modifier = Modifier.semantics {
                    isTraversalGroup = true
                    contentDescription = name
                },
            ) {
                entries.take(shown).forEachIndexed { index, entry ->
                    if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
                    AiActionLogItem(
                        entry = entry,
                        undo = undo,
                        onUndo = onUndo,
                        time = tokens.relativeTime(entry.date, reference, writeIn),
                        labels = labels,
                        announce = announcer::announce,
                        open = if (onOpenTarget != null && !entry.url.isNullOrBlank()) {
                            { onOpenTarget(entry) }
                        } else {
                            tokens.safeUrl(entry.url)?.let { url -> { uriHandler.openUri(url) } }
                        },
                    )
                }
            }
        }
        val hidden = entries.size - shown
        if (hidden > 0) {
            val adds = minOf(step, hidden)
            MonarchButton(
                text = labels.showMore?.invoke(adds)
                    ?: pluralStringResource(R.plurals.monarch_aiactionlog_show_more, adds, adds),
                onClick = { shown += step },
                size = MonarchButtonSize.Sm,
            )
        }
        // Always present, so an undo and its failure are announced as a change of this region.
        MonarchAnnouncerNode(announcer)
    }
}

/** One entry with its undo run in [undo] and its announcements sent to [announce]. */
@Composable
private fun AiActionLogItem(
    entry: MonarchAiActionLogEntry,
    undo: MutableMap<String, MonarchAiActionLogUndo>,
    onUndo: (suspend (id: String) -> Unit)?,
    time: String,
    labels: MonarchAiActionLogLabels,
    announce: (String) -> Unit,
    open: (() -> Unit)?,
) {
    val tokens = MonarchAiActionLogTokens
    val scope = rememberCoroutineScope()
    val state = undo[entry.id]
    val description = tokens.description(entry)
    val done = labels.undoneAnnouncement?.invoke(description)
        ?: stringResource(R.string.monarch_aiactionlog_undone_announcement, description)
    val failed = labels.undoFailed?.invoke(description)
        ?: stringResource(R.string.monarch_aiactionlog_undo_failed, description)
    AiActionLogEntryRow(
        entry = entry,
        state = state,
        canUndo = tokens.canUndo(entry, onUndo != null, state),
        time = time,
        failure = failed,
        labels = labels,
        onUndo = {
            if (onUndo != null && state != MonarchAiActionLogUndo.Pending) {
                undo[entry.id] = MonarchAiActionLogUndo.Pending
                announce("")
                scope.launch {
                    val succeeded = runCatching { onUndo(entry.id) }.isSuccess
                    undo[entry.id] = if (succeeded) MonarchAiActionLogUndo.Done else MonarchAiActionLogUndo.Failed
                    announce(if (succeeded) done else failed)
                }
            }
        },
        onOpen = open,
    )
}

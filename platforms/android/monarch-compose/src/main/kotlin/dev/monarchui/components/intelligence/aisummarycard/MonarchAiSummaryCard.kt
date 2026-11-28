package dev.monarchui.components.intelligence.aisummarycard

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.feedback.skeleton.MonarchSkeletonShape
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadge
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeSize
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdown
import dev.monarchui.components.intelligence.aisourcelist.MonarchAiSourceList
import dev.monarchui.components.intelligence.aisourcelist.MonarchAiSourceListItem
import dev.monarchui.components.intelligence.aisourcelist.MonarchAiSourceListLabels
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A model-written summary of a record or a page on a card: the summary as
 * Markdown, when it was written, its sources and a Refresh action, with
 * generating, stale and error states. It is always marked as an AI summary,
 * visibly and for TalkBack.
 *
 * A product pattern composed from Monarch parts: `MonarchCard`,
 * `MonarchAiGeneratedBadge`, `MonarchAiMarkdown`, `MonarchAiSourceList`,
 * `MonarchBanner` and `MonarchButton`. The title is a heading; Refresh shows a
 * spinner while busy; the stale warning and the error are banners, the error
 * announced assertively; "Generating summary…" and "Summary updated." are
 * announced politely when the work starts and ends, never when the card first
 * appears. The age is relative ("Generated 3 hours ago") and TalkBack reads
 * the date and time.
 *
 * @param title what is summarised, for example "Account summary".
 * @param summary the summary as Markdown, treated as untrusted model output. Blank while there is none.
 * @param modifier the modifier of the card.
 * @param status ready, generating, stale or error.
 * @param refreshing a refresh is running: Refresh shows a spinner.
 * @param generatedAt when the summary was written, in epoch milliseconds (UTC).
 * @param now the reference time for [generatedAt], in epoch milliseconds. `null` uses the current time.
 * @param sources the documents or records the summary was written from, listed by `MonarchAiSourceList`.
 * @param error a detail shown under the error title when [status] is `Error`.
 * @param onRefresh adds Refresh, which asks for a new summary.
 * @param onOpenSource opens a source; without it a source with a safe URL opens through the system.
 * @param onOpenLink opens a link in the summary; without it links open through the system.
 * @param icon the AI mark before "AI summary", for example a `MonarchIcon` with a `null` content description.
 * @param refreshIcon an icon before "Refresh". Monarch bundles no icons.
 * @param actions more actions after the age line, for example a Copy button.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchAiSummaryCard(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    status: MonarchAiSummaryCardStatus = MonarchAiSummaryCardStatus.Ready,
    refreshing: Boolean = false,
    generatedAt: Long? = null,
    now: Long? = null,
    sources: List<MonarchAiSourcePart> = emptyList(),
    error: String? = null,
    onRefresh: (() -> Unit)? = null,
    onOpenSource: ((MonarchAiSourcePart) -> Unit)? = null,
    onOpenLink: ((url: String) -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    refreshIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Retry,
    actions: (@Composable FlowRowScope.() -> Unit)? = null,
    labels: MonarchAiSummaryCardLabels = MonarchAiSummaryCardLabels(),
) {
    val tokens = MonarchAiSummaryCardTokens
    val busy = tokens.busy(status, refreshing)
    val generating = monarchString(labels.generating, R.string.monarch_aisummarycard_generating)
    val updated = monarchString(labels.updated, R.string.monarch_aisummarycard_updated)
    // Compared with the previous composition, so nothing is announced when the card first appears.
    val memory = remember { AnnouncementMemory(busy) }
    tokens.announcement(memory.wasBusy, busy, status, generating, updated)?.let { memory.announcement = it }
    memory.wasBusy = busy
    val announcement = memory.announcement
    MonarchCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
            Header(title, busy, onRefresh, icon, refreshIcon, labels)
            Notices(status, error, labels)
            Body(summary, busy, status, onOpenLink, generating, labels)
            if (sources.isNotEmpty()) {
                MonarchAiSourceList(
                    items = sources.map { MonarchAiSourceListItem(it) },
                    labels = MonarchAiSourceListLabels(
                        heading = monarchString(labels.sources, R.string.monarch_aisummarycard_sources),
                    ),
                    onOpen = onOpenSource?.let { open -> { item: MonarchAiSourceListItem -> open(item.source) } },
                )
            }
            Footer(generatedAt, now, actions, labels)
        }
        // Always present, so TalkBack announces the start and the end of the work.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** Whether the card was busy at the last composition, and what was last announced. */
private class AnnouncementMemory(var wasBusy: Boolean, var announcement: String = "")

@Composable
private fun Header(
    title: String,
    busy: Boolean,
    onRefresh: (() -> Unit)?,
    icon: (@Composable () -> Unit)?,
    refreshIcon: (@Composable () -> Unit)?,
    labels: MonarchAiSummaryCardLabels,
) {
    val tokens = MonarchAiSummaryCardTokens
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TitleGap)) {
            MonarchAiGeneratedBadge(
                size = MonarchAiGeneratedBadgeSize.Sm,
                icon = icon,
                label = monarchString(labels.aiSummary, R.string.monarch_aisummarycard_ai_summary),
            )
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.subheading,
                color = tokens.titleColor(MonarchTheme.colors),
            )
        }
        if (onRefresh != null) {
            MonarchButton(
                text = monarchString(labels.refresh, R.string.monarch_aisummarycard_refresh),
                onClick = onRefresh,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                loading = busy,
                leadingIcon = refreshIcon,
            )
        }
    }
}

@Composable
private fun Notices(status: MonarchAiSummaryCardStatus, error: String?, labels: MonarchAiSummaryCardLabels) {
    when (status) {
        MonarchAiSummaryCardStatus.Stale -> MonarchBanner(
            message = monarchString(labels.stale, R.string.monarch_aisummarycard_stale),
            tone = MonarchTone.Warning,
        )
        MonarchAiSummaryCardStatus.Error -> {
            val title = monarchString(labels.error, R.string.monarch_aisummarycard_error)
            MonarchBanner(
                message = error ?: title,
                tone = MonarchTone.Danger,
                title = if (error != null) title else null,
                announcement = MonarchBannerAnnouncement.Assertive,
            )
        }
        else -> Unit
    }
}

@Composable
private fun Body(
    summary: String,
    busy: Boolean,
    status: MonarchAiSummaryCardStatus,
    onOpenLink: ((String) -> Unit)?,
    generating: String,
    labels: MonarchAiSummaryCardLabels,
) {
    val tokens = MonarchAiSummaryCardTokens
    val muted = tokens.mutedColor(MonarchTheme.colors)
    when (tokens.body(summary, busy, status)) {
        MonarchAiSummaryCardBody.Summary -> MonarchAiMarkdown(text = summary, onOpenLink = onOpenLink)
        MonarchAiSummaryCardBody.Placeholder ->
            Column(verticalArrangement = Arrangement.spacedBy(tokens.PlaceholderGap)) {
                Text(text = generating, style = MonarchTheme.typography.body, color = muted)
                tokens.PlaceholderLines.forEach { fraction ->
                    MonarchSkeleton(modifier = Modifier.fillMaxWidth(fraction), shape = MonarchSkeletonShape.Text)
                }
            }
        MonarchAiSummaryCardBody.Nothing -> Unit
        MonarchAiSummaryCardBody.Empty -> Text(
            text = monarchString(labels.empty, R.string.monarch_aisummarycard_empty),
            style = MonarchTheme.typography.body,
            color = muted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Footer(
    generatedAt: Long?,
    now: Long?,
    actions: (@Composable FlowRowScope.() -> Unit)?,
    labels: MonarchAiSummaryCardLabels,
) {
    if (generatedAt == null && actions == null) return
    val tokens = MonarchAiSummaryCardTokens
    val context = LocalContext.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
        verticalArrangement = Arrangement.spacedBy(tokens.TitleGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (generatedAt != null) {
            val relative = MonarchRelativeTime.format(
                generatedAt,
                now ?: System.currentTimeMillis(),
                LocalConfiguration.current.locales[0],
            )
            val absolute = DateUtils.formatDateTime(
                context,
                generatedAt,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR,
            )
            val line = labels.generated?.invoke(relative)
                ?: stringResource(R.string.monarch_aisummarycard_generated, relative)
            val spoken = labels.generated?.invoke(absolute)
                ?: stringResource(R.string.monarch_aisummarycard_generated, absolute)
            Text(
                text = line,
                modifier = Modifier.semantics { contentDescription = spoken },
                style = MonarchTheme.typography.caption,
                color = tokens.mutedColor(MonarchTheme.colors),
            )
        }
        actions?.invoke(this)
    }
}

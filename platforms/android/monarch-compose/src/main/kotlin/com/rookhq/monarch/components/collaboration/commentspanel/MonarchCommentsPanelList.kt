package com.rookhq.monarch.components.collaboration.commentspanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.listrow.MonarchListRow
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.theme.MonarchTheme

/** The most lines of a quote in the list before it is cut. */
private const val QuoteLines = 2

/** The threads of the current filter, one `MonarchListRow` button each, in a `LazyColumn`. */
@Composable
internal fun MonarchCommentsPanelList(
    threads: List<MonarchCommentsPanelThread>,
    context: MonarchCommentsPanelContext,
    focusRow: String?,
    onFocused: () -> Unit,
    onOpen: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(threads, key = { _, thread -> thread.id }) { index, thread ->
            if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
            MonarchCommentsPanelRow(thread, context, focusRow == thread.id, onFocused, onOpen)
        }
    }
}

/** One thread: the starter, the quote, the number of comments and the last activity, read as one button. */
@Composable
private fun MonarchCommentsPanelRow(
    thread: MonarchCommentsPanelThread,
    context: MonarchCommentsPanelContext,
    focus: Boolean,
    onFocused: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val tokens = MonarchCommentsPanelTokens
    val colors = MonarchTheme.colors
    val labels = context.labels
    val requester = remember { FocusRequester() }
    LaunchedEffect(focus) {
        if (focus) {
            withFrameNanos { }
            requester.requestFocus()
            onFocused()
        }
    }
    val starter = thread.comments.firstOrNull()
    val count = thread.comments.size
    val countText = labels.commentCount?.invoke(count)
        ?: pluralStringResource(R.plurals.monarch_commentspanel_comment_count, count, count)
    val time = tokens.lastActivity(thread)?.let { tokens.relativeTime(it, context.now, context.locale) }
    val quote = thread.quote?.let {
        labels.quote?.invoke(it) ?: stringResource(R.string.monarch_commentspanel_quote, it)
    }
    MonarchListRow(
        headlineContent = {
            Text(
                text = starter?.author.orEmpty(),
                style = MonarchTheme.typography.bodyStrong,
                color = tokens.titleColor(colors),
            )
        },
        modifier = Modifier.focusRequester(requester),
        onClick = { onOpen(thread.id) },
        supportingContent = {
            // The quote and the line under it share one column under the starter.
            Column(verticalArrangement = Arrangement.spacedBy(tokens.SmallGap)) {
                if (quote != null) {
                    Text(
                        text = quote,
                        style = MonarchTheme.typography.body,
                        color = tokens.mutedColor(colors),
                        maxLines = QuoteLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = tokens.meta(countText, time),
                    style = MonarchTheme.typography.caption,
                    color = tokens.mutedColor(colors),
                )
            }
        },
        leadingContent = starter?.let { comment ->
            {
                MonarchAvatar(
                    image = comment.authorImage,
                    name = comment.author,
                    size = MonarchAvatarSize.Sm,
                    seed = comment.authorSeed,
                    decorative = true,
                )
            }
        },
    )
}

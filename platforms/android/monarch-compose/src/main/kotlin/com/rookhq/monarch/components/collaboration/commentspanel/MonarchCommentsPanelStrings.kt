package com.rookhq.monarch.components.collaboration.commentspanel

import androidx.compose.runtime.Composable
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.monarchString

/** The built-in strings of MonarchCommentsPanel that are plain text, resolved once. */
@Suppress("LongParameterList")
internal class MonarchCommentsPanelStrings(
    val title: String,
    val close: String,
    val open: String,
    val resolved: String,
    val back: String,
    val loading: String,
    val emptyOpen: String,
    val emptyResolved: String,
    val emptyHint: String,
    val resolvedNotice: String,
    val reopenedNotice: String,
)

/** Resolves the strings of a MonarchCommentsPanel: each override, or its built-in string. */
@Composable
internal fun monarchCommentsPanelStrings(labels: MonarchCommentsPanelLabels): MonarchCommentsPanelStrings =
    MonarchCommentsPanelStrings(
        title = monarchString(labels.title, R.string.monarch_commentspanel_title),
        close = monarchString(labels.close, R.string.monarch_commentspanel_close),
        open = monarchString(labels.open, R.string.monarch_commentspanel_open),
        resolved = monarchString(labels.resolved, R.string.monarch_commentspanel_resolved),
        back = monarchString(labels.back, R.string.monarch_commentspanel_back),
        loading = monarchString(labels.loading, R.string.monarch_commentspanel_loading),
        emptyOpen = monarchString(labels.emptyOpen, R.string.monarch_commentspanel_empty_open),
        emptyResolved = monarchString(labels.emptyResolved, R.string.monarch_commentspanel_empty_resolved),
        emptyHint = monarchString(labels.emptyHint, R.string.monarch_commentspanel_empty_hint),
        resolvedNotice = monarchString(labels.resolvedNotice, R.string.monarch_commentspanel_resolved_notice),
        reopenedNotice = monarchString(labels.reopenedNotice, R.string.monarch_commentspanel_reopened_notice),
    )

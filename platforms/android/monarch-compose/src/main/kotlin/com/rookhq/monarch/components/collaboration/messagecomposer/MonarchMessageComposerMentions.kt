package com.rookhq.monarch.components.collaboration.messagecomposer

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionItem
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionPickerStatus
import com.rookhq.monarch.components.collaboration.mentionpicker.MonarchMentionQuery

/**
 * Mention suggestions for a `MonarchMessageComposer`. The composer finds the
 * mention being typed and reports it; the app searches and passes the
 * results back. Picking a suggestion writes @Label and a space.
 *
 * @property suggestions the results for the last query.
 * @property status loading, ready or failed.
 * @property triggers the characters that start a mention; add `#` for channels.
 * @property onRetry repeats a failed search.
 * @property onQueryChange receives the mention being typed, or `null` when there is none.
 */
@Immutable
public class MonarchMessageComposerMentions(
    public val suggestions: List<MonarchMentionItem>,
    public val status: MonarchMentionPickerStatus = MonarchMentionPickerStatus.Ready,
    public val triggers: Set<Char> = setOf('@'),
    public val onRetry: (() -> Unit)? = null,
    public val onQueryChange: (MonarchMentionQuery?) -> Unit,
)

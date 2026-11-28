package com.rookhq.monarch.components.intelligence.aidraftqueue

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiDraftQueue`. Every
 * `null` keeps the built-in string `monarch_aidraftqueue_<key>`.
 *
 * @property label the TalkBack name of the queue when it has no title, "Drafts to review".
 * @property progress the progress line, from the number reviewed and the number of drafts: "2 of 5 reviewed".
 * @property empty shown when there are no drafts, "No drafts to review".
 * @property status the name of a status, shown in its badge: "Needs review", "Writing", "Approved", "Sent",
 * "Skipped".
 * @property writing shown instead of the text while a draft is written again, "AI is still writing…".
 * @property aiDraft read by TalkBack before the text of each draft, "AI draft".
 * @property context the heading of what the draft answers, "Context".
 * @property approve the approve button.
 * @property send the send button, shown on approved drafts when the queue has `onSend`.
 * @property edit the edit button.
 * @property regenerate the button that asks for a new version.
 * @property skip the skip button.
 * @property save the button that saves an edit.
 * @property cancel the button that closes the editor without saving.
 * @property editField the label of the editor, from the draft's title: "Edit draft: Reply to Alex".
 * @property announce announced when a draft's status changes, from the new status and the draft's title:
 * "Approved: Reply to Alex", or "New draft ready: Reply to Alex" when it needs review again.
 */
@Immutable
public class MonarchAiDraftQueueLabels(
    public val label: String? = null,
    public val progress: ((reviewed: Int, total: Int) -> String)? = null,
    public val empty: String? = null,
    public val status: ((status: MonarchAiDraftStatus) -> String)? = null,
    public val writing: String? = null,
    public val aiDraft: String? = null,
    public val context: String? = null,
    public val approve: String? = null,
    public val send: String? = null,
    public val edit: String? = null,
    public val regenerate: String? = null,
    public val skip: String? = null,
    public val save: String? = null,
    public val cancel: String? = null,
    public val editField: ((title: String) -> String)? = null,
    public val announce: ((status: MonarchAiDraftStatus, title: String) -> String)? = null,
)

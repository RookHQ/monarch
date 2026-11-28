package dev.monarchui.components.files.versionhistorypanel

import androidx.compose.runtime.Immutable
import dev.monarchui.components.files.diffviewer.MonarchDiffViewerLabels

/**
 * Replacements for the strings `MonarchVersionHistoryPanel` supplies. Each
 * `null` keeps the built-in English string `monarch_versionhistorypanel_<key>`.
 *
 * @property close the close button. Default "Close".
 * @property back the TalkBack name of the button that returns to the list. Default "Back to versions".
 * @property versions the visible text of that button. Default "Versions".
 * @property noteLabel the label of the name field. Default "Version name".
 * @property notePlaceholder the placeholder of the name field. Default "Name this version (optional)".
 * @property save the save button. Default "Save version".
 * @property saved announced after a save. Default "Version saved".
 * @property saveFailed shown when a save fails without a message. Default "The version could not be saved. Try
 * again.".
 * @property empty shown when there are no versions. Default "No versions yet. Save one to mark this moment.".
 * @property current the badge and state of the version that matches the content now. Default "Current".
 * @property automatic the name of a version saved automatically. Default "Saved automatically".
 * @property changes the view of the diff. Default "Changes".
 * @property fullText the view of the version's text. Default "Full text".
 * @property now the name of the content now in the diff. Default "Now".
 * @property identical shown when the version matches the content now. Default "This version is identical to the
 * current one.".
 * @property restore the restore button. Default "Restore this version".
 * @property restoreTitle the title of the confirmation. Default "Restore this version?".
 * @property restoreDescription the text of the confirmation. Default "The current content is replaced with this
 * version.".
 * @property restoreConfirm the confirming button. Default "Restore".
 * @property cancel the cancelling button. Default "Cancel".
 * @property restored announced after a restore. Default "Version restored".
 * @property restoreFailed shown when a restore fails without a message. Default "The version could not be
 * restored. Try again.".
 * @property loading read for the placeholders. Default "Loading".
 * @property diff the strings of the diff viewer.
 */
@Immutable
public class MonarchVersionHistoryPanelLabels(
    public val close: String? = null,
    public val back: String? = null,
    public val versions: String? = null,
    public val noteLabel: String? = null,
    public val notePlaceholder: String? = null,
    public val save: String? = null,
    public val saved: String? = null,
    public val saveFailed: String? = null,
    public val empty: String? = null,
    public val current: String? = null,
    public val automatic: String? = null,
    public val changes: String? = null,
    public val fullText: String? = null,
    public val now: String? = null,
    public val identical: String? = null,
    public val restore: String? = null,
    public val restoreTitle: String? = null,
    public val restoreDescription: String? = null,
    public val restoreConfirm: String? = null,
    public val cancel: String? = null,
    public val restored: String? = null,
    public val restoreFailed: String? = null,
    public val loading: String? = null,
    public val diff: MonarchDiffViewerLabels = MonarchDiffViewerLabels(),
) {
    private val all: List<Any?>
        get() = listOf(
            close, back, versions, noteLabel, notePlaceholder, save, saved, saveFailed, empty, current, automatic,
            changes, fullText, now, identical, restore, restoreTitle, restoreDescription, restoreConfirm, cancel,
            restored, restoreFailed, loading, diff,
        )

    override fun equals(other: Any?): Boolean = other is MonarchVersionHistoryPanelLabels && other.all == all

    override fun hashCode(): Int = all.hashCode()
}

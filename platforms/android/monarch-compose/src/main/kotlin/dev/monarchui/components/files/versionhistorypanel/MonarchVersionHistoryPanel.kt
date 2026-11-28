package dev.monarchui.components.files.versionhistorypanel

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import dev.monarchui.internal.MonarchAnnouncerNode
import dev.monarchui.internal.monarchTimeZone
import dev.monarchui.internal.rememberMonarchAnnouncer
import dev.monarchui.internal.rememberMonarchRelativeClock
import java.util.Locale
import java.util.TimeZone

/**
 * The version history of a document or a record: the saved versions, what
 * changed between a version and now, its full text, and restore behind a
 * confirmation.
 *
 * Built by Monarch as the phone's list and detail pattern, for a full-height
 * Material 3 `ModalBottomSheet` or a pane beside the document: the list is a
 * `LazyColumn` of `MonarchListRow` rows (avatar, relative time, author
 * and name, a Current badge) under an optional name field and Save version
 * button; opening a version shows its screen, where the system back gesture
 * and the Versions button return to the list. The version's screen has a
 * `MonarchSegmentedControl` for Changes (a `MonarchDiffViewer` from the
 * version to now) and Full text (selectable paragraphs), and Restore this
 * version behind a `MonarchDialog`. Saving and restoring are
 * suspending functions: the button shows progress, a thrown error is shown
 * and announced, and "Version saved" and "Version restored" are announced
 * through a polite live region that is always present. Give the panel a
 * bounded height, as for any lazy list.
 *
 * @param versions the saved versions, newest first.
 * @param selection the id of the version on screen, or `null` for the list.
 * @param onSelectionChange called with the version to open, or `null` to go back to the list. Load the
 * version's lines here.
 * @param currentLines the content now, one entry per block or paragraph.
 * @param modifier the modifier of the panel.
 * @param title the title of the list. `null` uses "History".
 * @param selectedLines the selected version's content. Placeholders show while it is `null`.
 * @param currentId the version that matches the content now, marked Current and without Restore. `null` marks
 * the newest.
 * @param loading shows placeholder rows while the list loads.
 * @param now the reference time for relative times, in epoch milliseconds. `null` follows the clock, refreshed
 * every minute.
 * @param locale the locale of times and dates. `null` uses the first locale of the configuration.
 * @param timeZone the zone of the date on a version's screen. An IANA id such as `"Europe/Berlin"`. `null`, or an id
 * the device does not know, is the device's zone.
 * @param onSaveVersion shows a name field and Save version above the list. Receives the trimmed name, at most
 * 200 characters. Throw to show the error.
 * @param onRestore shows Restore this version on versions other than the current one, behind a confirmation.
 * Throw to keep the screen with the error; on success the panel returns to the list.
 * @param onClose shows a Close button.
 * @param labels replacements for the built-in strings and the diff's strings.
 * @param closeIcon an icon (a close mark) that replaces the text of the Close button, as on the web. The button
 * keeps its spoken name "Close". `null` keeps the text button.
 */
@Composable
public fun MonarchVersionHistoryPanel(
    versions: List<MonarchVersionHistoryPanelVersion>,
    selection: String?,
    onSelectionChange: (String?) -> Unit,
    currentLines: List<String>,
    modifier: Modifier = Modifier,
    title: String? = null,
    selectedLines: List<String>? = null,
    currentId: String? = null,
    loading: Boolean = false,
    now: Long? = null,
    locale: Locale? = null,
    timeZone: String? = null,
    onSaveVersion: (suspend (note: String) -> Unit)? = null,
    onRestore: (suspend (id: String) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    labels: MonarchVersionHistoryPanelLabels = MonarchVersionHistoryPanelLabels(),
    closeIcon: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchVersionHistoryPanelTokens
    val context = VersionHistoryPanelContext(
        now = rememberMonarchRelativeClock(versions.map { it.date }, fixed = now),
        locale = locale ?: LocalConfiguration.current.locales[0],
        timeZone = monarchTimeZone(timeZone),
        labels = labels,
        onClose = onClose,
        closeIcon = closeIcon,
    )
    val announcer = rememberMonarchAnnouncer()
    val announce: (String) -> Unit = announcer::announce
    val current = tokens.currentId(currentId, versions)
    val selected = tokens.selected(selection, versions)
    Column(modifier = modifier) {
        if (selected == null) {
            VersionHistoryPanelList(
                title = title,
                versions = versions,
                currentId = current,
                loading = loading,
                onOpen = onSelectionChange,
                onSaveVersion = onSaveVersion,
                announce = announce,
                context = context,
                modifier = Modifier.weight(1f),
            )
        } else {
            VersionHistoryPanelDetail(
                version = selected,
                current = selected.id == current,
                currentLines = currentLines,
                selectedLines = selectedLines,
                onBack = { onSelectionChange(null) },
                onRestore = onRestore,
                announce = announce,
                context = context,
                modifier = Modifier.weight(1f),
            )
        }
        // Always present, so a save, a restore and their failures are announced as a change of this region.
        MonarchAnnouncerNode(announcer)
    }
}

/** What both screens of the panel share. */
internal class VersionHistoryPanelContext(
    val now: Long,
    val locale: Locale,
    val timeZone: TimeZone,
    val labels: MonarchVersionHistoryPanelLabels,
    val onClose: (() -> Unit)?,
    val closeIcon: (@Composable () -> Unit)? = null,
)

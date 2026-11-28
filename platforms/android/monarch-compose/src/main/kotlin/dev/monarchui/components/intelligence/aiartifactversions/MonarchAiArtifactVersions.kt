package dev.monarchui.components.intelligence.aiartifactversions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.tag.MonarchChip
import dev.monarchui.components.files.diffviewer.MonarchDiffViewer
import dev.monarchui.components.files.diffviewer.MonarchDiffViewerGranularity
import dev.monarchui.components.intelligence.aimessagebranch.MonarchAiMessageBranch
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchTimeZone
import java.util.Locale

/**
 * Steps through the versions of an AI artifact: previous and next buttons,
 * the position ("Version 3 of 5"), a menu to jump to any version, and Compare
 * with previous, which shows what changed in a `MonarchDiffViewer`.
 *
 * Composed from Monarch parts: `MonarchAiMessageBranch` for previous,
 * position and next; a `MonarchButton` that opens a `MonarchMenu` of every
 * version (the chosen one is selected and checked); a `MonarchChip` for
 * Compare, read as selected or not; and
 * `MonarchDiffViewer` from the previous version to this one, at most
 * 384dp tall so the stepper can sit in a screen that scrolls. A change from
 * the buttons is announced by the branch ("Version 3 of 5"), a change from the
 * menu by a polite live region ("Showing version 3 of 5"). With no versions
 * nothing is shown.
 *
 * @param selection the version number shown. A number not in [versions] shows the newest.
 * @param versions every version, in any order; shown oldest to newest, one per number.
 * @param onSelectionChange called with the chosen version number.
 * @param previousIcon the previous button's icon, such as a chevron that points to the start, as a `MonarchIcon`
 * with `mirrorInRtl = true` and a `null` content description. Monarch bundles no icons.
 * @param nextIcon the next button's icon, mirrored in the same way.
 * @param modifier the modifier of the stepper.
 * @param comparing whether the diff with the previous version shows, when the app owns it with
 * [onComparingChange]. `null` keeps the state in the stepper.
 * @param onComparingChange called when Compare is toggled.
 * @param granularity whether the diff marks changed words (prose) or whole lines (code).
 * @param showCompare whether Compare with previous is offered.
 * @param enabled whether the controls can be used, for example `false` while a version streams.
 * @param locale the locale of the dates in the menu. `null` uses the first locale of the configuration.
 * @param timeZone the zone of the dates in the menu. An IANA id such as `"Europe/Berlin"`. `null`, or an id the device
 * does not know, is the device's zone.
 * @param menuIcon an icon after "Choose a version", such as a drop-down arrow. Decorative.
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@ExperimentalMonarchApi
@Composable
public fun MonarchAiArtifactVersions(
    selection: Int,
    versions: List<MonarchAiArtifactVersion>,
    onSelectionChange: (Int) -> Unit,
    previousIcon: @Composable () -> Unit,
    nextIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    comparing: Boolean? = null,
    onComparingChange: ((Boolean) -> Unit)? = null,
    granularity: MonarchDiffViewerGranularity = MonarchDiffViewerGranularity.Word,
    showCompare: Boolean = true,
    enabled: Boolean = true,
    locale: Locale? = null,
    timeZone: String? = null,
    menuIcon: (@Composable () -> Unit)? = null,
    labels: MonarchAiArtifactVersionsLabels = MonarchAiArtifactVersionsLabels(),
) {
    val tokens = MonarchAiArtifactVersionsTokens
    val place = remember(versions, selection) { tokens.place(versions, selection) }
    val current = place.current ?: return
    val total = place.sorted.size
    var ownComparing by rememberSaveable { mutableStateOf(false) }
    val compareOn = comparing ?: ownComparing
    val canCompare = tokens.canCompare(place, showCompare)
    // The version the menu asked for, so its change is announced once it is shown.
    var fromMenu by remember { mutableStateOf<Int?>(null) }
    val announceText = aiArtifactVersionsAnnounce(labels, current.version, total)
    val position = aiArtifactVersionsPosition(labels, current.version, total)
    val name = monarchString(labels.label, R.string.monarch_aiartifactversions_label)
    Column(
        // The branch below carries the name "Versions"; naming this column too would read it twice.
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchAiMessageBranch(
                selection = place.index,
                count = total,
                onSelectionChange = { index ->
                    fromMenu = null
                    place.sorted.getOrNull(index)?.let { onSelectionChange(it.version) }
                },
                previousIcon = previousIcon,
                nextIcon = nextIcon,
                enabled = enabled,
                showSingle = true,
                label = name,
                previousLabel = monarchString(labels.previous, R.string.monarch_aiartifactversions_previous),
                nextLabel = monarchString(labels.next, R.string.monarch_aiartifactversions_next),
                formatPosition = { _, _ -> position },
                formatAnnouncement = { _, _ -> position },
            )
            AiArtifactVersionsMenu(
                place = place,
                enabled = enabled,
                locale = locale,
                timeZone = timeZone,
                menuIcon = menuIcon,
                labels = labels,
                onChoose = { version ->
                    if (version != current.version) {
                        fromMenu = version
                        onSelectionChange(version)
                    }
                },
            )
            if (canCompare) {
                AiArtifactVersionsCompare(compareOn, enabled, labels) { next ->
                    if (comparing == null) ownComparing = next
                    onComparingChange?.invoke(next)
                }
            }
        }
        if (canCompare && compareOn) AiArtifactVersionsDiff(place, granularity, labels)
        // Always present, so a version chosen from the menu is announced once it is shown.
        val spoken = if (fromMenu == current.version) announceText else ""
        MonarchLiveStatus(text = spoken.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** Compare with previous, a filter chip read as selected or not. */
@Composable
private fun AiArtifactVersionsCompare(
    on: Boolean,
    enabled: Boolean,
    labels: MonarchAiArtifactVersionsLabels,
    onChange: (Boolean) -> Unit,
) {
    MonarchChip(
        text = monarchString(labels.compare, R.string.monarch_aiartifactversions_compare),
        selected = on,
        onSelectedChange = onChange,
        enabled = enabled,
    )
}

/** The diff from the previous version to the one shown, held to a bounded height. */
@Composable
private fun AiArtifactVersionsDiff(
    place: AiArtifactVersionsPlace,
    granularity: MonarchDiffViewerGranularity,
    labels: MonarchAiArtifactVersionsLabels,
) {
    val previous = place.previous ?: return
    val current = place.current ?: return
    MonarchDiffViewer(
        before = previous.content.orEmpty(),
        after = current.content.orEmpty(),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = MonarchAiArtifactVersionsTokens.DiffMaxHeight),
        granularity = granularity,
        beforeLabel = versionName(labels, previous.version),
        afterLabel = versionName(labels, current.version),
        showModeSwitch = false,
    )
}

@Composable
private fun aiArtifactVersionsAnnounce(labels: MonarchAiArtifactVersionsLabels, version: Int, total: Int): String =
    labels.announce?.invoke(version, total)
        ?: stringResource(R.string.monarch_aiartifactversions_announce, version, total)

@Composable
private fun aiArtifactVersionsPosition(labels: MonarchAiArtifactVersionsLabels, version: Int, total: Int): String =
    labels.position?.invoke(version, total)
        ?: stringResource(R.string.monarch_aiartifactversions_position, version, total)

@Composable
private fun versionName(labels: MonarchAiArtifactVersionsLabels, version: Int): String =
    labels.versionName?.invoke(version) ?: stringResource(R.string.monarch_aiartifactversions_version_name, version)

/** "Choose a version" and its menu of every version, the shown one selected. */
@Composable
private fun AiArtifactVersionsMenu(
    place: AiArtifactVersionsPlace,
    enabled: Boolean,
    locale: Locale?,
    timeZone: String?,
    menuIcon: (@Composable () -> Unit)?,
    labels: MonarchAiArtifactVersionsLabels,
    onChoose: (Int) -> Unit,
) {
    val tokens = MonarchAiArtifactVersionsTokens
    var open by remember { mutableStateOf(false) }
    val current = place.current?.version
    val writeIn = locale ?: LocalConfiguration.current.locales[0]
    val zone = monarchTimeZone(timeZone)
    Box {
        val shown = place.current?.let { version ->
            val tag = labels.versionTag?.invoke(version.version)
                ?: stringResource(R.string.monarch_aiartifactversions_version_tag, version.version)
            tokens.option(tag, version.label, tokens.date(version.createdAt, writeIn, zone))
        }
        MonarchButton(
            text = labels.select ?: shown ?: stringResource(R.string.monarch_aiartifactversions_select),
            onClick = { open = true },
            variant = MonarchButtonVariant.Secondary,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
            trailingIcon = menuIcon,
        )
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
            place.sorted.forEach { version ->
                val chosen = version.version == current
                val tag = labels.versionTag?.invoke(version.version)
                    ?: stringResource(R.string.monarch_aiartifactversions_version_tag, version.version)
                MonarchMenuItem(
                    text = tokens.option(tag, version.label, tokens.date(version.createdAt, writeIn, zone)),
                    onClick = {
                        open = false
                        onChoose(version.version)
                    },
                    selected = chosen,
                )
            }
        }
    }
}

package dev.monarchui.components.intelligence.aimessagebranch

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The previous, position, next switcher between versions of a message, made
 * when a reply is regenerated or a prompt is edited and resent: "‹ 2 / 3 ›".
 *
 * Built by Monarch from a `Row` of two `MonarchIconButton`s and a `Text`.
 * The previous button is disabled on the first version and the next button on
 * the last. TalkBack reads the position as "Version 2 of 3", and announces the
 * new position after a button changed it; a change made by the app is not
 * announced. When the button that had keyboard focus becomes disabled at an
 * end, focus moves to the other button. With fewer than two versions nothing
 * is shown, unless [showSingle] is `true`.
 *
 * @param selection the version shown, counted from 0. A value outside the range shows the nearest version.
 * @param count how many versions exist. A count below 1 is treated as 1.
 * @param onSelectionChange called with the version to show, counted from 0.
 * @param previousIcon the icon of the previous button, for example a chevron that points to the start, as a
 * `MonarchIcon` with `mirrorInRtl = true` and a `null` content description. Drawn by default, as on the web; `null`
 * draws none.
 * @param nextIcon the icon of the next button, a chevron that points to the end, mirrored in the same way.
 * @param modifier the modifier of the switcher.
 * @param enabled whether the buttons can be used, for example `false` while a reply streams.
 * @param showSingle shows the switcher when there is only one version.
 * @param label the TalkBack name of the switcher. `null` uses `monarch_aimessagebranch_label`, "Versions".
 * @param previousLabel the TalkBack name of the previous button. `null` uses
 * `monarch_aimessagebranch_previous`, "Previous version".
 * @param nextLabel the TalkBack name of the next button. `null` uses `monarch_aimessagebranch_next`,
 * "Next version".
 * @param formatPosition the visible position from the version, counted from 1, and the count. `null` uses
 * `monarch_aimessagebranch_position`, "2 / 3".
 * @param formatAnnouncement the position as TalkBack reads it. `null` uses
 * `monarch_aimessagebranch_announcement`, "Version 2 of 3".
 */
@Composable
@Suppress("ModifierParameter") // The icon keeps its place before the modifier, so positional callers still compile.
public fun MonarchAiMessageBranch(
    selection: Int,
    count: Int,
    onSelectionChange: (Int) -> Unit,
    previousIcon: @Composable () -> Unit = MonarchDefaultIcons.ChevronLeft,
    nextIcon: @Composable () -> Unit = MonarchDefaultIcons.ChevronRight,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showSingle: Boolean = false,
    label: String? = null,
    previousLabel: String? = null,
    nextLabel: String? = null,
    formatPosition: ((Int, Int) -> String)? = null,
    formatAnnouncement: ((Int, Int) -> String)? = null,
) {
    if (!MonarchAiMessageBranchTokens.visible(count, showSingle)) return
    val total = MonarchAiMessageBranchTokens.total(count)
    val current = MonarchAiMessageBranchTokens.current(selection, count)
    val groupName = monarchString(label, R.string.monarch_aimessagebranch_label)
    val previousName = monarchString(previousLabel, R.string.monarch_aimessagebranch_previous)
    val nextName = monarchString(nextLabel, R.string.monarch_aimessagebranch_next)
    val position = formatted(formatPosition, R.string.monarch_aimessagebranch_position, current + 1, total)
    val spoken = formatted(formatAnnouncement, R.string.monarch_aimessagebranch_announcement, current + 1, total)

    // What the last button press asked for, and which button had focus. Plain holders: they are only read
    // in the composition that the new selection causes.
    val pending = remember { intArrayOf(NONE) }
    val focused = remember { intArrayOf(NONE) }
    val previousFocus = remember { FocusRequester() }
    val nextFocus = remember { FocusRequester() }
    val canGoBack = enabled && current > 0
    val canGoOn = enabled && current < total - 1
    fun go(target: Int, from: Int) {
        if (target !in 0 until total) return
        pending[0] = target
        if (focused[0] != from) focused[0] = NONE
        onSelectionChange(target)
    }

    Row(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = groupName
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(
            name = previousName,
            enabled = canGoBack,
            focus = previousFocus,
            onFocused = { focused[0] = PREVIOUS },
            onClick = { go(current - 1, PREVIOUS) },
            icon = previousIcon,
        )
        Position(text = position, spoken = spoken, announce = pending[0] == current, enabled = enabled)
        StepButton(
            name = nextName,
            enabled = canGoOn,
            focus = nextFocus,
            onFocused = { focused[0] = NEXT },
            onClick = { go(current + 1, NEXT) },
            icon = nextIcon,
        )
    }

    // A button that has just become disabled at an end loses focus: give it to the other one.
    LaunchedEffect(current) {
        if (pending[0] == current) {
            rescueFocus(focused[0], canGoBack, canGoOn, previousFocus, nextFocus)
        }
    }
}

private fun rescueFocus(
    focused: Int,
    canGoBack: Boolean,
    canGoOn: Boolean,
    previousFocus: FocusRequester,
    nextFocus: FocusRequester,
) {
    when {
        focused == PREVIOUS && !canGoBack && canGoOn -> nextFocus.requestFocus()
        focused == NEXT && !canGoOn && canGoBack -> previousFocus.requestFocus()
    }
}

@Composable
private fun formatted(format: ((Int, Int) -> String)?, @StringRes default: Int, version: Int, total: Int): String =
    format?.invoke(version, total) ?: stringResource(default, version, total)

/** The visible "2 / 3", read as "Version 2 of 3", and announced after a button changed it. */
@Composable
private fun Position(text: String, spoken: String, announce: Boolean, enabled: Boolean) {
    Box(
        modifier = Modifier
            .widthIn(min = MonarchAiMessageBranchTokens.PositionMinWidth)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                if (announce) liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MonarchTheme.typography.caption,
            color = MonarchAiMessageBranchTokens.contentColor(enabled, MonarchTheme.colors),
            maxLines = 1,
        )
    }
}

@Composable
private fun StepButton(
    name: String,
    enabled: Boolean,
    focus: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    MonarchIconButton(
        label = name,
        onClick = onClick,
        modifier = Modifier
            .focusRequester(focus)
            .onFocusChanged { if (it.isFocused) onFocused() },
        enabled = enabled,
        shape = MonarchTheme.shapes.full,
    ) { icon() }
}

private const val NONE = -1
private const val PREVIOUS = 0
private const val NEXT = 1

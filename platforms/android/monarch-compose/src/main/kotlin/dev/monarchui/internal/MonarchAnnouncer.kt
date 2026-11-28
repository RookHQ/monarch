package dev.monarchui.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.TextStyle
import dev.monarchui.theme.MonarchTheme

/**
 * What a polite announcer says: [text], with an invisible word joiner added
 * on every second repeat, so TalkBack sees a change and reads the same text
 * again ("Copied", then "Copied" once more).
 */
internal fun monarchSpokenAnnouncement(text: String, repeat: Int): String =
    if (repeat % 2 == 0) text else text + MonarchAnnouncer.REPEAT_MARK

/**
 * A polite announcement for TalkBack, made without moving focus.
 *
 * Compose has no announce call (and `announceForAccessibility` is
 * deprecated), so an announcement is the text of a live region that is
 * always in the layout: [MonarchAnnouncerNode]. Call [announce] from an event;
 * the same text twice is read twice.
 */
@Stable
internal class MonarchAnnouncer {
    /** The text of the last announcement, or `null` before the first one or after [clear]. */
    var text: String? by mutableStateOf(null)
        private set

    /** How many times [text] was announced in a row. */
    var repeat: Int by mutableIntStateOf(0)
        private set

    /** Announces [message], also when it is the same as the last one. */
    fun announce(message: String) {
        repeat = if (message == text) repeat + 1 else 0
        text = message
    }

    /** Removes the announcement, so nothing is read when the node is reached by swiping. */
    fun clear() {
        text = null
        repeat = 0
    }

    internal companion object {
        /** U+2060 WORD JOINER: not shown and not spoken, but a change of the text. */
        const val REPEAT_MARK: String = "⁠"
    }
}

/** A [MonarchAnnouncer] that lives as long as the composition. */
@Composable
internal fun rememberMonarchAnnouncer(): MonarchAnnouncer = remember { MonarchAnnouncer() }

/**
 * The always-present, invisible live region of [announcer]. Put it once in
 * the component, outside any branch that comes and goes.
 */
@Composable
internal fun MonarchAnnouncerNode(announcer: MonarchAnnouncer, modifier: Modifier = Modifier) {
    val spoken = announcer.text?.let { monarchSpokenAnnouncement(it, announcer.repeat) }
    Box(
        modifier = modifier.clearAndSetSemantics {
            liveRegion = LiveRegionMode.Polite
            if (spoken != null) contentDescription = spoken
        },
    )
}

/**
 * A status line that TalkBack reads politely whenever its text changes:
 * "3 results", "Saved", "Uploading 2 of 5". The node is always in the layout,
 * also while [text] is `null`, so the first message is read too.
 *
 * @param text the status, or `null` for none.
 * @param modifier the modifier of the line.
 * @param visible whether the text is also shown. When `false` the line takes no space and is only spoken.
 * @param repeat raise it to read the same [text] again.
 * @param style the text style of the visible line.
 * @param color the text colour of the visible line.
 */
@Composable
internal fun MonarchLiveStatus(
    text: String?,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    repeat: Int = 0,
    style: TextStyle = MonarchTheme.typography.caption,
    color: Color = MonarchTheme.colors.textMuted,
) {
    val spoken = text?.let { monarchSpokenAnnouncement(it, repeat) }
    Box(
        modifier = modifier.clearAndSetSemantics {
            liveRegion = LiveRegionMode.Polite
            if (spoken != null) contentDescription = spoken
        },
    ) {
        if (visible && text != null) Text(text = text, style = style, color = color)
    }
}

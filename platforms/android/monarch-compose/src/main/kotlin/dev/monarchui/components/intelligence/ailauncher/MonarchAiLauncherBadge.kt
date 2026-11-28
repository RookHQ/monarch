package dev.monarchui.components.intelligence.ailauncher

import androidx.compose.runtime.Immutable

/** What the badge of a `MonarchAiLauncher` shows. Every badge is also read by TalkBack. */
@Immutable
public sealed interface MonarchAiLauncherBadge {
    /** A dot: the assistant has new messages. */
    public data object Unread : MonarchAiLauncherBadge

    /** A ring that travels around the button: the assistant is working. Still when animations are off. */
    public data object Working : MonarchAiLauncherBadge

    /**
     * The number of unread messages. Shown as "99+" above 99; nothing is shown for zero or less.
     *
     * @property count the number of unread messages.
     */
    public data class Count(public val count: Int) : MonarchAiLauncherBadge
}

package dev.monarchui.components.datadisplay.leaderboard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchLeaderboard`. Every
 * `null` keeps the built-in string `monarch_leaderboard_<key>`.
 *
 * @property you the tag after the viewer's own name, "(you)".
 * @property empty shown when there are no entries, "Nothing to rank yet".
 * @property loading what TalkBack reads for the placeholder rows, "Loading ranking".
 * @property retry the button that repeats a failed load.
 * @property more the note or button for entries not shown, given how many: "12 more".
 * @property rank read before a row, given the rank: "Rank 1:".
 */
@Immutable
public class MonarchLeaderboardLabels(
    public val you: String? = null,
    public val empty: String? = null,
    public val loading: String? = null,
    public val retry: String? = null,
    public val more: ((Int) -> String)? = null,
    public val rank: ((Int) -> String)? = null,
)

package com.rookhq.monarch.components.identity.mystatus

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus

/**
 * Replacements for the built-in strings of a `MonarchMyStatus`. Every `null`
 * keeps the built-in string.
 *
 * @property hint the action TalkBack reads after "Double tap to", "open your status and settings".
 * @property setStatus shown when there is no status and the person is online or offline, "Set a status".
 * @property presence the name of a presence. `null` uses the strings of `MonarchPresenceDot`, such as "Away".
 */
@Immutable
public class MonarchMyStatusLabels(
    public val hint: String? = null,
    public val setStatus: String? = null,
    public val presence: ((MonarchPresenceStatus) -> String)? = null,
)

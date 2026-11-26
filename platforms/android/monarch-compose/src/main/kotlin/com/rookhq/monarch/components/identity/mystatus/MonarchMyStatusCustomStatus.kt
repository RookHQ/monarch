package com.rookhq.monarch.components.identity.mystatus

import androidx.compose.runtime.Immutable

/**
 * The signed-in person's own status message, shown by `MonarchMyStatus`.
 *
 * @property text the message, already localised, such as "On holiday".
 * @property emoji an emoji shown before the message.
 * @property until when the status clears itself, in epoch milliseconds (UTC). `null` keeps it.
 */
@Immutable
public data class MonarchMyStatusCustomStatus(
    public val text: String? = null,
    public val emoji: String? = null,
    public val until: Long? = null,
)

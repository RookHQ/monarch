package dev.monarchui.components.technical.installverify

import androidx.compose.runtime.Immutable

/**
 * A site the install is expected on, and when it was last seen there.
 *
 * @property label usually the host name, such as "www.example.com".
 * @property lastSeenAt when the installed code last reported from this site, in epoch milliseconds. `null`: not yet.
 * @property id a stable key. `null` uses [label].
 */
@Immutable
public data class MonarchInstallTarget(
    public val label: String,
    public val lastSeenAt: Long? = null,
    public val id: String? = null,
)

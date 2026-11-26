package com.rookhq.monarch.components.identity.sessionlist

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceKind

/**
 * One signed-in session, a browser or an app on a device, for `MonarchSessionList`.
 *
 * @property id a stable id, passed to `onSignOut` and `onSignOutOthers`.
 * @property label what the person recognises, such as "Chrome on macOS".
 * @property kind the kind of device, given to the icon slot.
 * @property detail a quieter line, such as an approximate location or an IP address.
 * @property signedInMillis when the session signed in, in epoch milliseconds (UTC).
 * @property lastActiveMillis when it was last used, in epoch milliseconds (UTC). Not shown for the current session.
 * @property current whether this is the session showing the list.
 */
@Immutable
public class MonarchSession(
    public val id: String,
    public val label: String,
    public val kind: MonarchDeviceKind = MonarchDeviceKind.Desktop,
    public val detail: String? = null,
    public val signedInMillis: Long? = null,
    public val lastActiveMillis: Long? = null,
    public val current: Boolean = false,
)

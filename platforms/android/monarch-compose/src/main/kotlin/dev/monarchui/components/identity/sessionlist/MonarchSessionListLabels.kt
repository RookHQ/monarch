package dev.monarchui.components.identity.sessionlist

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSessionList`. Every
 * `null` keeps the built-in string `monarch_sessionlist_<key>`.
 *
 * @property listLabel the spoken name of the list, "Signed-in sessions".
 * @property current the badge of the session showing the list, "This device".
 * @property signedIn before the sign-in time, "Signed in".
 * @property lastActive before the last-active time, "Last active".
 * @property signOut the row button and the confirm button for one session, "Sign out".
 * @property signOutLabel the spoken name of a row button: "Sign out Chrome on macOS".
 * @property signOutOthers the header button and its confirm button, "Sign out other devices".
 * @property confirmTitle the title of the confirmation for one session, given its label and whether it is the
 * current one: "Sign out Chrome on macOS?" or "Sign out of this device?".
 * @property confirmMessage the text of the confirmation for one session, given whether it is the current one.
 * @property othersTitle the title of the confirmation for every other session, "Sign out other devices?".
 * @property othersMessage the text of that confirmation, given the number of other sessions.
 * @property cancel the button that closes a confirmation, "Cancel".
 * @property signedOut announced after one session is signed out: "Signed out Chrome on macOS".
 * @property signedOutOthers announced after every other session is signed out, given their number.
 * @property signOutFailed shown and announced when signing one session out failed, given its label.
 * @property signOutOthersFailed shown and announced when signing the other sessions out failed.
 * @property loading the loading state, "Loading sessions".
 * @property emptyTitle the empty state, "No active sessions".
 * @property errorTitle the failed state, "Couldn't load your sessions".
 */
@Immutable
public class MonarchSessionListLabels(
    public val listLabel: String? = null,
    public val current: String? = null,
    public val signedIn: String? = null,
    public val lastActive: String? = null,
    public val signOut: String? = null,
    public val signOutLabel: ((String) -> String)? = null,
    public val signOutOthers: String? = null,
    public val confirmTitle: ((label: String, current: Boolean) -> String)? = null,
    public val confirmMessage: ((current: Boolean) -> String)? = null,
    public val othersTitle: String? = null,
    public val othersMessage: ((count: Int) -> String)? = null,
    public val cancel: String? = null,
    public val signedOut: ((String) -> String)? = null,
    public val signedOutOthers: ((count: Int) -> String)? = null,
    public val signOutFailed: ((String) -> String)? = null,
    public val signOutOthersFailed: String? = null,
    public val loading: String? = null,
    public val emptyTitle: String? = null,
    public val errorTitle: String? = null,
)

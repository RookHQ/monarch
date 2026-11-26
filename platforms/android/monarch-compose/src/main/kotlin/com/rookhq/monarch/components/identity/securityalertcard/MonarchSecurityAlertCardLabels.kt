package com.rookhq.monarch.components.identity.securityalertcard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSecurityAlertCard`.
 * Every `null` keeps the built-in string `monarch_securityalertcard_<key>`.
 *
 * @property title the card title, "Security alerts".
 * @property description the line under the title, "Recent security notifications and account activity."
 * @property listLabel the name TalkBack reads for the list of alerts, "Security alerts".
 * @property severity the name of a severity: "Info", "Low", "Medium", "High", "Critical".
 * @property unread the state TalkBack reads for an unread alert, "Unread".
 * @property unreadCount the header badge, from the number of unread alerts: "2 unread".
 * @property markRead the button that marks one alert read, "Mark as read".
 * @property markAllRead the button that marks every alert read, "Mark all as read".
 * @property dismissTitle the visible title of a dismiss button without an icon, "Dismiss".
 * @property dismiss the TalkBack name of a dismiss button, from the alert title: "Dismiss New sign-in".
 * @property dismissed announced after a dismissal, from the alert title: "Dismissed New sign-in".
 * @property loading read and shown while loading, "Loading alerts".
 * @property emptyTitle the empty state, "No security alerts".
 * @property emptyDescription the line under the empty state.
 * @property errorTitle the failed state, "Couldn’t load security alerts".
 */
@Immutable
public class MonarchSecurityAlertCardLabels(
    public val title: String? = null,
    public val description: String? = null,
    public val listLabel: String? = null,
    public val severity: ((MonarchSecurityAlertSeverity) -> String)? = null,
    public val unread: String? = null,
    public val unreadCount: ((count: Int) -> String)? = null,
    public val markRead: String? = null,
    public val markAllRead: String? = null,
    public val dismissTitle: String? = null,
    public val dismiss: ((title: String) -> String)? = null,
    public val dismissed: ((title: String) -> String)? = null,
    public val loading: String? = null,
    public val emptyTitle: String? = null,
    public val emptyDescription: String? = null,
    public val errorTitle: String? = null,
)

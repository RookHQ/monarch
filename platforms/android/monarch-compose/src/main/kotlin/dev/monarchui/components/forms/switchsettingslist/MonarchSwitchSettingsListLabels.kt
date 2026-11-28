package dev.monarchui.components.forms.switchsettingslist

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSwitchSettingsList` and
 * its rows. Every `null` keeps the built-in string
 * `monarch_switchsettingslist_<key>`, or, on a row, the list's replacement.
 *
 * @property saving the status while a save runs, and the name of its spinner ("Saving").
 * @property saved the status after a successful save, also announced ("Saved").
 * @property error the first sentence of a failed save, also announced ("Couldn't save.").
 * @property retry the visible text of the retry button ("Retry").
 * @property retryLabel the TalkBack name of the retry button, given the row label ("Retry Weekly summary").
 * @property errorMessage turns the exception of a failed save into the text shown after [error], or `null` for
 * none. The default shows the exception's message when it has one.
 */
@Immutable
public class MonarchSwitchSettingsListLabels(
    public val saving: String? = null,
    public val saved: String? = null,
    public val error: String? = null,
    public val retry: String? = null,
    public val retryLabel: ((String) -> String)? = null,
    public val errorMessage: ((Throwable) -> String?)? = null,
) {
    /** These labels, with every `null` taken from [fallback]. */
    internal fun or(fallback: MonarchSwitchSettingsListLabels): MonarchSwitchSettingsListLabels =
        MonarchSwitchSettingsListLabels(
            saving = saving ?: fallback.saving,
            saved = saved ?: fallback.saved,
            error = error ?: fallback.error,
            retry = retry ?: fallback.retry,
            retryLabel = retryLabel ?: fallback.retryLabel,
            errorMessage = errorMessage ?: fallback.errorMessage,
        )
}

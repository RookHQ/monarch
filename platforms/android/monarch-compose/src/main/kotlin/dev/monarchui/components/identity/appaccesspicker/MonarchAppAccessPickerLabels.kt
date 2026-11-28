package dev.monarchui.components.identity.appaccesspicker

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAppAccessPicker`. Every
 * `null` keeps the built-in string `monarch_appaccesspicker_<key>`.
 *
 * @property label the name of the group, "App access".
 * @property selectAll the bulk button while not every app is selected, "Select all".
 * @property clearAll the bulk button while every app is selected, "Clear all".
 * @property count the selection count, given the selected and total numbers: "2 of 5 selected".
 * @property loading shown to TalkBack while the apps load, "Loading apps".
 * @property empty shown when there are no apps, "No apps are available yet.".
 */
@Immutable
public class MonarchAppAccessPickerLabels(
    public val label: String? = null,
    public val selectAll: String? = null,
    public val clearAll: String? = null,
    public val count: ((selected: Int, total: Int) -> String)? = null,
    public val loading: String? = null,
    public val empty: String? = null,
)

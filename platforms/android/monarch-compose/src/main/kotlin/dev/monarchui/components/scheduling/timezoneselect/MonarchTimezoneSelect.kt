package dev.monarchui.components.scheduling.timezoneselect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import dev.monarchui.R
import dev.monarchui.components.forms.combobox.MonarchCombobox
import dev.monarchui.components.forms.combobox.MonarchComboboxLabels
import dev.monarchui.components.forms.combobox.MonarchComboboxPresentation
import dev.monarchui.internal.monarchString
import java.util.TimeZone

/**
 * A searchable IANA time zone picker, labelled by UTC offset and city and
 * sorted by offset, with Detected, Pinned and Recent sections.
 *
 * Built on `MonarchCombobox` in its sheet presentation: a field that opens a
 * Material 3 `ModalBottomSheet` with a search field and the list. Zones come
 * from the system's zones for places (`java.util.TimeZone`, aliases collapsed
 * with ICU's canonical id), old ids such as `Asia/Calcutta` are mapped to
 * their current names, and offsets
 * and long names are read for [now], daylight saving included. While the
 * person types, only the full list is searched, so a zone appears once;
 * every word typed must match the label, the id, the long name, an offset
 * (`+5:30`, `utc-8`) or a country name.
 *
 * @param label the visible label of the field and the title of the sheet. Already localised.
 * @param selected the IANA id of the chosen zone, or `null`. An old id selects its current name.
 * @param onSelectedChange called with the id of the chosen zone, or `null` when the selection is cleared.
 * @param modifier the modifier of the field.
 * @param enabled whether the list can be opened.
 * @param placeholder shown in the field while nothing is chosen.
 * @param helperText guidance below the field.
 * @param errorText the error message; the field is in the error state and TalkBack reads it with the field.
 * @param zones limits the list to these zones. `null` lists every location zone the system knows (about 420).
 * @param detectedZone the zone under Detected. `null` is the device's zone.
 * @param showDetected whether the Detected section is shown.
 * @param pinned zones listed under Pinned, such as a workspace's offices.
 * @param recent zones listed under Recent, such as the person's last choices.
 * @param now the instant offsets and long names are computed for, in epoch milliseconds. `null` is the time the
 * select first appeared.
 * @param formatLabel builds an option's label from the zone's information. `null` gives "(UTC+05:30) Kolkata".
 * @param clearIcon the icon of a clear button in the field while a zone is chosen. Monarch bundles no icons;
 * without it there is no clear button.
 * @param labels replacements for the built-in strings and those of the combobox.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchTimezoneSelect(
    label: String,
    selected: String?,
    onSelectedChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    zones: List<String>? = null,
    detectedZone: String? = null,
    showDetected: Boolean = true,
    pinned: List<String> = emptyList(),
    recent: List<String> = emptyList(),
    now: Long? = null,
    formatLabel: ((MonarchTimezoneInfo) -> String)? = null,
    clearIcon: (@Composable () -> Unit)? = null,
    labels: MonarchTimezoneSelectLabels = MonarchTimezoneSelectLabels(),
) {
    val firstShown = remember { System.currentTimeMillis() }
    val at = now ?: firstShown
    val locale = LocalConfiguration.current.locales[0]
    var query by rememberSaveable { mutableStateOf("") }
    val value = selected?.let(TimezoneSelectZones::canonical)
    val detected = if (showDetected) TimezoneSelectZones.canonical(detectedZone ?: TimeZone.getDefault().id) else null
    val sections = TimezoneSelectSections(
        detected = monarchString(labels.detected, R.string.monarch_timezoneselect_detected),
        pinned = monarchString(labels.pinned, R.string.monarch_timezoneselect_pinned),
        recent = monarchString(labels.recent, R.string.monarch_timezoneselect_recent),
        all = monarchString(labels.all, R.string.monarch_timezoneselect_all),
    )
    val empty = monarchString(labels.empty, R.string.monarch_timezoneselect_empty)
    val all = remember(zones, at) {
        val ids = zones?.map(TimezoneSelectZones::canonical)?.distinct() ?: TimezoneSelectZones.available()
        TimezoneSelectZones.sorted(ids, at)
    }
    val searching = query.isNotBlank()
    val options = remember(all, searching, value, detected, pinned, recent, at, locale, formatLabel, sections.all) {
        val infos = HashMap<String, MonarchTimezoneInfo>()
        val request = TimezoneSelectRequest(all, value, detected, pinned, recent, searching)
        TimezoneSelectZones.options(request, sections) { zone, group ->
            val info = infos.getOrPut(zone) { TimezoneSelectZones.info(zone, at, locale) }
            val text = formatLabel?.invoke(info) ?: TimezoneSelectZones.defaultLabel(info)
            TimezoneSelectZones.option(info, text, group)
        }
    }
    val combobox = labels.combobox
    MonarchCombobox(
        label = label,
        options = options,
        selected = value,
        onSelectedChange = onSelectedChange,
        modifier = modifier,
        presentation = MonarchComboboxPresentation.Sheet,
        enabled = enabled,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        query = query,
        onQueryChange = { query = it },
        filter = TimezoneSelectZones::matches,
        clearIcon = clearIcon,
        labels = MonarchComboboxLabels(
            search = combobox.search,
            loading = combobox.loading,
            empty = empty,
            create = combobox.create,
            more = combobox.more,
            cancel = combobox.cancel,
            clear = combobox.clear,
        ),
    )
}

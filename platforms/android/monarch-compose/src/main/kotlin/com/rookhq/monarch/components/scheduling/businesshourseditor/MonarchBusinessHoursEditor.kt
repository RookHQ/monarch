package com.rookhq.monarch.components.scheduling.businesshourseditor

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.forms.checkboxrow.MonarchCheckboxRow
import com.rookhq.monarch.components.forms.formfield.MonarchFormField
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.scheduling.timezoneselect.MonarchTimezoneSelect
import com.rookhq.monarch.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditor
import com.rookhq.monarch.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditorFormats
import com.rookhq.monarch.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditorLabels
import com.rookhq.monarch.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditorTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchRemovalFallback
import com.rookhq.monarch.internal.rememberMonarchRemovalFocus
import com.rookhq.monarch.theme.MonarchTheme

private typealias Tokens = MonarchBusinessHoursEditorTokens

private const val NameKey = "name"
private const val HoursKey = "hours"

/**
 * Edits a business-hours calendar: its name, time zone, weekly hours, optional effective dates, holidays and
 * special-hours dates, and whether it is the default. The value is plain data the app saves;
 * [MonarchBusinessHoursValue.validate] gives the problems for saving.
 *
 * Built by Monarch as a form of sections in a `Column`: `MonarchTextField` for the name, a `MonarchFormField`
 * with a button that opens the Material 3 `DatePickerDialog` for each date, `MonarchTimezoneSelect`,
 * `MonarchWeeklyHoursEditor`, one stacked row per exception (date, name, a `MonarchSelect` for closed or open
 * at other times, and `MonarchSelect` menus for the times) and `MonarchCheckboxRow` for the default. A field's
 * error shows once the person has changed it, or at once with [showErrors]; errors are announced through a
 * polite live region.
 *
 * The editor is not scrollable and is several screens tall: put it in a scrolling screen.
 *
 * @param value the calendar.
 * @param onValueChange called with the whole new value after every change.
 * @param modifier the modifier of the editor.
 * @param showName shows the name field.
 * @param showDefault shows the Default checkbox.
 * @param showEffectiveDates shows the first and last day the hours apply.
 * @param allowSpecialHours lets an exception be open at other times, not only closed.
 * @param requireName a name is required; also passed to [MonarchBusinessHoursValue.validate] for the errors.
 * @param requireOpen at least one open interval is required.
 * @param showErrors shows every error now, for example after a save attempt.
 * @param pinnedTimeZones zones listed first in the time zone picker.
 * @param firstDayOfWeek the first weekday shown, `Calendar.SUNDAY` to `Calendar.SATURDAY`. `null` follows the
 * locale.
 * @param is24Hour shows times on a 24-hour clock. `null` follows the device setting.
 * @param step the minutes between time options, 1 to 60.
 * @param enabled whether the calendar can be changed.
 * @param readOnly shows everything as text, without controls.
 * @param addIcon the icon of the Add date button. Monarch bundles no icons.
 * @param removeIcon the icon of each remove button; without it the button reads "Remove".
 * @param labels replacements for the built-in strings.
 * @param weeklyHoursLabels replacements for the strings of the weekly hours.
 * @param content extra content at the end, for example product-specific settings.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchBusinessHoursEditor(
    value: MonarchBusinessHoursValue,
    onValueChange: (MonarchBusinessHoursValue) -> Unit,
    modifier: Modifier = Modifier,
    showName: Boolean = true,
    showDefault: Boolean = false,
    showEffectiveDates: Boolean = false,
    allowSpecialHours: Boolean = true,
    requireName: Boolean = false,
    requireOpen: Boolean = false,
    showErrors: Boolean = false,
    pinnedTimeZones: List<String> = emptyList(),
    firstDayOfWeek: Int? = null,
    is24Hour: Boolean? = null,
    step: Int = MonarchWeeklyHoursEditorTokens.DEFAULT_STEP,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    addIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Plus,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchBusinessHoursEditorLabels = MonarchBusinessHoursEditorLabels(),
    weeklyHoursLabels: MonarchWeeklyHoursEditorLabels = MonarchWeeklyHoursEditorLabels(),
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { BusinessHoursEditorText(labels, resources) }
    val locale = LocalConfiguration.current.locales[0]
    val twentyFour = is24Hour ?: DateFormat.is24HourFormat(LocalContext.current)
    val endOfDay = text.endOfDay
    val formats = remember(locale, twentyFour, endOfDay) {
        MonarchWeeklyHoursEditorFormats(locale, twentyFour, endOfDay)
    }
    val errors = remember(value, requireName, requireOpen) { value.validate(requireName, requireOpen) }
    var touched by remember { mutableStateOf(emptySet<String>()) }
    val shows: (String) -> Boolean = { key -> showErrors || key in touched }
    val update: (MonarchBusinessHoursValue, String?) -> Unit = { next, touch ->
        if (touch != null && touch !in touched) touched = touched + touch
        onValueChange(next)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap)) {
        if (showName) {
            MonarchTextField(
                value = value.name,
                onValueChange = { update(value.copy(name = it), NameKey) },
                modifier = Modifier.fillMaxWidth(),
                label = text.name,
                required = requireName,
                placeholder = text.namePlaceholder,
                errorText = text.nameRequired.takeIf { errors.nameRequired && shows(NameKey) },
                enabled = enabled,
                readOnly = readOnly,
            )
        }
        if (showEffectiveDates) BusinessHoursEditorEffectiveDates(value, errors, text, enabled, readOnly, update)
        BusinessHoursEditorZone(value, text, pinnedTimeZones, enabled, readOnly) { zone ->
            update(value.copy(timeZone = zone), null)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Tokens.FieldGap)) {
            MonarchWeeklyHoursEditor(
                value = value.hours,
                onValueChange = { update(value.copy(hours = it), HoursKey) },
                firstDayOfWeek = firstDayOfWeek,
                is24Hour = twentyFour,
                step = step,
                enabled = enabled,
                readOnly = readOnly,
                timeZoneLabel = { BusinessHoursEditorNote(value.timeZone) },
                removeIcon = removeIcon,
                labels = weeklyHoursLabels,
            )
            if (errors.noOpenHours && shows(HoursKey)) BusinessHoursEditorError(text.noOpenHours)
        }
        BusinessHoursEditorExceptions(
            value = value,
            errors = errors,
            setup = BusinessHoursEditorRowSetup(
                text = text,
                formats = formats,
                step = MonarchWeeklyHoursEditorTokens.step(step),
                allowSpecialHours = allowSpecialHours,
                enabled = enabled,
                readOnly = readOnly,
                removeIcon = removeIcon,
            ),
            shows = shows,
            addIcon = addIcon,
            update = update,
        )
        if (showDefault) {
            MonarchCheckboxRow(
                checked = value.default,
                onCheckedChange = { update(value.copy(default = it), null) },
                label = text.default,
                enabled = enabled && !readOnly,
                description = text.defaultDescription,
            )
        }
        content?.invoke(this)
    }
}

@Suppress("LongParameterList")
@Composable
private fun BusinessHoursEditorEffectiveDates(
    value: MonarchBusinessHoursValue,
    errors: MonarchBusinessHoursErrors,
    text: BusinessHoursEditorText,
    enabled: Boolean,
    readOnly: Boolean,
    update: (MonarchBusinessHoursValue, String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.FieldGap)) {
        BusinessHoursEditorDateField(
            date = BusinessHoursEditorDate(
                label = text.effectiveFrom,
                value = value.effectiveFrom,
                onValueChange = { update(value.copy(effectiveFrom = it), null) },
                clearable = true,
            ),
            enabled = enabled,
            readOnly = readOnly,
            labels = text.labels,
        )
        BusinessHoursEditorDateField(
            date = BusinessHoursEditorDate(
                label = text.effectiveTo,
                value = value.effectiveTo,
                onValueChange = { update(value.copy(effectiveTo = it), null) },
                error = text.effectiveOrder.takeIf { errors.effectiveOrder },
                earliest = value.effectiveFrom,
                clearable = true,
            ),
            enabled = enabled,
            readOnly = readOnly,
            labels = text.labels,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun BusinessHoursEditorZone(
    value: MonarchBusinessHoursValue,
    text: BusinessHoursEditorText,
    pinned: List<String>,
    enabled: Boolean,
    readOnly: Boolean,
    onZoneChange: (String) -> Unit,
) {
    if (readOnly) {
        MonarchFormField(label = text.timeZone, enabled = enabled, helperText = text.timeZoneDescription) {
            MonarchText(text = value.timeZone, modifier = it)
        }
    } else {
        MonarchTimezoneSelect(
            label = text.timeZone,
            selected = value.timeZone,
            onSelectedChange = { zone -> zone?.let(onZoneChange) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            helperText = text.timeZoneDescription,
            pinned = pinned,
        )
    }
}

/** The holidays and special dates: a heading, its help, one row per date and the Add date button. */
@Suppress("LongParameterList")
@Composable
private fun BusinessHoursEditorExceptions(
    value: MonarchBusinessHoursValue,
    errors: MonarchBusinessHoursErrors,
    setup: BusinessHoursEditorRowSetup,
    shows: (String) -> Boolean,
    addIcon: (@Composable () -> Unit)?,
    update: (MonarchBusinessHoursValue, String?) -> Unit,
) {
    val text = setup.text
    // Stable ids, so keys and touched state follow a row when an earlier one is removed.
    val rows = remember { BusinessHoursEditorRows() }
    val ids = rows.sync(value.exceptions.size)
    // As on the web: after a removal focus goes to the next row's remove button, else the previous one's, else Add.
    val removalFocus = rememberMonarchRemovalFocus(ids)
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.FieldGap)) {
        MonarchText(text = text.exceptions, style = MonarchTextStyle.Subheading, heading = true)
        BusinessHoursEditorNote(text.exceptionsDescription)
        if (value.exceptions.isEmpty()) BusinessHoursEditorNote(text.noExceptions)
        value.exceptions.forEachIndexed { index, exception ->
            key(ids[index]) {
                if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
                val touchKey = "exception:${ids[index]}"
                BusinessHoursEditorExceptionRow(
                    position = index + 1,
                    exception = exception,
                    error = errors.exceptions[index]?.takeIf { shows(touchKey) }?.let(text::exceptionError),
                    setup = setup,
                    onChange = { update(Tokens.withException(value, index, it), touchKey) },
                    removeFocus = removalFocus.requester(ids[index]),
                    onRemove = {
                        removalFocus.removing(ids, ids[index])
                        rows.remove(index)
                        update(Tokens.withoutException(value, index), null)
                    },
                )
            }
        }
        if (!setup.readOnly) {
            MonarchButton(
                text = text.addException,
                onClick = { update(Tokens.withNewException(value), null) },
                modifier = Modifier.monarchRemovalFallback(removalFocus),
                size = MonarchButtonSize.Sm,
                enabled = setup.enabled,
                leadingIcon = addIcon,
            )
        }
    }
}

/** The stable ids of the exception rows of one editor. */
internal class BusinessHoursEditorRows {
    private var ids: List<Int> = emptyList()
    private var next = 0

    /** The ids for [count] rows: new rows at the end get new ids. */
    fun sync(count: Int): List<Int> {
        if (ids.size != count) {
            ids = Tokens.reconcile(ids, count, next)
            next = maxOf(next, (ids.maxOrNull() ?: -1) + 1)
        }
        return ids
    }

    /** Drops the id of the row at [index], so the rows after it keep theirs. */
    fun remove(index: Int) {
        ids = Tokens.reconcile(ids, ids.size - 1, next, removed = index)
    }
}

@Composable
private fun BusinessHoursEditorNote(text: String) {
    MonarchText(text = text, style = MonarchTextStyle.Caption, color = Tokens.noteColor(MonarchTheme.colors))
}

@Composable
private fun BusinessHoursEditorError(text: String) {
    MonarchText(
        text = text,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MonarchTextStyle.Caption,
        color = Tokens.errorColor(MonarchTheme.colors),
    )
}

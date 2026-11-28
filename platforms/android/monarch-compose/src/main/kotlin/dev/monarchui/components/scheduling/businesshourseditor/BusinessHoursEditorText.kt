package dev.monarchui.components.scheduling.businesshourseditor

import android.content.res.Resources
import androidx.compose.runtime.Composable
import dev.monarchui.R
import dev.monarchui.components.scheduling.weeklyhourseditor.MonarchWeeklyHoursEditorFormats

/** The strings of one MonarchBusinessHoursEditor: each label, or the built-in string when it is `null`. */
internal class BusinessHoursEditorText(val labels: MonarchBusinessHoursEditorLabels, private val resources: Resources) {
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val name: String get() = string(labels.name, R.string.monarch_businesshourseditor_name)
    val namePlaceholder: String
        get() = string(labels.namePlaceholder, R.string.monarch_businesshourseditor_name_placeholder)
    val nameRequired: String get() = string(labels.nameRequired, R.string.monarch_businesshourseditor_name_required)
    val timeZone: String get() = string(labels.timeZone, R.string.monarch_businesshourseditor_time_zone)
    val timeZoneDescription: String
        get() = string(labels.timeZoneDescription, R.string.monarch_businesshourseditor_time_zone_description)
    val noOpenHours: String get() = string(labels.noOpenHours, R.string.monarch_businesshourseditor_no_open_hours)
    val effectiveFrom: String get() = string(labels.effectiveFrom, R.string.monarch_businesshourseditor_effective_from)
    val effectiveTo: String get() = string(labels.effectiveTo, R.string.monarch_businesshourseditor_effective_to)
    val effectiveOrder: String
        get() = string(labels.effectiveOrder, R.string.monarch_businesshourseditor_effective_order)
    val exceptions: String get() = string(labels.exceptions, R.string.monarch_businesshourseditor_exceptions)
    val exceptionsDescription: String
        get() = string(labels.exceptionsDescription, R.string.monarch_businesshourseditor_exceptions_description)
    val noExceptions: String get() = string(labels.noExceptions, R.string.monarch_businesshourseditor_no_exceptions)
    val addException: String get() = string(labels.addException, R.string.monarch_businesshourseditor_add_exception)
    val exceptionNamePlaceholder: String
        get() = string(
            labels.exceptionNamePlaceholder,
            R.string.monarch_businesshourseditor_exception_name_placeholder,
        )
    val closed: String get() = string(labels.closed, R.string.monarch_businesshourseditor_closed)
    val specialHours: String get() = string(labels.specialHours, R.string.monarch_businesshourseditor_special_hours)
    val remove: String get() = string(labels.remove, R.string.monarch_businesshourseditor_remove)
    val default: String get() = string(labels.default, R.string.monarch_businesshourseditor_default)
    val defaultDescription: String
        get() = string(labels.defaultDescription, R.string.monarch_businesshourseditor_default_description)
    val endOfDay: String get() = string(labels.endOfDay, R.string.monarch_businesshourseditor_end_of_day)

    fun exceptionDate(position: Int): String = labels.exceptionDate?.invoke(position)
        ?: resources.getString(R.string.monarch_businesshourseditor_exception_date, position)

    fun exceptionName(position: Int): String = labels.exceptionName?.invoke(position)
        ?: resources.getString(R.string.monarch_businesshourseditor_exception_name, position)

    fun exceptionKind(position: Int): String = labels.exceptionKind?.invoke(position)
        ?: resources.getString(R.string.monarch_businesshourseditor_exception_kind, position)

    fun exceptionStart(position: Int): String = labels.exceptionStart?.invoke(position)
        ?: resources.getString(R.string.monarch_businesshourseditor_exception_start, position)

    fun exceptionEnd(position: Int): String = labels.exceptionEnd?.invoke(position)
        ?: resources.getString(R.string.monarch_businesshourseditor_exception_end, position)

    /** "Remove New Year's Day", or "Remove date 2" for a row without a name. */
    fun removeException(position: Int, name: String): String = labels.removeException?.invoke(position, name)
        ?: if (name.isNotEmpty()) {
            resources.getString(R.string.monarch_businesshourseditor_remove_named, name)
        } else {
            resources.getString(R.string.monarch_businesshourseditor_remove_position, position)
        }

    fun exceptionError(error: MonarchBusinessHoursExceptionError): String = labels.exceptionError?.invoke(error)
        ?: resources.getString(
            when (error) {
                MonarchBusinessHoursExceptionError.Date -> R.string.monarch_businesshourseditor_error_date
                MonarchBusinessHoursExceptionError.Name -> R.string.monarch_businesshourseditor_error_name
                MonarchBusinessHoursExceptionError.Duplicate -> R.string.monarch_businesshourseditor_error_duplicate
                MonarchBusinessHoursExceptionError.Hours -> R.string.monarch_businesshourseditor_error_hours
            },
        )
}

/** A date of a MonarchBusinessHoursEditor: its value, limits and labels. */
internal class BusinessHoursEditorDate(
    val label: String,
    val value: Long?,
    val onValueChange: (Long?) -> Unit,
    val error: String? = null,
    val earliest: Long? = null,
    val clearable: Boolean = false,
)

/** How the exception rows of one MonarchBusinessHoursEditor look and behave. */
internal class BusinessHoursEditorRowSetup(
    val text: BusinessHoursEditorText,
    val formats: MonarchWeeklyHoursEditorFormats,
    val step: Int,
    val allowSpecialHours: Boolean,
    val enabled: Boolean,
    val readOnly: Boolean,
    val removeIcon: (@Composable () -> Unit)?,
)

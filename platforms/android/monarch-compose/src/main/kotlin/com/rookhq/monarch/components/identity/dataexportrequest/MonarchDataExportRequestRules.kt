package com.rookhq.monarch.components.identity.dataexportrequest

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.currentIcuTimeZone
import java.util.Locale

/** The status logic of MonarchDataExportRequest, as pure functions. */
internal object MonarchDataExportRequestRules {
    /** The tone of the status badge. */
    fun tone(status: MonarchDataExportStatus): MonarchTone = when (status) {
        MonarchDataExportStatus.Idle, MonarchDataExportStatus.Expired -> MonarchTone.Neutral
        MonarchDataExportStatus.Requested, MonarchDataExportStatus.Preparing -> MonarchTone.Info
        MonarchDataExportStatus.Ready -> MonarchTone.Success
        MonarchDataExportStatus.Failed -> MonarchTone.Danger
    }

    /** Whether the request form is shown: with a request callback, before the first export or after one ended. */
    fun canRequest(status: MonarchDataExportStatus, hasOnRequest: Boolean): Boolean = hasOnRequest &&
        status in setOf(MonarchDataExportStatus.Idle, MonarchDataExportStatus.Expired, MonarchDataExportStatus.Failed)

    /** Whether the preparation note is shown: before requesting and while the export is on its way. */
    fun showsNote(status: MonarchDataExportStatus): Boolean = status in setOf(
        MonarchDataExportStatus.Idle,
        MonarchDataExportStatus.Requested,
        MonarchDataExportStatus.Preparing,
    )

    /** The chosen scope ids in option order, dropping ids that are no longer offered. */
    fun chosenScope(scope: List<MonarchDataExportScopeOption>, chosen: List<String>): List<String> =
        scope.map { it.id }.filter { it in chosen }

    /** Whether the request must stop because scope options are offered and none is ticked. */
    fun scopeMissing(scope: List<MonarchDataExportScopeOption>, chosen: List<String>): Boolean =
        scope.isNotEmpty() && chosenScope(scope, chosen).isEmpty()

    /** The format chosen at first: [defaultFormat] when offered, else the first format. */
    fun initialFormat(formats: List<MonarchDataExportFormatOption>, defaultFormat: String?): String? =
        formats.firstOrNull { it.value == defaultFormat }?.value ?: formats.firstOrNull()?.value

    /** A date in the medium date style and short time style of [locale], in [zone]. */
    fun formatDate(millis: Long, locale: Locale, zone: TimeZone = currentIcuTimeZone()): String {
        val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        format.timeZone = zone
        return format.format(millis)
    }
}

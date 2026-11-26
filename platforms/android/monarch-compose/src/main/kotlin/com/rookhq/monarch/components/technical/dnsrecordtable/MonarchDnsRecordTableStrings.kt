package com.rookhq.monarch.components.technical.dnsrecordtable

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.monarchString

/** The strings of one MonarchDnsRecordTable, with the caller's replacements applied. */
@Suppress("LongParameterList")
internal class MonarchDnsRecordTableStrings(
    val type: String,
    val name: String,
    val value: String,
    val ttl: String,
    val priority: String,
    val status: String,
    val found: String,
    val waiting: String,
    val purpose: (MonarchDnsRecordPurpose?) -> String,
    val copyName: (MonarchDnsRecord, String) -> String,
    val copyValue: (MonarchDnsRecord, String) -> String,
    val fullName: (String) -> String,
    val label: (String?) -> String,
    val summary: (Int, Int) -> String,
)

/** The strings of a table, from [labels] and the built-in resources. */
@Composable
internal fun monarchDnsRecordTableStrings(labels: MonarchDnsRecordTableLabels): MonarchDnsRecordTableStrings {
    val resources = LocalResources.current
    val routing = resources.getString(R.string.monarch_dnsrecordtable_purpose_routing)
    val verification = resources.getString(R.string.monarch_dnsrecordtable_purpose_verification)
    val certificate = resources.getString(R.string.monarch_dnsrecordtable_purpose_certificate)
    return MonarchDnsRecordTableStrings(
        type = monarchString(labels.type, R.string.monarch_dnsrecordtable_type),
        name = monarchString(labels.name, R.string.monarch_dnsrecordtable_name),
        value = monarchString(labels.value, R.string.monarch_dnsrecordtable_value),
        ttl = monarchString(labels.ttl, R.string.monarch_dnsrecordtable_ttl),
        priority = monarchString(labels.priority, R.string.monarch_dnsrecordtable_priority),
        status = monarchString(labels.status, R.string.monarch_dnsrecordtable_status),
        found = monarchString(labels.found, R.string.monarch_dnsrecordtable_found),
        waiting = monarchString(labels.waiting, R.string.monarch_dnsrecordtable_waiting),
        purpose = labels.purpose ?: { purpose ->
            when (purpose) {
                MonarchDnsRecordPurpose.Routing -> routing
                MonarchDnsRecordPurpose.Certificate -> certificate
                MonarchDnsRecordPurpose.Verification, null -> verification
                is MonarchDnsRecordPurpose.Custom -> purpose.heading
            }
        },
        copyName = labels.copyName ?: { record, _ ->
            resources.getString(R.string.monarch_dnsrecordtable_copy_name, record.type)
        },
        copyValue = labels.copyValue ?: { record, shown ->
            resources.getString(R.string.monarch_dnsrecordtable_copy_value, record.type, shown)
        },
        fullName = labels.fullName ?: { name -> resources.getString(R.string.monarch_dnsrecordtable_full_name, name) },
        label = labels.label ?: { zone ->
            if (zone.isNullOrEmpty()) {
                resources.getString(R.string.monarch_dnsrecordtable_label)
            } else {
                resources.getString(R.string.monarch_dnsrecordtable_label_zone, zone)
            }
        },
        summary = labels.summary ?: { found, checked ->
            resources.getQuantityString(R.plurals.monarch_dnsrecordtable_summary, checked, found, checked)
        },
    )
}

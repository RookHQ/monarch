package com.rookhq.monarch.components.technical.dnsrecordtable

import androidx.compose.runtime.Composable

/** What every card and row of one table shares. */
internal class MonarchDnsRecordContext(
    val zone: String?,
    val copyIcon: (@Composable () -> Unit)?,
    val onCopy: ((String) -> Unit)?,
    val strings: MonarchDnsRecordTableStrings,
)

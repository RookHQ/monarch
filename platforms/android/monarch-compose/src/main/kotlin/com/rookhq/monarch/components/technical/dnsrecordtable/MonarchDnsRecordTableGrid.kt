package com.rookhq.monarch.components.technical.dnsrecordtable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The records as a table: a heading row (Type, Name, Value, and Priority, TTL and Status only when a record uses
 * them) and one row per record, with single-line names and values shortened in the middle.
 */
@Composable
internal fun MonarchDnsRecordTableGrid(records: List<MonarchDnsRecord>, context: MonarchDnsRecordContext) {
    val colors = MonarchTheme.colors
    val strings = context.strings
    val shape = MonarchShapeDefaults.Container
    val columns = MonarchDnsRecordTableTokens.columns(records)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MonarchDnsRecordTableTokens.surface(colors), shape)
            .border(MonarchShapeDefaults.BorderWidth, MonarchDnsRecordTableTokens.border(colors), shape)
            .padding(horizontal = MonarchDnsRecordTableTokens.Padding),
    ) {
        TableRow(header = true) {
            Header(strings.type, MonarchDnsRecordTableTokens.TypeWeight)
            Header(strings.name, MonarchDnsRecordTableTokens.NameWeight)
            Header(strings.value, MonarchDnsRecordTableTokens.ValueWeight)
            if (columns.priority) Header(strings.priority, MonarchDnsRecordTableTokens.NumberWeight)
            if (columns.ttl) Header(strings.ttl, MonarchDnsRecordTableTokens.NumberWeight)
            if (columns.status) Header(strings.status, MonarchDnsRecordTableTokens.StatusWeight)
        }
        records.forEach { record ->
            val shown = MonarchDnsRecordTableTokens.relativeName(record.name, context.zone)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MonarchShapeDefaults.BorderWidth)
                    .background(MonarchDnsRecordTableTokens.border(colors))
                    .clearAndSetSemantics { },
            )
            TableRow(header = false) {
                Cell(MonarchDnsRecordTableTokens.TypeWeight) { MonarchDnsRecordPlainValue(record.type) }
                Cell(MonarchDnsRecordTableTokens.NameWeight) {
                    MonarchDnsRecordCopyValue(shown, shown, strings.copyName(record, shown), context, wrap = false)
                }
                Cell(MonarchDnsRecordTableTokens.ValueWeight) {
                    MonarchDnsRecordCopyValue(
                        record.value, record.value, strings.copyValue(record, shown), context, wrap = false,
                    )
                }
                if (columns.priority) {
                    Cell(MonarchDnsRecordTableTokens.NumberWeight) {
                        MonarchDnsRecordPlainValue(record.priority?.toString().orEmpty())
                    }
                }
                if (columns.ttl) {
                    Cell(MonarchDnsRecordTableTokens.NumberWeight) { MonarchDnsRecordPlainValue(record.ttl.orEmpty()) }
                }
                if (columns.status) {
                    Cell(MonarchDnsRecordTableTokens.StatusWeight) { MonarchDnsRecordStatus(record, strings) }
                }
            }
        }
    }
}

@Composable
private fun TableRow(header: Boolean, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MonarchDnsRecordTableTokens.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchDnsRecordTableTokens.ColumnGap),
        verticalAlignment = if (header) Alignment.Bottom else Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun RowScope.Header(text: String, weight: Float) {
    MonarchText(
        text = text,
        modifier = Modifier.weight(weight).semantics { heading() },
        style = MonarchTextStyle.Caption,
        color = MonarchDnsRecordTableTokens.muted(MonarchTheme.colors),
    )
}

@Composable
private fun RowScope.Cell(weight: Float, content: @Composable () -> Unit) {
    Box(modifier = Modifier.weight(weight), contentAlignment = Alignment.CenterStart) { content() }
}

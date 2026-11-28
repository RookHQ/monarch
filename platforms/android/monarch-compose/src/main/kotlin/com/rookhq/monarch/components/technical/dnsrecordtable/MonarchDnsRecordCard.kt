package com.rookhq.monarch.components.technical.dnsrecordtable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.copybutton.MonarchCopyButton
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/** One record as a card: the purpose as a heading, the status, the type, the name, the value and the full name. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonarchDnsRecordCard(record: MonarchDnsRecord, context: MonarchDnsRecordContext) {
    val colors = MonarchTheme.colors
    val strings = context.strings
    val shown = MonarchDnsRecordTableTokens.relativeName(record.name, context.zone)
    val shape = MonarchShapeDefaults.Container
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MonarchDnsRecordTableTokens.surface(colors), shape)
            .border(MonarchShapeDefaults.BorderWidth, MonarchDnsRecordTableTokens.border(colors), shape)
            .padding(MonarchDnsRecordTableTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(MonarchDnsRecordTableTokens.PartGap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchDnsRecordTableTokens.PartGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(
                text = strings.purpose(record.purpose),
                modifier = Modifier.weight(1f),
                style = MonarchTextStyle.Label,
                color = MonarchDnsRecordTableTokens.text(colors),
                heading = true,
            )
            MonarchDnsRecordStatus(record, strings)
        }
        val roomy = LocalDensity.current.fontScale <= InlineFontScale
        val inline = record.priority == null && record.ttl == null && roomy
        if (inline) {
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchDnsRecordTableTokens.ColumnGap)) {
                Term(strings.type, readTerm = true) { MonarchDnsRecordPlainValue(record.type) }
                Term(strings.name, readTerm = false, modifier = Modifier.weight(1f)) {
                    MonarchDnsRecordCopyValue(shown, shown, strings.copyName(record, shown), context, wrap = false)
                }
            }
            // The value has its own line, so a long value is shown whole on a phone.
            Term(strings.value, readTerm = false) {
                val spokenName = strings.copyValue(record, shown)
                MonarchDnsRecordCopyValue(record.value, record.value, spokenName, context, wrap = true)
            }
        } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchDnsRecordTableTokens.ColumnGap)) {
            Term(strings.type, readTerm = true) { MonarchDnsRecordPlainValue(record.type) }
            record.priority?.let { priority ->
                Term(strings.priority, readTerm = true) { MonarchDnsRecordPlainValue(priority.toString()) }
            }
            record.ttl?.let { Term(strings.ttl, readTerm = true) { MonarchDnsRecordPlainValue(it) } }
        }
        Term(strings.name, readTerm = false) {
            MonarchDnsRecordCopyValue(shown, shown, strings.copyName(record, shown), context, wrap = true)
        }
        Term(strings.value, readTerm = false) {
            val spokenName = strings.copyValue(record, shown)
            MonarchDnsRecordCopyValue(record.value, record.value, spokenName, context, wrap = true)
        }
        }
        if (shown != record.name) {
            MonarchText(
                text = strings.fullName(record.name),
                style = MonarchTextStyle.Caption,
                color = MonarchDnsRecordTableTokens.muted(colors),
            )
        }
    }
}

/** The status badge of a checked record. */
@Composable
internal fun MonarchDnsRecordStatus(record: MonarchDnsRecord, strings: MonarchDnsRecordTableStrings) {
    MonarchDnsRecordTableTokens.status(record, strings.found, strings.waiting)?.let {
        MonarchStatusBadge(definition = it, size = MonarchStatusBadgeSize.Sm)
    }
}

/**
 * A name or a value with its copy button. The text is read as its spoken name and the text; the button is its own
 * element named "Copy" and the spoken name.
 */
@Composable
internal fun MonarchDnsRecordCopyValue(
    text: String,
    copied: String,
    spokenName: String,
    context: MonarchDnsRecordContext,
    wrap: Boolean,
) {
    val spoken = spokenName + MonarchDnsRecordTableTokens.Separator + text
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MonarchDnsRecordTableTokens.ColumnGap),
    ) {
        MonarchText(
            text = text,
            modifier = Modifier.weight(1f).clearAndSetSemantics { contentDescription = spoken },
            style = MonarchTextStyle.Code,
            color = MonarchDnsRecordTableTokens.text(MonarchTheme.colors),
            overflow = if (wrap) TextOverflow.Clip else TextOverflow.MiddleEllipsis,
            maxLines = if (wrap) Int.MAX_VALUE else 1,
        )
        val icon = context.copyIcon
        if (icon != null) {
            MonarchCopyButton(value = copied, icon = icon, target = spokenName, onCopy = context.onCopy)
        } else {
            MonarchCopyButton(
                value = copied,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                target = spokenName,
                onCopy = context.onCopy,
            )
        }
    }
}

/** Text that is not copied: the type, the priority and the TTL. */
@Composable
internal fun MonarchDnsRecordPlainValue(text: String) {
    MonarchText(
        text = text,
        style = MonarchTextStyle.Code,
        color = MonarchDnsRecordTableTokens.text(MonarchTheme.colors),
    )
}

/**
 * A term above its value. With [readTerm] the two are read together ("Type, CNAME"); otherwise the term is hidden,
 * because the copy field carries its own spoken name.
 */
@Composable
private fun Term(term: String, readTerm: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.then(if (readTerm) Modifier.semantics(mergeDescendants = true) { } else Modifier),
        verticalArrangement = Arrangement.spacedBy(MonarchDnsRecordTableTokens.TermGap),
    ) {
        MonarchText(
            text = term,
            modifier = if (readTerm) Modifier else Modifier.clearAndSetSemantics { },
            style = MonarchTextStyle.Caption,
            color = MonarchDnsRecordTableTokens.muted(MonarchTheme.colors),
        )
        content()
    }
}

private const val InlineFontScale = 1.3f

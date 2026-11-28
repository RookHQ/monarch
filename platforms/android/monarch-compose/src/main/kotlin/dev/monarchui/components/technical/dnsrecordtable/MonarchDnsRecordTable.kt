package dev.monarchui.components.technical.dnsrecordtable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus

/**
 * The DNS records a user must add at their DNS provider to connect a custom domain or verify a sender: type, name and
 * value with copy buttons, what each record is for, and whether public DNS already shows it.
 *
 * Built by Monarch from Compose layouts, because Material 3 has no table: each record is a card in a narrow window
 * and a row under column headings in a wide one. Names and values are copied with `MonarchCopyButton`, and a record
 * that was checked shows a `MonarchStatusBadge` ("Found" or "Waiting", a word as well as a tone). With a [zone],
 * names are shown the way most providers want them typed (`book`, `@` for the apex) with the full name under the
 * card.
 *
 * For TalkBack, the records are one group named "DNS records for example.com", each card heading is a heading, a
 * name is read as "name of the CNAME record, book" and its copy button as "Copy name of the CNAME record". When the
 * number of found records changes, "1 of 2 records found." is announced through a polite live region.
 *
 * @param records the records, in the order the user should create them.
 * @param modifier the modifier of the records.
 * @param zone the DNS zone the user manages, such as `example.com`. Names inside it are shown relative to it.
 * @param layout cards, a table, or automatic: a table at 640dp and wider when the font scale is at most 1.3.
 * @param copyIcon a copy icon, such as a `MonarchIcon` with a `null` content description. Given, each copy button is
 * an icon button; otherwise a small text button "Copy".
 * @param onCopy called with the copied text after a name or a value is copied.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchDnsRecordTable(
    records: List<MonarchDnsRecord>,
    modifier: Modifier = Modifier,
    zone: String? = null,
    layout: MonarchDnsRecordTableLayout = MonarchDnsRecordTableLayout.Automatic,
    copyIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Copy,
    onCopy: ((String) -> Unit)? = null,
    labels: MonarchDnsRecordTableLabels = MonarchDnsRecordTableLabels(),
) {
    val strings = monarchDnsRecordTableStrings(labels)
    val counts = MonarchDnsRecordTableTokens.counts(records)
    var previousFound by remember { mutableIntStateOf(counts.found) }
    var announcement by remember { mutableStateOf("") }
    LaunchedEffect(counts.found, counts.checked) {
        if (counts.found != previousFound) {
            announcement = strings.summary(counts.found, counts.checked)
            previousFound = counts.found
        }
    }
    val name = strings.label(zone)
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().semantics { contentDescription = name }) {
            val fontScale = LocalDensity.current.fontScale
            val context = MonarchDnsRecordContext(zone, copyIcon, onCopy, strings)
            if (MonarchDnsRecordTableTokens.showsTable(layout, maxWidth, fontScale)) {
                MonarchDnsRecordTableGrid(records, context)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(MonarchDnsRecordTableTokens.CardGap)) {
                    records.forEach { record -> MonarchDnsRecordCard(record, context) }
                }
            }
        }
        // Always present, so a change of the found count is announced without moving focus.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

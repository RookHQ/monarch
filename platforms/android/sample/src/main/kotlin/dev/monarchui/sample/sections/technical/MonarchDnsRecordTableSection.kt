package dev.monarchui.sample.sections.technical

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.technical.dnsrecordtable.MonarchDnsRecord
import dev.monarchui.components.technical.dnsrecordtable.MonarchDnsRecordPurpose
import dev.monarchui.components.technical.dnsrecordtable.MonarchDnsRecordTable
import dev.monarchui.components.technical.dnsrecordtable.MonarchDnsRecordTableLayout
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of DnsRecordTable. */
object MonarchDnsRecordTableSection : CataloguePage {
    override val title = "DnsRecordTable"

    private const val MX_PRIORITY = 10
    private const val MX_BACKUP_PRIORITY = 20

    @Composable
    override fun Content() {
        var verified by rememberSaveable { mutableStateOf(false) }
        val records = listOf(
            MonarchDnsRecord(
                type = "CNAME",
                name = "book.example.com",
                value = "domains.example.net",
                purpose = MonarchDnsRecordPurpose.Routing,
                found = true,
            ),
            MonarchDnsRecord(
                type = "TXT",
                name = "_verify.book.example.com",
                value = "verify=4f7k2m9q8d3r6t1w",
                purpose = MonarchDnsRecordPurpose.Verification,
                ttl = "3600",
                found = verified,
            ),
        )
        CatalogueCaption("Cards, with the found state")
        MonarchSwitchRow(
            checked = verified,
            onCheckedChange = { verified = it },
            label = "Public DNS shows the TXT record",
        )
        MonarchDnsRecordTable(records = records, zone = "example.com", layout = MonarchDnsRecordTableLayout.Cards)
        CatalogueCaption("Table")
        MonarchDnsRecordTable(
            records = listOf(
                MonarchDnsRecord(type = "MX", name = "example.com", value = "mx1.example.net", priority = MX_PRIORITY),
                MonarchDnsRecord(
                    type = "MX",
                    name = "example.com",
                    value = "mx2.example.net",
                    priority = MX_BACKUP_PRIORITY,
                ),
            ),
            zone = "example.com",
            layout = MonarchDnsRecordTableLayout.Table,
        )
        CatalogueCaption("Automatic: a table at 640dp and wider")
        MonarchDnsRecordTable(records = records, zone = "example.com")
    }
}

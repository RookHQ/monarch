package com.rookhq.monarch.components.technical.dnsrecordtable

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val records = listOf(
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
        found = false,
    ),
    MonarchDnsRecord(type = "MX", name = "example.com", value = "mx.example.net", priority = 10),
)

@MonarchPreviews
@Composable
internal fun MonarchDnsRecordTableCardsPreview() {
    PreviewFrame {
        MonarchDnsRecordTable(records = records, zone = "example.com", layout = MonarchDnsRecordTableLayout.Cards)
    }
}

@MonarchPreviews
@Composable
internal fun MonarchDnsRecordTableTablePreview() {
    PreviewFrame {
        MonarchDnsRecordTable(records = records, zone = "example.com", layout = MonarchDnsRecordTableLayout.Table)
    }
}

import MonarchUI
import SwiftUI

struct MonarchDnsRecordTableSection: CataloguePage {
    static let title = "DNS record table"

    @State private var isRoutingFound = false
    @State private var lastCopied = "Nothing copied yet."

    private var records: [MonarchDnsRecord] {
        [
            MonarchDnsRecord(
                type: "CNAME",
                name: "book.example.com",
                value: "domains.example.net",
                purpose: .routing,
                isFound: isRoutingFound
            ),
            MonarchDnsRecord(
                type: "TXT",
                name: "_verify.book.example.com",
                value: "verify=4f7k2m9q8d3r6t1w",
                purpose: .verification,
                ttl: "3600",
                isFound: true
            ),
        ]
    }

    var body: some View {
        MonarchDnsRecordTable(records, zone: "example.com") { text in
            lastCopied = "Copied \(text)"
        }
        Text(lastCopied).monarchTextStyle(.caption)
        Toggle("Public DNS shows the CNAME record", isOn: $isRoutingFound)
            .monarchTextStyle(.body)

        CatalogueCaption("Cards")
        MonarchDnsRecordTable(records, zone: "example.com", layout: .cards)

        CatalogueCaption("Table, with priority and no check")
        MonarchDnsRecordTable(
            [
                MonarchDnsRecord(type: "MX", name: "example.com", value: "mx1.example.net", priority: 10),
                MonarchDnsRecord(type: "MX", name: "example.com", value: "mx2.example.net", priority: 20),
            ],
            zone: "example.com",
            layout: .table
        )
    }
}

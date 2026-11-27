import MonarchUI
import SwiftUI

struct MonarchMergeDialogSection: CataloguePage {
    static let title = "Merge dialog"

    private struct MergeRefused: LocalizedError {
        var errorDescription: String? { "The server refused the merge. Nothing was changed." }
    }

    @State private var isMerging = false
    @State private var failsNext = false
    @State private var result: MonarchMergeSelection?

    private let records = [
        MonarchMergeRecord(
            id: "a",
            title: "Acme Ltd",
            description: "Created 3 Mar 2025 · 12 invoices",
            values: ["email": "hello@acme.com", "phone": "", "country": "United Kingdom"]
        ),
        MonarchMergeRecord(
            id: "b",
            title: "ACME Limited",
            description: "Created 9 Jun 2025 · 2 invoices",
            values: ["email": "info@acme.com", "phone": "+44 20 7946 0000", "country": "United Kingdom"]
        ),
    ]

    var body: some View {
        Toggle("Fail the next merge", isOn: $failsNext)
            .monarchTextStyle(.body)
        MonarchButton("Merge 2 companies", systemImage: "arrow.triangle.merge", variant: .primary) {
            isMerging = true
        }
        .sheet(isPresented: $isMerging) {
            MonarchMergeDialog(
                records: records,
                fields: [
                    MonarchMergeField(id: "email", label: "Email"),
                    MonarchMergeField(id: "phone", label: "Phone"),
                    MonarchMergeField(id: "country", label: "Country"),
                ],
                consequences: [
                    MonarchConsequence(.changed, "invoices move to the kept company", count: 2),
                    MonarchConsequence(.removed, "ACME Limited is archived"),
                ]
            ) { selection in
                try await Task.sleep(for: .seconds(1))
                if failsNext {
                    failsNext = false
                    throw MergeRefused()
                }
                result = selection
            }
        }
        if let result {
            CatalogueCaption("Kept \(result.primaryID); merged \(result.mergedIDs.joined(separator: ", "))")
        }
    }
}

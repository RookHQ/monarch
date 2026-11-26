import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let acme = MonarchMergeRecord(
    id: "a",
    title: "Acme Ltd",
    description: "Created 3 Mar 2025 · 12 invoices",
    values: ["email": "hello@acme.com", "phone": "", "country": "UK"]
)
private let acmeLimited = MonarchMergeRecord(
    id: "b",
    title: "ACME Limited",
    values: ["email": "info@acme.com", "phone": "+44 20 7946 0000", "country": " UK "]
)
private let acmeGroup = MonarchMergeRecord(
    id: "c",
    title: "Acme Group",
    values: ["email": "hello@acme.com", "country": "UK"]
)
private let fields = [
    MonarchMergeField(id: "email", label: "Email"),
    MonarchMergeField(id: "phone", label: "Phone"),
    MonarchMergeField(id: "country", label: "Country"),
]

private struct MergeFailure: LocalizedError {
    var errorDescription: String? { "Acme Group has open invoices." }
}

private struct SilentFailure: Error {}

enum MonarchMergeDialogFixtures: ComponentFixtures {
    static let id = "merge-dialog"
    static let fixtures = [
        Fixture("two records with differing, empty and matching fields") {
            MonarchMergeDialog(records: [acme, acmeLimited], fields: fields) { _ in }
                .frame(height: MonarchTokens.Space.space24 * 8)
        },
        Fixture("three records, another primary, consequences") {
            MonarchMergeDialog(
                records: [acme, acmeLimited, acmeGroup],
                fields: fields,
                defaultPrimaryID: "b",
                consequences: [
                    MonarchConsequence(.changed, "invoices move to the kept record", count: 12),
                    MonarchConsequence(.removed, "The other records are archived"),
                ]
            ) { _ in }
            .frame(height: MonarchTokens.Space.space24 * 8)
        },
        Fixture("blocked, and a single record") {
            VStack {
                MonarchMergeDialog(
                    records: [acme, acmeLimited],
                    fields: fields,
                    blockers: ["ACME Limited is linked to an open contract."]
                ) { _ in }
                .frame(height: MonarchTokens.Space.space24 * 5)
                MonarchMergeDialog(records: [acme], fields: fields) { _ in }
                    .frame(height: MonarchTokens.Space.space24 * 3)
            }
        },
        Fixture("custom labels, long text") {
            MonarchMergeDialog(
                records: [
                    MonarchMergeRecord(
                        id: "x",
                        title: "The Very Long Company Name Holdings International Limited",
                        values: ["email": "a-very-long-address-for-accounts-payable@example.com"]
                    ),
                    MonarchMergeRecord(id: "y", title: "Short", values: [:]),
                ],
                fields: [MonarchMergeField(id: "email", label: "Billing email address")],
                labels: MonarchMergeDialogLabels(title: "Combine companies", confirm: { "Combine \($0)" })
            ) { _ in }
            .frame(height: MonarchTokens.Space.space24 * 6)
        },
    ]
}

@Suite("MonarchMergeDialog")
struct MonarchMergeDialogTests {
    private let labels = MonarchMergeDialogLabels()

    @Test("Fields that match in every record are listed once; blanks and spaces count as equal")
    func split() {
        let two = MonarchMergeDialogTokens.split(fields: fields, records: [acme, acmeLimited])
        #expect(two.differing.map(\.id) == ["email", "phone"])
        #expect(two.same.map(\.id) == ["country"])
        let three = MonarchMergeDialogTokens.split(fields: fields, records: [acme, acmeGroup])
        #expect(three.differing.isEmpty)
        #expect(three.same.map(\.id) == ["email", "phone", "country"])
    }

    @Test("The default keeps the primary value, and fills a blank from another record")
    func defaults() {
        let records = [acme, acmeLimited]
        #expect(MonarchMergeDialogTokens.defaultChoice(field: "email", records: records, primaryID: "a") == "a")
        #expect(MonarchMergeDialogTokens.defaultChoice(field: "phone", records: records, primaryID: "a") == "b")
        #expect(MonarchMergeDialogTokens.defaultChoice(field: "missing", records: records, primaryID: "a") == "a")
    }

    @Test("A pick wins over the default unless it names an unknown record")
    func choice() {
        let records = [acme, acmeLimited]
        #expect(
            MonarchMergeDialogTokens.choice(field: "email", picked: ["email": "b"], records: records, primaryID: "a")
                == "b"
        )
        #expect(
            MonarchMergeDialogTokens.choice(field: "email", picked: ["email": "z"], records: records, primaryID: "a")
                == "a"
        )
    }

    @Test("The selection names the primary, the merged records and every field")
    func selection() {
        let selection = MonarchMergeDialogTokens.selection(
            records: [acme, acmeLimited, acmeGroup],
            fields: fields,
            primaryID: "b",
            picked: ["email": "a"]
        )
        #expect(selection?.primaryID == "b")
        #expect(selection?.mergedIDs == ["a", "c"])
        #expect(selection?.fields == ["email": "a", "phone": "b", "country": "b"])
        let fallback = MonarchMergeDialogTokens.selection(
            records: [acme, acmeLimited],
            fields: fields,
            primaryID: "gone",
            picked: [:]
        )
        #expect(fallback?.primaryID == "a")
        #expect(MonarchMergeDialogTokens.selection(records: [], fields: fields, primaryID: "a", picked: [:]) == nil)
    }

    @Test("Fewer than two records or any blocker blocks the merge")
    func blocked() {
        #expect(!MonarchMergeDialogTokens.isBlocked(recordCount: 2, blockers: []))
        #expect(MonarchMergeDialogTokens.isBlocked(recordCount: 1, blockers: []))
        #expect(MonarchMergeDialogTokens.isBlocked(recordCount: 3, blockers: ["Open contract"]))
    }

    @Test("Options read their value and source; empty values read Empty")
    func optionLabels() {
        #expect(
            MonarchMergeDialogTokens.optionLabel(value: "hello@acme.com", record: acme, labels: labels)
                == "hello@acme.com, from Acme Ltd"
        )
        #expect(
            MonarchMergeDialogTokens.optionLabel(value: " ", record: acme, labels: labels) == "Empty, from Acme Ltd"
        )
        #expect(MonarchMergeDialogTokens.isEmpty(nil))
        #expect(MonarchMergeDialogTokens.isEmpty("  \n"))
        #expect(!MonarchMergeDialogTokens.isEmpty("x"))
        let custom = MonarchMergeDialogLabels(from: { "de \($0)" }, empty: "Vide")
        #expect(MonarchMergeDialogTokens.optionLabel(value: nil, record: acme, labels: custom) == "Vide, de Acme Ltd")
    }

    @Test("The confirm title counts the records")
    func confirm() {
        #expect(MonarchMergeDialogTokens.confirmTitle(count: 3, labels: labels) == "Merge 3 records")
        let custom = MonarchMergeDialogLabels(confirm: { "Fusionner \($0)" })
        #expect(MonarchMergeDialogTokens.confirmTitle(count: 2, labels: custom) == "Fusionner 2")
    }

    @Test("A failure shows the error's description, or the built-in message")
    func errors() {
        #expect(
            MonarchMergeDialogTokens.errorMessage(MergeFailure(), labels: labels) == "Acme Group has open invoices."
        )
        #expect(
            MonarchMergeDialogTokens.errorMessage(SilentFailure(), labels: labels)
                == "The records could not be merged. Nothing was changed."
        )
        #expect(
            MonarchMergeDialogTokens.errorMessage(SilentFailure(), labels: MonarchMergeDialogLabels(error: "Failed"))
                == "Failed"
        )
    }

    @Test("Colours")
    func colours() {
        #expect(MonarchMergeDialogTokens.canvas == \.bgCanvas)
        #expect(MonarchMergeDialogTokens.row == \.bgSurface)
        #expect(MonarchMergeDialogTokens.muted == \.textMuted)
        #expect(MonarchMergeDialogTokens.emptyText == \.textSubtle)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchMergeDialogStrings.title == "Merge records")
        #expect(MonarchMergeDialogStrings.description.hasPrefix("Choose the record to keep"))
        #expect(MonarchMergeDialogStrings.keep == "Record to keep")
        #expect(MonarchMergeDialogStrings.kept == "Kept")
        #expect(MonarchMergeDialogStrings.values == "Values to keep")
        #expect(MonarchMergeDialogStrings.from("X") == "from X")
        #expect(MonarchMergeDialogStrings.same == "Same in every record")
        #expect(MonarchMergeDialogStrings.empty == "Empty")
        #expect(MonarchMergeDialogStrings.consequences == "What happens")
        #expect(MonarchMergeDialogStrings.confirm(2) == "Merge 2 records")
        #expect(MonarchMergeDialogStrings.cancel == "Cancel")
        #expect(MonarchMergeDialogStrings.error == "The records could not be merged. Nothing was changed.")
    }
}

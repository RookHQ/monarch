import MonarchUI
import SwiftUI

struct MonarchEditableCellSection: CataloguePage {
    static let title = "Editable cell"

    @State private var title: MonarchEditableCellValue = .text("Quarterly review")
    @State private var quantity: MonarchEditableCellValue = .number(12)
    @State private var status: MonarchEditableCellValue = .text("open")
    @State private var due: MonarchEditableCellValue = .empty
    @State private var owner: MonarchEditableCellValue = .text("Sam Lee")

    private struct Conflict: LocalizedError {
        var errorDescription: String? { "Someone else changed this row. Reload to see it." }
    }

    var body: some View {
        CatalogueCaption("Touch a value to edit. Return saves, the cross cancels.")
        LabeledContent("Title") {
            MonarchEditableCell("Title", value: $title) { _ in try await Task.sleep(for: .milliseconds(600)) }
        }
        LabeledContent("Quantity") {
            MonarchEditableCell("Quantity", value: $quantity, kind: .number, minimum: 1, maximum: 99, isRequired: true)
            { _ in }
        }

        CatalogueCaption("Choosing saves at once")
        LabeledContent("Status") {
            MonarchEditableCell(
                "Status",
                value: $status,
                kind: .select,
                options: [
                    MonarchEditableCellOption("Open", value: "open"),
                    MonarchEditableCellOption("In review", value: "review"),
                    MonarchEditableCellOption("Done", value: "done"),
                ]
            ) { _ in try await Task.sleep(for: .milliseconds(600)) }
        }
        LabeledContent("Due") {
            MonarchEditableCell("Due", value: $due, kind: .date, placeholder: "No due date") { _ in }
        }

        CatalogueCaption("A rule of the app, and a save that always fails")
        LabeledContent("Owner") {
            MonarchEditableCell(
                "Owner",
                value: $owner,
                validate: { value in
                    if case .text(let text) = value, text.count < 2 { return "Use at least 2 characters." }
                    return nil
                },
                onCommit: { _ in
                    try await Task.sleep(for: .milliseconds(600))
                    throw Conflict()
                }
            )
        }

        CatalogueCaption("Disabled")
        LabeledContent("Plan") {
            MonarchEditableCell("Plan", value: .constant(.text("Enterprise"))) { _ in }
                .disabled(true)
        }
    }
}

import MonarchUI
import SwiftUI

struct MonarchPreviewRowsSection: CataloguePage {
    static let title = "Preview rows"

    @State private var filter = MonarchPreviewRowsFilter.all

    private let columns = [
        MonarchPreviewRowsColumn("Name", id: "name"),
        MonarchPreviewRowsColumn("Email", id: "email"),
        MonarchPreviewRowsColumn("Seats", id: "seats", isNumeric: true),
    ]

    private let rows = [
        MonarchPreviewRow(row: 1, values: ["name": "Ada Lovelace", "email": "ada@example.com", "seats": "3"]),
        MonarchPreviewRow(
            row: 2,
            values: ["name": "Alan Turing", "email": "alan@", "seats": "1"],
            errors: [MonarchPreviewRowIssue("Not a valid address", field: "Email")]
        ),
        MonarchPreviewRow(
            row: 3,
            values: ["name": "Grace Hopper", "seats": "2"],
            warnings: [MonarchPreviewRowIssue("Missing, the contact is imported without one", field: "Email")]
        ),
        MonarchPreviewRow(
            row: 4,
            values: ["name": "Ada Lovelace", "email": "ada@example.com", "seats": "3"],
            status: .duplicate,
            note: "Matches an existing contact"
        ),
        MonarchPreviewRow(row: 5, values: ["name": "Katherine Johnson", "email": "kj@example.com", "seats": "5"]),
    ]

    var body: some View {
        CatalogueCaption("An import with every result")
        MonarchPreviewRows(
            "Preview of contacts.csv",
            rows: rows,
            columns: columns,
            filter: $filter,
            pageSize: 3
        )
        CatalogueCaption("A file with no data rows")
        MonarchPreviewRows("Preview of empty.csv", rows: [], columns: columns)
    }
}

import MonarchUI
import SwiftUI

struct MonarchTableBlockSection: CataloguePage {
    static let title = "Table block"

    @State private var table: MonarchTableBlockData? = MonarchTableBlockData(rows: [
        ["Plan", "Seats", "Price"],
        ["Team", "10", "12"],
        ["Business", "Unlimited", "30"],
    ])

    private let comparison = MonarchTableBlockData(rows: [
        ["Feature", "Starter", "Team", "Business", "Enterprise"],
        ["Audit log", "", "30 days", "1 year", "Unlimited"],
        ["Support", "Community", "Email", "Priority email and chat", "A named contact"],
    ])

    var body: some View {
        CatalogueCaption("Read-only. Scrolls sideways when it is wider than the page.")
        MonarchTableBlock("Plan comparison", data: comparison)

        CatalogueCaption("Editable. The menu acts on the cell that last had focus.")
        if let binding = Binding($table) {
            MonarchTableBlock("Plans", data: binding) {
                table = nil
            }
        } else {
            MonarchButton("Insert a table", systemImage: "tablecells") {
                table = MonarchTableBlockData()
            }
        }
    }
}

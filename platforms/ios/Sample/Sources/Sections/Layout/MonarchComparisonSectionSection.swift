import MonarchUI
import SwiftUI

struct MonarchComparisonSectionSection: CataloguePage {
    static let title = "Comparison section"

    var body: some View {
        MonarchComparisonSection(
            "How it compares",
            eyebrow: "Compare",
            description: "Neutral sample rows for documentation.",
            columns: [
                MonarchComparisonColumn("product", name: "Product A", badge: "This product", isHighlighted: true),
                MonarchComparisonColumn("sheets", name: "Spreadsheets"),
            ],
            rows: [
                MonarchPricingFeature(
                    "history",
                    label: "Change history",
                    values: ["product": true, "sheets": "Per file"]
                ),
                MonarchPricingFeature("assign", label: "Assign an owner", values: ["product": true, "sheets": false]),
            ],
            bestFor: [
                MonarchComparisonBestFor(
                    "product",
                    title: "Product A suits",
                    description: "Teams that share and hand over work every day.",
                    isHighlighted: true
                )
            ],
            note: "Sample note: say when the comparison was checked and against which public sources."
        ) {
            MonarchButton("Try Product A", variant: .primary) {}
        }
    }
}

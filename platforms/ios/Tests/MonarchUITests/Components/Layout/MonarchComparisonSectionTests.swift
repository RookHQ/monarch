import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchComparisonSectionFixtures: ComponentFixtures {
    static let id = "comparison-section"
    static let columns = [
        MonarchComparisonColumn("product", name: "Product A", badge: "This product", isHighlighted: true),
        MonarchComparisonColumn("sheets", name: "Spreadsheets"),
        MonarchComparisonColumn("email", name: "Email threads"),
    ]
    static let rows = [
        MonarchPricingFeature(
            "history",
            label: "Change history",
            values: ["product": true, "sheets": "Per file", "email": false]
        ),
        MonarchPricingFeature(
            "assign",
            label: "Assign an owner",
            values: ["product": true, "sheets": false, "email": false]
        ),
        MonarchPricingFeature(
            "roles",
            label: "Roles and permissions",
            group: "Access",
            values: ["product": true, "sheets": "Per file", "email": false]
        ),
    ]
    static let fixtures = [
        Fixture("product beside alternatives") {
            MonarchComparisonSection(
                "How it compares",
                eyebrow: "Compare",
                description: "Neutral sample rows for documentation.",
                columns: columns,
                rows: rows,
                bestFor: [
                    MonarchComparisonBestFor(
                        "product",
                        title: "Product A suits",
                        description: "Teams that share and hand over work every day.",
                        isHighlighted: true
                    ),
                    MonarchComparisonBestFor(
                        "sheets",
                        title: "Spreadsheets suit",
                        description: "One-off lists kept by one person."
                    ),
                ],
                note: "Sample note: say when the comparison was checked."
            ) {
                MonarchButton("Try Product A", variant: .primary) {}
            }
        },
        Fixture("table only, centred") {
            MonarchComparisonSection(
                "Before and after",
                alignment: .center,
                columns: Array(columns.prefix(2)),
                rows: Array(rows.prefix(2))
            )
        },
        Fixture("three best-for cards stack") {
            MonarchComparisonSection(
                "Who it suits",
                columns: Array(columns.prefix(1)),
                rows: Array(rows.prefix(1)),
                bestFor: columns.map { MonarchComparisonBestFor($0.id, title: $0.name) }
            )
        },
    ]
}

@Suite("MonarchComparisonSection")
struct MonarchComparisonSectionTests {
    @Test("A highlighted card uses the accent's subtle fill and border")
    func cardColours() {
        #expect(MonarchComparisonSectionTokens.cardFill(isHighlighted: true) == \MonarchColorTokens.accentSubtle)
        #expect(MonarchComparisonSectionTokens.cardFill(isHighlighted: false) == \MonarchColorTokens.bgSurface)
        #expect(MonarchComparisonSectionTokens.cardBorder(isHighlighted: true) == \MonarchColorTokens.accentBorder)
        #expect(MonarchComparisonSectionTokens.cardBorder(isHighlighted: false) == \MonarchColorTokens.borderSubtle)
    }

    @Test("Up to two cards sit side by side, below the accessibility sizes")
    func cardsInRow() {
        #expect(MonarchComparisonSectionTokens.placesCardsInRow(count: 2, isAccessibilitySize: false))
        #expect(!MonarchComparisonSectionTokens.placesCardsInRow(count: 3, isAccessibilitySize: false))
        #expect(!MonarchComparisonSectionTokens.placesCardsInRow(count: 2, isAccessibilitySize: true))
    }

    @Test("The table scrolls below a readable width per column")
    func tableWidth() {
        #expect(
            MonarchComparisonSectionTokens.tableWidth(columns: 3)
                > MonarchComparisonSectionTokens.tableWidth(columns: 2)
        )
        #expect(
            MonarchComparisonSectionTokens.tableWidth(columns: 0) == MonarchPricingTableTokens.featureColumnMinWidth
        )
    }

    @Test("A column keeps its data")
    func column() {
        let column = MonarchComparisonSectionFixtures.columns[0]
        #expect(column.badge == "This product")
        #expect(column.isHighlighted)
        #expect(!MonarchComparisonSectionFixtures.columns[1].isHighlighted)
    }

    @Test("Cell values are read as words, not as the check or dash")
    func valueText() {
        let labels = MonarchPricingTableLabels()
        #expect(MonarchPricingTableTokens.valueText(.included, labels: labels) == "Included")
        #expect(MonarchPricingTableTokens.valueText(.notIncluded, labels: labels) == "Not included")
        #expect(MonarchPricingTableTokens.valueText(.text("Per file"), labels: labels) == "Per file")
    }
}

import MonarchUI
import SwiftUI

struct MonarchQueryBuilderSection: CataloguePage {
    static let title = "Query builder"

    @Environment(\.monarchTheme) private var theme
    @State private var query = MonarchQueryGroup(
        id: "root",
        rules: [MonarchCondition(id: "r1", field: "status", operator: .equals, value: .option("open"))],
        groups: [
            MonarchQueryGroup(
                id: "g1",
                combinator: .or,
                rules: [
                    MonarchCondition(id: "r2", field: "amount", operator: .atLeast, value: .number(500)),
                    MonarchCondition(id: "r3", field: "flagged", operator: .isTrue),
                ]
            )
        ]
    )
    @State private var showsErrors = false

    private let fields = [
        MonarchConditionField(
            id: "status",
            label: "Status",
            kind: .select([
                MonarchComboboxOption(id: "open", label: "Open"),
                MonarchComboboxOption(id: "pending", label: "Pending"),
                MonarchComboboxOption(id: "closed", label: "Closed"),
            ])
        ),
        MonarchConditionField(id: "title", label: "Title", kind: .text),
        MonarchConditionField(id: "amount", label: "Amount", kind: .number, prompt: "Amount"),
        MonarchConditionField(id: "created", label: "Created", kind: .date),
        MonarchConditionField(id: "flagged", label: "Flagged", kind: .boolean),
    ]

    private var sample: [String: MonarchConditionValue] {
        ["status": .option("open"), "amount": .number(120), "flagged": .boolean(true)]
    }

    var body: some View {
        MonarchQueryBuilder(query: $query, fields: fields, showsErrors: showsErrors)
        Text(
            query.matches(sample)
                ? "Sample record (open, amount 120, flagged) matches."
                : "Sample record (open, amount 120, flagged) does not match."
        )
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors.textMuted)
        MonarchButton("Save query", variant: .primary) {
            showsErrors = !query.problems(fields: fields).isEmpty
        }
    }
}

import MonarchUI
import SwiftUI

struct MonarchConditionEditorSection: CataloguePage {
    static let title = "Condition editor"

    private static let fields = [
        MonarchConditionField(
            id: "priority",
            label: "Priority",
            kind: .select([
                MonarchComboboxOption(id: "urgent", label: "Urgent"),
                MonarchComboboxOption(id: "high", label: "High"),
                MonarchComboboxOption(id: "normal", label: "Normal"),
            ])
        ),
        MonarchConditionField(id: "subject", label: "Subject", kind: .text, prompt: "Text"),
        MonarchConditionField(id: "hours", label: "Hours since created", kind: .number, prompt: "Hours"),
        MonarchConditionField(id: "due", label: "Due date", kind: .date),
        MonarchConditionField(id: "vip", label: "VIP customer", kind: .boolean),
    ]

    @State private var conditions = [
        MonarchCondition(id: "c1", field: "priority", operator: .equals, value: .option("urgent")),
        MonarchCondition(id: "c2", field: "hours", operator: .atLeast, value: .number(4)),
    ]
    @State private var match = MonarchConditionMatch.all
    @State private var showsErrors = false

    private var record: [String: MonarchConditionValue] {
        [
            "priority": .option("urgent"), "subject": .text("Refund request"), "hours": .number(6),
            "vip": .boolean(false),
        ]
    }

    var body: some View {
        CatalogueCaption("Escalate a ticket when")
        MonarchConditionEditor(
            conditions: $conditions,
            fields: Self.fields,
            match: $match,
            showsErrors: showsErrors
        )
        Toggle("Show what is missing", isOn: $showsErrors).monarchTextStyle(.body)
        let combinator: MonarchQueryCombinator = match == .all ? .and : .or
        let matches = MonarchQueryGroup(id: "preview", combinator: combinator, rules: conditions).matches(record)
        Text(matches ? "The sample ticket matches." : "The sample ticket does not match.")
            .monarchTextStyle(.caption)
        CatalogueCaption("Empty")
        MonarchConditionEditor(conditions: .constant([]), fields: Self.fields)
    }
}

import Foundation
import SwiftUI

/// Builds a filter query from AND and OR groups of field, operator and value
/// rules, nested up to a set depth.
///
/// ```swift
/// @State private var query = MonarchQueryGroup(id: "root", rules: [
///     MonarchCondition(id: "r1", field: "status", operator: .equals, value: .option("open")),
/// ])
///
/// MonarchQueryBuilder(query: $query, fields: [
///     MonarchConditionField(id: "status", label: "Status", kind: .select([
///         MonarchComboboxOption(id: "open", label: "Open"),
///         MonarchComboboxOption(id: "closed", label: "Closed"),
///     ])),
///     MonarchConditionField(id: "amount", label: "Amount", kind: .number),
/// ])
/// ```
///
/// Each group has a segmented AND / OR `Picker`, its rules and its nested
/// groups. On a phone each rule stacks vertically: a field menu, an operator
/// menu (both `Picker` with the menu style) and a value input that suits the
/// field (text, number, `DatePicker`, or a menu of choices). Check the query
/// with ``MonarchQueryGroup/problems(fields:)`` before saving and pass
/// `showsErrors` to show what is missing; test records with
/// ``MonarchQueryGroup/matches(_:calendar:)``.
///
/// Accessibility: the builder is a container named "Query"; each group is a
/// container named after it ("Group 2.1") and each rule a container named
/// "Group 2.1, rule 1". Every menu, field and button carries its group and
/// position ("Top group, rule 1 field", "Remove top group, rule 1"). A shown
/// problem is text with an icon and is read as the rule's hint. Touch
/// targets are at least 44 points.
public struct MonarchQueryBuilder: View {
    @Binding private var query: MonarchQueryGroup
    private let context: MonarchQueryBuilderContext

    /// A query builder.
    ///
    /// - Parameters:
    ///   - query: The top group of the query.
    ///   - fields: The fields a rule can test.
    ///   - maxDepth: How many levels of groups are allowed, counting the top group. 1 turns off nested groups.
    ///   - maxRulesPerGroup: The most rules in one group.
    ///   - showsErrors: Shows what is missing under each incomplete rule, for example after a save attempt.
    ///   - labels: Replacements for the built-in strings.
    ///   - makeID: Makes the id of a new rule or group.
    public init(
        query: Binding<MonarchQueryGroup>,
        fields: [MonarchConditionField],
        maxDepth: Int = 3,
        maxRulesPerGroup: Int = 20,
        showsErrors: Bool = false,
        labels: MonarchQueryBuilderLabels = MonarchQueryBuilderLabels(),
        makeID: @escaping @MainActor () -> String = { UUID().uuidString }
    ) {
        _query = query
        self.context = MonarchQueryBuilderContext(
            fields: fields,
            maxDepth: max(maxDepth, 1),
            maxRulesPerGroup: max(maxRulesPerGroup, 0),
            showsErrors: showsErrors,
            text: MonarchQueryBuilderText(labels: labels),
            makeID: makeID
        )
    }

    /// The content of the view.
    public var body: some View {
        MonarchQueryBuilderGroupView(group: $query, path: [1], context: context, onRemove: nil)
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .contain)
            .accessibilityLabel(context.text.label)
    }
}

/// What every group and rule of one ``MonarchQueryBuilder`` shares.
struct MonarchQueryBuilderContext {
    let fields: [MonarchConditionField]
    let maxDepth: Int
    let maxRulesPerGroup: Int
    let showsErrors: Bool
    let text: MonarchQueryBuilderText
    let makeID: @MainActor () -> String

    func field(_ id: String) -> MonarchConditionField? {
        fields.first { $0.id == id }
    }
}

#if DEBUG
    #Preview("MonarchQueryBuilder") {
        let fields = [
            MonarchConditionField(
                id: "status",
                label: "Status",
                kind: .select([
                    MonarchComboboxOption(id: "open", label: "Open"),
                    MonarchComboboxOption(id: "closed", label: "Closed"),
                ])
            ),
            MonarchConditionField(id: "title", label: "Title", kind: .text),
            MonarchConditionField(id: "amount", label: "Amount", kind: .number, prompt: "Amount"),
            MonarchConditionField(id: "flagged", label: "Flagged", kind: .boolean),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchQueryBuilder(
                    query: .constant(
                        MonarchQueryGroup(
                            id: "root",
                            rules: [
                                MonarchCondition(id: "r1", field: "status", operator: .equals, value: .option("open"))
                            ],
                            groups: [
                                MonarchQueryGroup(
                                    id: "g1",
                                    combinator: .or,
                                    rules: [
                                        MonarchCondition(
                                            id: "r2",
                                            field: "amount",
                                            operator: .atLeast,
                                            value: .number(500)
                                        ),
                                        MonarchCondition(id: "r3", field: "flagged", operator: .isTrue),
                                    ]
                                )
                            ]
                        )
                    ),
                    fields: fields
                )
                MonarchQueryBuilder(
                    query: .constant(MonarchQueryGroup(id: "v", rules: [MonarchCondition(id: "e", field: nil)])),
                    fields: fields,
                    maxDepth: 1,
                    showsErrors: true
                )
            }
        }
    }
#endif

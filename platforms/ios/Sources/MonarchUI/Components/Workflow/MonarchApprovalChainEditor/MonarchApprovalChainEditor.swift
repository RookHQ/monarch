import Foundation
import SwiftUI

/// Edits the ordered steps of an approval chain: who approves each step (a
/// kind of approver, plus a role or person where the kind needs one), an
/// optional label, and the order.
///
/// ```swift
/// @State private var steps = [MonarchApprovalChainStep(id: "s1", approver: "manager")]
///
/// MonarchApprovalChainEditor(steps: $steps, approverTypes: [
///     MonarchApprovalApproverType(id: "manager", label: "Reporting manager"),
///     MonarchApprovalApproverType(id: "role", label: "Anyone with a role", targets: [
///         MonarchComboboxOption(id: "finance", label: "Finance team"),
///     ], targetLabel: "Role"),
/// ])
/// ```
///
/// Each step is a card with its title, a Reorder menu (Move up, Move down),
/// a remove button, an approver menu (`Picker` with the menu style), a
/// target menu when the approver type has targets, and a label field. Check
/// the chain with ``validate(_:approverTypes:)`` before saving and pass
/// `showsErrors` to show which steps still need a target. The app owns the
/// chain's other settings and saving.
///
/// Accessibility: the list is a container named "Steps, in order"; each
/// step is a container read as "Step 1, Finance check (Role: Finance
/// team)" with Move up and Move down as accessibility actions, and every
/// move is announced. The remove button is named "Remove step 1" and is
/// disabled at `minSteps`. A missing target is an error with an icon and
/// text. Touch targets are at least 44 points.
public struct MonarchApprovalChainEditor: View {
    @Binding private var steps: [MonarchApprovalChainStep]
    private let approverTypes: [MonarchApprovalApproverType]
    private let description: String?
    private let minSteps: Int
    private let maxSteps: Int
    private let showsLabels: Bool
    private let showsErrors: Bool
    private let labels: MonarchApprovalChainEditorLabels
    private let makeID: @MainActor () -> String

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// An approval chain editor.
    ///
    /// - Parameters:
    ///   - steps: The steps, in order.
    ///   - approverTypes: The kinds of approver a step can name. New steps use the first.
    ///   - description: Help under the heading, such as how skipped steps work.
    ///   - minSteps: The fewest steps; remove is disabled at this count.
    ///   - maxSteps: The most steps; the add button is replaced by a note at this count.
    ///   - showsLabels: Shows the optional label field on each step.
    ///   - showsErrors: Shows which steps still need a target, for example after a save attempt.
    ///   - labels: Replacements for the built-in strings.
    ///   - makeID: Makes the id of a new step.
    public init(
        steps: Binding<[MonarchApprovalChainStep]>,
        approverTypes: [MonarchApprovalApproverType],
        description: String? = nil,
        minSteps: Int = 1,
        maxSteps: Int = 10,
        showsLabels: Bool = true,
        showsErrors: Bool = false,
        labels: MonarchApprovalChainEditorLabels = MonarchApprovalChainEditorLabels(),
        makeID: @escaping @MainActor () -> String = { UUID().uuidString }
    ) {
        _steps = steps
        self.approverTypes = approverTypes
        self.description = description
        self.minSteps = max(minSteps, 0)
        self.maxSteps = max(maxSteps, 1)
        self.showsLabels = showsLabels
        self.showsErrors = showsErrors
        self.labels = labels
        self.makeID = makeID
    }

    /// The ids of the steps whose approver type needs a target (a role or a
    /// person) and has none chosen. Empty when the chain can be saved.
    ///
    /// - Parameters:
    ///   - steps: The steps.
    ///   - approverTypes: The approver types the steps name.
    /// - Returns: The ids of the incomplete steps.
    nonisolated public static func validate(
        _ steps: [MonarchApprovalChainStep],
        approverTypes: [MonarchApprovalApproverType]
    ) -> Set<String> {
        MonarchApprovalChainEditorTokens.invalidSteps(steps, types: approverTypes)
    }

    /// The chain in words, such as "Reporting manager → Finance check
    /// (Role: Finance team)", for list rows and confirmations.
    ///
    /// - Parameters:
    ///   - steps: The steps.
    ///   - approverTypes: The approver types the steps name.
    ///   - separator: Between steps.
    /// - Returns: The summary.
    nonisolated public static func summarize(
        _ steps: [MonarchApprovalChainStep],
        approverTypes: [MonarchApprovalApproverType],
        separator: String = " → "
    ) -> String {
        MonarchApprovalChainEditorTokens.summary(steps, types: approverTypes, separator: separator)
    }

    private typealias Tokens = MonarchApprovalChainEditorTokens

    /// The content of the view.
    public var body: some View {
        let invalid = showsErrors ? Tokens.invalidSteps(steps, types: approverTypes) : []
        VStack(alignment: .leading, spacing: Tokens.listSpacing) {
            Text(labels.legend ?? MonarchApprovalChainEditorStrings.legend)
                .monarchTextStyle(.subheading)
                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                .accessibilityAddTraits(.isHeader)
            if let description {
                Text(description)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.mutedText])
            }
            ForEach(Array(steps.enumerated()), id: \.element.id) { index, step in
                MonarchApprovalChainEditorStepCard(
                    step: binding(step),
                    position: index + 1,
                    count: steps.count,
                    approverTypes: approverTypes,
                    canRemove: Tokens.canRemove(count: steps.count, minSteps: minSteps),
                    showsLabels: showsLabels,
                    isInvalid: invalid.contains(step.id),
                    labels: labels,
                    onMove: { move(step.id, by: $0) },
                    onRemove: { steps.removeAll { $0.id == step.id } }
                )
            }
            if Tokens.canAdd(count: steps.count, maxSteps: maxSteps) {
                Button(labels.addStep ?? MonarchApprovalChainEditorStrings.addStep, systemImage: Tokens.addSymbol) {
                    steps.append(Tokens.newStep(types: approverTypes, id: makeID()))
                }
                .buttonStyle(.monarch(.secondary, size: .sm))
                .disabled(approverTypes.isEmpty)
            } else {
                Text(labels.maxReached?(maxSteps) ?? MonarchApprovalChainEditorStrings.maxReached(maxSteps))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: Tokens.mutedText])
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.legend ?? MonarchApprovalChainEditorStrings.legend)
    }

    private func move(_ id: String, by offset: Int) {
        let next = Tokens.moving(steps, id: id, by: offset)
        guard next != steps, let position = next.firstIndex(where: { $0.id == id }) else { return }
        steps = next
        AccessibilityNotification.Announcement(
            labels.moved?(position + 1) ?? MonarchApprovalChainEditorStrings.moved(position + 1)
        )
        .post()
    }

    private func binding(_ step: MonarchApprovalChainStep) -> Binding<MonarchApprovalChainStep> {
        Binding(
            get: { steps.first { $0.id == step.id } ?? step },
            set: { new in
                if let index = steps.firstIndex(where: { $0.id == step.id }) { steps[index] = new }
            }
        )
    }
}

#if DEBUG
    #Preview("MonarchApprovalChainEditor") {
        MonarchPreviewMatrix {
            MonarchApprovalChainEditor(
                steps: .constant([
                    MonarchApprovalChainStep(id: "1", approver: "manager"),
                    MonarchApprovalChainStep(id: "2", approver: "role", label: "Finance check"),
                ]),
                approverTypes: [
                    MonarchApprovalApproverType(id: "manager", label: "Reporting manager"),
                    MonarchApprovalApproverType(
                        id: "role",
                        label: "Anyone with a role",
                        targets: [MonarchComboboxOption(id: "finance", label: "Finance team")],
                        targetLabel: "Role"
                    ),
                ],
                showsErrors: true
            )
        }
    }
#endif

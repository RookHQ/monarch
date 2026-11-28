import SwiftUI

/// One step of a ``MonarchApprovalChainEditor``: its title, reorder and remove, approver, target and label.
struct MonarchApprovalChainEditorStepCard: View {
    @Binding var step: MonarchApprovalChainStep
    let position: Int
    let count: Int
    let approverTypes: [MonarchApprovalApproverType]
    let canRemove: Bool
    let showsLabels: Bool
    let isInvalid: Bool
    let labels: MonarchApprovalChainEditorLabels
    let onMove: (Int) -> Void
    let onRemove: () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    private typealias Tokens = MonarchApprovalChainEditorTokens

    private var type: MonarchApprovalApproverType? { Tokens.type(of: step, in: approverTypes) }
    private var moveUp: String { labels.moveUp ?? MonarchApprovalChainEditorStrings.moveUp }
    private var moveDown: String { labels.moveDown ?? MonarchApprovalChainEditorStrings.moveDown }

    var body: some View {
        MonarchCard {
            VStack(alignment: .leading, spacing: Tokens.stepSpacing) {
                header
                MonarchSelectField(
                    labels.approver ?? MonarchApprovalChainEditorStrings.approver,
                    selection: Binding(
                        get: { step.approver },
                        set: { step = Tokens.changingApprover(step, to: $0, types: approverTypes) }
                    ),
                    valueText: type?.label ?? step.approver
                ) {
                    if type == nil {
                        Text(step.approver).tag(step.approver)
                    }
                    ForEach(approverTypes) { option in
                        Text(option.label).tag(option.id)
                    }
                }
                if let type, let targets = type.targets {
                    targetPicker(type: type, targets: targets)
                }
                if showsLabels {
                    MonarchTextField(
                        labels.label ?? MonarchApprovalChainEditorStrings.label,
                        text: Binding(
                            get: { step.label ?? "" },
                            set: { step.label = Tokens.cleanedLabel($0) }
                        ),
                        prompt: labels.labelPlaceholder ?? MonarchApprovalChainEditorStrings.labelPlaceholder
                    )
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(
            Tokens.accessibilityLabel(position: position, step: step, types: approverTypes, labels: labels)
        )
        .accessibilityActions {
            if isEnabled {
                if position > 1 { Button(moveUp) { onMove(-1) } }
                if position < count { Button(moveDown) { onMove(1) } }
            }
        }
    }

    private var header: some View {
        // As the web: a grip, the step number, then remove and the up and down arrows.
        HStack(alignment: .center) {
            if count > 1 {
                MonarchDragHandle(
                    labels.reorder?(position) ?? MonarchApprovalChainEditorStrings.reorder(position),
                    actionLabels: [.up: moveUp, .down: moveDown]
                ) { direction in
                    switch direction {
                    case .up, .backward: if position > 1 { onMove(-1) }
                    case .down, .forward: if position < count { onMove(1) }
                    }
                }
            }
            Text(Tokens.stepTitle(position, labels: labels))
                .monarchTextStyle(.caption)
                .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
                .accessibilityAddTraits(.isHeader)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: onRemove) {
                Label(
                    labels.removeStep?(position) ?? MonarchApprovalChainEditorStrings.removeStep(position),
                    systemImage: Tokens.removeSymbol
                )
                .labelStyle(.iconOnly)
            }
            .buttonStyle(.monarch(.ghost, size: .sm))
            .disabled(!canRemove)
            if count > 1 {
                Button {
                    onMove(-1)
                } label: {
                    Label(moveUp, systemImage: "chevron.up").labelStyle(.iconOnly)
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
                .disabled(position == 1)
                Button {
                    onMove(1)
                } label: {
                    Label(moveDown, systemImage: "chevron.down").labelStyle(.iconOnly)
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
                .disabled(position == count)
            }
        }
    }

    private func targetPicker(type: MonarchApprovalApproverType, targets: [MonarchComboboxOption]) -> some View {
        let title = type.targetLabel ?? labels.target ?? MonarchApprovalChainEditorStrings.target
        return MonarchSelectField(
            title,
            selection: Binding(get: { step.target ?? "" }, set: { step.target = $0.isEmpty ? nil : $0 }),
            valueText: targets.first { $0.id == step.target }?.label,
            prompt: labels.targetPlaceholder ?? MonarchApprovalChainEditorStrings.targetPlaceholder,
            error: isInvalid ? labels.targetRequired ?? MonarchApprovalChainEditorStrings.targetRequired : nil,
            isRequired: true
        ) {
            Text(labels.targetPlaceholder ?? MonarchApprovalChainEditorStrings.targetPlaceholder).tag("")
            ForEach(targets) { option in
                Text(option.label).tag(option.id).disabled(option.isDisabled)
            }
        }
    }
}

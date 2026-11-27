import SwiftUI

/// One action of a ``MonarchActionEditor``: its title, reorder and remove, type menu and inputs.
struct MonarchActionEditorCard: View {
    @Binding var action: MonarchRuleAction
    let position: Int
    let count: Int
    let types: [MonarchActionType]
    let isOrdered: Bool
    let showsErrors: Bool
    let text: MonarchActionEditorText
    let onMove: (Int) -> Void
    let onRemove: () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    private var type: MonarchActionType? { types.first { $0.id == action.type } }

    var body: some View {
        MonarchCard {
            VStack(alignment: .leading, spacing: MonarchActionEditorTokens.actionSpacing) {
                header
                typePicker
                if let type, type.isDisabled {
                    MonarchBanner(
                        message: MonarchActionEditorTokens.unavailableMessage(type, text: text),
                        tone: .warning
                    )
                }
                if let type {
                    ForEach(MonarchActionEditorTokens.visibleParameters(type, values: action.parameters)) { parameter in
                        MonarchActionEditorParameterInput(
                            parameter: parameter,
                            value: Binding(
                                get: { action.parameters[parameter.key] ?? parameter.blankValue },
                                set: { action.parameters[parameter.key] = $0 }
                            ),
                            error: showsErrors
                                ? MonarchActionEditorTokens.error(parameter, values: action.parameters, text: text)
                                : nil,
                            text: text
                        )
                    }
                }
                if showsErrors, let problem = MonarchActionEditorTokens.customProblem(type, values: action.parameters) {
                    HStack(alignment: .firstTextBaseline, spacing: MonarchActionEditorTokens.messageIconSpacing) {
                        MonarchIcon(systemName: MonarchActionEditorTokens.problemSymbol, size: .sm)
                        Text(problem).monarchTextStyle(.caption)
                    }
                    .foregroundStyle(theme.colors.dangerStrong)
                }
                if isOrdered { footer }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(
            MonarchActionEditorTokens.accessibilityLabel(position: position, action: action, type: type, text: text)
        )
        .accessibilityActions {
            if isOrdered && isEnabled {
                if position > 1 { Button(text.moveUp) { onMove(-1) } }
                if position < count { Button(text.moveDown) { onMove(1) } }
            }
        }
    }

    /// True when an error shows on this action, which the web marks by colouring the title.
    private var hasError: Bool {
        guard showsErrors, let type else { return false }
        return MonarchActionEditorTokens.visibleParameters(type, values: action.parameters).contains {
            MonarchActionEditorTokens.error($0, values: action.parameters, text: text) != nil
        } || MonarchActionEditorTokens.customProblem(type, values: action.parameters) != nil
    }

    /// As on the web: the grip and a small title on the left, remove on the right.
    private var header: some View {
        HStack(alignment: .center) {
            if isOrdered {
                MonarchDragHandle(text.reorder(position), onMove: { onMove($0 == .up ? -1 : 1) })
            }
            Text(text.action(position))
                .monarchTextStyle(.caption)
                .foregroundStyle(
                    !isEnabled
                        ? theme.colors.textDisabled : hasError ? theme.colors.dangerStrong : theme.colors.textMuted
                )
                .accessibilityAddTraits(.isHeader)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: onRemove) {
                MonarchIcon(systemName: MonarchActionEditorTokens.removeSymbol, size: .sm)
                    .frame(width: 32, height: MonarchAccessibility.minimumHitTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(text.remove(position))
        }
    }

    /// One step up and one step down, at the foot of the card as on the web.
    private var footer: some View {
        HStack(spacing: 0) {
            Spacer(minLength: 0)
            Button {
                onMove(-1)
            } label: {
                MonarchIcon(systemName: "chevron.up", size: .sm)
                    .frame(width: 32, height: MonarchAccessibility.minimumHitTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(text.moveUp)
            .disabled(position == 1)
            Button {
                onMove(1)
            } label: {
                MonarchIcon(systemName: "chevron.down", size: .sm)
                    .frame(width: 32, height: MonarchAccessibility.minimumHitTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(text.moveDown)
            .disabled(position == count)
        }
    }

    private var typePicker: some View {
        MonarchSelectField(
            text.type,
            selection: Binding(
                get: { action.type },
                set: { id in action = MonarchRuleAction(id: action.id, type: types.first { $0.id == id }) }
            ),
            valueText: type?.label ?? (action.type.isEmpty ? nil : text.unknownType(action.type)),
            prompt: text.choose,
            helper: type?.description
        ) {
            if type == nil {
                Text(action.type.isEmpty ? text.choose : text.unknownType(action.type)).tag(action.type)
            }
            ForEach(MonarchActionEditorTokens.sections(types), id: \.title) { section in
                if let title = section.title {
                    Section(title) { typeOptions(section.types) }
                } else {
                    typeOptions(section.types)
                }
            }
        }
    }

    private func typeOptions(_ list: [MonarchActionType]) -> some View {
        ForEach(list) { option in
            Text(MonarchActionEditorTokens.menuLabel(option))
                .tag(option.id)
                .disabled(option.isDisabled && option.id != action.type)
        }
    }
}

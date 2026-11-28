import SwiftUI

/// The list of models shown in a sheet by ``MonarchAiModelPicker``: one
/// section per provider, a row per model.
struct MonarchAiModelPickerList: View {
    let models: [MonarchAiModelOption]
    let selection: String?
    let canConfigureKey: Bool
    let labels: MonarchAiModelPickerLabels
    let onChoose: @MainActor (MonarchAiModelOption) -> Void
    let onDone: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    private typealias Tokens = MonarchAiModelPickerTokens

    var body: some View {
        do {
            List {
                Group {
                    ForEach(Tokens.groups(models), id: \.provider) { group in
                        Section {
                            ForEach(group.models) { model in
                                row(model)
                            }
                        } header: {
                            Text(group.provider)
                                .monarchTextStyle(.caption, weight: Tokens.nameWeight)
                                .foregroundStyle(theme.colors.textMuted)
                        }
                    }

                }
                .monarchRowsOnCanvas()
            }
            .listStyle(.plain)
            .monarchCanvas()
            .accessibilityLabel(labels.label ?? MonarchAiModelPickerStrings.label)
            .accessibilityAction(.escape) { onDone() }
        }
    }

    private func row(_ model: MonarchAiModelOption) -> some View {
        let state = Tokens.state(model, canConfigureKey: canConfigureKey)
        let isSelected = model.id == selection
        // A model that only needs a key keeps its normal emphasis, as on the web; only a disabled one is dimmed.
        let roles = Tokens.textRoles(state: model.isDisabled ? .blocked : .available)
        return Button {
            onChoose(model)
        } label: {
            HStack(alignment: .firstTextBaseline, spacing: Tokens.rowSpacing) {
                Group {
                    if isSelected {
                        MonarchIcon(systemName: Tokens.checkSymbol, size: .sm)
                            .foregroundStyle(theme.colors.aiStrong)
                    }
                }
                .frame(width: Tokens.checkWidth)
                VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
                    MonarchFlowLayout(spacing: Tokens.badgeSpacing) {
                        Text(model.name)
                            .monarchTextStyle(.body, weight: Tokens.nameWeight)
                            .foregroundStyle(theme.colors[role: roles.name])
                        if let key = Tokens.keyText(model, labels: labels) {
                            MonarchBadge(
                                key,
                                tone: model.isKeyConfigured ? .success : .warning,
                                systemImage: model.isKeyConfigured ? Tokens.keySymbol : Tokens.lockSymbol
                            )
                        }
                    }
                    if let description = model.description, !description.isEmpty {
                        Text(description)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: roles.detail])
                    }
                    if !model.capabilities.isEmpty {
                        MonarchFlowLayout(spacing: Tokens.badgeSpacing) {
                            ForEach(model.capabilities, id: \.self) { capability in
                                MonarchBadge(Tokens.capabilityName(capability, labels: labels), tone: .neutral)
                            }
                        }
                    }
                    if let meta = Tokens.metaText(model, locale: locale, labels: labels) {
                        Text(meta)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: roles.detail])
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .monarchMinimumHitTarget()
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .listRowBackground(isSelected ? theme.colors.aiSubtle : nil)
        .disabled(model.isDisabled)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.rowLabel(model, labels: labels))
        .accessibilityValue(Tokens.rowValue(model, state: state, locale: locale, labels: labels))
        .accessibilityHint(Tokens.rowHint(state: state, labels: labels) ?? "")
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

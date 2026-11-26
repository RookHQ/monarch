import SwiftUI

/// One on/off setting that saves on its own, with its saving, saved and
/// failed state on the row.
///
/// The switch moves at once and `save` runs. While it runs, a labelled
/// spinner shows and further toggles are ignored. When it returns, the
/// binding takes the new value and "Saved" shows for a moment. When it
/// throws, the switch goes back, the error shows under the row and a Retry
/// button saves the failed value again.
///
/// ```swift
/// MonarchSwitchSettingsRow("Weekly summary", isOn: $weekly, description: "Sent on Mondays.") { isOn in
///     try await api.updatePreference("weekly", isOn)
/// }
/// ```
///
/// Use rows inside ``MonarchSwitchSettingsList``, which draws the group, or
/// directly in a system `Form` section.
///
/// Accessibility: the switch is read as a standard `Toggle`, labelled by the label
/// and description. While a save has failed, the error is the hint of the
/// switch. "Saved" and the error are announced. The spinner is labelled
/// "Saving". The retry button is named "Retry" and the label, so several
/// failed rows stay distinct, and has a 44-point touch target. The status
/// line has an icon and text, not colour alone.
public struct MonarchSwitchSettingsRow: View {
    private let label: String
    @Binding private var isOn: Bool
    private let description: String?
    private let rowLabels: MonarchSwitchSettingsListLabels?
    private let onStatusChange: (@MainActor (MonarchSwitchSettingsStatus) -> Void)?
    private let save: @MainActor (Bool) async throws -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchSwitchSettingsContext) private var context
    @State private var state = MonarchSwitchSettingsRowState()

    /// A setting row.
    ///
    /// - Parameters:
    ///   - label: The name of the setting, already localised. Labels the switch.
    ///   - isOn: The saved value. Set by the row when a save succeeds.
    ///   - description: What the setting does, under the label.
    ///   - labels: Replacements for this row only; `nil` properties keep the list's.
    ///   - onStatusChange: Runs when the save status changes, for example to block leaving while saving.
    ///   - save: Saves the new value. Throw to roll the switch back and show the error.
    public init(
        _ label: String,
        isOn: Binding<Bool>,
        description: String? = nil,
        labels: MonarchSwitchSettingsListLabels? = nil,
        onStatusChange: (@MainActor (MonarchSwitchSettingsStatus) -> Void)? = nil,
        save: @escaping @MainActor (Bool) async throws -> Void
    ) {
        self.label = label
        _isOn = isOn
        self.description = description
        self.rowLabels = labels
        self.onStatusChange = onStatusChange
        self.save = save
    }

    /// A row that starts in a given save state, for tests and previews of
    /// the saving, saved and failed states.
    init(
        _ label: String,
        isOn: Binding<Bool>,
        description: String? = nil,
        state: MonarchSwitchSettingsRowState,
        save: @escaping @MainActor (Bool) async throws -> Void
    ) {
        self.init(label, isOn: isOn, description: description, save: save)
        _state = State(initialValue: state)
    }

    private var labels: MonarchSwitchSettingsListLabels {
        rowLabels?.merged(over: context.labels) ?? context.labels
    }

    private var shownValue: Binding<Bool> {
        Binding(
            get: { state.shown(saved: isOn) },
            set: { perform($0) }
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchSwitchSettingsListTokens.statusSpacing) {
            Toggle(isOn: shownValue) {
                VStack(alignment: .leading, spacing: MonarchSwitchSettingsListTokens.textSpacing) {
                    Text(label)
                        .monarchTextStyle(.body)
                        .foregroundStyle(
                            theme.colors[role: MonarchSwitchSettingsListTokens.labelColor(isEnabled: isEnabled)]
                        )
                    if let description, !description.isEmpty {
                        Text(description)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(
                                theme.colors[
                                    role: MonarchSwitchSettingsListTokens.descriptionColor(isEnabled: isEnabled)
                                ]
                            )
                    }
                }
            }
            .toggleStyle(.monarchSwitch)
            .accessibilityHint(
                MonarchSwitchSettingsListTokens.switchHint(state.status, message: state.message, labels: labels)
            )
            statusLine
        }
        .padding(.horizontal, context.isInList ? MonarchSwitchSettingsListTokens.rowHorizontalPadding : 0)
        .padding(.vertical, context.isInList ? MonarchSwitchSettingsListTokens.rowVerticalPadding : 0)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(alignment: .bottom) {
            if context.isInList {
                Rectangle()
                    .fill(theme.colors.borderSubtle)
                    .frame(height: MonarchSwitchSettingsListTokens.borderWidth)
                    .accessibilityHidden(true)
            }
        }
        .onChange(of: state.status) { _, status in
            if let text = MonarchSwitchSettingsListTokens.announcement(status, message: state.message, labels: labels) {
                AccessibilityNotification.Announcement(text).post()
            }
            onStatusChange?(status)
        }
    }

    @ViewBuilder private var statusLine: some View {
        let color = theme.colors[role: MonarchSwitchSettingsListTokens.statusColor(state.status)]
        let text = MonarchSwitchSettingsListTokens.statusText(state.status, message: state.message, labels: labels)
        switch state.status {
        case .idle:
            EmptyView()
        case .saving:
            HStack(spacing: MonarchSwitchSettingsListTokens.iconSpacing) {
                ProgressView().controlSize(.small)
                Text(text ?? "").monarchTextStyle(.caption)
            }
            .foregroundStyle(color)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(text ?? "")
        case .saved:
            HStack(alignment: .firstTextBaseline, spacing: MonarchSwitchSettingsListTokens.iconSpacing) {
                MonarchIcon(systemName: MonarchSwitchSettingsListTokens.savedSymbol, size: .sm)
                Text(text ?? "").monarchTextStyle(.caption)
            }
            .foregroundStyle(color)
            .accessibilityElement(children: .combine)
        case .error:
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchSwitchSettingsListTokens.iconSpacing) {
                    errorMessage(text ?? "", color: color)
                    Spacer(minLength: 0)
                    retryButton
                }
                VStack(alignment: .leading, spacing: MonarchSwitchSettingsListTokens.iconSpacing) {
                    errorMessage(text ?? "", color: color)
                    retryButton
                }
            }
        }
    }

    private func errorMessage(_ text: String, color: Color) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchSwitchSettingsListTokens.iconSpacing) {
            MonarchIcon(systemName: MonarchSwitchSettingsListTokens.errorSymbol, size: .sm)
            Text(text)
                .monarchTextStyle(.caption)
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(color)
        // The switch's hint already reads the error.
        .accessibilityHidden(true)
    }

    @ViewBuilder private var retryButton: some View {
        if let retryValue = state.retryValue {
            MonarchButton(
                MonarchSwitchSettingsListTokens.retryTitle(labels: labels),
                systemImage: MonarchSwitchSettingsListTokens.retrySymbol,
                variant: .ghost,
                size: .sm
            ) {
                perform(retryValue)
            }
            .accessibilityLabel(MonarchSwitchSettingsListTokens.retryLabel(rowLabel: label, labels: labels))
        }
    }

    private func perform(_ value: Bool) {
        guard state.begin(value, isEnabled: isEnabled) else { return }
        let generation = state.generation
        let duration = context.savedDuration
        let labels = labels
        Task { @MainActor in
            do {
                try await save(value)
                isOn = value
                state.succeed()
                try? await Task.sleep(for: duration)
                state.clearSaved(generation: generation)
            } catch {
                state.fail(value, message: MonarchSwitchSettingsListTokens.errorMessage(error, labels: labels))
            }
        }
    }
}

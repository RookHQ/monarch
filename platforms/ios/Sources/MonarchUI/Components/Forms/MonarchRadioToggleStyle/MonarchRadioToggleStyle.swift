import SwiftUI

/// A radio button look for the standard `Toggle`, for one choice among
/// several: a 16 point ring that holds a dot when it is the choice.
///
/// iOS has no radio button; a `Picker` with `.pickerStyle(.inline)` shows a
/// check mark. Use this style when a list of choices should look like the
/// Monarch radio group. Bind each `Toggle` to "is this the selection":
///
/// ```swift
/// ForEach(plans) { plan in
///     Toggle(plan.name, isOn: Binding(
///         get: { selection == plan.id },
///         set: { if $0 { selection = plan.id } }
///     ))
///     .toggleStyle(.monarchRadio)
/// }
/// ```
///
/// Accessibility: each row is a button with the selected trait, so VoiceOver
/// reads "Monthly, selected, button". The whole row, label included, is the
/// touch target and is at least 44 points high. The ring scales with Dynamic
/// Type, and the selected state is shown by the dot as well as by colour.
public struct MonarchRadioToggleStyle: ToggleStyle {
    /// A radio toggle style.
    public init() {}

    /// The styled view for a configuration.
    public func makeBody(configuration: Configuration) -> some View {
        MonarchRadioBody(configuration: configuration)
    }
}

extension ToggleStyle where Self == MonarchRadioToggleStyle {
    /// The Monarch radio style.
    public static var monarchRadio: MonarchRadioToggleStyle { MonarchRadioToggleStyle() }
}

/// Reads the environment, which a `ToggleStyle` value cannot do itself.
private struct MonarchRadioBody: View {
    let configuration: ToggleStyleConfiguration

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @ScaledMetric(relativeTo: .body) private var ringSize = MonarchRadioTokens.ringSize
    @ScaledMetric(relativeTo: .body) private var dotSize = MonarchRadioTokens.dotSize

    var body: some View {
        Button {
            // A radio chooses; choosing the chosen one again does nothing.
            if !configuration.isOn { configuration.isOn = true }
        } label: {
            HStack(spacing: MonarchRadioTokens.labelSpacing) {
                ring
                MonarchToggleLabel(label: configuration.label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: MonarchRadioTokens.labelColor(isEnabled: isEnabled)])
                    .multilineTextAlignment(.leading)
                Spacer(minLength: 0)
            }
            // 44 points of touch area, with the mark on the leading edge, so
            // a checkbox without a visible label lines up with the text above.
            .frame(
                minWidth: MonarchAccessibility.minimumHitTarget,
                minHeight: MonarchAccessibility.minimumHitTarget,
                alignment: .leading
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(configuration.isOn ? .isSelected : [])
        .sensoryFeedback(.selection, trigger: configuration.isOn) { _, isOn in isOn }
    }

    private var ring: some View {
        let appearance = MonarchRadioTokens.appearance(isSelected: configuration.isOn, isEnabled: isEnabled)
        return Circle()
            .fill(theme.colors[role: appearance.fill])
            .overlay(
                Circle().strokeBorder(theme.colors[role: appearance.border], lineWidth: MonarchRadioTokens.borderWidth)
            )
            .overlay {
                if configuration.isOn {
                    // A 10 point dot inside the ring, with a gap of surface colour around it.
                    Circle()
                        .fill(theme.colors[role: appearance.dot])
                        .frame(width: dotSize, height: dotSize)
                }
            }
            .frame(width: ringSize, height: ringSize)
            .monarchAnimation(.fast, value: configuration.isOn)
    }
}

#if DEBUG
    #Preview("MonarchRadioToggleStyle") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                Toggle("Monthly", isOn: .constant(true))
                Toggle("Yearly", isOn: .constant(false))
                Toggle("Disabled", isOn: .constant(true)).disabled(true)
            }
            .toggleStyle(.monarchRadio)
        }
    }
#endif

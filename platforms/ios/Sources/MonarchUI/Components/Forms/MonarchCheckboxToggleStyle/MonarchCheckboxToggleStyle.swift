import SwiftUI

/// A checkbox look for the standard `Toggle`, because iOS has no checkbox.
///
/// Use a plain `Toggle` (a switch) for a setting that takes effect at once,
/// and this style for selecting items in a list or agreeing to terms.
///
/// ```swift
/// Toggle("Email me a receipt", isOn: $wantsReceipt)
///     .toggleStyle(.monarchCheckbox)
/// ```
///
/// Accessibility: VoiceOver is given a standard `Toggle` with the same label
/// and binding, so the role, the on, off and mixed values and the activation
/// gesture are the system's. The whole row, label included, is the touch
/// target and is at least 44 points high. The box scales with Dynamic Type.
public struct MonarchCheckboxToggleStyle: ToggleStyle {
    /// A checkbox toggle style.
    public init() {}

    /// The styled view for a configuration.
    public func makeBody(configuration: Configuration) -> some View {
        MonarchCheckboxBody(configuration: configuration)
    }
}

extension ToggleStyle where Self == MonarchCheckboxToggleStyle {
    /// The Monarch checkbox style.
    public static var monarchCheckbox: MonarchCheckboxToggleStyle { MonarchCheckboxToggleStyle() }
}

/// Reads the environment, which a `ToggleStyle` value cannot do itself.
private struct MonarchCheckboxBody: View {
    let configuration: ToggleStyleConfiguration

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchAiTone) private var aiTone
    @ScaledMetric(relativeTo: .body) private var boxSize = MonarchCheckboxTokens.boxSize
    @ScaledMetric(relativeTo: .body) private var glyphSize = MonarchCheckboxTokens.glyphSize

    private var isMarked: Bool { configuration.isOn || configuration.isMixed }

    var body: some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            HStack(spacing: MonarchCheckboxTokens.labelSpacing) {
                box
                MonarchToggleLabel(label: configuration.label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                    .multilineTextAlignment(.leading)
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
        .sensoryFeedback(.selection, trigger: configuration.isOn)
        .accessibilityRepresentation {
            Toggle(isOn: configuration.$isOn) { configuration.label }
        }
    }

    private var box: some View {
        var appearance = MonarchCheckboxTokens.appearance(isMarked: isMarked, isEnabled: isEnabled)
        // Inside an AI component the marked box is the AI colour, as on the web.
        if aiTone, isMarked, isEnabled {
            appearance = MonarchCheckboxTokens.Appearance(fill: \.aiSolid, border: \.aiSolid, glyph: \.aiOnSolid)
        }
        let shape = RoundedRectangle(cornerRadius: MonarchCheckboxTokens.cornerRadius, style: .continuous)
        return
            shape
            .fill(theme.colors[role: appearance.fill])
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: appearance.border],
                    lineWidth: MonarchCheckboxTokens.borderWidth
                )
            )
            .overlay {
                if isMarked {
                    Image(
                        systemName: configuration.isMixed
                            ? MonarchCheckboxTokens.mixedSymbol : MonarchCheckboxTokens.checkedSymbol
                    )
                    .font(.system(size: glyphSize, weight: .bold))
                    .foregroundStyle(theme.colors[role: appearance.glyph])
                }
            }
            .frame(width: boxSize, height: boxSize)
            .opacity(isEnabled ? 1 : 0.5)
            .monarchAnimation(.fast, value: isMarked)
    }
}

#if DEBUG
    #Preview("MonarchCheckboxToggleStyle") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                Toggle("Unchecked", isOn: .constant(false))
                Toggle("Checked", isOn: .constant(true))
                Toggle("Disabled", isOn: .constant(true)).disabled(true)
            }
            .toggleStyle(.monarchCheckbox)
        }
    }
#endif

import SwiftUI

/// A button that stays on or off, for the standard `Toggle`: Bold in a text
/// toolbar, or "Show archived" above a list.
///
/// ```swift
/// Toggle("Bold", systemImage: "bold", isOn: $isBold)
///     .toggleStyle(.monarchButton())
/// ```
///
/// The label names the thing that is switched and does not change with the
/// state. While on, the button has the tinted fill of its tone
/// (the accent by default, `.ai` for an AI control such as "Use web
/// search" in a prompt toolbar); with the Differentiate Without Colour
/// setting it also shows a checkmark.
///
/// Accessibility: VoiceOver is given a standard `Toggle` with the same label
/// and binding, so the role, the on and off values and the activation gesture
/// are the system's. The touch target is at least 44 by 44 points and the
/// label scales with Dynamic Type.
public struct MonarchToggleButtonStyle: ToggleStyle {
    private let variant: MonarchToggleButtonVariant
    private let size: MonarchButtonSize
    private let tone: MonarchTone

    /// A toggle button style.
    ///
    /// - Parameters:
    ///   - variant: Whether the button has a border while off.
    ///   - size: The height and padding.
    ///   - tone: The colours while on. The accent by default; `.ai` for AI controls.
    public init(
        _ variant: MonarchToggleButtonVariant = .ghost,
        size: MonarchButtonSize = .md,
        tone: MonarchTone = .accent
    ) {
        self.variant = variant
        self.size = size
        self.tone = tone
    }

    /// The styled view for a configuration.
    public func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            configuration.label
        }
        .buttonStyle(
            MonarchToggleButtonSurfaceStyle(variant: variant, size: size, tone: tone, isOn: configuration.isOn)
        )
        .accessibilityRepresentation {
            Toggle(isOn: configuration.$isOn) { configuration.label }
        }
    }
}

extension ToggleStyle where Self == MonarchToggleButtonStyle {
    /// The Monarch toggle button style.
    public static func monarchButton(
        _ variant: MonarchToggleButtonVariant = .ghost,
        size: MonarchButtonSize = .md,
        tone: MonarchTone = .accent
    ) -> MonarchToggleButtonStyle {
        MonarchToggleButtonStyle(variant, size: size, tone: tone)
    }
}

/// Supplies the pressed state, which a `ToggleStyle` does not have.
private struct MonarchToggleButtonSurfaceStyle: ButtonStyle {
    let variant: MonarchToggleButtonVariant
    let size: MonarchButtonSize
    let tone: MonarchTone
    let isOn: Bool

    func makeBody(configuration: Configuration) -> some View {
        MonarchToggleButtonSurface(configuration: configuration, variant: variant, size: size, tone: tone, isOn: isOn)
    }
}

/// Reads the environment, which a style value cannot do itself.
private struct MonarchToggleButtonSurface: View {
    let configuration: ButtonStyleConfiguration
    let variant: MonarchToggleButtonVariant
    let size: MonarchButtonSize
    let tone: MonarchTone
    let isOn: Bool

    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.accessibilityDifferentiateWithoutColor) private var differentiateWithoutColor

    var body: some View {
        let appearance = MonarchToggleButtonTokens.appearance(
            variant: variant,
            tone: tone,
            isOn: isOn,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        HStack(spacing: MonarchButtonTokens.iconSpacing) {
            if MonarchToggleButtonTokens.showsCheckmark(
                isOn: isOn,
                differentiateWithoutColor: differentiateWithoutColor
            ) {
                MonarchIcon(systemName: MonarchToggleButtonTokens.checkedSymbol, size: .sm)
            }
            configuration.label
        }
        .monarchTextStyle(MonarchButtonTokens.textStyle(size), weight: MonarchButtonTokens.fontWeight)
        .multilineTextAlignment(.center)
        .padding(.horizontal, MonarchButtonTokens.horizontalPadding(size))
        .padding(.vertical, MonarchButtonTokens.verticalPadding)
        .frame(minHeight: MonarchButtonTokens.minHeight(size))
        .monarchSurface(appearance, in: MonarchShapes.control)
        .monarchMinimumHitTarget()
        .monarchAnimation(.fast, value: isOn)
        .monarchAnimation(.fast, value: configuration.isPressed)
    }
}

#if DEBUG
    #Preview("MonarchToggleButtonStyle") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchToggleButtonVariant.allCases, id: \.self) { variant in
                    HStack {
                        Toggle("Off", isOn: .constant(false))
                        Toggle("On", isOn: .constant(true))
                        Toggle("Disabled", isOn: .constant(true)).disabled(true)
                    }
                    .toggleStyle(.monarchButton(variant))
                }
                Toggle("Bold", systemImage: "bold", isOn: .constant(true))
                    .labelStyle(.iconOnly)
                    .toggleStyle(.monarchButton(.outline, size: .lg))
                Toggle("Web search", systemImage: "globe", isOn: .constant(true))
                    .toggleStyle(.monarchButton(.outline, tone: .ai))
            }
        }
    }
#endif

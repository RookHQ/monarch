import SwiftUI

/// The Monarch look for any SwiftUI `Button`.
///
/// Prefer the style over a wrapper: the button stays a standard `Button`,
/// so roles, menus, `ShareLink` and keyboard shortcuts keep working.
///
/// ```swift
/// Button("Save") { save() }
///     .buttonStyle(.monarch(.primary))
/// ```
///
/// The visible height follows the size (32, 36 or 40 points) and the touch
/// target is at least 44 by 44 points. The label scales with Dynamic Type and
/// wraps rather than truncating.
public struct MonarchButtonStyle: ButtonStyle {
    private let variant: MonarchButtonVariant
    private let size: MonarchButtonSize
    private let fullWidth: Bool
    private let isIconOnly: Bool

    /// A button style.
    ///
    /// - Parameters:
    ///   - variant: The visual weight.
    ///   - size: The height and padding.
    ///   - fullWidth: Whether the button fills the available width.
    public init(
        _ variant: MonarchButtonVariant = .secondary,
        size: MonarchButtonSize = .md,
        fullWidth: Bool = false
    ) {
        self.init(variant, size: size, fullWidth: fullWidth, isIconOnly: false)
    }

    /// A button style for a label that may be one icon, drawn square.
    init(_ variant: MonarchButtonVariant, size: MonarchButtonSize, fullWidth: Bool, isIconOnly: Bool) {
        self.variant = variant
        self.size = size
        self.fullWidth = fullWidth
        self.isIconOnly = isIconOnly
    }

    /// The styled view for a configuration.
    public func makeBody(configuration: Configuration) -> some View {
        MonarchButtonStyleBody(
            configuration: configuration,
            variant: variant,
            size: size,
            fullWidth: fullWidth,
            isIconOnly: isIconOnly
        )
    }
}

extension ButtonStyle where Self == MonarchButtonStyle {
    /// The Monarch button style.
    public static func monarch(
        _ variant: MonarchButtonVariant = .secondary,
        size: MonarchButtonSize = .md,
        fullWidth: Bool = false
    ) -> MonarchButtonStyle {
        MonarchButtonStyle(variant, size: size, fullWidth: fullWidth)
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchButtonStyleBody: View {
    let configuration: ButtonStyleConfiguration
    let variant: MonarchButtonVariant
    let size: MonarchButtonSize
    let fullWidth: Bool
    let isIconOnly: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchButtonAppearance.resolve(
            variant: variant,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        let foreground = theme.colors[role: appearance.foreground]
        let shape = RoundedRectangle(cornerRadius: MonarchButtonTokens.cornerRadius, style: .continuous)

        configuration.label
            .monarchTextStyle(
                MonarchButtonTokens.textStyle(size, variant: variant),
                weight: MonarchButtonTokens.fontWeight
            )
            .multilineTextAlignment(.center)
            .foregroundStyle(foreground)
            .tint(foreground)
            .padding(.horizontal, MonarchButtonTokens.horizontalPadding(size, isIconOnly: isIconOnly))
            .padding(.vertical, MonarchButtonTokens.verticalPadding)
            .frame(
                minWidth: isIconOnly ? MonarchButtonTokens.minHeight(size) : nil,
                maxWidth: fullWidth ? .infinity : nil,
                minHeight: MonarchButtonTokens.minHeight(size)
            )
            .background {
                if let background = appearance.background {
                    shape.fill(theme.colors[role: background])
                }
            }
            .overlay {
                if let border = appearance.border {
                    shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchButtonTokens.borderWidth)
                }
            }
            .opacity(isEnabled ? 1 : 0.5)
            .monarchMinimumHitTarget()
            .monarchAnimation(.fast, value: configuration.isPressed)
    }
}

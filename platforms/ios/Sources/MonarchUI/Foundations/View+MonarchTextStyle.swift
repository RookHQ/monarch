import SwiftUI

/// Applies a type role. The size is a `ScaledMetric`, so it follows Dynamic
/// Type for the system font and for a custom family alike.
struct MonarchTextStyleModifier: ViewModifier {
    private let spec: MonarchTextStyleSpec
    private let weight: Font.Weight

    @ScaledMetric private var size: CGFloat
    @Environment(\.monarchTheme) private var theme

    init(style: MonarchTextStyle, weight: Font.Weight?) {
        let spec = style.spec
        self.spec = spec
        self.weight = weight ?? MonarchTextStyleSpec.fontWeight(spec.weight)
        _size = ScaledMetric(wrappedValue: spec.size, relativeTo: spec.relativeTo)
    }

    func body(content: Content) -> some View {
        content
            .font(font)
            .tracking(spec.letterSpacing * size)
            .lineSpacing(max(0, (spec.lineHeight - MonarchTextStyleSpec.systemLineHeight) * size))
    }

    private var font: Font {
        Self.font(spec: spec, weight: weight, size: size, theme: theme)
    }

    /// The font of a type role at a resolved size and weight, for a run of text that needs its own weight.
    static func font(spec: MonarchTextStyleSpec, weight: Font.Weight, size: CGFloat, theme: MonarchTheme) -> Font {
        let family = spec.isMonospaced ? theme.typography.codeFontFamily : theme.typography.fontFamily
        if let family {
            if !spec.isMonospaced, let face = theme.typography.face(for: weight) {
                return .custom(face, fixedSize: size)
            }
            return .custom(family, fixedSize: size).weight(weight)
        }
        return .system(size: size, weight: weight, design: spec.isMonospaced ? .monospaced : .default)
    }
}

extension View {
    /// Applies a Monarch type role: size, weight, tracking and line spacing.
    ///
    /// The text scales with Dynamic Type. Colour is not set; use
    /// `foregroundStyle` with a colour from the theme.
    ///
    /// ```swift
    /// Text("Invoices").monarchTextStyle(.title)
    /// ```
    public func monarchTextStyle(_ style: MonarchTextStyle) -> some View {
        modifier(MonarchTextStyleModifier(style: style, weight: nil))
    }

    /// Applies a type role with another weight, for example the large lead size in the regular weight.
    ///
    /// ```swift
    /// Text("Large lead text").monarchTextStyle(.subheading, weight: .regular)
    /// ```
    public func monarchTextStyle(_ style: MonarchTextStyle, weight: Font.Weight) -> some View {
        modifier(MonarchTextStyleModifier(style: style, weight: weight))
    }
}

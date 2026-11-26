import SwiftUI

/// The Monarch look for a text link: a SwiftUI `Link`, or a `Button` that navigates.
///
/// The control stays the standard one, so `Link` keeps the link trait and
/// opens its URL through the environment's `openURL` action.
///
/// ```swift
/// Link("Terms of service", destination: termsURL)
///     .buttonStyle(.monarchLink(isExternal: true))
///
/// Button("Forgot password?") { showsReset = true }
///     .buttonStyle(.monarchLink(.subtle))
/// ```
///
/// The style sets the colour, the underline and, for `standalone`, the
/// weight. It does not set the type size: the link takes the font of the text
/// around it, so apply `monarchTextStyle(_:)` to the link or to its container.
///
/// Accessibility: with `isExternal` the icon after the title is read with the
/// title ("Terms of service, opens in the browser"). When the Button Shapes
/// setting is on every variant is underlined. The touch target is at least 44
/// by 44 points.
public struct MonarchLinkStyle: ButtonStyle {
    private let variant: MonarchLinkVariant
    private let isExternal: Bool
    private let externalLabel: String?

    /// A link style.
    ///
    /// - Parameters:
    ///   - variant: How the link is drawn.
    ///   - isExternal: Whether the link leaves the app, which adds an icon after the title.
    ///   - externalLabel: Replaces the built-in "opens in the browser", read after the title.
    public init(
        _ variant: MonarchLinkVariant = .inline,
        isExternal: Bool = false,
        externalLabel: String? = nil
    ) {
        self.variant = variant
        self.isExternal = isExternal
        self.externalLabel = externalLabel
    }

    /// The styled view for a configuration.
    public func makeBody(configuration: Configuration) -> some View {
        MonarchLinkStyleBody(
            configuration: configuration,
            variant: variant,
            isExternal: isExternal,
            externalLabel: externalLabel
        )
    }
}

extension ButtonStyle where Self == MonarchLinkStyle {
    /// The Monarch link style.
    public static func monarchLink(
        _ variant: MonarchLinkVariant = .inline,
        isExternal: Bool = false,
        externalLabel: String? = nil
    ) -> MonarchLinkStyle {
        MonarchLinkStyle(variant, isExternal: isExternal, externalLabel: externalLabel)
    }
}

/// Sets a variant's weight. With a custom font a plain `fontWeight` keeps the regular face, so a weight is set
/// through the text style (the label size, which the standalone link uses on the web).
private struct MonarchLinkWeight: ViewModifier {
    let weight: Font.Weight?

    func body(content: Content) -> some View {
        if let weight {
            content.monarchTextStyle(.label, weight: weight)
        } else {
            content
        }
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchLinkStyleBody: View {
    let configuration: ButtonStyleConfiguration
    let variant: MonarchLinkVariant
    let isExternal: Bool
    let externalLabel: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.accessibilityShowButtonShapes) private var showsButtonShapes

    var body: some View {
        let appearance = MonarchLinkTokens.appearance(
            variant: variant,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled,
            showsButtonShapes: showsButtonShapes
        )
        HStack(alignment: .firstTextBaseline, spacing: MonarchLinkTokens.iconSpacing) {
            configuration.label
                .underline(appearance.isUnderlined)
                .multilineTextAlignment(.leading)
            if isExternal {
                MonarchIcon(
                    systemName: MonarchLinkTokens.externalSymbol,
                    label: externalLabel ?? MonarchLinkStrings.external,
                    size: .sm
                )
            }
        }
        .modifier(MonarchLinkWeight(weight: MonarchLinkTokens.fontWeight(variant)))
        .foregroundStyle(theme.colors[role: appearance.foreground])
        .monarchMinimumHitTarget()
        .monarchAnimation(.fast, value: configuration.isPressed)
    }
}

#if DEBUG
    #Preview("MonarchLinkStyle") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchLinkVariant.allCases, id: \.self) { variant in
                    Button("Billing settings") {}
                        .buttonStyle(.monarchLink(variant))
                }
                Button("Terms of service") {}
                    .buttonStyle(.monarchLink(isExternal: true))
                Button("Unavailable") {}
                    .buttonStyle(.monarchLink())
                    .disabled(true)
            }
            .monarchTextStyle(.body)
        }
    }
#endif

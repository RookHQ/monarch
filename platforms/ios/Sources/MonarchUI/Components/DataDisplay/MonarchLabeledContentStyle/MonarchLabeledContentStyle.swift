import SwiftUI

/// The Monarch look for the standard `LabeledContent`: one fact of a
/// record, such as "Owner: Ada Lovelace". A column of them is a description
/// list.
///
/// ```swift
/// VStack(alignment: .leading, spacing: theme.spacing.space4) {
///     LabeledContent("Owner", value: "Ada Lovelace")
///     LabeledContent("Stage", value: "Negotiation")
/// }
/// .labeledContentStyle(.monarch)
/// ```
///
/// The label uses the caption role in the muted text colour and the value
/// the body role in the default text colour. `.stacked` puts the label
/// above the value; `.inline` puts them on one row and stacks them again
/// when they do not fit, for example at large text sizes. The value can be
/// any view: text, a badge, a ``MonarchTruncatedText`` or a row with a
/// ``MonarchCopyButton``.
///
/// Accessibility: the label and the value are combined into one element,
/// read as the label and then the value. A button inside the value, such as
/// a copy button, stays available as the element's action.
public struct MonarchLabeledContentStyle: LabeledContentStyle {
    private let layout: MonarchLabeledContentLayout

    /// A labeled content style.
    ///
    /// - Parameter layout: Whether the label is above the value or beside it.
    public init(_ layout: MonarchLabeledContentLayout = .stacked) {
        self.layout = layout
    }

    /// The styled view for a configuration.
    public func makeBody(configuration: Configuration) -> some View {
        MonarchLabeledContentBody(configuration: configuration, layout: layout)
    }
}

extension LabeledContentStyle where Self == MonarchLabeledContentStyle {
    /// The Monarch labeled content style, with the label above the value.
    public static var monarch: MonarchLabeledContentStyle { MonarchLabeledContentStyle() }

    /// The Monarch labeled content style with a layout.
    public static func monarch(_ layout: MonarchLabeledContentLayout) -> MonarchLabeledContentStyle {
        MonarchLabeledContentStyle(layout)
    }
}

/// Reads the environment, which a style value cannot do itself.
private struct MonarchLabeledContentBody: View {
    let configuration: LabeledContentStyleConfiguration
    let layout: MonarchLabeledContentLayout

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        Group {
            switch layout {
            case .stacked:
                stacked
            case .inline:
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline, spacing: MonarchLabeledContentTokens.inlineSpacing) {
                        label
                        Spacer(minLength: 0)
                        value
                    }
                    stacked
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }

    private var stacked: some View {
        VStack(alignment: .leading, spacing: MonarchLabeledContentTokens.stackedSpacing) {
            label
            configuration.content
                .monarchTextStyle(MonarchLabeledContentTokens.valueStyle)
                .foregroundStyle(theme.colors[role: MonarchLabeledContentTokens.value(isEnabled: isEnabled)])
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var label: some View {
        configuration.label
            .monarchTextStyle(MonarchLabeledContentTokens.labelStyle)
            .foregroundStyle(theme.colors[role: MonarchLabeledContentTokens.label(isEnabled: isEnabled)])
            .multilineTextAlignment(.leading)
    }

    private var value: some View {
        configuration.content
            .monarchTextStyle(MonarchLabeledContentTokens.valueStyle)
            .foregroundStyle(theme.colors[role: MonarchLabeledContentTokens.value(isEnabled: isEnabled)])
            .multilineTextAlignment(MonarchLabeledContentTokens.valueAlignment(layout))
    }
}

#if DEBUG
    #Preview("MonarchLabeledContentStyle") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchLabeledContentLayout.allCases, id: \.self) { layout in
                    VStack(alignment: .leading) {
                        LabeledContent("Owner", value: "Ada Lovelace")
                        LabeledContent("Stage") { MonarchBadge("Negotiation", tone: .info) }
                        LabeledContent("Notes", value: "Renewal depends on the security review in May.")
                    }
                    .labeledContentStyle(.monarch(layout))
                }
            }
        }
    }
#endif

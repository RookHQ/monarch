import SwiftUI

/// The opening of a welcome, onboarding or upgrade screen: an eyebrow, a large title, a lead sentence, one or two
/// actions and a short note.
///
/// ```swift
/// MonarchMarketingHero(
///     "Write it once. Find it whenever.",
///     eyebrow: "Rook Example",
///     lead: "Structured documents for teams whose decisions need to stay connected.",
///     note: "Set up in under five minutes."
/// ) {
///     MonarchButton("Start writing", variant: .primary) { start() }
///     MonarchButton("See how it works") { tour() }
/// }
/// ```
///
/// The title uses the display type role and grows with Dynamic Type. A soft wash of the accent's subtle colour sits
/// behind the text and fades out; turn it off with `showsBackdrop: false`.
///
/// Accessibility: the title has the header trait. The eyebrow is read before it, then the lead, the actions, each
/// its own element, and the note.
public struct MonarchMarketingHero<Actions: View>: View {
    private let title: String
    private let eyebrow: String?
    private let lead: String?
    private let note: String?
    private let alignment: MonarchMarketingAlignment
    private let showsBackdrop: Bool
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme

    /// A hero with actions.
    ///
    /// - Parameters:
    ///   - title: The headline, already localised.
    ///   - eyebrow: A short label above the title, such as the product name.
    ///   - lead: One or two sentences under the title.
    ///   - note: Small print under the actions.
    ///   - alignment: Centred (the default) or leading text.
    ///   - showsBackdrop: Whether the accent wash is drawn behind the text.
    ///   - actions: One or two buttons, the first one primary.
    public init(
        _ title: String,
        eyebrow: String? = nil,
        lead: String? = nil,
        note: String? = nil,
        alignment: MonarchMarketingAlignment = .center,
        showsBackdrop: Bool = true,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.eyebrow = eyebrow
        self.lead = lead
        self.note = note
        self.alignment = alignment
        self.showsBackdrop = showsBackdrop
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        VStack(
            alignment: MonarchMarketingSectionTokens.horizontal(alignment),
            spacing: MonarchMarketingHeroTokens.spacing
        ) {
            VStack(
                alignment: MonarchMarketingSectionTokens.horizontal(alignment),
                spacing: MonarchMarketingHeroTokens.eyebrowSpacing
            ) {
                if let eyebrow {
                    Text(eyebrow)
                        .monarchTextStyle(
                            MonarchMarketingSectionTokens.eyebrowStyle,
                            weight: MonarchMarketingSectionTokens.eyebrowWeight
                        )
                        .foregroundStyle(theme.colors[role: MonarchMarketingHeroTokens.eyebrowColor])
                }
                Text(title)
                    .monarchTextStyle(MonarchMarketingHeroTokens.titleStyle)
                    .foregroundStyle(theme.colors[role: MonarchMarketingHeroTokens.titleColor])
                    .accessibilityAddTraits(.isHeader)
            }
            if let lead {
                Text(lead)
                    .monarchTextStyle(
                        MonarchMarketingHeroTokens.leadStyle,
                        weight: MonarchMarketingHeroTokens.leadWeight
                    )
                    .foregroundStyle(theme.colors[role: MonarchMarketingHeroTokens.supportingColor])
            }
            MonarchFlowLayout(
                spacing: MonarchMarketingHeroTokens.actionSpacing,
                justification: alignment == .center ? .center : .leading
            ) {
                actions
            }
            if let note {
                Text(note)
                    .monarchTextStyle(MonarchMarketingHeroTokens.noteStyle)
                    .foregroundStyle(theme.colors[role: MonarchMarketingHeroTokens.supportingColor])
            }
        }
        .multilineTextAlignment(MonarchMarketingSectionTokens.text(alignment))
        .fixedSize(horizontal: false, vertical: true)
        .padding(.horizontal, MonarchMarketingHeroTokens.horizontalPadding)
        .padding(.vertical, MonarchMarketingHeroTokens.verticalPadding)
        .frame(maxWidth: .infinity, alignment: MonarchMarketingSectionTokens.frame(alignment))
        .background {
            if showsBackdrop {
                LinearGradient(
                    colors: [theme.colors[role: MonarchMarketingHeroTokens.backdropColor], .clear],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .clipShape(RoundedRectangle(cornerRadius: MonarchMarketingHeroTokens.cornerRadius, style: .continuous))
                .accessibilityHidden(true)
            }
        }
        .accessibilityElement(children: .contain)
    }
}

extension MonarchMarketingHero where Actions == EmptyView {
    /// A hero without actions.
    ///
    /// - Parameters:
    ///   - title: The headline, already localised.
    ///   - eyebrow: A short label above the title, such as the product name.
    ///   - lead: One or two sentences under the title.
    ///   - note: Small print under the lead.
    ///   - alignment: Centred (the default) or leading text.
    ///   - showsBackdrop: Whether the accent wash is drawn behind the text.
    public init(
        _ title: String,
        eyebrow: String? = nil,
        lead: String? = nil,
        note: String? = nil,
        alignment: MonarchMarketingAlignment = .center,
        showsBackdrop: Bool = true
    ) {
        self.init(
            title,
            eyebrow: eyebrow,
            lead: lead,
            note: note,
            alignment: alignment,
            showsBackdrop: showsBackdrop
        ) {
            EmptyView()
        }
    }
}

#if DEBUG
    #Preview("MonarchMarketingHero") {
        MonarchPreviewMatrix {
            MonarchMarketingHero(
                "Write it once. Find it whenever.",
                eyebrow: "Rook Example",
                lead: "Structured documents for teams whose decisions need to stay connected.",
                note: "Set up in under five minutes."
            ) {
                MonarchButton("Start writing", variant: .primary) {}
                MonarchButton("See how it works") {}
            }
        }
    }
#endif

import SwiftUI

/// A small provenance badge that marks content as produced by AI, written
/// with AI help, or edited by a person after AI wrote it.
///
/// ```swift
/// MonarchAiGeneratedBadge()
/// MonarchAiGeneratedBadge(.assisted, model: "gemini-2.5-pro", date: createdAt)
/// MonarchAiGeneratedBadge(.edited, size: .sm, isIconOnly: true)
/// ```
///
/// The badge shows ``MonarchAiBotGlyph`` and a label in the AI colours. The
/// model and the time are supplementary: VoiceOver reads them as the value,
/// and a pointer shows them as the system tooltip. Touch shows no tooltip,
/// so never put essential information there.
///
/// Accessibility: one element, read as the label and then the provenance.
/// The label is read for an icon-only badge too. "Edited after AI" has a
/// dashed border, so the kinds do not differ by colour alone. The badge is
/// not interactive and scales with Dynamic Type.
public struct MonarchAiGeneratedBadge: View {
    private let kind: MonarchAiGeneratedBadgeKind
    private let model: String?
    private let date: Date?
    private let size: MonarchAiGeneratedBadgeSize
    private let isIconOnly: Bool
    private let label: String?
    private let detail: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.calendar) private var calendar
    @Environment(\.timeZone) private var timeZone

    /// An AI provenance badge.
    ///
    /// - Parameters:
    ///   - kind: How much of the content came from AI.
    ///   - model: The model that produced the content, for example `gemini-2.5-pro`.
    ///   - date: When the content was produced.
    ///   - size: The size of the badge.
    ///   - isIconOnly: Whether only the glyph is shown. The label stays available to VoiceOver.
    ///   - label: Replaces the built-in label of the kind.
    ///   - detail: Replaces the built-in provenance line made from the model and the date.
    public init(
        _ kind: MonarchAiGeneratedBadgeKind = .generated,
        model: String? = nil,
        date: Date? = nil,
        size: MonarchAiGeneratedBadgeSize = .md,
        isIconOnly: Bool = false,
        label: String? = nil,
        detail: String? = nil
    ) {
        self.kind = kind
        self.model = model
        self.date = date
        self.size = size
        self.isIconOnly = isIconOnly
        self.label = label
        self.detail = detail
    }

    private var details: String {
        if let detail { return detail }
        let time = date.map {
            MonarchAiGeneratedBadgeTokens.formattedTime($0, locale: locale, calendar: calendar, timeZone: timeZone)
        }
        return MonarchAiGeneratedBadgeTokens.details(model: model, time: time)
    }

    /// The content of the view.
    public var body: some View {
        let text = label ?? MonarchAiGeneratedBadgeStrings.label(kind)
        let details = details
        let dash = MonarchAiGeneratedBadgeTokens.dash
        let appearance = MonarchAiGeneratedBadgeTokens.appearance
        HStack(spacing: MonarchAiGeneratedBadgeTokens.iconSpacing) {
            MonarchAiBotGlyph(pointSize: MonarchAiGeneratedBadgeTokens.glyphSize(size))
            if !isIconOnly {
                Text(text)
                    .monarchTextStyle(
                        MonarchAiGeneratedBadgeTokens.textStyle,
                        weight: MonarchAiGeneratedBadgeTokens.fontWeight
                    )
            }
        }
        .padding(
            .horizontal,
            isIconOnly
                ? MonarchAiGeneratedBadgeTokens.iconOnlyPadding
                : MonarchAiGeneratedBadgeTokens.horizontalPadding(size)
        )
        .padding(
            .vertical,
            isIconOnly
                ? MonarchAiGeneratedBadgeTokens.iconOnlyPadding : MonarchAiGeneratedBadgeTokens.verticalPadding
        )
        .foregroundStyle(theme.colors[role: appearance.foreground])
        .background {
            if let background = appearance.background {
                Capsule().fill(theme.colors[role: background])
            }
        }
        .overlay {
            if let border = appearance.border {
                Capsule().strokeBorder(
                    theme.colors[role: border],
                    style: StrokeStyle(
                        lineWidth: MonarchAiGeneratedBadgeTokens.borderWidth,
                        dash: MonarchAiGeneratedBadgeTokens.isDashed(kind) ? [dash, dash] : []
                    )
                )
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(text)
        .accessibilityValue(details)
        .help(MonarchAiGeneratedBadgeTokens.help(label: text, details: details, isIconOnly: isIconOnly))
    }
}

#if DEBUG
    #Preview("MonarchAiGeneratedBadge") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchAiGeneratedBadgeKind.allCases, id: \.self) { kind in
                    HStack {
                        MonarchAiGeneratedBadge(kind)
                        MonarchAiGeneratedBadge(kind, size: .sm)
                        MonarchAiGeneratedBadge(kind, isIconOnly: true)
                        MonarchAiGeneratedBadge(kind, size: .sm, isIconOnly: true)
                    }
                }
                MonarchAiGeneratedBadge(model: "gemini-2.5-pro", date: Date())
            }
        }
    }
#endif

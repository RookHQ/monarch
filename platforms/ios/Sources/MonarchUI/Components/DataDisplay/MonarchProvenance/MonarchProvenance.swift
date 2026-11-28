import SwiftUI

/// Where a value came from and how fresh it is: "Registry · 2 hours ago".
///
/// ```swift
/// MonarchProvenance("Companies House", checkedAt: company.checkedAt)
/// ```
///
/// Put it next to any value the app did not get from the person. With a
/// `url` the note opens the source record. Nothing is drawn when the source
/// is empty. The relative time is redrawn every minute.
///
/// Accessibility: the note is one element read as "Source: Registry, checked
/// 2 hours ago". With a URL it is a link with a 44-point touch target. The
/// icon is decorative. Text scales with Dynamic Type and wraps.
public struct MonarchProvenance: View {
    private let source: String
    private let checkedAt: Date?
    private let systemImage: String?
    private let url: URL?
    private let variant: MonarchProvenanceVariant
    private let labels: MonarchProvenanceLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A provenance note.
    ///
    /// - Parameters:
    ///   - source: Where the value came from, in words the reader knows, such
    ///     as "Registry" or "Entered by hand". Already localised.
    ///   - checkedAt: When the value was fetched, checked or computed.
    ///   - systemImage: An SF Symbol before the text.
    ///   - url: Opens the source record. Only http, https and mailto URLs are opened.
    ///   - variant: A bordered chip, or plain text.
    ///   - labels: Replacements for the spoken strings.
    public init(
        _ source: String,
        checkedAt: Date? = nil,
        systemImage: String? = nil,
        url: URL? = nil,
        variant: MonarchProvenanceVariant = .chip,
        labels: MonarchProvenanceLabels = MonarchProvenanceLabels()
    ) {
        self.source = source
        self.checkedAt = checkedAt
        self.systemImage = systemImage
        self.url = url
        self.variant = variant
        self.labels = labels
    }

    /// The content of the view.
    public var body: some View {
        if MonarchProvenanceTokens.isVisible(source: source) {
            MonarchRelativeTimeClock(dates: [checkedAt]) { current in
                note(now: current)
            }
        }
    }

    @ViewBuilder private func note(now: Date) -> some View {
        let time = checkedAt.map { MonarchProvenanceTokens.relativeText($0, now: now, locale: locale) }
        let label = MonarchProvenanceTokens.accessibilityLabel(source: source, time: time, labels: labels)
        if let destination = MonarchProvenanceTokens.safeURL(url) {
            Link(destination: destination) {
                surface(time: time, isLink: true)
                    .monarchMinimumHitTarget()
            }
            .accessibilityLabel(label)
        } else {
            surface(time: time, isLink: false)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        }
    }

    private func surface(time: String?, isLink: Bool) -> some View {
        let isChip = variant == .chip
        let shape = RoundedRectangle(cornerRadius: MonarchProvenanceTokens.chipRadius, style: .continuous)
        return content(time: time, isLink: isLink)
            .padding(.horizontal, isChip ? MonarchProvenanceTokens.chipHorizontalPadding : 0)
            .padding(.vertical, isChip ? MonarchProvenanceTokens.chipVerticalPadding : 0)
            .background {
                if isChip { shape.fill(theme.colors[role: MonarchProvenanceTokens.chipFill]) }
            }
            .overlay {
                if isChip {
                    shape.strokeBorder(
                        theme.colors[role: MonarchProvenanceTokens.chipBorder],
                        lineWidth: MonarchShapes.borderWidth
                    )
                }
            }
    }

    private func content(time: String?, isLink: Bool) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchProvenanceTokens.iconSpacing) {
            if let systemImage {
                MonarchIcon(systemName: systemImage, size: .sm)
            }
            text(time: time, isLink: isLink)
                .monarchTextStyle(MonarchProvenanceTokens.textStyle)
                .multilineTextAlignment(.leading)
        }
        .foregroundStyle(theme.colors[role: MonarchProvenanceTokens.text])
    }

    private func text(time: String?, isLink: Bool) -> Text {
        var sourceText = Text(verbatim: source)
            .foregroundStyle(theme.colors[role: MonarchProvenanceTokens.source(isLink: isLink)])
        if isLink { sourceText = sourceText.underline() }
        guard let time else { return sourceText }
        return sourceText + Text(verbatim: MonarchProvenanceTokens.separator + time).monospacedDigit()
    }
}

#if DEBUG
    #Preview("MonarchProvenance") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchProvenance("Registry", checkedAt: .now.addingTimeInterval(-7200))
                MonarchProvenance("Imported file", systemImage: "doc", variant: .inline)
                MonarchProvenance(
                    "Companies House",
                    checkedAt: .now.addingTimeInterval(-300),
                    url: URL(string: "https://example.com")
                )
            }
        }
    }
#endif

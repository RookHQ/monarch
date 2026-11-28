import SwiftUI

/// A `[1]` marker that points a sentence of an AI answer at its source.
/// Tapping it opens a popover card with the source's title, where it lives,
/// the quoted passage and a way to open it.
///
/// ```swift
/// let numbers = MonarchAiCitation.sourceNumbers(in: message.parts)
/// HStack(alignment: .firstTextBaseline) {
///     Text("Revenue grew 12% in the third quarter.")
///     MonarchAiCitation(numbers[source.id] ?? 1, source: source, quote: citation.quote)
/// }
/// ```
///
/// It takes a ``MonarchAiSourcePart`` and the quote of a
/// ``MonarchAiCitationPart`` from ``MonarchAiConversation``'s message model;
/// number a message's sources with ``sourceNumbers(in:)``. SwiftUI text
/// cannot hold a button, so place the marker after the sentence, in an
/// `HStack` or a ``MonarchFlowLayout``. With `onOpen` the card has an Open
/// source button (for an in-app preview); otherwise it links to the source's
/// address when it is `http`, `https` or `mailto`. The card is a popover on
/// every size class.
///
/// Accessibility: the marker is a button named "Source 1: Quarterly report"
/// with a 44-point touch target; the brackets are not read. The card's
/// heading is a header and the source icon is decorative.
public struct MonarchAiCitation: View {
    private let index: Int
    private let source: MonarchAiSourcePart
    private let quote: String?
    private let isPresented: Binding<Bool>?
    private let labels: MonarchAiCitationLabels
    private let onOpen: (@MainActor (MonarchAiSourcePart) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var isShowing = false

    /// A citation marker.
    ///
    /// - Parameters:
    ///   - index: The number shown in the marker: 1 shows `[1]`.
    ///   - source: The cited source.
    ///   - quote: The passage the answer relies on, from the citation part.
    ///   - isPresented: Controls the card from outside. Without it the marker keeps its own state.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpen: Opens the source inside the app. Shows "Open source" instead of the "Visit page" link.
    public init(
        _ index: Int,
        source: MonarchAiSourcePart,
        quote: String? = nil,
        isPresented: Binding<Bool>? = nil,
        labels: MonarchAiCitationLabels = MonarchAiCitationLabels(),
        onOpen: (@MainActor (MonarchAiSourcePart) -> Void)? = nil
    ) {
        self.index = index
        self.source = source
        self.quote = quote
        self.isPresented = isPresented
        self.labels = labels
        self.onOpen = onOpen
    }

    /// The numbers of the sources of a message, 1, 2, 3 in the order the
    /// source parts appear, keyed by source id. A source repeated later keeps
    /// its first number.
    ///
    /// - Parameter parts: The parts of one message.
    /// - Returns: The number of each source id.
    nonisolated public static func sourceNumbers(in parts: [MonarchAiPart]) -> [String: Int] {
        MonarchAiCitationTokens.sourceNumbers(in: parts)
    }

    private var presented: Binding<Bool> {
        isPresented ?? $isShowing
    }

    /// Closes the card and calls `onOpen`, or `nil` without a handler.
    private var openAction: (@MainActor () -> Void)? {
        guard let onOpen else { return nil }
        let presented = presented
        let source = source
        return {
            presented.wrappedValue = false
            onOpen(source)
        }
    }

    /// The content of the view.
    public var body: some View {
        Button {
            presented.wrappedValue.toggle()
        } label: {
            Text(MonarchAiCitationTokens.markerText(index))
                .monarchTextStyle(.caption, weight: .semibold)
                .foregroundStyle(theme.colors[role: MonarchAiCitationTokens.markerRole])
                .baselineOffset(MonarchAiCitationTokens.markerRaise)
                .padding(.horizontal, MonarchAiCitationTokens.markerPadding)
                // The touch target reaches 44 points without taking room in the running text.
                .contentShape(Rectangle().inset(by: -MonarchAiCitationTokens.markerHitInset))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(MonarchAiCitationTokens.markerLabel(index, source: source, labels: labels))
        .popover(isPresented: presented, arrowEdge: .bottom) {
            MonarchAiCitationCard(
                index: index,
                source: source,
                quote: quote,
                labels: labels,
                onOpen: openAction
            )
            .presentationCompactAdaptation(.popover)
        }
    }
}

/// The card a citation opens: heading, source, meta line, quote and action.
struct MonarchAiCitationCard: View {
    let index: Int
    let source: MonarchAiSourcePart
    let quote: String?
    let labels: MonarchAiCitationLabels
    let onOpen: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var maxWidth = MonarchAiCitationTokens.cardMaxWidth

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchAiCitationTokens.cardSpacing) {
            Text(labels.card?(index) ?? MonarchAiCitationStrings.card(index))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityAddTraits(.isHeader)
            HStack(alignment: .firstTextBaseline, spacing: MonarchAiCitationTokens.iconSpacing) {
                icon
                Text(source.title)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
            }
            let meta = MonarchAiCitationTokens.meta(source, labels: labels)
            if !meta.isEmpty {
                Text(meta)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if let quote, !quote.isEmpty {
                Text(quote)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                    .padding(MonarchAiCitationTokens.quotePadding)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(theme.colors.bgSubtle)
                    .overlay(alignment: .leading) {
                        Rectangle()
                            .fill(theme.colors.borderStrong)
                            .frame(width: MonarchAiCitationTokens.quoteBarWidth)
                    }
            }
            switch MonarchAiCitationTokens.action(source, hasOpenHandler: onOpen != nil) {
            case .open:
                MonarchButton(labels.open ?? MonarchAiCitationStrings.open, variant: .secondary, size: .sm) {
                    onOpen?()
                }
            case .visit(let url):
                Link(labels.visit ?? MonarchAiCitationStrings.visit, destination: url)
                    .buttonStyle(.monarchLink(.standalone, isExternal: true))
                    .monarchTextStyle(.label)
            case .none:
                EmptyView()
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .padding(MonarchAiCitationTokens.cardPadding)
        .frame(maxWidth: maxWidth, alignment: .leading)
        .background(theme.colors.bgRaised)
    }

    @ViewBuilder private var icon: some View {
        if let url = MonarchAiSourceFormat.faviconURL(source.favicon) {
            MonarchFavicon(url: url, name: source.title)
        } else {
            MonarchIcon(systemName: MonarchAiSourceFormat.symbol(source.kind), size: .sm)
                .foregroundStyle(theme.colors.textMuted)
        }
    }
}

#if DEBUG
    #Preview("MonarchAiCitation") {
        let source = MonarchAiSourcePart(
            id: "s1",
            title: "Quarterly report",
            url: "https://www.example.com/q3",
            kind: .doc,
            product: "Dossier"
        )
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                HStack(alignment: .firstTextBaseline) {
                    Text("Revenue grew 12% in the third quarter.")
                    MonarchAiCitation(1, source: source, quote: "Revenue rose 12% quarter on quarter.")
                }
                MonarchAiCitationCard(
                    index: 1,
                    source: source,
                    quote: "Revenue rose 12% quarter on quarter.",
                    labels: MonarchAiCitationLabels(),
                    onOpen: nil
                )
            }
        }
    }
#endif

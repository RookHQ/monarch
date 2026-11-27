import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiCitation``.
enum MonarchAiCitationTokens {
    /// The horizontal padding inside the marker.
    static let markerPadding: CGFloat = MonarchTokens.Space.space0_5
    /// How far the marker sits above the baseline, like the web superscript.
    static let markerRaise: CGFloat = MonarchTokens.Space.space1
    /// How far the touch target reaches past the marker on every side.
    static let markerHitInset: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts of the card.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the icon and the title.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding inside the card.
    static let cardPadding: CGFloat = MonarchTokens.Space.space4
    /// The padding inside the quote.
    static let quotePadding: CGFloat = MonarchTokens.Space.space2
    /// The width of the bar at the leading edge of the quote.
    static let quoteBarWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The widest the card grows, before Dynamic Type.
    static let cardMaxWidth: CGFloat = MonarchTokens.Space.space16 * 5
    /// The colour role of the marker.
    static var markerRole: MonarchColorRole { \.aiCitation }

    /// The text of the marker: "[1]".
    static func markerText(_ index: Int) -> String {
        "[\(index.formatted())]"
    }

    /// The name of a kind of source.
    static func kindName(_ kind: MonarchAiSourceKind, labels: MonarchAiCitationLabels) -> String {
        labels.kind?(kind) ?? MonarchAiCitationStrings.kind(kind)
    }

    /// The line under the title: the host, the product and the kind, joined with " · ".
    static func meta(_ source: MonarchAiSourcePart, labels: MonarchAiCitationLabels) -> String {
        [MonarchAiSourceFormat.host(source.url), source.product, kindName(source.kind, labels: labels)]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: " · ")
    }

    /// The spoken name of the marker.
    static func markerLabel(_ index: Int, source: MonarchAiSourcePart, labels: MonarchAiCitationLabels) -> String {
        labels.marker?(index, source.title) ?? MonarchAiCitationStrings.marker(index, title: source.title)
    }

    /// The numbers of the sources of a message, 1, 2, 3 in the order they appear.
    static func sourceNumbers(in parts: [MonarchAiPart]) -> [String: Int] {
        var numbers: [String: Int] = [:]
        for part in parts {
            if case .source(let source) = part, numbers[source.id] == nil {
                numbers[source.id] = numbers.count + 1
            }
        }
        return numbers
    }

    /// How the card lets the person open the source.
    enum Action: Equatable {
        /// A button that calls `onOpen`.
        case open
        /// A link to the source's address.
        case visit(URL)
    }

    /// The card's action: Open source with a handler, else Visit page with a safe address, else none.
    static func action(_ source: MonarchAiSourcePart, hasOpenHandler: Bool) -> Action? {
        if hasOpenHandler { return .open }
        return MonarchAiSourceFormat.safeURL(source.url).map(Action.visit)
    }
}

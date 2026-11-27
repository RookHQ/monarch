import MonarchTokens
import SwiftUI

/// The form a ``MonarchDnsRecordTable`` is drawn in, once `automatic` is resolved.
enum MonarchDnsRecordTableForm: Hashable {
    /// One card per record.
    case cards
    /// One grid row per record.
    case table
}

/// Component tokens and pure logic for ``MonarchDnsRecordTable``.
enum MonarchDnsRecordTableTokens {
    /// The gap between cards.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space3
    /// The padding inside a card.
    static let cardPadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the parts of a card.
    static let cardContentSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between a term and its value.
    static let termSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the columns of the table.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space3
    /// The padding above and below a table row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space2
    /// The padding around the table.
    static let tablePadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the heading and the status in a card.
    static let headSpacing: CGFloat = MonarchTokens.Space.space2
    /// The fill of a card and of the table.
    static var surfaceRole: MonarchColorRole { \.bgSurface }
    /// The border of a card and of the table.
    static var borderRole: MonarchColorRole { \.borderDefault }
    /// The colour of the terms and of the full name line.
    static var mutedRole: MonarchColorRole { \.textMuted }
    /// The colour of the heading and of the type, TTL and priority.
    static var textRole: MonarchColorRole { \.textDefault }

    /// The record name as most DNS providers want it typed: relative to the
    /// zone (`book` for `book.example.com` in `example.com`), `@` for the zone
    /// apex. The full name unchanged when it is not inside the zone. Case and
    /// a trailing dot are ignored when comparing.
    static func relativeName(_ name: String, zone: String?) -> String {
        guard let zone, !zone.isEmpty else { return name }
        let trimmedName = name.hasSuffix(".") ? String(name.dropLast()) : name
        let trimmedZone = zone.hasSuffix(".") ? String(zone.dropLast()) : zone
        let lowerName = trimmedName.lowercased()
        let lowerZone = trimmedZone.lowercased()
        if lowerName == lowerZone { return "@" }
        guard lowerName.hasSuffix("." + lowerZone) else { return name }
        return String(trimmedName.prefix(trimmedName.count - trimmedZone.count - 1))
    }

    /// The form to draw: cards in a compact width or at accessibility text
    /// sizes when automatic.
    static func form(
        _ layout: MonarchDnsRecordTableLayout,
        isRegularWidth: Bool,
        isAccessibilitySize: Bool
    ) -> MonarchDnsRecordTableForm {
        switch layout {
        case .cards: .cards
        case .table: .table
        case .automatic: isRegularWidth && !isAccessibilitySize ? .table : .cards
        }
    }

    /// How many records were checked and how many of them were found.
    static func counts(_ records: [MonarchDnsRecord]) -> (found: Int, checked: Int) {
        let checked = records.filter { $0.isFound != nil }
        return (checked.filter { $0.isFound == true }.count, checked.count)
    }

    /// Whether the table needs a TTL column.
    static func hasTTL(_ records: [MonarchDnsRecord]) -> Bool {
        records.contains { $0.ttl != nil }
    }

    /// Whether the table needs a priority column.
    static func hasPriority(_ records: [MonarchDnsRecord]) -> Bool {
        records.contains { $0.priority != nil }
    }

    /// The status badge of a record, or `nil` when it was not checked.
    /// The status is a word as well as a tone.
    static func status(_ record: MonarchDnsRecord, labels: MonarchDnsRecordTableLabels) -> MonarchStatusDefinition? {
        guard let isFound = record.isFound else { return nil }
        return isFound
            ? MonarchStatusDefinition(labels.resolvedFound, tone: .success)
            : MonarchStatusDefinition(labels.resolvedWaiting, tone: .neutral)
    }

    /// What VoiceOver reads for a table row before its cells: the type and
    /// the name as shown.
    static func rowLabel(_ record: MonarchDnsRecord, shown: String) -> String {
        "\(record.type) \(shown)"
    }
}

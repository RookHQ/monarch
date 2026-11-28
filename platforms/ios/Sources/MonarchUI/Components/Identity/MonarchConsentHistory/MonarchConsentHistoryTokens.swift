import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchConsentHistory``.
enum MonarchConsentHistoryTokens {
    /// The width of the label column of a record, as on the web.
    static let labelWidth: CGFloat = 112
    /// The gap between the header, the message and the records.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between records.
    static let recordSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a record.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a line's name and its value.
    static let valueSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between an icon and its text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// Padding inside a record.
    static let recordPadding: CGFloat = MonarchTokens.Space.space4
    /// The width of the border of a record.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The SF Symbol of an allowed category.
    static let allowedSymbol = "checkmark"
    /// The SF Symbol of a refused category.
    static let deniedSymbol = "xmark"
    /// The SF Symbol of the export button.
    static let exportSymbol = "arrow.down.doc"
    /// The SF Symbol of the export failure.
    static let errorSymbol = "exclamationmark.circle.fill"
    /// The SF Symbol of the empty state.
    static let emptySymbol = "checkmark.shield"

    /// One category of a record, in display order.
    struct CategoryRow: Equatable, Identifiable {
        let id: String
        let label: String
        let isAllowed: Bool
    }

    /// The records, newest first. Records with the same date keep their order.
    static func sorted(_ records: [MonarchConsentRecord]) -> [MonarchConsentRecord] {
        records.enumerated()
            .sorted { lhs, rhs in
                lhs.element.date != rhs.element.date ? lhs.element.date > rhs.element.date : lhs.offset < rhs.offset
            }
            .map(\.element)
    }

    /// The categories of a record: the known ones in their order and with
    /// their labels, then any other ids by id, shown as the raw id.
    static func categoryRows(
        _ record: MonarchConsentRecord,
        categories: [MonarchConsentCategory]
    ) -> [CategoryRow] {
        let known = categories.compactMap { category in
            record.categories[category.id].map { CategoryRow(id: category.id, label: category.label, isAllowed: $0) }
        }
        let knownIDs = Set(categories.map(\.id))
        let rest = record.categories.keys
            .filter { !knownIDs.contains($0) }
            .sorted()
            .map { CategoryRow(id: $0, label: $0, isAllowed: record.categories[$0] == true) }
        return known + rest
    }

    /// How a method reads.
    static func methodText(_ method: MonarchConsentMethod, labels: MonarchConsentHistoryLabels) -> String {
        labels.method?(method) ?? MonarchConsentHistoryStrings.method(method)
    }

    /// How a regime reads.
    static func regimeText(_ regime: MonarchConsentRegime, labels: MonarchConsentHistoryLabels) -> String {
        labels.regime?(regime) ?? MonarchConsentHistoryStrings.regime(regime)
    }

    /// The text after a category.
    static func decisionText(isAllowed: Bool, labels: MonarchConsentHistoryLabels) -> String {
        isAllowed
            ? labels.allowed ?? MonarchConsentHistoryStrings.allowed
            : labels.denied ?? MonarchConsentHistoryStrings.denied
    }

    /// What VoiceOver reads for one category: "Analytics, Allowed".
    static func categoryLabel(_ row: CategoryRow, labels: MonarchConsentHistoryLabels) -> String {
        "\(row.label), \(decisionText(isAllowed: row.isAllowed, labels: labels))"
    }

    /// The extra signal line, or `nil` when the method already says it or
    /// there was no signal.
    static func signalNote(_ record: MonarchConsentRecord, labels: MonarchConsentHistoryLabels) -> String? {
        guard record.isGlobalPrivacyControl, record.method != .globalPrivacyControl else { return nil }
        return labels.signalPresent ?? MonarchConsentHistoryStrings.signalPresent
    }

    /// The colour role of a category's icon. The text says the same, so the
    /// colour is never the only signal.
    static func decisionColor(isAllowed: Bool) -> MonarchColorRole {
        isAllowed ? \.successStrong : \.textMuted
    }
}

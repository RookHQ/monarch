import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and the rules of ``MonarchMergeDialog``.
enum MonarchMergeDialogTokens {
    /// The gap between a value and its source, and a title and its description.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between a record and its badge.
    static let badgeSpacing: CGFloat = MonarchTokens.Space.space2
    /// The canvas behind the form.
    static var canvas: MonarchColorRole { \.bgCanvas }
    /// The fill of the rows.
    static var row: MonarchColorRole { \.bgSurface }
    /// The main text.
    static var text: MonarchColorRole { \.textDefault }
    /// Secondary text: descriptions and sources.
    static var muted: MonarchColorRole { \.textMuted }
    /// An empty value.
    static var emptyText: MonarchColorRole { \.textSubtle }

    /// Whether a value counts as empty: missing, or blank after trimming.
    static func isEmpty(_ value: String?) -> Bool {
        value?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true
    }

    /// The fields whose values differ between the records, and those that
    /// match in every record (empty counts as equal to empty), in field order.
    static func split(fields: [MonarchMergeField], records: [MonarchMergeRecord]) -> (
        differing: [MonarchMergeField], same: [MonarchMergeField]
    ) {
        var differing: [MonarchMergeField] = []
        var same: [MonarchMergeField] = []
        for field in fields {
            let keys = Set(
                records.map { $0.values[field.id]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "" }
            )
            if keys.count <= 1 { same.append(field) } else { differing.append(field) }
        }
        return (differing, same)
    }

    /// The record whose value is kept by default: the primary record's, or
    /// the first other record with a value when the primary one is empty.
    static func defaultChoice(field: String, records: [MonarchMergeRecord], primaryID: String) -> String {
        if let primary = records.first(where: { $0.id == primaryID }), !isEmpty(primary.values[field]) {
            return primaryID
        }
        return records.first { $0.id != primaryID && !isEmpty($0.values[field]) }?.id ?? primaryID
    }

    /// The record whose value is kept: the person's pick, or the default.
    static func choice(
        field: String,
        picked: [String: String],
        records: [MonarchMergeRecord],
        primaryID: String
    ) -> String {
        if let pick = picked[field], records.contains(where: { $0.id == pick }) { return pick }
        return defaultChoice(field: field, records: records, primaryID: primaryID)
    }

    /// The primary record: the chosen one, or the first.
    static func primaryID(_ chosen: String, records: [MonarchMergeRecord]) -> String? {
        records.contains { $0.id == chosen } ? chosen : records.first?.id
    }

    /// What the person chose, or `nil` when there is no record.
    static func selection(
        records: [MonarchMergeRecord],
        fields: [MonarchMergeField],
        primaryID chosen: String,
        picked: [String: String]
    ) -> MonarchMergeSelection? {
        guard let primaryID = primaryID(chosen, records: records) else { return nil }
        var choices: [String: String] = [:]
        for field in fields {
            choices[field.id] = choice(field: field.id, picked: picked, records: records, primaryID: primaryID)
        }
        return MonarchMergeSelection(
            primaryID: primaryID,
            mergedIDs: records.map(\.id).filter { $0 != primaryID },
            fields: choices
        )
    }

    /// Whether the merge cannot run: fewer than two records, or a blocker.
    static func isBlocked(recordCount: Int, blockers: [String]) -> Bool {
        recordCount < 2 || !blockers.isEmpty
    }

    /// The text of a value, or "Empty".
    static func valueText(_ value: String?, labels: MonarchMergeDialogLabels) -> String {
        isEmpty(value) ? (labels.empty ?? MonarchMergeDialogStrings.empty) : (value ?? "")
    }

    /// The source line under a value: "from Acme Ltd".
    static func sourceText(_ record: MonarchMergeRecord, labels: MonarchMergeDialogLabels) -> String {
        labels.from?(record.title) ?? MonarchMergeDialogStrings.from(record.title)
    }

    /// What VoiceOver reads for one value option: "hello@acme.com, from Acme Ltd".
    static func optionLabel(value: String?, record: MonarchMergeRecord, labels: MonarchMergeDialogLabels) -> String {
        "\(valueText(value, labels: labels)), \(sourceText(record, labels: labels))"
    }

    /// The confirm button's title.
    static func confirmTitle(count: Int, labels: MonarchMergeDialogLabels) -> String {
        labels.confirm?(count) ?? MonarchMergeDialogStrings.confirm(count)
    }

    /// The message shown when the merge throws: the error's description
    /// when it has one, or the built-in message.
    static func errorMessage(_ error: any Error, labels: MonarchMergeDialogLabels) -> String {
        MonarchFailureMessage.message(for: error, fallback: labels.error ?? MonarchMergeDialogStrings.error)
    }
}

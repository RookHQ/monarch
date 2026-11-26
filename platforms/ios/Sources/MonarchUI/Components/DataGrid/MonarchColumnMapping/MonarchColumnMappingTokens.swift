import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchColumnMapping``.
enum MonarchColumnMappingTokens {
    /// The gap between the lines of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the bulk action buttons.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The most example values shown per column.
    static let maxSamples = 3
    /// The lowest score of a close suggestion.
    static let defaultThreshold = 0.75
    /// The SF Symbol in front of a duplicate error.
    static let errorSymbol = "exclamationmark.circle"
    /// Colours.
    static var nameColor: MonarchColorRole { \.textDefault }
    static var mutedColor: MonarchColorRole { \.textMuted }
    static var errorColor: MonarchColorRole { \.dangerStrong }

    /// The badge tone of a suggestion: success when exact, info when close.
    static func badgeTone(_ confidence: MonarchColumnMappingConfidence) -> MonarchTone {
        confidence == .exact ? .success : .info
    }

    /// The badge text of a suggestion.
    static func badgeText(_ confidence: MonarchColumnMappingConfidence, labels: MonarchColumnMappingLabels) -> String {
        switch confidence {
        case .exact: labels.exactMatch ?? MonarchColumnMappingStrings.exactMatch
        case .similar: labels.suggested ?? MonarchColumnMappingStrings.suggested
        }
    }

    // MARK: Names and similarity

    /// Lower-cases and keeps only letters and digits: "E-mail Address" gives "emailaddress".
    static func normalize(_ value: String) -> String {
        String(
            String.UnicodeScalarView(
                value.decomposedStringWithCompatibilityMapping.lowercased().unicodeScalars.filter(isLetterOrDigit)
            )
        )
    }

    /// The words of a name, splitting camel case: "firstName" gives ["first", "name"].
    static func words(_ value: String) -> [String] {
        var spaced = ""
        var previous: Character?
        for character in value.decomposedStringWithCompatibilityMapping {
            if let previous, previous.isLowercase, character.isUppercase { spaced.append(" ") }
            spaced.append(character)
            previous = character
        }
        var words: [String] = []
        var current = String.UnicodeScalarView()
        for scalar in spaced.lowercased().unicodeScalars {
            if isLetterOrDigit(scalar) {
                current.append(scalar)
            } else if !current.isEmpty {
                words.append(String(current))
                current = String.UnicodeScalarView()
            }
        }
        if !current.isEmpty { words.append(String(current)) }
        return words
    }

    /// The similarity of two short names, from 0 to 1: the best of word
    /// overlap, one name containing the other, and edit distance.
    static func similarity(_ a: String, _ b: String) -> Double {
        let na = Array(normalize(a).unicodeScalars)
        let nb = Array(normalize(b).unicodeScalars)
        guard !na.isEmpty, !nb.isEmpty else { return 0 }
        if na == nb { return 1 }
        let wa = Set(words(a))
        let wb = Set(words(b))
        let dice = wa.count + wb.count > 0 ? Double(2 * wa.intersection(wb).count) / Double(wa.count + wb.count) : 0
        let shorter = min(na.count, nb.count)
        let longer = max(na.count, nb.count)
        let sa = String(String.UnicodeScalarView(na))
        let sb = String(String.UnicodeScalarView(nb))
        let contains =
            shorter >= 3 && (sa.contains(sb) || sb.contains(sa))
            ? 0.6 + 0.3 * Double(shorter) / Double(longer) : 0
        let edit = 1 - Double(distance(na, nb)) / Double(longer)
        return max(dice, contains, edit)
    }

    /// The Levenshtein distance of two scalar arrays.
    static func distance(_ a: [Unicode.Scalar], _ b: [Unicode.Scalar]) -> Int {
        guard !a.isEmpty else { return b.count }
        guard !b.isEmpty else { return a.count }
        var previous = Array(0...b.count)
        for i in 1...a.count {
            var diagonal = previous[0]
            previous[0] = i
            for j in 1...b.count {
                let up = previous[j]
                previous[j] = min(up + 1, previous[j - 1] + 1, diagonal + (a[i - 1] == b[j - 1] ? 0 : 1))
                diagonal = up
            }
        }
        return previous[b.count]
    }

    private static func isLetterOrDigit(_ scalar: Unicode.Scalar) -> Bool {
        switch scalar.properties.generalCategory {
        case .uppercaseLetter, .lowercaseLetter, .titlecaseLetter, .modifierLetter, .otherLetter,
            .decimalNumber, .letterNumber, .otherNumber:
            true
        default:
            false
        }
    }

    // MARK: Mapping

    /// Suggests a field for each source: exact matches first, then close
    /// matches scoring at least `threshold`, each field for one column at most.
    static func suggest(
        _ sources: [MonarchColumnMappingSource],
        fields: [MonarchColumnMappingField],
        threshold: Double
    ) -> [String: MonarchColumnMappingSuggestion] {
        var candidates: [(order: Int, source: String, field: String, score: Double)] = []
        for source in sources {
            for field in fields {
                var best = 0.0
                for name in [field.id, field.label] + field.aliases {
                    best = max(best, similarity(source.name, name))
                    if best == 1 { break }
                }
                if best >= threshold {
                    candidates.append((candidates.count, source.name, field.id, best))
                }
            }
        }
        candidates.sort { ($1.score, $0.order) < ($0.score, $1.order) }
        var result: [String: MonarchColumnMappingSuggestion] = [:]
        var usedFields = Set<String>()
        for candidate in candidates where result[candidate.source] == nil && !usedFields.contains(candidate.field) {
            result[candidate.source] = MonarchColumnMappingSuggestion(
                fieldID: candidate.field,
                confidence: candidate.score == 1 ? .exact : .similar,
                score: (candidate.score * 100).rounded() / 100
            )
            usedFields.insert(candidate.field)
        }
        return result
    }

    /// Required fields that no column maps to, and fields chosen for more than one column.
    static func issues(
        _ mapping: [String: String],
        fields: [MonarchColumnMappingField],
        sourceOrder: [String]
    ) -> (missing: [MonarchColumnMappingField], duplicates: [String: [String]]) {
        var bySource: [String: [String]] = [:]
        var seen = Set<String>()
        let names = (sourceOrder + mapping.keys.sorted()).filter { seen.insert($0).inserted }
        for name in names {
            guard let field = mapping[name] else { continue }
            bySource[field, default: []].append(name)
        }
        let duplicates = bySource.filter { $0.value.count > 1 }
        return (fields.filter { $0.isRequired && bySource[$0.id] == nil }, duplicates)
    }

    /// The number of sources mapped to a known field.
    static func mappedCount(
        _ mapping: [String: String],
        sources: [MonarchColumnMappingSource],
        fields: [MonarchColumnMappingField]
    ) -> Int {
        let ids = Set(fields.map(\.id))
        return sources.filter { mapping[$0.name].map(ids.contains) ?? false }.count
    }

    /// The mapping of the suggestions.
    static func mapping(from suggestions: [String: MonarchColumnMappingSuggestion]) -> [String: String] {
        suggestions.mapValues(\.fieldID)
    }

    /// Whether "Use suggestions" is offered: there are suggestions and the
    /// mapping differs from them.
    static func offersSuggestions(
        _ mapping: [String: String],
        suggestions: [String: MonarchColumnMappingSuggestion],
        sources: [MonarchColumnMappingSource]
    ) -> Bool {
        let suggested = self.mapping(from: suggestions)
        return !suggestions.isEmpty && sources.contains { mapping[$0.name] != suggested[$0.name] }
    }

    /// Whether "Don’t import any" is offered: some column is mapped.
    static func offersSkipAll(_ mapping: [String: String], sources: [MonarchColumnMappingSource]) -> Bool {
        sources.contains { mapping[$0.name] != nil }
    }

    // MARK: Text

    /// The name of a source, or its position when the header is blank.
    static func displayName(_ source: MonarchColumnMappingSource, index: Int) -> String {
        source.name.trimmingCharacters(in: .whitespaces).isEmpty ? "#\(index + 1)" : source.name
    }

    /// The example values line.
    static func samplesText(_ source: MonarchColumnMappingSource, labels: MonarchColumnMappingLabels) -> String {
        let shown = source.samples.filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }.prefix(maxSamples)
        return shown.isEmpty ? labels.noSamples ?? MonarchColumnMappingStrings.noSamples : shown.joined(separator: ", ")
    }

    /// A field's name in the picker, with "(required)" when it must be mapped.
    static func optionTitle(_ field: MonarchColumnMappingField, labels: MonarchColumnMappingLabels) -> String {
        field.isRequired ? "\(field.label) (\(labels.required ?? MonarchColumnMappingStrings.required))" : field.label
    }

    /// The summary line.
    static func summary(mapped: Int, total: Int, labels: MonarchColumnMappingLabels) -> String {
        labels.summary?(mapped, total) ?? MonarchColumnMappingStrings.summary(mapped: mapped, total: total)
    }

    /// The error for missing required fields, or `nil` when none is missing.
    static func missingText(_ missing: [MonarchColumnMappingField], labels: MonarchColumnMappingLabels) -> String? {
        guard !missing.isEmpty else { return nil }
        let names = missing.map(\.label).joined(separator: ", ")
        return labels.missing?(names) ?? MonarchColumnMappingStrings.missing(names)
    }

    /// The error of a column whose field is also chosen for other columns, or `nil`.
    static func duplicateText(
        source: String,
        field: MonarchColumnMappingField?,
        duplicates: [String: [String]],
        labels: MonarchColumnMappingLabels
    ) -> String? {
        guard let field, let list = duplicates[field.id] else { return nil }
        let others = list.filter { $0 != source }
        guard !others.isEmpty else { return nil }
        let joined = others.joined(separator: ", ")
        return labels.duplicate?(field.label, joined)
            ?? MonarchColumnMappingStrings.duplicate(field.label, others: joined)
    }

    /// The spoken name of a picker.
    static func pickerLabel(_ name: String, labels: MonarchColumnMappingLabels) -> String {
        labels.fieldFor?(name) ?? MonarchColumnMappingStrings.fieldFor(name)
    }

    /// What VoiceOver reads after a picker's name and value: the example
    /// values, the suggestion badge, the error and the field's help.
    static func pickerHint(samples: String, badge: String?, error: String?, help: String?) -> String {
        [samples, badge, error, help].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ". ")
    }
}

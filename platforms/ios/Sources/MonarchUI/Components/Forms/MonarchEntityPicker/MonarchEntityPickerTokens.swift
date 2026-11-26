import Foundation
import MonarchTokens
import SwiftUI

/// What the list of a ``MonarchEntityPicker`` shows.
enum MonarchEntityPickerContent: Equatable {
    /// Rows, by id.
    case rows([String])
    /// A progress row.
    case loading
    /// "Type at least N characters to search."
    case tooShort
    /// The error with a retry button.
    case failed
    /// "No matches".
    case empty
}

/// Component tokens and pure logic of ``MonarchEntityPicker``.
enum MonarchEntityPickerTokens {
    /// The avatar size in the field and in the rows.
    static let fieldAvatarSize: MonarchAvatarSize = .sm
    /// The avatar size in the rows.
    static let rowAvatarSize: MonarchAvatarSize = .md
    /// The gap between the parts of the field and of a row.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The default wait after typing before searching.
    static let defaultDebounce: Duration = .milliseconds(250)

    /// The default filter of a static list: the name, hint or keywords
    /// contain the query, ignoring case and diacritics.
    static func matches(_ entity: MonarchEntityPickerEntity, query: String) -> Bool {
        let query = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return true }
        return [entity.name, entity.hint, entity.keywords].contains { $0?.localizedStandardContains(query) ?? false }
    }

    /// What the list shows. Without a search the static list is filtered
    /// here; with one the search phase decides, and the static list is shown
    /// until the first search runs.
    static func content(
        entities: [MonarchEntityPickerEntity],
        query: String,
        hasSearch: Bool,
        phase: MonarchDebouncedSearchPhase,
        results: [MonarchEntityPickerEntity],
        isLoading: Bool
    ) -> MonarchEntityPickerContent {
        let shown: [MonarchEntityPickerEntity]
        if !hasSearch {
            shown = entities.filter { matches($0, query: query) }
        } else {
            switch phase {
            case .idle: shown = entities
            case .tooShort: return .tooShort
            case .loading: return .loading
            case .failed: return .failed
            case .done: shown = results
            }
        }
        if shown.isEmpty { return isLoading ? .loading : .empty }
        return .rows(shown.map(\.id))
    }

    /// Whether to offer creating the typed text: there is a create handler,
    /// the text is not blank and no shown entity has that name.
    static func showsCreate(query: String, shown: [MonarchEntityPickerEntity], canCreate: Bool) -> Bool {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard canCreate, !trimmed.isEmpty else { return false }
        return !shown.contains {
            $0.name.compare(trimmed, options: [.caseInsensitive, .diacriticInsensitive]) == .orderedSame
        }
    }

    /// The selection after a row is activated in a multiple picker: a chosen
    /// id is removed, a new one is added at the end.
    static func toggled(_ selection: [String], id: String) -> [String] {
        selection.contains(id) ? selection.filter { $0 != id } : selection + [id]
    }

    /// The text of the field: the name of the one chosen entity, "First +N"
    /// for several, or `nil` for none.
    static func summary(names: [String], labels: MonarchEntityPickerLabels) -> String? {
        guard let first = names.first else { return nil }
        guard names.count > 1 else { return first }
        return labels.summary?(first, names.count - 1) ?? MonarchEntityPickerStrings.summary(first, names.count - 1)
    }

    /// The name of a chosen id: from the shown entities, the known ones, or the raw id.
    static func name(of id: String, known: [String: MonarchEntityPickerEntity]) -> String {
        known[id]?.name ?? id
    }

    /// What VoiceOver reads as the field's value: every chosen name, or "None".
    static func accessibilityValue(names: [String], labels: MonarchEntityPickerLabels) -> String {
        names.isEmpty ? (labels.noSelection ?? MonarchEntityPickerStrings.noSelection) : names.joined(separator: ", ")
    }

    /// One heading with its entities.
    struct Section: Equatable {
        /// The heading, or `nil` for the entities without a group.
        let title: String?
        /// The entities under it, in their order.
        let ids: [String]
    }

    /// The entities in sections: those without a group first, then each
    /// group in the order it first appears, as the combobox does.
    static func sections(_ entities: [MonarchEntityPickerEntity]) -> [Section] {
        var order: [String?] = []
        var grouped: [String?: [String]] = [:]
        for entity in entities {
            let key = entity.group.flatMap { $0.isEmpty ? nil : $0 }
            if grouped[key] == nil { order.append(key) }
            grouped[key, default: []].append(entity.id)
        }
        if let index = order.firstIndex(where: { $0 == nil }), index > 0 {
            order.remove(at: index)
            order.insert(nil, at: 0)
        }
        return order.map { Section(title: $0, ids: grouped[$0] ?? []) }
    }

    /// What VoiceOver reads for a row: the name, the description, the hint
    /// and the presence.
    static func accessibilityLabel(_ entity: MonarchEntityPickerEntity) -> String {
        let presence = entity.status.map { status in
            entity.statusLabel.flatMap { $0.isEmpty ? nil : $0 } ?? MonarchPresenceDot.label(for: status)
        }
        return [entity.name, entity.description, entity.hint, presence]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }

    /// The message of the content states that have one.
    static func message(_ content: MonarchEntityPickerContent, minLength: Int, labels: MonarchEntityPickerLabels)
        -> String?
    {
        switch content {
        case .rows: nil
        case .loading: labels.loading ?? MonarchEntityPickerStrings.loading
        case .tooShort: labels.tooShort?(minLength) ?? MonarchEntityPickerStrings.tooShort(minLength)
        case .failed: labels.failed ?? MonarchEntityPickerStrings.failed
        case .empty: labels.empty ?? MonarchEntityPickerStrings.empty
        }
    }
}

import MonarchTokens
import SwiftUI

/// The check state of one source in the tree.
enum MonarchAiKnowledgeScopeCheck: Equatable {
    /// Selected, itself or through a group.
    case checked
    /// Not selected.
    case unchecked
    /// A group with only some of its contents selected.
    case mixed
}

/// One visible row of the tree.
struct MonarchAiKnowledgeScopeRow: Identifiable, Equatable {
    /// The source.
    let source: MonarchAiKnowledgeSource
    /// The depth, 1 for a top-level source.
    let level: Int
    /// Whether the source has children.
    var isGroup: Bool { !source.children.isEmpty }
    /// The id of the source.
    var id: String { source.id }
}

/// Component tokens and logic for ``MonarchAiKnowledgeScope``. The selection
/// logic matches the web's `aiKnowledgeScopeState` and `toggleAiKnowledgeSource`.
enum MonarchAiKnowledgeScopeTokens {
    /// The indent of each level.
    static let indent: CGFloat = MonarchTokens.Space.space5
    /// The gap between the parts of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the label and the description.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the tree and the summary.
    static let summarySpacing: CGFloat = MonarchTokens.Space.space2
    /// The width kept for the disclosure button, so rows line up.
    static let disclosureWidth: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The SF Symbol of a collapsed group. It follows the reading direction.
    static let collapsedSymbol = "chevron.forward"
    /// The SF Symbol of an expanded group.
    static let expandedSymbol = "chevron.down"
    /// The SF Symbol of the empty state.
    static let emptySymbol = "books.vertical"
    /// The colour role of a label.
    static var label: MonarchColorRole { \.textDefault }
    /// The colour role of a description, a kind and the summary.
    static var secondary: MonarchColorRole { \.textMuted }
    /// The colour role of the disclosure chevron.
    static var chevron: MonarchColorRole { \.textSubtle }

    /// The check state of every source for a selection, by id. A source is
    /// checked when its id is selected, when an ancestor's is, or when all
    /// its children are checked; mixed when only some of its descendants are.
    static func states(
        _ sources: [MonarchAiKnowledgeSource],
        selection: [String]
    ) -> [String: MonarchAiKnowledgeScopeCheck] {
        let selected = Set(selection)
        var out: [String: MonarchAiKnowledgeScopeCheck] = [:]
        func visit(_ node: MonarchAiKnowledgeSource, inherited: Bool) -> MonarchAiKnowledgeScopeCheck {
            let isSelf = inherited || selected.contains(node.id)
            let state: MonarchAiKnowledgeScopeCheck
            if node.children.isEmpty {
                state = isSelf ? .checked : .unchecked
            } else {
                let children = node.children.map { visit($0, inherited: isSelf) }
                if isSelf || children.allSatisfy({ $0 == .checked }) {
                    state = .checked
                } else if children.allSatisfy({ $0 == .unchecked }) {
                    state = .unchecked
                } else {
                    state = .mixed
                }
            }
            out[node.id] = state
            return state
        }
        for source in sources { _ = visit(source, inherited: false) }
        return out
    }

    /// The selection after a person toggles source `id`: a checked or mixed
    /// source becomes unchecked with everything in it, an unchecked one
    /// becomes checked with everything in it.
    static func toggled(_ sources: [MonarchAiKnowledgeSource], selection: [String], id: String) -> [String] {
        let isOn = states(sources, selection: selection)[id] == .unchecked
        return setting(sources, selection: selection, id: id, isOn: isOn)
    }

    /// The selection after source `id` and everything in it is turned on or
    /// off. The result lists every fully selected source, groups included,
    /// in tree order; ids that are not in the tree are kept at the end.
    static func setting(
        _ sources: [MonarchAiKnowledgeSource],
        selection: [String],
        id: String,
        isOn: Bool
    ) -> [String] {
        let state = states(sources, selection: selection)
        guard let target = find(id, in: sources) else { return selection }
        var checked = Set(state.filter { $0.value == .checked }.map(\.key))
        func mark(_ node: MonarchAiKnowledgeSource) {
            if isOn { checked.insert(node.id) } else { checked.remove(node.id) }
            node.children.forEach(mark)
        }
        mark(target)
        // Groups follow their children, bottom up.
        func settle(_ node: MonarchAiKnowledgeSource) -> Bool {
            guard !node.children.isEmpty else { return checked.contains(node.id) }
            let all = node.children.map(settle).allSatisfy { $0 }
            if all { checked.insert(node.id) } else { checked.remove(node.id) }
            return all
        }
        for source in sources { _ = settle(source) }
        var result: [String] = []
        func collect(_ node: MonarchAiKnowledgeSource) {
            if checked.contains(node.id) { result.append(node.id) }
            node.children.forEach(collect)
        }
        sources.forEach(collect)
        for value in selection where state[value] == nil && !result.contains(value) {
            result.append(value)
        }
        return result
    }

    /// The source with an id, anywhere in the tree.
    static func find(_ id: String, in sources: [MonarchAiKnowledgeSource]) -> MonarchAiKnowledgeSource? {
        for source in sources {
            if source.id == id { return source }
            if let hit = find(id, in: source.children) { return hit }
        }
        return nil
    }

    /// The number of selected sources, a fully selected group counting as one.
    static func selectedCount(
        _ sources: [MonarchAiKnowledgeSource],
        states: [String: MonarchAiKnowledgeScopeCheck]
    ) -> Int {
        sources.reduce(0) { count, source in
            switch states[source.id] {
            case .checked: count + 1
            case .mixed: count + selectedCount(source.children, states: states)
            case .unchecked, nil: count
            }
        }
    }

    /// The rows to show: every top-level source, and the children of each
    /// expanded group, in tree order.
    static func visibleRows(
        _ sources: [MonarchAiKnowledgeSource],
        expanded: Set<String>,
        level: Int = 1
    ) -> [MonarchAiKnowledgeScopeRow] {
        sources.flatMap { source -> [MonarchAiKnowledgeScopeRow] in
            let row = MonarchAiKnowledgeScopeRow(source: source, level: level)
            guard row.isGroup, expanded.contains(source.id) else { return [row] }
            return [row] + visibleRows(source.children, expanded: expanded, level: level + 1)
        }
    }

    /// The leading indent of a row.
    static func indent(level: Int) -> CGFloat {
        CGFloat(max(0, level - 1)) * indent
    }

    /// The name of a kind.
    static func kindName(_ kind: MonarchAiKnowledgeSourceKind, labels: MonarchAiKnowledgeScopeLabels) -> String {
        labels.kind?(kind) ?? MonarchAiKnowledgeScopeStrings.kind(kind)
    }

    /// What VoiceOver reads for a source: "Handbook, Space".
    static func itemLabel(_ source: MonarchAiKnowledgeSource, labels: MonarchAiKnowledgeScopeLabels) -> String {
        let kind = kindName(source.kind, labels: labels)
        return labels.itemLabel?(source.label, kind)
            ?? MonarchAiKnowledgeScopeStrings.itemLabel(source.label, kind: kind)
    }

    /// The spoken name of a group's disclosure button.
    static func disclosureLabel(
        _ source: MonarchAiKnowledgeSource,
        isExpanded: Bool,
        labels: MonarchAiKnowledgeScopeLabels
    ) -> String {
        isExpanded
            ? (labels.collapse?(source.label) ?? MonarchAiKnowledgeScopeStrings.collapse(source.label))
            : (labels.expand?(source.label) ?? MonarchAiKnowledgeScopeStrings.expand(source.label))
    }

    /// The summary under the tree.
    static func summary(count: Int, labels: MonarchAiKnowledgeScopeLabels) -> String {
        labels.summary?(count) ?? MonarchAiKnowledgeScopeStrings.summary(count)
    }
}

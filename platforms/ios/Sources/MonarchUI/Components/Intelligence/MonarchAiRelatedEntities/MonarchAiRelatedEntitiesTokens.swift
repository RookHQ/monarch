import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiRelatedEntities``.
enum MonarchAiRelatedEntitiesTokens {
    /// The gap between list rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts of a row.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The padding inside a list row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space2
    /// The width of a relevance bar, before Dynamic Type.
    static let weightWidth: CGFloat = MonarchTokens.Space.space12
    /// The height of a relevance bar.
    static let weightHeight: CGFloat = MonarchTokens.Space.space1
    /// The width of a graph node, before Dynamic Type.
    static let nodeWidth: CGFloat = MonarchTokens.Space.space20
    /// The largest side of the graph, in node widths.
    static let graphSideInNodes: CGFloat = 4
    /// The line width of a relation in the graph.
    static let edgeWidth: CGFloat = MonarchTokens.Space.px
    /// The most lines of a graph node's label.
    static let nodeLabelLines = 2
    /// The most graph nodes by default.
    static let defaultMaxGraphNodes = 12
    /// The SF Symbol of the empty state.
    static let emptySymbol = "point.3.connected.trianglepath.dotted"
    /// The colour role of names.
    static var text: MonarchColorRole { \.textDefault }
    /// The colour role of relations and node labels.
    static var muted: MonarchColorRole { \.textMuted }
    /// The colour role of the relevance track.
    static var track: MonarchColorRole { \.bgMuted }
    /// The colour role of the relevance fill.
    static var fill: MonarchColorRole { \.aiSolid }
    /// The colour role of relation lines.
    static var edge: MonarchColorRole { \.borderDefault }
    /// The colour role of the row border.
    static var border: MonarchColorRole { \.borderSubtle }

    /// A weight limited to 0 through 1, or `nil` when missing or invalid.
    static func clamped(_ weight: Double?) -> Double? {
        guard let weight, weight.isFinite else { return nil }
        return min(1, max(0, weight))
    }

    /// The entities by weight, highest first; entities without a weight keep
    /// their order after the weighted ones.
    static func sorted(_ entities: [MonarchAiRelatedEntity]) -> [MonarchAiRelatedEntity] {
        entities.enumerated()
            .sorted { lhs, rhs in
                let left = clamped(lhs.element.weight) ?? -1
                let right = clamped(rhs.element.weight) ?? -1
                return left != right ? left > right : lhs.offset < rhs.offset
            }
            .map(\.element)
    }

    /// Whether the diagram is drawn: the graph variant, at least one entity,
    /// no more than the limit, and not at an accessibility text size.
    static func showsGraph(
        variant: MonarchAiRelatedEntitiesVariant,
        count: Int,
        maxNodes: Int,
        isAccessibilitySize: Bool
    ) -> Bool {
        variant == .graph && count > 0 && count <= maxNodes && !isAccessibilitySize
    }

    /// The entity in the middle of the graph: `centerID`, else the first by weight.
    static func center(of sorted: [MonarchAiRelatedEntity], centerID: String?) -> MonarchAiRelatedEntity? {
        sorted.first { $0.id == centerID } ?? sorted.first
    }

    /// The graph's entities in drawing order: the centre, then the ring by weight.
    static func graphOrder(_ entities: [MonarchAiRelatedEntity], centerID: String?) -> [MonarchAiRelatedEntity] {
        let ordered = sorted(entities)
        guard let center = center(of: ordered, centerID: centerID) else { return [] }
        return [center] + ordered.filter { $0.id != center.id }
    }

    /// The name of a kind.
    static func kindName(_ kind: MonarchAiRelatedEntityKind, labels: MonarchAiRelatedEntitiesLabels) -> String {
        labels.kind?(kind) ?? MonarchAiRelatedEntitiesStrings.kind(kind)
    }

    /// The SF Symbol of a kind's avatar; `nil` for a person, who gets initials.
    static func symbol(_ kind: MonarchAiRelatedEntityKind) -> String? {
        switch kind {
        case .person: nil
        case .org: "building.2"
        case .project: "folder"
        case .document: "doc.text"
        case .record: "tablecells"
        case .place: "mappin.and.ellipse"
        default: "circle.hexagongrid"
        }
    }

    /// The spoken relevance: "Relevance 80%".
    static func weightText(_ weight: Double, locale: Locale, labels: MonarchAiRelatedEntitiesLabels) -> String {
        labels.weight?(weight)
            ?? MonarchAiRelatedEntitiesStrings.weight(
                weight.formatted(.percent.precision(.fractionLength(0)).locale(locale))
            )
    }

    /// The relations of an entity, as text: "works at Acme".
    static func relations(
        of id: String,
        entities: [MonarchAiRelatedEntity],
        relations: [MonarchAiEntityRelation],
        labels: MonarchAiRelatedEntitiesLabels
    ) -> [String] {
        let byID = Dictionary(entities.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        return relations.compactMap { relation in
            let otherID = relation.from == id ? relation.to : (relation.to == id ? relation.from : nil)
            guard let otherID, let other = byID[otherID] else { return nil }
            if let custom = labels.relation { return custom(other.label, relation.label) }
            guard let label = relation.label, !label.isEmpty else { return other.label }
            return MonarchAiRelatedEntitiesStrings.relation(other.label, label: label)
        }
    }

    /// The relations line: "Related to: works at Acme, Q3 launch", or `nil`.
    static func relationsLine(_ relations: [String], labels: MonarchAiRelatedEntitiesLabels) -> String? {
        guard !relations.isEmpty else { return nil }
        return "\(labels.relatedTo ?? MonarchAiRelatedEntitiesStrings.relatedTo): \(relations.joined(separator: ", "))"
    }

    /// What VoiceOver reads for an entity: its name, kind, relevance and relations.
    static func accessibilityLabel(
        _ entity: MonarchAiRelatedEntity,
        entities: [MonarchAiRelatedEntity],
        relations: [MonarchAiEntityRelation],
        locale: Locale,
        labels: MonarchAiRelatedEntitiesLabels
    ) -> String {
        var parts = [entity.label, kindName(entity.kind, labels: labels)]
        if let weight = clamped(entity.weight) { parts.append(weightText(weight, locale: locale, labels: labels)) }
        let related = self.relations(of: entity.id, entities: entities, relations: relations, labels: labels)
        if let line = relationsLine(related, labels: labels) { parts.append(line) }
        return parts.joined(separator: ". ")
    }

    /// The avatar size of a graph node: large in the middle, medium for a
    /// strong relation (weight 0.5 or more, or none), small otherwise.
    static func nodeSize(isCenter: Bool, weight: Double?) -> MonarchAvatarSize {
        if isCenter { return .lg }
        return (clamped(weight) ?? 0.5) >= 0.5 ? .md : .sm
    }

    /// The side of the graph for a proposed width.
    static func graphSide(proposedWidth: CGFloat?, nodeWidth: CGFloat) -> CGFloat {
        let largest = nodeWidth * graphSideInNodes
        guard let proposedWidth, proposedWidth.isFinite else { return largest }
        return min(largest, max(nodeWidth, proposedWidth))
    }

    /// The centre of every graph node in a square of `side`: index 0 in the
    /// middle, the others on a ring from the top, clockwise, kept `inset`
    /// from the edge.
    static func nodePoints(count: Int, side: CGFloat, inset: CGFloat) -> [CGPoint] {
        guard count > 0 else { return [] }
        let middle = CGPoint(x: side / 2, y: side / 2)
        let ring = count - 1
        let radius = max(0, side / 2 - inset)
        return [middle]
            + (0..<ring).map { index in
                let angle = -Double.pi / 2 + 2 * Double.pi * Double(index) / Double(ring)
                return CGPoint(x: middle.x + radius * cos(angle), y: middle.y + radius * sin(angle))
            }
    }

    /// The relations as pairs of node indices in graph order; relations to
    /// entities not in the graph are left out.
    static func edges(_ relations: [MonarchAiEntityRelation], order: [MonarchAiRelatedEntity])
        -> [MonarchAiRelatedEntitiesEdge]
    {
        let index = Dictionary(
            order.enumerated().map { ($0.element.id, $0.offset) },
            uniquingKeysWith: { first, _ in first }
        )
        return relations.compactMap { relation in
            guard let from = index[relation.from], let to = index[relation.to], from != to else { return nil }
            return MonarchAiRelatedEntitiesEdge(from: from, to: to)
        }
    }
}

/// A relation line between two graph nodes, by their index in graph order.
struct MonarchAiRelatedEntitiesEdge: Hashable {
    /// The index of one node.
    let from: Int
    /// The index of the other node.
    let to: Int
}

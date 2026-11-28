import SwiftUI

/// People, organisations, projects and records the assistant found related
/// to the current content, ranked by relevance with their relations. A list
/// by default; the graph variant draws a radial diagram.
///
/// ```swift
/// MonarchAiRelatedEntities(
///     [
///         MonarchAiRelatedEntity(id: "jordan", label: "Jordan Lee", kind: .person, weight: 0.9),
///         MonarchAiRelatedEntity(id: "acme", label: "Acme", kind: .org, weight: 0.7),
///     ],
///     relations: [MonarchAiEntityRelation(from: "jordan", to: "acme", label: "works at")]
/// ) { entity in
///     open(entity)
/// }
/// ```
///
/// Each entity shows a ``MonarchAvatar`` (initials for a person, the kind's
/// symbol otherwise), its name, its kind as a ``MonarchBadge``, its
/// relations and a relevance bar. With `onSelect` every entity is a button.
/// The graph places the centre entity in the middle and the others on a ring
/// with a custom `Layout`, and draws each relation as a line. It falls back
/// to the list above `maxGraphNodes` entities and at accessibility text
/// sizes, where the diagram cannot stay readable.
///
/// Accessibility: the list or diagram is a container named "Related
/// entities". Each entity is one element read as "Jordan Lee. Person.
/// Relevance 90%. Related to: works at Acme", a button when selectable.
/// The relevance bars and relation lines are hidden; their meaning is in
/// that text. The diagram is drawn left to right in every layout direction;
/// its centred labels read the same either way.
public struct MonarchAiRelatedEntities: View {
    private let entities: [MonarchAiRelatedEntity]
    private let relations: [MonarchAiEntityRelation]
    private let variant: MonarchAiRelatedEntitiesVariant
    private let centerID: String?
    private let maxGraphNodes: Int
    private let labels: MonarchAiRelatedEntitiesLabels
    private let onSelect: (@MainActor (MonarchAiRelatedEntity) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .caption) private var nodeWidth = MonarchAiRelatedEntitiesTokens.nodeWidth
    @ScaledMetric(relativeTo: .body) private var weightWidth = MonarchAiRelatedEntitiesTokens.weightWidth

    /// Related entities.
    ///
    /// - Parameters:
    ///   - entities: The entities. The list is sorted by weight, highest first.
    ///   - relations: Links between entities, shown under each entity and as lines in the graph.
    ///   - variant: A ranked list, or a radial graph.
    ///   - centerID: The graph's middle entity. The one with the highest weight when `nil`.
    ///   - maxGraphNodes: Above this many entities the graph shows the list instead.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSelect: Makes every entity a button that calls this with the entity.
    public init(
        _ entities: [MonarchAiRelatedEntity],
        relations: [MonarchAiEntityRelation] = [],
        variant: MonarchAiRelatedEntitiesVariant = .list,
        centerID: String? = nil,
        maxGraphNodes: Int = 12,
        labels: MonarchAiRelatedEntitiesLabels = MonarchAiRelatedEntitiesLabels(),
        onSelect: (@MainActor (MonarchAiRelatedEntity) -> Void)? = nil
    ) {
        self.entities = entities
        self.relations = relations
        self.variant = variant
        self.centerID = centerID
        self.maxGraphNodes = maxGraphNodes
        self.labels = labels
        self.onSelect = onSelect
    }

    private var name: String { labels.label ?? MonarchAiRelatedEntitiesStrings.label }

    /// The content of the view.
    public var body: some View {
        if entities.isEmpty {
            MonarchEmptyState(
                labels.empty ?? MonarchAiRelatedEntitiesStrings.empty,
                systemImage: MonarchAiRelatedEntitiesTokens.emptySymbol,
                size: .sm
            )
        } else if MonarchAiRelatedEntitiesTokens.showsGraph(
            variant: variant,
            count: entities.count,
            maxNodes: maxGraphNodes,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
        ) {
            graph
        } else {
            list
        }
    }

    // MARK: List

    private var list: some View {
        VStack(alignment: .leading, spacing: MonarchAiRelatedEntitiesTokens.rowSpacing) {
            ForEach(MonarchAiRelatedEntitiesTokens.sorted(entities)) { entity in
                selectable(entity) { row(entity) }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(name)
    }

    private func row(_ entity: MonarchAiRelatedEntity) -> some View {
        let related = MonarchAiRelatedEntitiesTokens.relations(
            of: entity.id,
            entities: entities,
            relations: relations,
            labels: labels
        )
        // As the web row: the name with its kind beside it, the relations under it and the weight bar under those.
        return VStack(alignment: .leading, spacing: MonarchAiRelatedEntitiesTokens.lineSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchAiRelatedEntitiesTokens.itemSpacing) {
                Text(entity.label)
                    .monarchTextStyle(.label, weight: .medium)
                    .foregroundStyle(theme.colors[role: MonarchAiRelatedEntitiesTokens.text])
                    .multilineTextAlignment(.leading)
                Text(MonarchAiRelatedEntitiesTokens.kindName(entity.kind, labels: labels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAiRelatedEntitiesTokens.muted])
            }
            if let line = MonarchAiRelatedEntitiesTokens.relationsLine(related, labels: labels) {
                Text(line)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAiRelatedEntitiesTokens.muted])
                    .multilineTextAlignment(.leading)
            }
            if let weight = MonarchAiRelatedEntitiesTokens.clamped(entity.weight) {
                weightBar(weight)
            }
        }
        .padding(MonarchAiRelatedEntitiesTokens.rowPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
        .overlay(
            MonarchShapes.container.strokeBorder(
                theme.colors[role: MonarchAiRelatedEntitiesTokens.border],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .contentShape(MonarchShapes.container)
    }

    private func weightBar(_ weight: Double) -> some View {
        ZStack(alignment: .leading) {
            Capsule().fill(theme.colors[role: MonarchAiRelatedEntitiesTokens.track])
            Capsule()
                .fill(theme.colors.soft(MonarchAiRelatedEntitiesTokens.fill, by: 0.5))
                .frame(width: weightWidth * weight)
        }
        .frame(width: weightWidth, height: MonarchAiRelatedEntitiesTokens.weightHeight)
    }

    // MARK: Graph

    private var graph: some View {
        let order = MonarchAiRelatedEntitiesTokens.graphOrder(entities, centerID: centerID)
        let width = nodeWidth
        return ZStack {
            MonarchAiRelatedEntitiesEdges(
                edges: MonarchAiRelatedEntitiesTokens.edges(relations, order: order),
                count: order.count,
                nodeWidth: width
            )
            .stroke(
                theme.colors[role: MonarchAiRelatedEntitiesTokens.edge],
                lineWidth: MonarchAiRelatedEntitiesTokens.edgeWidth
            )
            .accessibilityHidden(true)
            MonarchAiRelatedEntitiesRadialLayout(nodeWidth: width) {
                ForEach(Array(order.enumerated()), id: \.element.id) { index, entity in
                    selectable(entity) { node(entity, isCenter: index == 0) }
                }
            }
        }
        .modifier(MonarchAiRelatedEntitiesFixedDirection())
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(name)
    }

    private func node(_ entity: MonarchAiRelatedEntity, isCenter: Bool) -> some View {
        VStack(spacing: MonarchAiRelatedEntitiesTokens.lineSpacing) {
            avatar(entity, size: MonarchAiRelatedEntitiesTokens.nodeSize(isCenter: isCenter, weight: entity.weight))
            Text(entity.label)
                .monarchTextStyle(.caption)
                .foregroundStyle(
                    theme.colors[
                        role: isCenter ? MonarchAiRelatedEntitiesTokens.text : MonarchAiRelatedEntitiesTokens.muted
                    ]
                )
                .multilineTextAlignment(.center)
                .lineLimit(MonarchAiRelatedEntitiesTokens.nodeLabelLines)
        }
        .frame(width: nodeWidth)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .contentShape(Rectangle())
    }

    // MARK: Shared

    private func avatar(_ entity: MonarchAiRelatedEntity, size: MonarchAvatarSize) -> some View {
        MonarchAvatar(
            url: entity.imageURL,
            name: entity.label,
            seed: entity.id,
            systemImage: MonarchAiRelatedEntitiesTokens.symbol(entity.kind),
            size: size,
            isDecorative: true
        )
    }

    @ViewBuilder private func selectable(
        _ entity: MonarchAiRelatedEntity,
        @ViewBuilder content: () -> some View
    ) -> some View {
        let spoken = MonarchAiRelatedEntitiesTokens.accessibilityLabel(
            entity,
            entities: entities,
            relations: relations,
            locale: locale,
            labels: labels
        )
        if let onSelect {
            Button {
                onSelect(entity)
            } label: {
                content()
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(spoken)
            .accessibilityAddTraits(.isButton)
        } else {
            content()
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(spoken)
        }
    }
}

/// Draws the graph left to right, so the lines and the nodes agree. A radial
/// diagram has no reading direction, and node labels are centred.
private struct MonarchAiRelatedEntitiesFixedDirection: ViewModifier {
    func body(content: Content) -> some View {
        content.environment(\.layoutDirection, .leftToRight)
    }
}

#if DEBUG
    #Preview("MonarchAiRelatedEntities") {
        let entities = [
            MonarchAiRelatedEntity(id: "jordan", label: "Jordan Lee", kind: .person, weight: 0.9),
            MonarchAiRelatedEntity(id: "acme", label: "Acme", kind: .org, weight: 0.7),
            MonarchAiRelatedEntity(id: "launch", label: "Q3 launch", kind: .project, weight: 0.4),
            MonarchAiRelatedEntity(id: "brief", label: "Launch brief", kind: .document),
        ]
        let relations = [
            MonarchAiEntityRelation(from: "jordan", to: "acme", label: "works at"),
            MonarchAiEntityRelation(from: "jordan", to: "launch", label: "leads"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiRelatedEntities(entities, relations: relations)
                MonarchAiRelatedEntities(entities, relations: relations, variant: .graph)
            }
        }
    }
#endif

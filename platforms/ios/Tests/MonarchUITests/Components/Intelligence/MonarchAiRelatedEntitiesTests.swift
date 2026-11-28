import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiRelatedEntitiesFixtures: ComponentFixtures {
    static let id = "ai-related-entities"
    static let entities = [
        MonarchAiRelatedEntity(id: "launch", label: "Q3 launch", kind: .project, weight: 0.4),
        MonarchAiRelatedEntity(id: "jordan", label: "Jordan Lee", kind: .person, weight: 0.9),
        MonarchAiRelatedEntity(id: "acme", label: "Acme", kind: .org, weight: 0.7),
        MonarchAiRelatedEntity(id: "brief", label: "Launch brief", kind: .document),
        MonarchAiRelatedEntity(id: "deal", label: "Acme renewal", kind: .record, weight: 0.2),
        MonarchAiRelatedEntity(id: "office", label: "Lisbon office", kind: .place, weight: 0.1),
        MonarchAiRelatedEntity(id: "tool", label: "Build server", kind: "system"),
    ]
    static let relations = [
        MonarchAiEntityRelation(from: "jordan", to: "acme", label: "works at"),
        MonarchAiEntityRelation(from: "jordan", to: "launch", label: "leads"),
        MonarchAiEntityRelation(from: "deal", to: "acme"),
        MonarchAiEntityRelation(from: "brief", to: "missing"),
    ]

    static let fixtures = [
        Fixture("list, every kind") {
            MonarchAiRelatedEntities(entities, relations: relations)
        },
        Fixture("list, selectable") {
            MonarchAiRelatedEntities(entities, relations: relations) { _ in }
        },
        Fixture("graph") {
            MonarchAiRelatedEntities(entities, relations: relations, variant: .graph)
        },
        Fixture("graph, centre chosen, selectable") {
            MonarchAiRelatedEntities(entities, relations: relations, variant: .graph, centerID: "acme") { _ in }
        },
        Fixture("graph over the limit falls back to the list") {
            MonarchAiRelatedEntities(entities, relations: relations, variant: .graph, maxGraphNodes: 3)
        },
        Fixture("single entity graph") {
            MonarchAiRelatedEntities(
                [MonarchAiRelatedEntity(id: "solo", label: "Jordan Lee", kind: .person)],
                variant: .graph
            )
        },
        Fixture("empty") {
            MonarchAiRelatedEntities([])
        },
        Fixture("long names") {
            MonarchAiRelatedEntities(
                [
                    MonarchAiRelatedEntity(
                        id: "long",
                        label: "International Federation of Independent Logistics Providers",
                        kind: .org,
                        weight: 0.8
                    )
                ],
                variant: .graph
            )
        },
    ]
}

@Suite("MonarchAiRelatedEntities")
struct MonarchAiRelatedEntitiesTests {
    private let entities = MonarchAiRelatedEntitiesFixtures.entities
    private let relations = MonarchAiRelatedEntitiesFixtures.relations
    private let labels = MonarchAiRelatedEntitiesLabels()
    private let english = Locale(identifier: "en_US")

    @Test("Entities sort by weight; unweighted ones keep their order at the end")
    func sorting() {
        #expect(
            MonarchAiRelatedEntitiesTokens.sorted(entities).map(\.id) == [
                "jordan", "acme", "launch", "deal", "office", "brief", "tool",
            ]
        )
        #expect(MonarchAiRelatedEntitiesTokens.clamped(1.4) == 1)
        #expect(MonarchAiRelatedEntitiesTokens.clamped(-1) == 0)
        #expect(MonarchAiRelatedEntitiesTokens.clamped(.infinity) == nil)
        #expect(MonarchAiRelatedEntitiesTokens.clamped(nil) == nil)
    }

    @Test("The graph needs the graph variant, a count within the limit and a non-accessibility size")
    func graphChoice() {
        #expect(
            MonarchAiRelatedEntitiesTokens.showsGraph(
                variant: .graph,
                count: 7,
                maxNodes: 12,
                isAccessibilitySize: false
            )
        )
        #expect(
            !MonarchAiRelatedEntitiesTokens.showsGraph(
                variant: .list,
                count: 7,
                maxNodes: 12,
                isAccessibilitySize: false
            )
        )
        #expect(
            !MonarchAiRelatedEntitiesTokens.showsGraph(
                variant: .graph,
                count: 13,
                maxNodes: 12,
                isAccessibilitySize: false
            )
        )
        #expect(
            !MonarchAiRelatedEntitiesTokens.showsGraph(
                variant: .graph,
                count: 0,
                maxNodes: 12,
                isAccessibilitySize: false
            )
        )
        #expect(
            !MonarchAiRelatedEntitiesTokens.showsGraph(
                variant: .graph,
                count: 3,
                maxNodes: 12,
                isAccessibilitySize: true
            )
        )
    }

    @Test("The centre is the chosen entity or the strongest; the ring follows by weight")
    func order() {
        #expect(MonarchAiRelatedEntitiesTokens.graphOrder(entities, centerID: nil).first?.id == "jordan")
        let chosen = MonarchAiRelatedEntitiesTokens.graphOrder(entities, centerID: "acme")
        #expect(chosen.map(\.id) == ["acme", "jordan", "launch", "deal", "office", "brief", "tool"])
        #expect(MonarchAiRelatedEntitiesTokens.graphOrder(entities, centerID: "nope").first?.id == "jordan")
        #expect(MonarchAiRelatedEntitiesTokens.graphOrder([], centerID: nil).isEmpty)
    }

    @Test("Relations are read from both ends; unknown entities are left out")
    func relationText() {
        #expect(
            MonarchAiRelatedEntitiesTokens.relations(
                of: "jordan",
                entities: entities,
                relations: relations,
                labels: labels
            )
                == ["works at Acme", "leads Q3 launch"]
        )
        #expect(
            MonarchAiRelatedEntitiesTokens.relations(
                of: "acme",
                entities: entities,
                relations: relations,
                labels: labels
            )
                == ["works at Jordan Lee", "Acme renewal"]
        )
        #expect(
            MonarchAiRelatedEntitiesTokens.relations(
                of: "brief",
                entities: entities,
                relations: relations,
                labels: labels
            )
            .isEmpty
        )
        #expect(
            MonarchAiRelatedEntitiesTokens.relationsLine(["works at Acme"], labels: labels)
                == "Related to: works at Acme"
        )
        #expect(MonarchAiRelatedEntitiesTokens.relationsLine([], labels: labels) == nil)
    }

    @Test("The spoken name has the name, kind, relevance and relations")
    func accessibility() {
        let jordan = entities[1]
        #expect(
            MonarchAiRelatedEntitiesTokens.accessibilityLabel(
                jordan,
                entities: entities,
                relations: relations,
                locale: english,
                labels: labels
            ) == "Jordan Lee. Person. Relevance 90%. Related to: works at Acme, leads Q3 launch"
        )
        #expect(
            MonarchAiRelatedEntitiesTokens.accessibilityLabel(
                entities[6],
                entities: entities,
                relations: relations,
                locale: english,
                labels: labels
            ) == "Build server. system"
        )
        let custom = MonarchAiRelatedEntitiesLabels(weight: { "Pertinence \(Int($0 * 100))" }, relatedTo: "Lié à")
        #expect(
            MonarchAiRelatedEntitiesTokens.accessibilityLabel(
                entities[2],
                entities: entities,
                relations: relations,
                locale: english,
                labels: custom
            ) == "Acme. Organization. Pertinence 70. Lié à: works at Jordan Lee, Acme renewal"
        )
    }

    @Test("Kinds have names and symbols; a person gets initials")
    func kinds() {
        let known: [MonarchAiRelatedEntityKind] = [.person, .org, .project, .document, .record, .place, .other]
        #expect(
            known.map { MonarchAiRelatedEntitiesTokens.kindName($0, labels: labels) } == [
                "Person", "Organization", "Project", "Document", "Record", "Place", "Other",
            ]
        )
        #expect(MonarchAiRelatedEntitiesTokens.symbol(.person) == nil)
        #expect(Set(known.dropFirst().compactMap(MonarchAiRelatedEntitiesTokens.symbol)).count == 6)
        #expect(MonarchAiRelatedEntitiesTokens.symbol("custom") == MonarchAiRelatedEntitiesTokens.symbol(.other))
    }

    @Test("Graph geometry: the centre in the middle, the ring from the top, clockwise")
    func geometry() {
        let points = MonarchAiRelatedEntitiesTokens.nodePoints(count: 5, side: 200, inset: 20)
        #expect(points.count == 5)
        #expect(points[0] == CGPoint(x: 100, y: 100))
        #expect(abs(points[1].x - 100) < 0.001 && abs(points[1].y - 20) < 0.001)
        #expect(abs(points[2].x - 180) < 0.001 && abs(points[2].y - 100) < 0.001)
        #expect(abs(points[3].y - 180) < 0.001)
        #expect(MonarchAiRelatedEntitiesTokens.nodePoints(count: 0, side: 200, inset: 20).isEmpty)
        #expect(MonarchAiRelatedEntitiesTokens.graphSide(proposedWidth: nil, nodeWidth: 80) == 320)
        #expect(MonarchAiRelatedEntitiesTokens.graphSide(proposedWidth: 1_000, nodeWidth: 80) == 320)
        #expect(MonarchAiRelatedEntitiesTokens.graphSide(proposedWidth: 200, nodeWidth: 80) == 200)
        #expect(MonarchAiRelatedEntitiesTokens.graphSide(proposedWidth: 10, nodeWidth: 80) == 80)
    }

    @Test("Edges join nodes in graph order and skip unknown ends")
    func edges() {
        let order = MonarchAiRelatedEntitiesTokens.graphOrder(entities, centerID: nil)
        let edges = MonarchAiRelatedEntitiesTokens.edges(relations, order: order)
        #expect(
            edges == [
                MonarchAiRelatedEntitiesEdge(from: 0, to: 1),
                MonarchAiRelatedEntitiesEdge(from: 0, to: 2),
                MonarchAiRelatedEntitiesEdge(from: 3, to: 1),
            ]
        )
    }

    @Test("Node sizes follow the centre and the weight")
    func nodeSizes() {
        #expect(MonarchAiRelatedEntitiesTokens.nodeSize(isCenter: true, weight: 0.1) == .lg)
        #expect(MonarchAiRelatedEntitiesTokens.nodeSize(isCenter: false, weight: 0.8) == .md)
        #expect(MonarchAiRelatedEntitiesTokens.nodeSize(isCenter: false, weight: nil) == .md)
        #expect(MonarchAiRelatedEntitiesTokens.nodeSize(isCenter: false, weight: 0.2) == .sm)
    }

    @Test("Built-in strings resolve; colours are theme roles")
    func stringsAndColours() {
        #expect(MonarchAiRelatedEntitiesStrings.label == "Related entities")
        #expect(MonarchAiRelatedEntitiesStrings.empty == "No related entities")
        #expect(MonarchAiRelatedEntitiesStrings.weight("80%") == "Relevance 80%")
        #expect(MonarchAiRelatedEntitiesTokens.fill == \MonarchColorTokens.aiSolid)
        #expect(MonarchAiRelatedEntitiesTokens.edge == \MonarchColorTokens.borderDefault)
        #expect(MonarchAiRelatedEntitiesTokens.text == \MonarchColorTokens.textDefault)
    }
}

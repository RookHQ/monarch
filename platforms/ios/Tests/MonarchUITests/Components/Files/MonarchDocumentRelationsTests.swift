import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let plan = MonarchDocumentRelationTarget(
    id: "d1",
    title: "Q3 plan",
    description: "Finance",
    fileName: "plan.pdf"
)
private let roadmap = MonarchDocumentRelationTarget(id: "d2", title: "Roadmap")
private let spec = MonarchDocumentRelationTarget(id: "d3", title: "Billing specification", description: "Engineering")
private let relationList = [
    MonarchDocumentRelation(id: "r1", type: "parent-of", target: plan),
    MonarchDocumentRelation(id: "r2", type: "parent-of", direction: .incoming, target: roadmap),
    MonarchDocumentRelation(id: "r3", type: "implements", target: spec),
    MonarchDocumentRelation(
        id: "r4",
        type: "supersedes",
        target: MonarchDocumentRelationTarget(id: "d4", title: "Old plan")
    ),
]

private struct RelationFailure: LocalizedError {
    var errorDescription: String? { "You can’t link archived documents." }
}

private struct SilentFailure: Error {}

enum MonarchDocumentRelationsFixtures: ComponentFixtures {
    static let id = "document-relations"
    static let fixtures = [
        Fixture("every kind, open, add and remove") {
            MonarchDocumentRelations(relationList, onOpen: { _ in }, onAdd: { _, _ in }, onRemove: { _ in })
        },
        Fixture("read only") {
            MonarchDocumentRelations(relationList)
        },
        Fixture("empty with add") {
            MonarchDocumentRelations([], showsEmpty: true, onAdd: { _, _ in })
        },
        Fixture("empty, nothing shown") {
            MonarchDocumentRelations([])
        },
        Fixture("long titles and a custom type") {
            MonarchDocumentRelations(
                [
                    MonarchDocumentRelation(
                        id: "x",
                        type: "blocks",
                        target: MonarchDocumentRelationTarget(
                            id: "x",
                            title: "A document title that is long enough to wrap onto several lines",
                            description: "A space with a long name / A folder with a long name"
                        )
                    )
                ],
                types: [MonarchDocumentRelationType(value: "blocks", label: "Blocks", inverse: "blocked-by")],
                onRemove: { _ in }
            )
        },
    ]
}

@Suite("MonarchDocumentRelations")
@MainActor
struct MonarchDocumentRelationsTests {
    typealias Tokens = MonarchDocumentRelationsTokens
    let labels = MonarchDocumentRelationsLabels()
    let types = MonarchDocumentRelationType.defaults

    @Test("The default types match the web, with inverses")
    func defaults() {
        #expect(
            types.map(\.value) == [
                "related-to", "parent-of", "child-of", "supersedes", "superseded-by", "implements", "specified-by",
            ]
        )
        #expect(
            types.map(\.label) == [
                "Related to", "Parent of", "Child of", "Supersedes", "Superseded by", "Implements", "Specified by",
            ]
        )
        #expect(types.first { $0.value == "parent-of" }?.inverse == "child-of")
        #expect(types.first { $0.value == "related-to" }?.inverse == "related-to")
        #expect(MonarchDocumentRelationType(value: "a", label: "A").inverse == "a")
    }

    @Test("Outgoing relations read as their type, incoming ones as the inverse")
    func direction() {
        #expect(Tokens.type(of: relationList[0], types: types) == ("parent-of", "Parent of"))
        #expect(Tokens.type(of: relationList[1], types: types) == ("child-of", "Child of"))
        let unknown = MonarchDocumentRelation(id: "u", type: "blocks", direction: .incoming, target: plan)
        #expect(Tokens.type(of: unknown, types: types) == ("blocks", "blocks"))
    }

    @Test("Each type has its symbol")
    func symbols() {
        #expect(Tokens.symbol("parent-of") == "arrow.up")
        #expect(Tokens.symbol("child-of") == "arrow.down")
        #expect(Tokens.symbol("implements") == "checkmark")
        #expect(Tokens.symbol("related-to") == "arrow.forward")
    }

    @Test("The sheet opens on the preferred type, else related-to, else the first")
    func initialType() {
        #expect(Tokens.initialType(nil, types: types) == "related-to")
        #expect(Tokens.initialType("supersedes", types: types) == "supersedes")
        #expect(Tokens.initialType("missing", types: types) == "related-to")
        let custom = [MonarchDocumentRelationType(value: "blocks", label: "Blocks")]
        #expect(Tokens.initialType(nil, types: custom) == "blocks")
        #expect(Tokens.initialType(nil, types: []) == "")
    }

    @Test("Results leave out related documents, filter by title and stop at twelve")
    func results() {
        let candidates = [
            plan, MonarchDocumentRelationTarget(id: "n", title: "Notes"),
            MonarchDocumentRelationTarget(id: "p", title: "Pricing plan"),
        ]
        #expect(
            Tokens.results(candidates, relations: relationList, query: "", searchesRemotely: false).map(\.id) == [
                "n", "p",
            ]
        )
        #expect(
            Tokens.results(candidates, relations: [], query: "PLAN", searchesRemotely: false).map(\.id) == ["d1", "p"]
        )
        #expect(Tokens.results(candidates, relations: [], query: "plan", searchesRemotely: true).count == 3)
        let many = (0..<20).map { MonarchDocumentRelationTarget(id: "\($0)", title: "Doc \($0)") }
        #expect(Tokens.results(many, relations: [], query: "", searchesRemotely: false).count == 12)
    }

    @Test("A relation is read as its type, title and description")
    func accessibilityLabel() {
        #expect(Tokens.accessibilityLabel(typeLabel: "Parent of", target: plan) == "Parent of, Q3 plan, Finance")
        #expect(Tokens.accessibilityLabel(typeLabel: "Child of", target: roadmap) == "Child of, Roadmap")
        #expect(Tokens.removeLabel("Q3 plan", labels: labels) == "Remove relation to Q3 plan")
        #expect(Tokens.addedText("Q3 plan", labels: labels) == "Relation to Q3 plan added")
        #expect(Tokens.removedText("Q3 plan", labels: labels) == "Relation to Q3 plan removed")
    }

    @Test("A failure shows the error's description, or the built-in message")
    func failure() {
        #expect(Tokens.failureMessage(RelationFailure(), labels: labels) == "You can’t link archived documents.")
        #expect(Tokens.failureMessage(SilentFailure(), labels: labels) == "That didn’t work. Try again.")
        #expect(
            Tokens.failureMessage(SilentFailure(), labels: MonarchDocumentRelationsLabels(failed: "Fehler")) == "Fehler"
        )
    }

    @Test("Colours come from roles")
    func colors() {
        #expect(Tokens.titleColor == \.textDefault)
        #expect(Tokens.mutedColor == \.textMuted)
        #expect(Tokens.errorColor == \.dangerStrong)
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let custom = MonarchDocumentRelationsLabels(
            remove: { "Entfernen: \($0)" },
            added: { "\($0) verknüpft" },
            removed: { "\($0) entfernt" }
        )
        #expect(Tokens.removeLabel("A", labels: custom) == "Entfernen: A")
        #expect(Tokens.addedText("A", labels: custom) == "A verknüpft")
        #expect(Tokens.removedText("A", labels: custom) == "A entfernt")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchDocumentRelationsStrings.label == "Relations")
        #expect(MonarchDocumentRelationsStrings.add == "Add relation")
        #expect(MonarchDocumentRelationsStrings.sheetTitle == "Add a relation")
        #expect(MonarchDocumentRelationsStrings.search == "Search documents…")
        #expect(MonarchDocumentRelationsStrings.noResults == "No matching documents.")
        #expect(MonarchDocumentRelationsStrings.none == "No relations yet.")
        #expect(MonarchDocumentRelationsStrings.cancel == "Cancel")
    }

    @Test("Nothing is drawn for no relations unless the empty text is asked for")
    func empty() {
        #expect(Host.layout(MonarchDocumentRelations([])).height < 1)
        #expect(Host.layout(MonarchDocumentRelations([], showsEmpty: true)).height > 0)
    }

    @Test("After a removal focus goes to the next relation, else the previous, else Add")
    func focusAfterRemoval() {
        typealias Tokens = MonarchDocumentRelationsTokens
        #expect(Tokens.focusAfterRemoval(of: "b", in: ["a", "b", "c"], canAdd: true) == "c")
        #expect(Tokens.focusAfterRemoval(of: "c", in: ["a", "b", "c"], canAdd: true) == "b")
        #expect(Tokens.focusAfterRemoval(of: "a", in: ["a"], canAdd: true) == Tokens.addFocusID)
        #expect(Tokens.focusAfterRemoval(of: "a", in: ["a"], canAdd: false) == nil)
    }
}

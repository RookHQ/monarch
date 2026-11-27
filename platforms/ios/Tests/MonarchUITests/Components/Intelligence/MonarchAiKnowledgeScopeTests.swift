import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiKnowledgeScopeFixtures: ComponentFixtures {
    static let id = "ai-knowledge-scope"

    static let sources = [
        MonarchAiKnowledgeSource(
            id: "handbook",
            label: "Handbook",
            kind: .space,
            description: "3 documents",
            children: [
                MonarchAiKnowledgeSource(id: "policies", label: "Policies", kind: .document),
                MonarchAiKnowledgeSource(id: "benefits", label: "Benefits", kind: .document),
                MonarchAiKnowledgeSource(
                    id: "travel",
                    label: "Travel",
                    kind: .space,
                    children: [
                        MonarchAiKnowledgeSource(id: "flights", label: "Flights", kind: .document),
                        MonarchAiKnowledgeSource(id: "hotels", label: "Hotels", kind: .document),
                    ]
                ),
            ]
        ),
        MonarchAiKnowledgeSource(id: "crm", label: "Sales CRM", kind: .app, description: "Connected yesterday"),
        MonarchAiKnowledgeSource(id: "wiki", label: "Engineering wiki", kind: .space),
    ]

    static let fixtures = [
        Fixture("collapsed, nothing selected") {
            MonarchAiKnowledgeScope(sources, selection: .constant([]))
        },
        Fixture("expanded, mixed group") {
            MonarchAiKnowledgeScope(
                sources,
                selection: .constant(["policies", "flights", "crm"]),
                startsExpanded: ["handbook", "travel"]
            )
        },
        Fixture("group fully selected") {
            MonarchAiKnowledgeScope(sources, selection: .constant(["handbook"]), startsExpanded: ["handbook"])
        },
        Fixture("no summary") {
            MonarchAiKnowledgeScope(sources, selection: .constant(["wiki"]), showsSummary: false)
        },
        Fixture("disabled") {
            MonarchAiKnowledgeScope(sources, selection: .constant(["crm"]), startsExpanded: ["handbook"])
                .disabled(true)
        },
        Fixture("empty") {
            MonarchAiKnowledgeScope([], selection: .constant([]))
        },
        Fixture("long names") {
            MonarchAiKnowledgeScope(
                [
                    MonarchAiKnowledgeSource(
                        id: "long",
                        label: "Quarterly planning notes for the customer success and support teams",
                        kind: .document,
                        description: "Updated by the operations team every Monday morning before the review"
                    )
                ],
                selection: .constant(["long"])
            )
        },
    ]
}

@Suite("MonarchAiKnowledgeScope")
struct MonarchAiKnowledgeScopeTests {
    private let sources = MonarchAiKnowledgeScopeFixtures.sources
    private let labels = MonarchAiKnowledgeScopeLabels()

    @Test("Groups are checked, unchecked or mixed from their contents")
    func states() {
        let none = MonarchAiKnowledgeScopeTokens.states(sources, selection: [])
        #expect(none.values.allSatisfy { $0 == .unchecked })
        #expect(none.count == 8)

        let some = MonarchAiKnowledgeScopeTokens.states(sources, selection: ["flights"])
        #expect(some["flights"] == .checked)
        #expect(some["travel"] == .mixed)
        #expect(some["handbook"] == .mixed)
        #expect(some["policies"] == .unchecked)

        let group = MonarchAiKnowledgeScopeTokens.states(sources, selection: ["handbook"])
        #expect(group["hotels"] == .checked)
        #expect(group["travel"] == .checked)

        let allChildren = MonarchAiKnowledgeScopeTokens.states(sources, selection: ["flights", "hotels"])
        #expect(allChildren["travel"] == .checked)
        #expect(allChildren["handbook"] == .mixed)
    }

    @Test("Toggling selects or clears a source and everything in it")
    func toggling() {
        // Unchecked group: everything in it, groups included, in tree order.
        #expect(
            MonarchAiKnowledgeScopeTokens.toggled(sources, selection: [], id: "travel") == [
                "travel", "flights", "hotels",
            ]
        )
        // Mixed group: cleared.
        #expect(MonarchAiKnowledgeScopeTokens.toggled(sources, selection: ["flights", "crm"], id: "travel") == ["crm"])
        // Checked leaf inside a selected group: the group is no longer whole.
        #expect(
            MonarchAiKnowledgeScopeTokens.toggled(sources, selection: ["handbook"], id: "benefits") == [
                "policies", "travel", "flights", "hotels",
            ]
        )
        // Completing a group selects it.
        #expect(
            MonarchAiKnowledgeScopeTokens.toggled(
                sources,
                selection: ["policies", "benefits", "hotels"],
                id: "flights"
            ).first == "handbook"
        )
        // Unknown ids are kept at the end; an unknown target changes nothing.
        #expect(
            MonarchAiKnowledgeScopeTokens.toggled(sources, selection: ["gone"], id: "wiki") == ["wiki", "gone"]
        )
        #expect(MonarchAiKnowledgeScopeTokens.toggled(sources, selection: ["crm"], id: "nope") == ["crm"])
    }

    @Test("Setting is idempotent, so a mixed toggle writing two sources changes the selection once")
    func idempotent() {
        let once = MonarchAiKnowledgeScopeTokens.setting(sources, selection: ["flights"], id: "handbook", isOn: false)
        let twice = MonarchAiKnowledgeScopeTokens.setting(sources, selection: once, id: "handbook", isOn: false)
        #expect(once == [])
        #expect(twice == once)
        let on = MonarchAiKnowledgeScopeTokens.setting(sources, selection: [], id: "wiki", isOn: true)
        #expect(MonarchAiKnowledgeScopeTokens.setting(sources, selection: on, id: "wiki", isOn: true) == ["wiki"])
    }

    @Test("The count treats a whole group as one source")
    func count() {
        func count(_ selection: [String]) -> Int {
            MonarchAiKnowledgeScopeTokens.selectedCount(
                sources,
                states: MonarchAiKnowledgeScopeTokens.states(sources, selection: selection)
            )
        }
        #expect(count([]) == 0)
        #expect(count(["handbook"]) == 1)
        #expect(count(["travel", "policies", "crm"]) == 3)
        #expect(count(["flights"]) == 1)
        #expect(MonarchAiKnowledgeScopeTokens.summary(count: 0, labels: labels) == "No sources selected")
        #expect(MonarchAiKnowledgeScopeTokens.summary(count: 1, labels: labels) == "1 source selected")
        #expect(MonarchAiKnowledgeScopeTokens.summary(count: 3, labels: labels) == "3 sources selected")
    }

    @Test("Only expanded groups show their contents, indented by level")
    func rows() {
        let collapsed = MonarchAiKnowledgeScopeTokens.visibleRows(sources, expanded: [])
        #expect(collapsed.map(\.id) == ["handbook", "crm", "wiki"])
        let open = MonarchAiKnowledgeScopeTokens.visibleRows(sources, expanded: ["handbook", "travel"])
        #expect(open.map(\.id) == ["handbook", "policies", "benefits", "travel", "flights", "hotels", "crm", "wiki"])
        #expect(open.map(\.level) == [1, 2, 2, 2, 3, 3, 1, 1])
        #expect(open[0].isGroup && !open[1].isGroup)
        // A nested group stays hidden while its parent is closed.
        #expect(MonarchAiKnowledgeScopeTokens.visibleRows(sources, expanded: ["travel"]).count == 3)
        #expect(MonarchAiKnowledgeScopeTokens.indent(level: 1) == 0)
        #expect(MonarchAiKnowledgeScopeTokens.indent(level: 3) == 2 * MonarchAiKnowledgeScopeTokens.indent)
    }

    @Test("Spoken names include the kind; disclosure buttons name the group")
    func accessibility() {
        #expect(MonarchAiKnowledgeScopeTokens.itemLabel(sources[0], labels: labels) == "Handbook, Space")
        #expect(MonarchAiKnowledgeScopeTokens.itemLabel(sources[1], labels: labels) == "Sales CRM, App")
        #expect(
            MonarchAiKnowledgeScopeTokens.disclosureLabel(sources[0], isExpanded: false, labels: labels)
                == "Show contents of Handbook"
        )
        #expect(
            MonarchAiKnowledgeScopeTokens.disclosureLabel(sources[0], isExpanded: true, labels: labels)
                == "Hide contents of Handbook"
        )
        let custom = MonarchAiKnowledgeScopeLabels(
            kind: { $0 == .app ? "Appli" : "Autre" },
            itemLabel: { "\($1) : \($0)" }
        )
        #expect(MonarchAiKnowledgeScopeTokens.itemLabel(sources[1], labels: custom) == "Appli : Sales CRM")
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchAiKnowledgeScopeStrings.label == "Knowledge sources")
        #expect(
            MonarchAiKnowledgeSourceKind.allCases.map(MonarchAiKnowledgeScopeStrings.kind) == [
                "Space", "Document", "App",
            ]
        )
        #expect(MonarchAiKnowledgeScopeStrings.empty == "No sources available")
        #expect(MonarchAiKnowledgeSourceKind.document.rawValue == "doc")
    }

    @Test("Colours are text roles")
    func appearance() {
        #expect(MonarchAiKnowledgeScopeTokens.label == \MonarchColorTokens.textDefault)
        #expect(MonarchAiKnowledgeScopeTokens.secondary == \MonarchColorTokens.textMuted)
        #expect(MonarchAiKnowledgeScopeTokens.chevron == \MonarchColorTokens.textSubtle)
    }
}

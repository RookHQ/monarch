import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let projectsGroup = MonarchScopeGroup(
    id: "projects",
    label: "Projects API",
    description: "Example Inc.",
    systemImage: "folder",
    scopes: [
        MonarchScopeOption("projects:read", description: "List and read projects."),
        MonarchScopeOption("projects:write", label: "Write projects", description: "Create and update projects."),
        MonarchScopeOption("projects:admin", isDisabled: true),
    ]
)
private let reportsGroup = MonarchScopeGroup(
    id: "reports",
    label: "Reports API",
    scopes: [MonarchScopeOption("reports:read"), MonarchScopeOption("reports:export")]
)
private let loadingGroup = MonarchScopeGroup(id: "billing", label: "Billing API", scopes: [], isLoading: true)
private let emptyGroup = MonarchScopeGroup(id: "audit", label: "Audit API", scopes: [], emptyMessage: "Ask an admin.")

enum MonarchScopeSelectorFixtures: ComponentFixtures {
    static let id = "scope-selector"
    static let fixtures = [
        Fixture("expanded with a partial selection") {
            MonarchScopeSelector(
                "Scopes",
                selection: .constant(["projects:read"]),
                groups: [projectsGroup, reportsGroup],
                initiallyExpanded: ["projects", "reports"]
            )
        },
        Fixture("loading, no scopes, collapsed") {
            MonarchScopeSelector(
                "Scopes",
                selection: .constant([]),
                groups: [loadingGroup, emptyGroup, reportsGroup],
                initiallyExpanded: ["billing"]
            )
        },
        Fixture("read only, hidden label, paged") {
            MonarchScopeSelector(
                "Scopes",
                selection: .constant(["projects:read", "reports:read"]),
                groups: [projectsGroup, reportsGroup, emptyGroup],
                initiallyExpanded: ["projects"],
                isLabelHidden: true,
                isReadOnly: true,
                pageSize: 2
            )
        },
        Fixture("empty, disabled, long names") {
            VStack(alignment: .leading) {
                MonarchScopeSelector("Scopes", selection: .constant([]), groups: [])
                MonarchScopeSelector(
                    "Scopes",
                    selection: .constant([]),
                    groups: [
                        MonarchScopeGroup(
                            id: "long",
                            label: "Customer relationship management records and their attachments API",
                            scopes: [MonarchScopeOption("crm.records.attachments:read-and-write-including-archived")]
                        )
                    ],
                    initiallyExpanded: ["long"]
                )
                .disabled(true)
            }
        },
    ]
}

@Suite("MonarchScopeSelector")
struct MonarchScopeSelectorTests {
    private let text = MonarchScopeSelectorText(labels: MonarchScopeSelectorLabels())

    @Test("Summaries: loading, no scopes, the app's empty message, counts")
    func summaries() {
        #expect(MonarchScopeSelectorTokens.summary(loadingGroup, selection: [], text: text) == "Loading scopes…")
        #expect(
            MonarchScopeSelectorTokens.summary(
                MonarchScopeGroup(id: "x", label: "X", scopes: []),
                selection: [],
                text: text
            ) == "No scopes available."
        )
        #expect(MonarchScopeSelectorTokens.summary(emptyGroup, selection: [], text: text) == "Ask an admin.")
        #expect(
            MonarchScopeSelectorTokens.summary(projectsGroup, selection: ["projects:read", "other"], text: text)
                == "1 of 3 scopes"
        )
    }

    @Test("Select all acts on enabled scopes only")
    func enabled() {
        #expect(MonarchScopeSelectorTokens.enabledValues(projectsGroup) == ["projects:read", "projects:write"])
        #expect(MonarchScopeSelectorTokens.selectedCount(projectsGroup, selection: ["projects:admin"]) == 1)
    }

    @Test("Setting a scope inserts or removes it")
    func setting() {
        #expect(MonarchScopeSelectorTokens.setting("a", isOn: true, in: ["b"]) == ["a", "b"])
        #expect(MonarchScopeSelectorTokens.setting("b", isOn: false, in: ["b"]).isEmpty)
    }

    @Test("Paging shows a page and counts the rest")
    func paging() {
        let groups = (1...25).map { MonarchScopeGroup(id: "g\($0)", label: "G\($0)", scopes: []) }
        let first = MonarchScopeSelectorTokens.page(groups, limit: 10, pageSize: 10)
        #expect(first.visible.count == 10)
        #expect(first.next == 10)
        #expect(first.remaining == 15)
        let last = MonarchScopeSelectorTokens.page(groups, limit: 20, pageSize: 10)
        #expect(last.next == 5)
        #expect(MonarchScopeSelectorTokens.page(groups, limit: 30, pageSize: 10).remaining == 0)
        #expect(text.showMore(first.next, first.remaining) == "Show 10 more of 15")
    }

    @Test("Auto-select all: each loaded group with nothing selected, once")
    func autoSelectAll() {
        let first = MonarchScopeSelectorTokens.autoSelect(
            [projectsGroup, reportsGroup, loadingGroup],
            selection: ["reports:read"],
            seen: nil,
            mode: .all
        )
        #expect(first.selection == ["reports:read", "projects:read", "projects:write"])
        #expect(first.seen == ["projects", "reports"])
        let cleared = MonarchScopeSelectorTokens.autoSelect(
            [projectsGroup, reportsGroup],
            selection: [],
            seen: first.seen,
            mode: .all
        )
        #expect(cleared.selection.isEmpty)
        let loaded = MonarchScopeGroup(
            id: "billing",
            label: "Billing API",
            scopes: [MonarchScopeOption("billing:read")]
        )
        let later = MonarchScopeSelectorTokens.autoSelect([loaded], selection: [], seen: first.seen, mode: .all)
        #expect(later.selection == ["billing:read"])
    }

    @Test("Auto-select added: only groups that appear later")
    func autoSelectAdded() {
        let first = MonarchScopeSelectorTokens.autoSelect([projectsGroup], selection: [], seen: nil, mode: .added)
        #expect(first.selection.isEmpty)
        let next = MonarchScopeSelectorTokens.autoSelect(
            [projectsGroup, reportsGroup],
            selection: [],
            seen: first.seen,
            mode: .added
        )
        #expect(next.selection == ["reports:read", "reports:export"])
        let off = MonarchScopeSelectorTokens.autoSelect([reportsGroup], selection: [], seen: nil, mode: nil)
        #expect(off.selection.isEmpty)
        #expect(off.seen == nil)
    }

    @Test("Colours: muted summary, disabled role")
    func colors() {
        #expect(MonarchScopeSelectorTokens.summaryColor(selected: 1, isEnabled: true) == \.textMuted)
        #expect(MonarchScopeSelectorTokens.summaryColor(selected: 0, isEnabled: true) == \.textMuted)
        #expect(MonarchScopeSelectorTokens.summaryColor(selected: 1, isEnabled: false) == \.textDisabled)
        #expect(MonarchScopeSelectorTokens.secondaryColor(isEnabled: false) == \.textDisabled)
    }

    @Test("A scope without a label is read as its scope string")
    func accessibility() {
        #expect(MonarchScopeSelectorTokens.scopeName(projectsGroup.scopes[0]) == "projects:read")
        #expect(MonarchScopeSelectorTokens.scopeName(projectsGroup.scopes[1]) == "Write projects")
    }

    @Test("Built-in strings come from the table and can be replaced")
    func strings() {
        #expect(text.empty == "There are no scopes to choose from.")
        #expect(text.selectAll == "Select all")
        #expect(text.count(1, 2) == "1 of 2 scopes")
        let custom = MonarchScopeSelectorText(
            labels: MonarchScopeSelectorLabels(
                loading: "Wait",
                count: { "\($0)/\($1)" },
                showMore: { next, _ in "+\(next)" }
            )
        )
        #expect(custom.loading == "Wait")
        #expect(custom.count(1, 2) == "1/2")
        #expect(custom.showMore(3, 9) == "+3")
    }
}

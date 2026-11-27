import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiMessageBranchFixtures: ComponentFixtures {
    static let id = "ai-message-branch"
    static let fixtures = [
        Fixture("first, middle and last") {
            VStack(alignment: .leading) {
                MonarchAiMessageBranch(selection: .constant(0), count: 3)
                MonarchAiMessageBranch(selection: .constant(1), count: 3)
                MonarchAiMessageBranch(selection: .constant(2), count: 3)
            }
        },
        Fixture("single, shown on request") {
            MonarchAiMessageBranch(selection: .constant(0), count: 1, showsSingle: true)
        },
        Fixture("disabled") {
            MonarchAiMessageBranch(selection: .constant(1), count: 3).disabled(true)
        },
        Fixture("many versions and custom strings") {
            MonarchAiMessageBranch(
                selection: .constant(118),
                count: 240,
                label: "Drafts",
                previousLabel: "Previous draft",
                nextLabel: "Next draft",
                formatPosition: { "Draft \($0) of \($1)" },
                formatAnnouncement: { "Draft \($0) of \($1)" }
            )
        },
        Fixture("index out of range") {
            MonarchAiMessageBranch(selection: .constant(99), count: 2)
        },
    ]
}

@Suite("MonarchAiMessageBranch")
struct MonarchAiMessageBranchTests {
    @Test("The index is kept inside the range")
    func current() {
        #expect(MonarchAiMessageBranchTokens.current(0, count: 3) == 0)
        #expect(MonarchAiMessageBranchTokens.current(2, count: 3) == 2)
        #expect(MonarchAiMessageBranchTokens.current(9, count: 3) == 2)
        #expect(MonarchAiMessageBranchTokens.current(-4, count: 3) == 0)
        #expect(MonarchAiMessageBranchTokens.current(5, count: 0) == 0)
        #expect(MonarchAiMessageBranchTokens.total(0) == 1)
        #expect(MonarchAiMessageBranchTokens.total(-2) == 1)
    }

    @Test("With fewer than two versions the switcher is hidden unless asked")
    func visibility() {
        #expect(!MonarchAiMessageBranchTokens.isVisible(count: 0, showsSingle: false))
        #expect(!MonarchAiMessageBranchTokens.isVisible(count: 1, showsSingle: false))
        #expect(MonarchAiMessageBranchTokens.isVisible(count: 1, showsSingle: true))
        #expect(MonarchAiMessageBranchTokens.isVisible(count: 2, showsSingle: false))
    }

    @Test("The buttons stop at either end")
    func ends() {
        #expect(!MonarchAiMessageBranchTokens.canGoBack(index: 0, count: 3))
        #expect(MonarchAiMessageBranchTokens.canGoForward(index: 0, count: 3))
        #expect(MonarchAiMessageBranchTokens.canGoBack(index: 1, count: 3))
        #expect(MonarchAiMessageBranchTokens.canGoForward(index: 1, count: 3))
        #expect(MonarchAiMessageBranchTokens.canGoBack(index: 2, count: 3))
        #expect(!MonarchAiMessageBranchTokens.canGoForward(index: 2, count: 3))
        #expect(!MonarchAiMessageBranchTokens.canGoBack(index: 0, count: 1))
        #expect(!MonarchAiMessageBranchTokens.canGoForward(index: 0, count: 1))

        #expect(MonarchAiMessageBranchTokens.target(index: 0, count: 3, step: -1) == nil)
        #expect(MonarchAiMessageBranchTokens.target(index: 0, count: 3, step: 1) == 1)
        #expect(MonarchAiMessageBranchTokens.target(index: 2, count: 3, step: 1) == nil)
        #expect(MonarchAiMessageBranchTokens.target(index: 2, count: 3, step: -1) == 1)
        // From an index that is out of range, a step starts at the version that is shown.
        #expect(MonarchAiMessageBranchTokens.target(index: 99, count: 3, step: -1) == 1)
    }

    @Test("The position is muted, and disabled when the switcher is")
    func positionRole() {
        #expect(MonarchAiMessageBranchTokens.positionRole(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchAiMessageBranchTokens.positionRole(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("The chevrons are the directional symbols, which mirror in right-to-left layouts")
    func symbols() {
        #expect(MonarchAiMessageBranchTokens.previousSymbol == "chevron.backward")
        #expect(MonarchAiMessageBranchTokens.nextSymbol == "chevron.forward")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiMessageBranchStrings.label == "Versions")
        #expect(MonarchAiMessageBranchStrings.previous == "Previous version")
        #expect(MonarchAiMessageBranchStrings.next == "Next version")
        #expect(MonarchAiMessageBranchStrings.position(2, of: 3) == "2 / 3")
        #expect(MonarchAiMessageBranchStrings.announcement(2, of: 3) == "Version 2 of 3")
    }

    @Test("Nothing is laid out for a single version")
    @MainActor
    func hiddenForSingle() {
        let hidden = Host.layout(MonarchAiMessageBranch(selection: .constant(0), count: 1))
        let shown = Host.layout(MonarchAiMessageBranch(selection: .constant(0), count: 1, showsSingle: true))
        #expect(hidden.height < shown.height)
    }

    #if os(iOS)
        @Test("The buttons have a 44-point touch target")
        @MainActor
        func hitTarget() {
            let size = Host.layout(MonarchAiMessageBranch(selection: .constant(0), count: 3).fixedSize())
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

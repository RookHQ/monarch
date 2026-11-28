import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchDuplicatePanelFixtures: ComponentFixtures {
    static let id = "duplicate-panel"
    private static let one = [
        MonarchDuplicateMatch(
            id: "1",
            title: "Example Ltd",
            kind: "Client",
            reasons: ["Same email", "Same company domain"],
            detail: "Created 3 Mar",
            score: 0.92
        )
    ]
    private static let several =
        one + [
            MonarchDuplicateMatch(id: "2", title: "Example Limited", kind: "Open lead", reasons: ["Same name"]),
            MonarchDuplicateMatch(
                id: "3",
                title: "A record with a very long name that wraps onto a second line in a narrow column",
                detail: "Archived",
                score: 0.4
            ),
        ]
    static let fixtures = [
        Fixture("hint, one match, every action") {
            MonarchDuplicatePanel(
                one,
                footer: "You can still create it.",
                onOpen: { _ in },
                onMerge: { _ in },
                onDismiss: { _ in }
            )
        },
        Fixture("hint, several matches, no actions") {
            MonarchDuplicatePanel(several)
        },
        Fixture("panel, custom strings") {
            MonarchDuplicatePanel(
                several,
                variant: .panel,
                title: "Similar records",
                openLabel: "View",
                mergeLabel: "Combine",
                dismissLabel: "Keep both",
                onOpen: { _ in },
                onMerge: { _ in },
                onDismiss: { _ in }
            )
        },
        Fixture("no matches draws nothing") {
            VStack {
                MonarchDuplicatePanel([])
                Text("Form")
            }
        },
    ]
}

@Suite("MonarchDuplicatePanel")
struct MonarchDuplicatePanelTests {
    private let english = Locale(identifier: "en_US")

    @Test("The hint is a warning surface and the panel has no surface")
    func appearance() {
        let hint = MonarchDuplicatePanelTokens.appearance(variant: .hint)
        #expect(hint.background == \MonarchColorTokens.warningSubtle)
        #expect(hint.border == \MonarchColorTokens.warningBorder)
        #expect(hint.icon == \MonarchColorTokens.warningStrong)

        let panel = MonarchDuplicatePanelTokens.appearance(variant: .panel)
        #expect(panel.background == nil)
        #expect(panel.border == nil)
        #expect(panel.icon == nil)
    }

    @Test("The heading is singular for one match and can be replaced")
    func heading() {
        #expect(MonarchDuplicatePanelTokens.heading(count: 1, override: nil) == "Possible duplicate")
        #expect(MonarchDuplicatePanelTokens.heading(count: 3, override: nil) == "Possible duplicates")
        #expect(MonarchDuplicatePanelTokens.heading(count: 3, override: "") == "Possible duplicates")
        #expect(MonarchDuplicatePanelTokens.heading(count: 3, override: "Similar records") == "Similar records")
    }

    @Test("Reasons are joined into one line and empty reasons are left out")
    func reasons() {
        #expect(MonarchDuplicatePanelTokens.reasonsText([], locale: english) == nil)
        #expect(MonarchDuplicatePanelTokens.reasonsText([""], locale: english) == nil)
        #expect(MonarchDuplicatePanelTokens.reasonsText(["Same email"], locale: english) == "Same email")
        #expect(
            MonarchDuplicatePanelTokens.reasonsText(["Same email", "", "Same name"], locale: english)
                == "Same email, Same name"
        )
    }

    @Test("The score is a whole percentage limited to 0 through 100")
    func score() {
        #expect(MonarchDuplicatePanelTokens.scoreText(nil, locale: english) == nil)
        #expect(MonarchDuplicatePanelTokens.scoreText(.nan, locale: english) == nil)
        #expect(MonarchDuplicatePanelTokens.scoreText(0.92, locale: english) == "92% match")
        #expect(MonarchDuplicatePanelTokens.scoreText(1.7, locale: english) == "100% match")
        #expect(MonarchDuplicatePanelTokens.scoreText(-1, locale: english) == "0% match")
    }

    @Test("Each button is named after its record")
    func actionLabel() {
        #expect(MonarchDuplicatePanelTokens.actionLabel(action: "Open", record: "Example Ltd") == "Open: Example Ltd")
        #expect(
            MonarchDuplicatePanelTokens.actionLabel(action: "Merge", record: "Example Ltd")
                != MonarchDuplicatePanelTokens.actionLabel(action: "Merge", record: "Example Limited")
        )
    }

    @Test("The announcement is the heading and the names of the matches")
    func announcement() {
        let matches = [
            MonarchDuplicateMatch(id: "1", title: "Example Ltd"),
            MonarchDuplicateMatch(id: "2", title: "Example Limited"),
        ]
        #expect(
            MonarchDuplicatePanelTokens.announcement(heading: "Possible duplicates", matches: matches, locale: english)
                == "Possible duplicates: Example Ltd, Example Limited"
        )
        #expect(
            MonarchDuplicatePanelTokens.announcement(heading: "Possible duplicate", matches: [], locale: english)
                == "Possible duplicate"
        )
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchDuplicatePanelStrings.open == "Open")
        #expect(MonarchDuplicatePanelStrings.merge == "Merge")
        #expect(MonarchDuplicatePanelStrings.dismiss == "Not a duplicate")
    }

    @Test("A panel with no matches takes no space")
    @MainActor
    func emptyTakesNoSpace() {
        let alone = Host.layout(Text("Form"))
        let withEmptyPanel = Host.layout(
            VStack(spacing: 0) {
                MonarchDuplicatePanel([])
                Text("Form")
            }
        )
        #expect(withEmptyPanel.height == alone.height)
    }
}

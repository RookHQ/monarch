import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiArtifactCardFixtures: ComponentFixtures {
    static let id = "ai-artifact-card"
    static let fixtures = [
        Fixture("every kind") {
            VStack(alignment: .leading) {
                ForEach(MonarchAiArtifactKind.allCases, id: \.self) { kind in
                    MonarchAiArtifactCard("Q3 summary", kind: kind, version: 3) {}
                }
            }
        },
        Fixture("active with description") {
            MonarchAiArtifactCard(
                "Revenue by region",
                kind: .chart,
                version: 12,
                description: "Updated just now",
                isActive: true
            ) {}
        },
        Fixture("long title and custom labels") {
            MonarchAiArtifactCard(
                "A quarterly summary of revenue, churn and expansion for every region and every plan",
                kind: .document,
                version: 2,
                description: "Drafted from the board deck and the finance export.",
                kindLabel: "Report",
                versionLabel: "second draft",
                actionLabel: "Read"
            ) {}
        },
        Fixture("disabled") {
            VStack(alignment: .leading) {
                MonarchAiArtifactCard("parser.swift", kind: .code, version: 1) {}.disabled(true)
                MonarchAiArtifactCard("parser.swift", kind: .code, version: 1, isActive: true) {}.disabled(true)
            }
        },
    ]
}

@Suite("MonarchAiArtifactCard")
struct MonarchAiArtifactCardTests {
    typealias Tokens = MonarchAiArtifactCardTokens

    @Test("Every kind has its own icon and name")
    func kinds() {
        let symbols = MonarchAiArtifactKind.allCases.map(Tokens.symbol)
        #expect(Set(symbols).count == MonarchAiArtifactKind.allCases.count)
        let names = MonarchAiArtifactKind.allCases.map(MonarchAiArtifactCardStrings.kind)
        #expect(names == ["Document", "Code", "Table", "Chart", "Image", "Web page"])
    }

    @Test("The visible line is the kind and the short version")
    func meta() {
        #expect(Tokens.meta(kind: .document, version: 3, kindLabel: nil, versionLabel: nil) == "Document · v3")
        #expect(Tokens.meta(kind: .code, version: 1, kindLabel: "Script", versionLabel: nil) == "Script · v1")
        #expect(Tokens.meta(kind: .code, version: 1, kindLabel: nil, versionLabel: "draft") == "Code · draft")
        #expect(Tokens.meta(kind: .code, version: 1, kindLabel: nil, versionLabel: "") == "Code")
    }

    @Test("The action reads Open, or Showing for the artifact on show")
    func action() {
        #expect(Tokens.actionText(isActive: false, actionLabel: nil) == "Open")
        #expect(Tokens.actionText(isActive: true, actionLabel: nil) == "Showing")
        #expect(Tokens.actionText(isActive: true, actionLabel: "Read") == "Read")
    }

    @Test("VoiceOver reads the title, the kind, the version in words, the description and the action")
    func accessibilityLabel() {
        #expect(
            Tokens.accessibilityLabel(
                title: "Q3 summary",
                kind: .document,
                version: 3,
                description: nil,
                isActive: false
            )
                == "Q3 summary, Document, version 3, Open"
        )
        #expect(
            Tokens.accessibilityLabel(
                title: "Revenue",
                kind: .chart,
                version: 12,
                description: "Updated just now",
                isActive: true
            ) == "Revenue, Chart, version 12, Updated just now, Showing"
        )
        #expect(
            Tokens.accessibilityLabel(
                title: "Notes",
                kind: .document,
                version: 2,
                description: "",
                isActive: false,
                kindLabel: "Report",
                versionLabel: "second draft",
                actionLabel: "Read"
            ) == "Notes, Report, second draft, Read"
        )
    }

    @Test("The artifact on show uses the AI colours and a heavier border")
    func active() {
        let active = Tokens.appearance(isActive: true, isPressed: false, isEnabled: true)
        #expect(active.background == \MonarchColorTokens.aiSubtle)
        #expect(active.border == \MonarchColorTokens.aiBorder)
        #expect(active.iconTile != active.background)
        #expect(Tokens.borderWidth(isActive: true) > Tokens.borderWidth(isActive: false))
    }

    @Test("A card at rest is a bordered surface, and pressing changes its fill and border")
    func restingAndPressed() {
        let resting = Tokens.appearance(isActive: false, isPressed: false, isEnabled: true)
        #expect(resting.background == \MonarchColorTokens.bgSurface)
        #expect(resting.border == \MonarchColorTokens.borderDefault)
        #expect(resting.title == \MonarchColorTokens.textDefault)
        #expect(resting.secondary == \MonarchColorTokens.textMuted)
        #expect(resting.accent == \MonarchColorTokens.aiStrong)
        #expect(resting.iconTile == \MonarchColorTokens.aiSubtle)
        let pressed = Tokens.appearance(isActive: false, isPressed: true, isEnabled: true)
        #expect(pressed.background == \MonarchColorTokens.bgSubtle)
        #expect(pressed.border == \MonarchColorTokens.aiBorder)
        #expect(pressed.title == resting.title)
    }

    @Test("A disabled card uses the disabled roles and ignores presses", arguments: [true, false])
    func disabled(isActive: Bool) {
        let disabled = Tokens.appearance(isActive: isActive, isPressed: false, isEnabled: false)
        #expect(disabled.title == \MonarchColorTokens.textDisabled)
        #expect(disabled.accent == \MonarchColorTokens.textDisabled)
        #expect(disabled.border == \MonarchColorTokens.borderSubtle)
        #expect(disabled == Tokens.appearance(isActive: isActive, isPressed: true, isEnabled: false))
    }

    @Test("The card is at most 448 points wide and the chevron mirrors")
    func layoutTokens() {
        #expect(Tokens.maxWidth == 448)
        #expect(Tokens.chevronSymbol.hasSuffix(".forward"))
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiArtifactCardStrings.version(3) == "v3")
        #expect(MonarchAiArtifactCardStrings.versionLong(3) == "version 3")
        #expect(MonarchAiArtifactCardStrings.open == "Open")
        #expect(MonarchAiArtifactCardStrings.opened == "Showing")
    }

    #if os(iOS)
        @Test("The card keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let card = MonarchAiArtifactCard("Notes", kind: .document, version: 1) {}
            #expect(Host.layout(card).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

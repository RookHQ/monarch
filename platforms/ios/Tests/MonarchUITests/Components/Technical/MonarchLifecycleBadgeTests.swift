import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchLifecycleBadgeFixtures: ComponentFixtures {
    static let id = "lifecycle-badge"
    static let fixtures = [
        Fixture("built-in stages") {
            VStack(alignment: .leading) {
                ForEach(MonarchLifecycle.allCases, id: \.self) { lifecycle in
                    MonarchLifecycleBadge(lifecycle)
                }
            }
        },
        Fixture("every tone") {
            VStack(alignment: .leading) {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchLifecycleBadge(.stable, tone: tone)
                }
            }
        },
        Fixture("without dot") {
            MonarchLifecycleBadge(.deprecated, showsDot: false)
        },
        Fixture("custom stage") {
            VStack(alignment: .leading) {
                MonarchLifecycleBadge(stage: "early_access")
                MonarchLifecycleBadge(stage: "sunset", label: "Sunset on 1 March", tone: .danger)
                MonarchLifecycleBadge(stage: "stable")
            }
        },
        Fixture("empty and long") {
            VStack(alignment: .leading) {
                MonarchLifecycleBadge(stage: "")
                MonarchLifecycleBadge(.preview, label: "Preview for customers on the enterprise plan in Europe")
            }
        },
    ]
}

@Suite("MonarchLifecycleBadge")
struct MonarchLifecycleBadgeTests {
    @Test("Each built-in stage has its tone")
    func stageTones() {
        #expect(MonarchLifecycleBadgeTokens.tone(for: .planned) == .neutral)
        #expect(MonarchLifecycleBadgeTokens.tone(for: .preview) == .info)
        #expect(MonarchLifecycleBadgeTokens.tone(for: .stable) == .success)
        #expect(MonarchLifecycleBadgeTokens.tone(for: .deprecated) == .warning)
    }

    @Test("A key that names a built-in stage is drawn as that stage", arguments: MonarchLifecycle.allCases)
    func builtInKey(lifecycle: MonarchLifecycle) {
        let key = lifecycle.rawValue
        #expect(MonarchLifecycleBadgeTokens.tone(forKey: key) == MonarchLifecycleBadgeTokens.tone(for: lifecycle))
        #expect(MonarchLifecycleBadgeTokens.label(forKey: key) == MonarchLifecycleBadgeStrings.label(lifecycle))
    }

    @Test("An unknown key is neutral and shown as readable text")
    func unknownKey() {
        #expect(MonarchLifecycleBadgeTokens.tone(forKey: "early_access") == .neutral)
        #expect(MonarchLifecycleBadgeTokens.label(forKey: "early_access") == "Early access")
        // Keys are matched exactly, as the server sends them.
        #expect(MonarchLifecycleBadgeTokens.tone(forKey: "Stable") == .neutral)
    }

    @Test("A key is made readable")
    func readable() {
        #expect(MonarchLifecycleBadgeTokens.readable("early_access") == "Early access")
        #expect(MonarchLifecycleBadgeTokens.readable("end-of-life") == "End of life")
        #expect(MonarchLifecycleBadgeTokens.readable("__general__availability--") == "General availability")
        #expect(MonarchLifecycleBadgeTokens.readable("GA") == "GA")
        #expect(MonarchLifecycleBadgeTokens.readable("") == "")
    }

    @Test("The built-in labels come from the component's strings table")
    func strings() {
        #expect(MonarchLifecycleBadgeStrings.label(.planned) == "Planned")
        #expect(MonarchLifecycleBadgeStrings.label(.preview) == "Preview")
        #expect(MonarchLifecycleBadgeStrings.label(.stable) == "Stable")
        #expect(MonarchLifecycleBadgeStrings.label(.deprecated) == "Deprecated")
    }

    @Test("The capsule is quiet in every tone; the tone is the border and the dot")
    func appearance() {
        let success = MonarchLifecycleBadgeTokens.appearance(tone: .success)
        #expect(success.surface.background == \MonarchColorTokens.bgSurface)
        #expect(success.surface.foreground == \MonarchColorTokens.textDefault)
        #expect(success.surface.border == \MonarchColorTokens.successBorder)
        #expect(success.dot == \MonarchColorTokens.successStrong)

        let warning = MonarchLifecycleBadgeTokens.appearance(tone: .warning)
        #expect(warning.surface.foreground == \MonarchColorTokens.textDefault)
        #expect(warning.surface.border == \MonarchColorTokens.warningBorder)
        #expect(warning.dot == \MonarchColorTokens.warningStrong)
    }

    @Test("The neutral badge has the default border and a subtle dot")
    func neutralAppearance() {
        let neutral = MonarchLifecycleBadgeTokens.appearance(tone: .neutral)
        #expect(neutral.surface.border == \MonarchColorTokens.borderDefault)
        #expect(neutral.dot == \MonarchColorTokens.textSubtle)
    }

    @Test("Every tone has its own dot colour")
    func dotsAreDistinct() {
        let dots = MonarchTone.allCases.map { MonarchLifecycleBadgeTokens.appearance(tone: $0).dot }
        #expect(Set(dots).count == MonarchTone.allCases.count)
    }
}

import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchMethodBadgeFixtures: ComponentFixtures {
    static let id = "method-badge"
    static let fixtures = [
        Fixture("common methods") {
            VStack(alignment: .leading) {
                ForEach(["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"], id: \.self) { method in
                    MonarchMethodBadge(method)
                }
            }
        },
        Fixture("unknown and lower case") {
            VStack(alignment: .leading) {
                MonarchMethodBadge("purge")
                MonarchMethodBadge("get")
            }
        },
        Fixture("tone override") {
            HStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchMethodBadge("GET", tone: tone)
                }
            }
        },
        Fixture("empty and long") {
            VStack(alignment: .leading) {
                MonarchMethodBadge("")
                MonarchMethodBadge("a-method-name-that-is-far-too-long-for-a-badge-in-a-row")
            }
        },
    ]
}

@Suite("MonarchMethodBadge")
struct MonarchMethodBadgeTests {
    @Test(
        "Each common method has its tone",
        arguments: [
            ("GET", MonarchTone.success), ("POST", .info), ("PUT", .warning), ("PATCH", .warning),
            ("DELETE", .danger), ("HEAD", .neutral), ("OPTIONS", .neutral),
        ]
    )
    func methodTone(method: String, tone: MonarchTone) {
        #expect(MonarchMethodBadgeTokens.tone(for: method) == tone)
        #expect(MonarchMethodBadge.tone(for: method) == tone)
    }

    @Test("The tone is matched in any case and around white space")
    func toneIgnoresCase() {
        #expect(MonarchMethodBadgeTokens.tone(for: "delete") == .danger)
        #expect(MonarchMethodBadgeTokens.tone(for: " Post\n") == .info)
    }

    @Test("A method outside the common set is neutral")
    func unknownMethod() {
        #expect(MonarchMethodBadgeTokens.tone(for: "PURGE") == .neutral)
        #expect(MonarchMethodBadgeTokens.tone(for: "") == .neutral)
    }

    @Test("The method is shown trimmed and in upper case")
    func text() {
        #expect(MonarchMethodBadgeTokens.text("get") == "GET")
        #expect(MonarchMethodBadgeTokens.text("  patch ") == "PATCH")
        #expect(MonarchMethodBadgeTokens.text("") == "")
    }

    @Test("A toned badge has the tone's tinted fill, strong text and border")
    func tonedAppearance() {
        let danger = MonarchMethodBadgeTokens.appearance(tone: .danger)
        #expect(danger.background == \MonarchColorTokens.dangerSubtle)
        #expect(danger.foreground == \MonarchColorTokens.dangerStrong)
        #expect(danger.border == \MonarchColorTokens.dangerBorder)

        let success = MonarchMethodBadgeTokens.appearance(tone: .success)
        #expect(success.background == \MonarchColorTokens.successSubtle)
        #expect(success.foreground == \MonarchColorTokens.successStrong)
        #expect(success.border == \MonarchColorTokens.successBorder)
    }

    @Test("The neutral badge is muted and has no border")
    func neutralAppearance() {
        let neutral = MonarchMethodBadgeTokens.appearance(tone: .neutral)
        #expect(neutral.background == \MonarchColorTokens.bgMuted)
        #expect(neutral.foreground == \MonarchColorTokens.textMuted)
        #expect(neutral.border == nil)
    }

    @Test("Every tone has its own text colour")
    func tonesAreDistinct() {
        let foregrounds = MonarchTone.allCases.map { MonarchMethodBadgeTokens.appearance(tone: $0).foreground }
        #expect(Set(foregrounds).count == MonarchTone.allCases.count)
    }

    @Test("The method is set in the monospaced code role")
    func typeRole() {
        #expect(MonarchMethodBadgeTokens.textStyle.spec.isMonospaced)
    }
}

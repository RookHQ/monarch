import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiLauncherFixtures: ComponentFixtures {
    static let id = "ai-launcher"
    static let fixtures = [
        Fixture("solid, closed and open") {
            HStack {
                MonarchAiLauncher(isOpen: .constant(false))
                MonarchAiLauncher(isOpen: .constant(true))
                MonarchAiLauncher(isOpen: .constant(false), showsLabel: true)
            }
        },
        Fixture("badges") {
            HStack {
                MonarchAiLauncher(isOpen: .constant(false), badge: .unread)
                MonarchAiLauncher(isOpen: .constant(false), badge: .working)
                MonarchAiLauncher(isOpen: .constant(false), badge: .count(3))
                MonarchAiLauncher(isOpen: .constant(false), badge: .count(250))
                MonarchAiLauncher(isOpen: .constant(false), badge: .count(0))
            }
        },
        Fixture("subtle") {
            VStack(alignment: .leading) {
                MonarchAiLauncher(isOpen: .constant(false), variant: .subtle)
                MonarchAiLauncher(isOpen: .constant(true), variant: .subtle, badge: .working)
                MonarchAiLauncher(isOpen: .constant(false), variant: .subtle, badge: .count(12), showsLabel: false)
            }
        },
        Fixture("custom symbol, label and no shortcut") {
            MonarchAiLauncher(
                isOpen: .constant(false),
                systemImage: "bubble.left.and.text.bubble.right",
                variant: .subtle,
                badge: .unread,
                shortcut: nil,
                label: "Ask the workspace assistant about this page",
                badgeLabel: "Two replies are waiting",
                openLabel: "Showing"
            )
        },
        Fixture("disabled") {
            HStack {
                MonarchAiLauncher(isOpen: .constant(false)).disabled(true)
                MonarchAiLauncher(isOpen: .constant(false), variant: .subtle).disabled(true)
            }
        },
    ]
}

@Suite("MonarchAiLauncher")
struct MonarchAiLauncherTests {
    @Test("The solid launcher is filled with the AI colour and darkens while pressed")
    func solidAppearance() {
        let resting = MonarchAiLauncherTokens.appearance(
            variant: .solid,
            isOpen: false,
            isPressed: false,
            isEnabled: true
        )
        #expect(resting.background == \MonarchColorTokens.bgInverse)
        #expect(resting.foreground == \MonarchColorTokens.textInverse)
        #expect(resting.border == nil)

        let pressed = MonarchAiLauncherTokens.appearance(
            variant: .solid,
            isOpen: true,
            isPressed: true,
            isEnabled: true
        )
        #expect(pressed.background == \MonarchColorTokens.textDefault)
        #expect(pressed.foreground == \MonarchColorTokens.textInverse)

        let disabled = MonarchAiLauncherTokens.appearance(
            variant: .solid,
            isOpen: false,
            isPressed: true,
            isEnabled: false
        )
        #expect(disabled.background == \MonarchColorTokens.bgMuted)
        #expect(disabled.foreground == \MonarchColorTokens.textDisabled)
    }

    @Test("The subtle launcher is tinted, and its border is stronger while the assistant is open")
    func subtleAppearance() {
        let closed = MonarchAiLauncherTokens.appearance(
            variant: .subtle,
            isOpen: false,
            isPressed: false,
            isEnabled: true
        )
        #expect(closed.background == \MonarchColorTokens.bgSurface)
        #expect(closed.foreground == \MonarchColorTokens.aiStrong)
        #expect(closed.border == \MonarchColorTokens.borderDefault)

        let open = MonarchAiLauncherTokens.appearance(variant: .subtle, isOpen: true, isPressed: false, isEnabled: true)
        #expect(open.border == \MonarchColorTokens.aiSolid)

        let pressed = MonarchAiLauncherTokens.appearance(
            variant: .subtle,
            isOpen: false,
            isPressed: true,
            isEnabled: true
        )
        #expect(pressed.background == \MonarchColorTokens.bgMuted)

        let disabled = MonarchAiLauncherTokens.appearance(
            variant: .subtle,
            isOpen: true,
            isPressed: false,
            isEnabled: false
        )
        #expect(disabled.background == \MonarchColorTokens.bgSubtle)
        #expect(disabled.foreground == \MonarchColorTokens.textDisabled)
        #expect(disabled.border == \MonarchColorTokens.borderSubtle)
    }

    @Test("An open floating launcher shows a close mark; otherwise the app's symbol or the AI glyph")
    func glyph() {
        #expect(MonarchAiLauncherTokens.glyph(isOpen: false, variant: .solid, systemImage: nil) == .bot)
        #expect(MonarchAiLauncherTokens.glyph(isOpen: true, variant: .solid, systemImage: nil) == .close)
        #expect(MonarchAiLauncherTokens.glyph(isOpen: true, variant: .solid, systemImage: "bubble") == .close)
        #expect(
            MonarchAiLauncherTokens.glyph(isOpen: false, variant: .solid, systemImage: "bubble") == .symbol("bubble")
        )
        #expect(MonarchAiLauncherTokens.glyph(isOpen: true, variant: .subtle, systemImage: nil) == .bot)
        #expect(
            MonarchAiLauncherTokens.glyph(isOpen: true, variant: .subtle, systemImage: "bubble") == .symbol("bubble")
        )
    }

    @Test("The label is shown as asked, otherwise only by the subtle launcher")
    func showsLabel() {
        #expect(!MonarchAiLauncherTokens.showsLabel(nil, variant: .solid))
        #expect(MonarchAiLauncherTokens.showsLabel(nil, variant: .subtle))
        #expect(MonarchAiLauncherTokens.showsLabel(true, variant: .solid))
        #expect(!MonarchAiLauncherTokens.showsLabel(false, variant: .subtle))
    }

    @Test("A count is shown in digits up to 99")
    func countText() {
        #expect(MonarchAiLauncherTokens.countText(0) == nil)
        #expect(MonarchAiLauncherTokens.countText(-3) == nil)
        #expect(MonarchAiLauncherTokens.countText(1) == "1")
        #expect(MonarchAiLauncherTokens.countText(99) == "99")
        #expect(MonarchAiLauncherTokens.countText(100) == "99+")
    }

    @Test("VoiceOver reads the open state and the badge as the value")
    func accessibilityValue() {
        func value(isOpen: Bool, badge: MonarchAiLauncherBadge?) -> String {
            MonarchAiLauncherTokens.accessibilityValue(
                isOpen: isOpen,
                openLabel: MonarchAiLauncherStrings.open,
                badgeLabel: MonarchAiLauncherStrings.badge(badge)
            )
        }
        #expect(value(isOpen: false, badge: nil).isEmpty)
        #expect(value(isOpen: true, badge: nil) == "Open")
        #expect(value(isOpen: false, badge: .unread) == "New messages")
        #expect(value(isOpen: false, badge: .working) == "Working")
        #expect(value(isOpen: false, badge: .count(1)) == "1 unread message")
        #expect(value(isOpen: true, badge: .count(120)) == "Open, 120 unread messages")
        #expect(value(isOpen: false, badge: .count(0)).isEmpty)
        #expect(
            MonarchAiLauncherTokens.accessibilityValue(isOpen: true, openLabel: "Showing", badgeLabel: "") == "Showing"
        )
    }

    @Test("The touch target is at least 44 points in both variants")
    func hitTarget() {
        for variant in MonarchAiLauncherVariant.allCases {
            #expect(MonarchAiLauncherTokens.minSize(variant) >= MonarchAccessibility.minimumHitTarget)
        }
        #expect(MonarchAiLauncherTokens.minSize(.solid) > MonarchAiLauncherTokens.minSize(.subtle))
        #expect(MonarchAiLauncherTokens.elevation(.solid) != .none)
        #expect(MonarchAiLauncherTokens.elevation(.subtle) == .none)
    }

    @Test("The moving part of the working ring covers half the outline and wraps around its end")
    func ringSegments() {
        #expect(MonarchAiLauncherTokens.ringSegments(phase: 0) == [0...0.5])
        #expect(MonarchAiLauncherTokens.ringSegments(phase: 0.25) == [0.25...0.75])
        #expect(MonarchAiLauncherTokens.ringSegments(phase: 0.75) == [0.75...1, 0...0.25])
        // The phase counts turns, so a whole number of turns later the ring is in the same place.
        #expect(MonarchAiLauncherTokens.ringSegments(phase: 7.25) == [0.25...0.75])
        for phase in stride(from: 0.0, to: 3.0, by: 0.07) {
            let covered = MonarchAiLauncherTokens.ringSegments(phase: phase)
                .reduce(0) { $0 + ($1.upperBound - $1.lowerBound) }
            #expect(abs(covered - MonarchAiLauncherTokens.ringArc) < 0.000_001)
        }
        #expect(MonarchAiLauncherTokens.ringPeriod > 0)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiLauncherStrings.label == "Assistant")
        #expect(MonarchAiLauncherStrings.open == "Open")
        #expect(MonarchAiLauncherStrings.unread == "New messages")
        #expect(MonarchAiLauncherStrings.working == "Working")
        #expect(MonarchAiLauncherStrings.count(1) == "1 unread message")
        #expect(MonarchAiLauncherStrings.count(2) == "2 unread messages")
        #expect(MonarchAiLauncherStrings.badge(nil) == nil)
    }

    #if os(iOS)
        @Test("The launcher is at least 44 by 44 points")
        @MainActor
        func layoutHitTarget() {
            for variant in MonarchAiLauncherVariant.allCases {
                let size = Host.layout(MonarchAiLauncher(isOpen: .constant(false), variant: variant).fixedSize())
                #expect(size.height >= MonarchAccessibility.minimumHitTarget)
            }
        }
    #endif
}

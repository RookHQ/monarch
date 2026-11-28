import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchNotificationsBellFixtures: ComponentFixtures {
    static let id = "notifications-bell"
    static let fixtures = [
        Fixture("icon: no unread, a count, a count above the limit") {
            HStack {
                MonarchNotificationsBell {}
                MonarchNotificationsBell(unreadCount: 3) {}
                MonarchNotificationsBell(unreadCount: 250) {}
            }
        },
        Fixture("icon: disabled") {
            MonarchNotificationsBell(unreadCount: 3) {}.disabled(true)
        },
        Fixture("row: no unread and a count") {
            VStack {
                MonarchNotificationsBell(variant: .row) {}
                MonarchNotificationsBell(unreadCount: 12, variant: .row) {}
                MonarchNotificationsBell(unreadCount: 1200, variant: .row) {}.disabled(true)
            }
        },
        Fixture("row: long title and spoken overrides") {
            MonarchNotificationsBell(
                unreadCount: 4,
                variant: .row,
                title: "Notifications from every workspace you belong to",
                unreadLabel: "4 new since yesterday"
            ) {}
        },
    ]
}

@Suite("MonarchNotificationsBell")
struct MonarchNotificationsBellTests {
    @Test("There is no badge when nothing is unread", arguments: [0, -1, -40])
    func noBadge(count: Int) {
        #expect(MonarchNotificationsBellTokens.badgeText(count) == nil)
    }

    @Test("The badge shows the count up to 99 and 99+ above it")
    func badgeText() {
        #expect(MonarchNotificationsBellTokens.badgeText(1) == "1")
        #expect(MonarchNotificationsBellTokens.badgeText(99) == "99")
        #expect(MonarchNotificationsBellTokens.badgeText(100) == "99+")
        #expect(MonarchNotificationsBellTokens.badgeText(12_000) == "99+")
    }

    @Test("The spoken value is the exact count, even above the badge limit")
    func accessibilityValue() {
        #expect(MonarchNotificationsBellTokens.accessibilityValue(count: 0, override: nil).isEmpty)
        #expect(MonarchNotificationsBellTokens.accessibilityValue(count: -2, override: "2 new").isEmpty)
        #expect(MonarchNotificationsBellTokens.accessibilityValue(count: 3, override: nil) == "3 unread")
        #expect(
            MonarchNotificationsBellTokens.accessibilityValue(count: 250, override: nil)
                == "\(250.formatted()) unread"
        )
        #expect(MonarchNotificationsBellTokens.accessibilityValue(count: 3, override: "3 new") == "3 new")
    }

    @Test("Colours at rest, pressed and disabled")
    func appearance() {
        let resting = MonarchNotificationsBellTokens.appearance(isPressed: false, isEnabled: true)
        #expect(resting.background == nil)
        #expect(resting.foreground == \MonarchColorTokens.textMuted)

        let pressed = MonarchNotificationsBellTokens.appearance(isPressed: true, isEnabled: true)
        #expect(pressed.background == \MonarchColorTokens.bgMuted)
        #expect(pressed.foreground == \MonarchColorTokens.textDefault)

        let disabled = MonarchNotificationsBellTokens.appearance(isPressed: false, isEnabled: false)
        #expect(disabled.foreground == \MonarchColorTokens.textDisabled)
        #expect(disabled == MonarchNotificationsBellTokens.appearance(isPressed: true, isEnabled: false))
        #expect([resting, pressed, disabled].allSatisfy { $0.border == nil })
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchNotificationsBellStrings.title == "Notifications")
        #expect(MonarchNotificationsBellStrings.unread("7") == "7 unread")
        #expect(MonarchNotificationsBellStrings.overflow("99") == "99+")
    }

    @Test("The visible box is a medium control and most of the badge lies past the bell")
    func tokens() {
        #expect(MonarchNotificationsBellTokens.boxSize == 36)
        #expect(MonarchNotificationsBellTokens.badgeOverlap > 0)
        #expect(MonarchNotificationsBellTokens.badgeOverlap < 0.5)
    }

    #if os(iOS)
        @Test("Both variants keep a 44-point touch target", arguments: MonarchNotificationsBellVariant.allCases)
        @MainActor
        func hitTarget(variant: MonarchNotificationsBellVariant) {
            let size = Host.layout(MonarchNotificationsBell(unreadCount: 2, variant: variant) {}.fixedSize())
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
            #expect(size.width >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

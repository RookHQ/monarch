import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchChatDockFixtures: ComponentFixtures {
    static let id = "chat-dock"
    static let fixtures = [
        Fixture("three unread") {
            MonarchChatDock(isPresented: .constant(false), unread: 3) { Text("Chat") }
        },
        Fixture("none unread, own name") {
            MonarchChatDock(isPresented: .constant(false), title: "Team chat") { Text("Chat") }
        },
        Fixture("more than 99 unread") {
            MonarchChatDock(isPresented: .constant(false), unread: 120) { Text("Chat") }
        },
    ]
}

@Suite("MonarchChatDock")
struct MonarchChatDockTests {
    @Test("The badge shows the count, 99+ above 99, and nothing at zero")
    func badge() {
        #expect(MonarchChatDockTokens.badgeText(0) == nil)
        #expect(MonarchChatDockTokens.badgeText(3) == "3")
        #expect(MonarchChatDockTokens.badgeText(99) == "99")
        #expect(MonarchChatDockTokens.badgeText(120) == "99+")
    }

    @Test("The launcher's value is the unread count")
    func accessibilityValue() {
        #expect(MonarchChatDockTokens.accessibilityValue(unread: 0, format: nil).isEmpty)
        #expect(MonarchChatDockTokens.accessibilityValue(unread: 3, format: nil) == "3 unread")
        #expect(MonarchChatDockTokens.accessibilityValue(unread: 2, format: "%d new") == "2 new")
    }

    @Test("Colours come from theme roles")
    func colours() {
        #expect(MonarchChatDockTokens.launcherFill == \MonarchColorTokens.bgInverse)
        #expect(MonarchChatDockTokens.launcherForeground == \MonarchColorTokens.textInverse)
        #expect(MonarchChatDockTokens.badgeFill == \MonarchColorTokens.dangerSolid)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchChatDockStrings.label == "Rook Chat")
        #expect(MonarchChatDockStrings.unread == "%d unread")
    }

    #if os(iOS)
        @Test("The launcher is at least 44 points")
        @MainActor
        func hitTarget() {
            let size = Host.layout(MonarchChatDock(isPresented: .constant(false)) { Text("Chat") })
            #expect(size.width >= MonarchAccessibility.minimumHitTarget)
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

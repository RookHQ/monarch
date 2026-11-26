import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct SkipLinkFixture: View {
    let title: String?
    let isAlwaysVisible: Bool
    @AccessibilityFocusState private var isMainFocused: Bool

    var body: some View {
        VStack(alignment: .leading) {
            MonarchSkipLink(title, target: $isMainFocused, isAlwaysVisible: isAlwaysVisible)
            Text("Main content starts here.").accessibilityFocused($isMainFocused)
        }
    }
}

enum MonarchSkipLinkFixtures: ComponentFixtures {
    static let id = "skip-link"
    static let fixtures = [
        Fixture("always visible") { SkipLinkFixture(title: nil, isAlwaysVisible: true) },
        Fixture("custom title") { SkipLinkFixture(title: "Skip to search", isAlwaysVisible: true) },
        Fixture("only for assistive technologies") { SkipLinkFixture(title: nil, isAlwaysVisible: false) },
    ]
}

@Suite("MonarchSkipLink")
struct MonarchSkipLinkTests {
    @Test("The control is drawn when asked, for VoiceOver or Switch Control, or with keyboard focus")
    func visibility() {
        #expect(
            !MonarchSkipLinkTokens.isVisible(
                isAlwaysVisible: false,
                isVoiceOverRunning: false,
                isSwitchControlRunning: false,
                isFocused: false
            )
        )
        #expect(
            MonarchSkipLinkTokens.isVisible(
                isAlwaysVisible: true,
                isVoiceOverRunning: false,
                isSwitchControlRunning: false,
                isFocused: false
            )
        )
        #expect(
            MonarchSkipLinkTokens.isVisible(
                isAlwaysVisible: false,
                isVoiceOverRunning: true,
                isSwitchControlRunning: false,
                isFocused: false
            )
        )
        #expect(
            MonarchSkipLinkTokens.isVisible(
                isAlwaysVisible: false,
                isVoiceOverRunning: false,
                isSwitchControlRunning: true,
                isFocused: false
            )
        )
        #expect(
            MonarchSkipLinkTokens.isVisible(
                isAlwaysVisible: false,
                isVoiceOverRunning: false,
                isSwitchControlRunning: false,
                isFocused: true
            )
        )
    }

    @Test("The built-in title resolves")
    func strings() {
        #expect(MonarchSkipLinkStrings.label == "Skip to main content")
    }
}

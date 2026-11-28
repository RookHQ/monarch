import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct EmailNotifications: View {
    @State private var assigned = true
    @State private var summary = false
    @State private var mentions = true
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            Text("Email notifications").monarchTextStyle(.bodyStrong)
            Text("Choose which updates reach your inbox.").monarchTextStyle(.bodySmall)
            Toggle("New tickets assigned to me", isOn: $assigned).toggleStyle(.monarchCheckbox)
            Toggle("Daily summary", isOn: $summary).toggleStyle(.monarchCheckbox)
            Toggle("Mentions", isOn: $mentions).toggleStyle(.monarchCheckbox)
        }
    }
}

enum MonarchEmbeddedAppFrameFixtures: ComponentFixtures {
    static let id = "embedded-app-frame"
    static let fixtures = [
        Fixture("ready") {
            MonarchEmbeddedAppFrame("Example Desk: Email notifications") { EmailNotifications() }
        },
        Fixture("loading, fixed height") {
            MonarchEmbeddedAppFrame("Example Desk: Billing", state: .loading, height: MonarchTokens.Space.space24 * 2) {
                EmptyView()
            }
        },
        Fixture("failed, with retry and open") {
            MonarchEmbeddedAppFrame(
                "Example Desk: Billing",
                state: .failed,
                openURL: URL(string: "https://example.com/billing"),
                labels: MonarchEmbeddedAppFrameLabels(open: "Open in Example Desk"),
                onRetry: {}
            ) {
                EmptyView()
            }
        },
    ]
}

@Suite("MonarchEmbeddedAppFrame")
struct MonarchEmbeddedAppFrameTests {
    @Test("The frame's spoken value follows its state")
    func accessibilityValue() {
        let labels = MonarchEmbeddedAppFrameLabels()
        #expect(MonarchEmbeddedAppFrameTokens.accessibilityValue(.ready, labels: labels).isEmpty)
        #expect(MonarchEmbeddedAppFrameTokens.accessibilityValue(.loading, labels: labels) == "Loading…")
        #expect(MonarchEmbeddedAppFrameTokens.accessibilityValue(.failed, labels: labels) == "This page didn’t open")
        let custom = MonarchEmbeddedAppFrameLabels(loading: "Opening")
        #expect(MonarchEmbeddedAppFrameTokens.accessibilityValue(.loading, labels: custom) == "Opening")
    }

    @Test("Colours come from theme roles")
    func colours() {
        #expect(MonarchEmbeddedAppFrameTokens.fill == \MonarchColorTokens.bgSurface)
        #expect(MonarchEmbeddedAppFrameTokens.border == \MonarchColorTokens.borderSubtle)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchEmbeddedAppFrameStrings.retry == "Try again")
        #expect(MonarchEmbeddedAppFrameStrings.open == "Open page")
        #expect(MonarchEmbeddedAppFrameStrings.errorDescription.hasPrefix("The app didn’t respond in time."))
    }
}

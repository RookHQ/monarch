import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchListRowFixtures: ComponentFixtures {
    static let id = "list-row"
    static let fixtures = [
        Fixture("rows") {
            VStack(spacing: 0) {
                MonarchListRow("Ana Lima", subtitle: "Owner", systemImage: "person", showsChevron: true)
                MonarchListRow("Notifications", systemImage: "bell") { MonarchBadge("3", tone: .info) }
                MonarchListRow("Billing", subtitle: "Plan and invoices", showsDivider: false)
                MonarchListRow("Disabled", subtitle: "Cannot open", showsChevron: true).disabled(true)
            }
        },
        Fixture("long title") {
            MonarchListRow(
                "A title that is long enough to wrap onto a second line on a phone screen",
                subtitle: "And supporting text that is long as well, so it wraps too",
                systemImage: "doc"
            )
        },
        Fixture("in a list") {
            List {
                ForEach(["One", "Two", "Three"], id: \.self) { MonarchListRow($0).monarchListRowStyle() }
            }
            .listStyle(.plain)
            .monarchCanvas()
            .frame(height: 200)
        },
    ]
}

@Suite("MonarchListRow")
struct MonarchListRowTests {
    @Test("A row is at least 44 points high, with 16 point side padding")
    func metrics() {
        #expect(MonarchListRowTokens.minHeight >= MonarchAccessibility.minimumHitTarget)
        #expect(MonarchListRowTokens.horizontalPadding == 16)
        #expect(MonarchListRowTokens.spacing == 12)
    }

    @Test("The divider starts under the text: past the icon when there is one")
    func dividerInset() {
        #expect(MonarchListRowTokens.dividerLeadingInset(hasIcon: false) == 16)
        #expect(
            MonarchListRowTokens.dividerLeadingInset(hasIcon: true)
                == MonarchListRowTokens.horizontalPadding + MonarchListRowTokens.leadingWidth
                + MonarchListRowTokens.spacing
        )
    }

    @Test("Colours follow the enabled state")
    func colors() {
        #expect(MonarchListRowTokens.titleColor(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchListRowTokens.titleColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchListRowTokens.supportingColor(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchListRowTokens.supportingColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchListRowTokens.dividerColor == \MonarchColorTokens.borderSubtle)
    }
}

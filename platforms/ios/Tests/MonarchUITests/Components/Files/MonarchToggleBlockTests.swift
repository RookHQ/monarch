import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchToggleBlockFixtures: ComponentFixtures {
    static let id = "toggle-block"
    static let fixtures = [
        Fixture("collapsed and expanded") {
            VStack(alignment: .leading) {
                MonarchToggleBlock("Collapsed section") { Text("Hidden") }
                MonarchToggleBlock("Expanded section", initiallyExpanded: true) {
                    Text("The importer runs nightly and keeps a week of logs.").monarchTextStyle(.body)
                }
            }
        },
        Fixture("bound, custom spoken values, long summary") {
            MonarchToggleBlock(
                "Why the nightly import sometimes skips records that were changed during the run",
                isExpanded: .constant(true),
                expandedLabel: "Open",
                collapsedLabel: "Closed"
            ) {
                Text("Records changed after the snapshot are picked up the next night.").monarchTextStyle(.body)
            }
        },
        Fixture("disabled") {
            MonarchToggleBlock("Disabled") { Text("Hidden") }.disabled(true)
        },
    ]
}

@Suite("MonarchToggleBlock")
struct MonarchToggleBlockTests {
    @Test("The chevron points forward while collapsed and down while expanded")
    func chevron() {
        #expect(MonarchToggleBlockTokens.chevronSymbol(isExpanded: false) == "chevron.forward")
        #expect(MonarchToggleBlockTokens.chevronSymbol(isExpanded: true) == "chevron.down")
    }

    @Test("Colours for enabled and disabled")
    func colours() {
        #expect(MonarchToggleBlockTokens.summary(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchToggleBlockTokens.summary(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchToggleBlockTokens.chevron(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchToggleBlockTokens.chevron(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("The spoken value is the state, or the app's words")
    func accessibilityValue() {
        #expect(
            MonarchDisclosureGroupTokens.accessibilityValue(isExpanded: true, expandedLabel: nil, collapsedLabel: nil)
                == "Expanded"
        )
        #expect(
            MonarchDisclosureGroupTokens.accessibilityValue(
                isExpanded: false,
                expandedLabel: nil,
                collapsedLabel: "Shut"
            )
                == "Shut"
        )
    }

    @Test("Expanded content takes room and collapsed content takes none")
    @MainActor
    func layout() {
        let collapsed = Host.layout(MonarchToggleBlock("Section") { Text("Line one\nLine two\nLine three") })
        let expanded = Host.layout(
            MonarchToggleBlock("Section", initiallyExpanded: true) { Text("Line one\nLine two\nLine three") }
        )
        #expect(expanded.height > collapsed.height)
        #expect(collapsed.height >= MonarchAccessibility.minimumHitTarget)
    }
}

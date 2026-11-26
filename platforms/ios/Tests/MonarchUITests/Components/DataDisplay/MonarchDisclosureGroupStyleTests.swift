import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchDisclosureGroupStyleFixtures: ComponentFixtures {
    static let id = "collapsible"
    static let fixtures = [
        Fixture("collapsed") {
            DisclosureGroup("Advanced settings") { Text("Content") }
                .disclosureGroupStyle(.monarch)
        },
        Fixture("expanded") {
            DisclosureGroup("Advanced settings", isExpanded: .constant(true)) {
                Text("The proxy is used for every request.").monarchTextStyle(.body)
                Toggle("Use a proxy", isOn: .constant(true))
            }
            .disclosureGroupStyle(.monarch)
        },
        Fixture("disabled") {
            DisclosureGroup("Advanced settings", isExpanded: .constant(true)) { Text("Content") }
                .disclosureGroupStyle(.monarch)
                .disabled(true)
        },
        Fixture("long label and custom spoken values") {
            DisclosureGroup(
                "Show the settings that most people never need to change, such as the proxy and the timeouts"
            ) {
                Text("Content")
            }
            .disclosureGroupStyle(MonarchDisclosureGroupStyle(expandedLabel: "Open", collapsedLabel: "Closed"))
        },
    ]
}

@Suite("MonarchDisclosureGroupStyle")
@MainActor
struct MonarchDisclosureGroupStyleTests {
    @Test("VoiceOver reads the state as the value of the header")
    func accessibilityValue() {
        typealias Tokens = MonarchDisclosureGroupTokens
        #expect(Tokens.accessibilityValue(isExpanded: true, expandedLabel: nil, collapsedLabel: nil) == "Expanded")
        #expect(Tokens.accessibilityValue(isExpanded: false, expandedLabel: nil, collapsedLabel: nil) == "Collapsed")
        #expect(Tokens.accessibilityValue(isExpanded: true, expandedLabel: "Open", collapsedLabel: "Closed") == "Open")
        #expect(
            Tokens.accessibilityValue(isExpanded: false, expandedLabel: "Open", collapsedLabel: "Closed") == "Closed"
        )
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchDisclosureGroupStrings.expanded == "Expanded")
        #expect(MonarchDisclosureGroupStrings.collapsed == "Collapsed")
    }

    @Test("A disabled header uses the disabled text role")
    func roles() {
        #expect(MonarchDisclosureGroupTokens.label(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchDisclosureGroupTokens.label(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchDisclosureGroupTokens.chevron(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchDisclosureGroupTokens.chevron(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("The chevron turns half a circle when expanded")
    func rotation() {
        #expect(MonarchDisclosureGroupTokens.rotation(isExpanded: false) == 0)
        #expect(MonarchDisclosureGroupTokens.rotation(isExpanded: true) == 180)
    }

    @Test("The header is a touch target and the content adds height only when expanded")
    func layout() {
        let collapsed = Host.layout(
            DisclosureGroup("Details", isExpanded: .constant(false)) { Text("Content") }
                .disclosureGroupStyle(.monarch)
        )
        let expanded = Host.layout(
            DisclosureGroup("Details", isExpanded: .constant(true)) { Text("Content") }
                .disclosureGroupStyle(.monarch)
        )
        #expect(collapsed.height >= MonarchAccessibility.minimumHitTarget)
        #expect(expanded.height > collapsed.height)
    }
}

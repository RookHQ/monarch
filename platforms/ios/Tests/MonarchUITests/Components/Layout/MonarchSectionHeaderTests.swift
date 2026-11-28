import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchSectionHeaderFixtures: ComponentFixtures {
    static let id = "section-header"
    static let fixtures = [
        Fixture("title only, both sizes") {
            VStack {
                ForEach(MonarchSectionHeaderSize.allCases, id: \.self) { size in
                    MonarchSectionHeader("Recent activity", size: size)
                }
            }
        },
        Fixture("description, meta and actions") {
            MonarchSectionHeader("Invoices", description: "Sent in the last 30 days", meta: "12 invoices") {
                MonarchButton("Export", variant: .ghost, size: .sm) {}
                MonarchButton("New invoice", size: .sm) {}
            }
        },
        Fixture("busy, with and without a label") {
            VStack {
                MonarchSectionHeader("Team", size: .md, isBusy: true)
                MonarchSectionHeader("Members", isBusy: true, busyLabel: "Refreshing members") {
                    MonarchButton("Invite", size: .sm) {}
                }
            }
        },
        Fixture("long text") {
            MonarchSectionHeader(
                "Scheduled reports that are sent to every member of the finance workspace",
                description: "Reports are generated at midnight in the time zone of the workspace and sent by email.",
                meta: "Updated 5 minutes ago by the reporting service"
            ) {
                MonarchButton("Manage", size: .sm) {}
            }
        },
        Fixture("as the header of a List section") {
            List {
                Section {
                    Text("Ada Lovelace")
                    Text("Grace Hopper")
                } header: {
                    MonarchSectionHeader("Members", meta: "2 people")
                }
            }
            .frame(height: MonarchTokens.Space.space24 * 3)
        },
    ]
}

@Suite("MonarchSectionHeader")
struct MonarchSectionHeaderTests {
    @Test("The title is 14 points when small and 16 when medium, always semibold")
    func titleStyle() {
        #expect(MonarchSectionHeaderTokens.titleStyle(.sm).spec.size == 14)
        #expect(MonarchSectionHeaderTokens.titleStyle(.md).spec.size == 16)
        #expect(MonarchSectionHeaderTokens.titleWeight == .semibold)
    }

    @Test("Supporting text is smaller or lighter than the title and muted")
    func supportingText() {
        #expect(MonarchSectionHeaderTokens.descriptionStyle.spec.size == 14)
        #expect(MonarchSectionHeaderTokens.descriptionWeight == .regular)
        #expect(MonarchSectionHeaderTokens.metaStyle.spec.size < MonarchSectionHeaderTokens.descriptionStyle.spec.size)
        #expect(MonarchSectionHeaderTokens.titleColor == \MonarchColorTokens.textDefault)
        #expect(MonarchSectionHeaderTokens.supportingColor == \MonarchColorTokens.textMuted)
    }

    @Test("Actions sit beside the text at standard sizes and below it at accessibility sizes")
    func stacking() {
        #expect(!MonarchSectionHeaderTokens.stacksActions(at: .large))
        #expect(!MonarchSectionHeaderTokens.stacksActions(at: .xxxLarge))
        #expect(MonarchSectionHeaderTokens.stacksActions(at: .accessibility1))
        #expect(MonarchSectionHeaderTokens.stacksActions(at: .accessibility5))
    }

    @Test("The title has a spoken value only while busy")
    func accessibilityValue() {
        #expect(MonarchSectionHeaderTokens.accessibilityValue(isBusy: false, busyLabel: nil).isEmpty)
        #expect(MonarchSectionHeaderTokens.accessibilityValue(isBusy: false, busyLabel: "Refreshing").isEmpty)
        #expect(MonarchSectionHeaderTokens.accessibilityValue(isBusy: true, busyLabel: nil) == "Loading")
        #expect(MonarchSectionHeaderTokens.accessibilityValue(isBusy: true, busyLabel: "Refreshing") == "Refreshing")
    }

    @Test("The busy label comes from the component's strings table")
    func strings() {
        #expect(MonarchSectionHeaderStrings.busy == "Loading")
    }

    @Test("A header with a description is taller than the title alone")
    @MainActor
    func descriptionAddsHeight() {
        let plain = Host.layout(MonarchSectionHeader("Invoices"))
        let described = Host.layout(MonarchSectionHeader("Invoices", description: "Sent in the last 30 days"))
        #expect(described.height > plain.height)
    }
}

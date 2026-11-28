import SwiftUI
import Testing

@testable import MonarchUI

/// An accordion as the inventory recipe builds it: standard disclosure
/// groups with the Monarch style, a divider between them, and one binding
/// that keeps at most one open.
private struct AccordionFixture: View {
    let sections: [(id: String, title: String, body: String)]
    @State var expanded: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(sections, id: \.id) { section in
                DisclosureGroup(
                    section.title,
                    isExpanded: Binding(
                        get: { expanded == section.id },
                        set: { expanded = $0 ? section.id : nil }
                    )
                ) {
                    Text(section.body).monarchTextStyle(.body)
                }
                .accessibilityAddTraits(.isHeader)
                MonarchDivider()
            }
        }
        .disclosureGroupStyle(.monarch)
    }
}

private let faq: [(id: String, title: String, body: String)] = [
    ("billing", "How does billing work?", "You are billed at the start of each period for the seats you have."),
    ("cancel", "Can I cancel at any time?", "Yes. The plan stays active until the end of the period you paid for."),
    (
        "export",
        "Can I export my data before I leave, including attachments and the full history of every record?",
        "Export from Settings, Data. The archive includes attachments."
    ),
]

enum AccordionFixtures: ComponentFixtures {
    static let id = "accordion"
    static let fixtures = [
        Fixture("all collapsed") { AccordionFixture(sections: faq, expanded: nil) },
        Fixture("one expanded, long title") { AccordionFixture(sections: faq, expanded: "export") },
        Fixture("several expanded") {
            VStack(alignment: .leading, spacing: 0) {
                DisclosureGroup("Shipping", isExpanded: .constant(true)) { Text("Two to four days.") }
                MonarchDivider()
                DisclosureGroup("Returns", isExpanded: .constant(true)) { Text("Within 30 days.") }
                MonarchDivider()
                DisclosureGroup("Warranty") { Text("One year.") }.disabled(true)
            }
            .disclosureGroupStyle(.monarch)
        },
    ]
}

@Suite("Accordion")
struct AccordionTests {
    @Test("An expanded section adds its content to the height")
    @MainActor
    func expandedIsTaller() {
        let collapsed = Host.layout(AccordionFixture(sections: faq, expanded: nil))
        let expanded = Host.layout(AccordionFixture(sections: faq, expanded: "billing"))
        #expect(expanded.height > collapsed.height)
    }

    @Test("Each header keeps a 44-point touch target")
    @MainActor
    func headerHeight() {
        let one = Host.layout(AccordionFixture(sections: Array(faq.prefix(1)), expanded: nil))
        #expect(one.height >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("The header speaks Expanded or Collapsed from the disclosure style's strings")
    func spokenState() {
        let open = MonarchDisclosureGroupTokens.accessibilityValue(
            isExpanded: true,
            expandedLabel: nil,
            collapsedLabel: nil
        )
        let closed = MonarchDisclosureGroupTokens.accessibilityValue(
            isExpanded: false,
            expandedLabel: nil,
            collapsedLabel: nil
        )
        #expect(open == "Expanded")
        #expect(closed == "Collapsed")
    }
}

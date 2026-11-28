import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let tocItems = [
    MonarchTableOfContentsItem("Overview", id: "overview"),
    MonarchTableOfContentsItem("Billing", id: "billing"),
    MonarchTableOfContentsItem("Invoices", id: "invoices", level: 3),
    MonarchTableOfContentsItem("Payment methods", id: "methods", level: 3),
    MonarchTableOfContentsItem("Members", id: "members"),
]

enum MonarchTableOfContentsFixtures: ComponentFixtures {
    static let id = "table-of-contents"
    static let fixtures = [
        Fixture("nested, a subsection current") {
            MonarchTableOfContents(tocItems, selection: .constant("invoices")) { _ in }
        },
        Fixture("no current section, title hidden") {
            MonarchTableOfContents(tocItems, selection: .constant(nil), showsTitle: false) { _ in }
        },
        Fixture("custom title and current label") {
            MonarchTableOfContents(
                tocItems,
                selection: .constant("overview"),
                title: "Contents",
                currentLabel: "Reading"
            ) { _ in }
        },
        Fixture("disabled") {
            MonarchTableOfContents(tocItems, selection: .constant("billing")) { _ in }.disabled(true)
        },
        Fixture("long headings and deep nesting") {
            MonarchTableOfContents(
                [
                    MonarchTableOfContentsItem("How the organisation stores invoices after it is closed", id: 1),
                    MonarchTableOfContentsItem("Retention periods in each country we operate in", id: 2, level: 3),
                    MonarchTableOfContentsItem("Exceptions agreed with the tax office", id: 3, level: 4),
                ],
                selection: .constant(3)
            ) { _ in }
        },
        Fixture("empty, title only") {
            MonarchTableOfContents([MonarchTableOfContentsItem<Int>](), selection: .constant(nil)) { _ in }
        },
        Fixture("with the spy in a scroll view") {
            ScrollView {
                VStack(alignment: .leading) {
                    ForEach(tocItems) { item in
                        Text(item.title).monarchTextStyle(.heading).monarchTableOfContentsTarget(item.id)
                    }
                }
            }
            .monarchTableOfContentsSpy(selection: .constant(String?.none))
            .frame(height: MonarchTokens.Space.space24)
        },
    ]
}

@Suite("MonarchTableOfContents")
struct MonarchTableOfContentsTests {
    @Test("Deeper levels nest under the entry before them, as on the web")
    func depths() {
        #expect(MonarchTableOfContentsTokens.depths(levels: [2, 2, 3, 3, 2]) == [0, 0, 1, 1, 0])
        #expect(MonarchTableOfContentsTokens.depths(levels: [2, 3, 4, 3, 2]) == [0, 1, 2, 1, 0])
        #expect(MonarchTableOfContentsTokens.depths(levels: [3, 2, 3]) == [0, 0, 1])
        #expect(MonarchTableOfContentsTokens.depths(levels: [2, 4]) == [0, 1])
        #expect(MonarchTableOfContentsTokens.depths(levels: []).isEmpty)
    }

    @Test("The first heading in the top 40% is current")
    func inZone() {
        let positions: [String: CGFloat] = ["a": -300, "b": 120, "c": 300, "d": 900]
        #expect(MonarchTableOfContentsTokens.current(positions: positions, viewportHeight: 1000, offset: 0) == "b")
    }

    @Test("With no heading in the zone, the last one scrolled past is current")
    func above() {
        let positions: [String: CGFloat] = ["a": -900, "b": -200, "c": 700]
        #expect(MonarchTableOfContentsTokens.current(positions: positions, viewportHeight: 1000, offset: 0) == "b")
    }

    @Test("The offset moves the line a heading must pass")
    func offset() {
        let positions: [String: CGFloat] = ["a": 40, "b": 200]
        #expect(MonarchTableOfContentsTokens.current(positions: positions, viewportHeight: 1000, offset: 0) == "a")
        // Under a 60-point header, "a" is covered, so the first heading in the zone is "b".
        #expect(MonarchTableOfContentsTokens.current(positions: positions, viewportHeight: 1000, offset: 60) == "b")
        #expect(
            MonarchTableOfContentsTokens.current(positions: ["a": 70, "b": 200], viewportHeight: 1000, offset: 60)
                == "a"
        )
        #expect(
            MonarchTableOfContentsTokens.current(positions: ["a": 50, "b": 600], viewportHeight: 1000, offset: 60)
                == "a"
        )
    }

    @Test("Nothing is current while every heading is below the zone, so the previous one stays")
    func belowZone() {
        let positions: [String: CGFloat] = ["a": 600, "b": 800]
        #expect(MonarchTableOfContentsTokens.current(positions: positions, viewportHeight: 1000, offset: 0) == nil)
        #expect(
            MonarchTableOfContentsTokens.current(positions: [String: CGFloat](), viewportHeight: 1000, offset: 0) == nil
        )
    }

    @Test("The current entry has the accent text colour and the accent marker")
    func colors() {
        let current = MonarchTableOfContentsTokens.colors(isCurrent: true, isEnabled: true)
        #expect(current.text == \MonarchColorTokens.accentStrong)
        #expect(current.marker == \MonarchColorTokens.accentStrong)
        let other = MonarchTableOfContentsTokens.colors(isCurrent: false, isEnabled: true)
        #expect(other.text == \MonarchColorTokens.textMuted)
        #expect(other.marker == \MonarchColorTokens.borderSubtle)
        for isCurrent in [true, false] {
            #expect(
                MonarchTableOfContentsTokens.colors(isCurrent: isCurrent, isEnabled: false).text
                    == \MonarchColorTokens.textDisabled
            )
        }
    }

    @Test("Spoken strings come from the table and can be replaced")
    func strings() {
        #expect(MonarchTableOfContentsTokens.title(nil) == "On this page")
        #expect(MonarchTableOfContentsTokens.title("Contents") == "Contents")
        #expect(MonarchTableOfContentsTokens.value(isCurrent: true, currentLabel: nil) == "Current section")
        #expect(MonarchTableOfContentsTokens.value(isCurrent: true, currentLabel: "Reading") == "Reading")
        #expect(MonarchTableOfContentsTokens.value(isCurrent: false, currentLabel: nil).isEmpty)
    }

    @Test("An item keeps what it is given and defaults to level 2")
    func item() {
        let item = MonarchTableOfContentsItem("Billing", id: 4)
        #expect(item.id == 4)
        #expect(item.title == "Billing")
        #expect(item.level == 2)
        #expect(MonarchTableOfContentsItem("Invoices", id: 5, level: 3).level == 3)
    }

    #if os(iOS)
        @Test("An entry keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let list = MonarchTableOfContents(
                [MonarchTableOfContentsItem("A", id: 1)],
                selection: .constant(nil),
                showsTitle: false
            ) { _ in }
            #expect(Host.layout(list).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

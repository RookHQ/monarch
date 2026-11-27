import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchMegaMenuFixtures: ComponentFixtures {
    static let id = "mega-menu"
    static let items = [
        MonarchMegaMenuItem(
            "product",
            label: "Product",
            groups: [
                MonarchMegaMenuGroup(
                    "Plan",
                    links: [
                        MonarchMegaMenuLink(
                            "Projects",
                            description: "Track work from idea to launch",
                            systemImage: "square.grid.2x2"
                        ),
                        MonarchMegaMenuLink(
                            "Calendar",
                            description: "Deadlines and milestones in one view",
                            systemImage: "calendar",
                            badge: "New"
                        ),
                    ]
                )
            ],
            featured: MonarchMegaMenuLink(
                "What changed this month",
                description: "A short tour of the newest features and fixes.",
                systemImage: "square.grid.2x2",
                badge: "Updated"
            )
        ),
        MonarchMegaMenuItem(
            "company",
            label: "Company",
            groups: [
                MonarchMegaMenuGroup(
                    "About",
                    links: [
                        MonarchMegaMenuLink(
                            "Status",
                            destination: URL(string: "https://status.example.com"),
                            description: "Service health",
                            isExternal: true
                        )
                    ]
                )
            ]
        ),
        MonarchMegaMenuItem("support", label: "Support", destination: URL(string: "https://example.com/support")),
    ]
    static let fixtures = [
        Fixture("entries, closed") { MonarchMegaMenu(items: items) },
        Fixture("an entry open") { MonarchMegaMenu(items: items, openItem: .constant("product")) },
        Fixture("links only") { MonarchMegaMenu(items: Array(items.suffix(1))) },
    ]
}

@Suite("MonarchMegaMenu")
struct MonarchMegaMenuTests {
    @Test("An entry with groups or a featured card opens a panel; a link does not")
    func isMenu() {
        #expect(MonarchMegaMenuFixtures.items[0].isMenu)
        #expect(MonarchMegaMenuFixtures.items[1].isMenu)
        #expect(!MonarchMegaMenuFixtures.items[2].isMenu)
        #expect(MonarchMegaMenuItem("x", label: "X", groups: [], featured: MonarchMegaMenuLink("Tour")).isMenu)
    }

    @Test("A link is read as its title, badge and description")
    func accessibilityLabel() {
        let link = MonarchMegaMenuFixtures.items[0].groups[0].links[1]
        #expect(MonarchMegaMenuTokens.accessibilityLabel(link) == "Calendar, New, Deadlines and milestones in one view")
        #expect(MonarchMegaMenuTokens.accessibilityLabel(MonarchMegaMenuLink("Teams")) == "Teams")
    }

    @Test("An open entry uses the default text colour")
    func triggerColour() {
        #expect(MonarchMegaMenuTokens.triggerColor(isOpen: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchMegaMenuTokens.triggerColor(isOpen: false) == \MonarchColorTokens.textMuted)
    }

    @Test("The built-in string resolves")
    func strings() {
        #expect(MonarchMegaMenuStrings.explore == "Explore")
    }
}

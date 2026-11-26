import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchSiteShellFixtures: ComponentFixtures {
    static let id = "marketing-site-shell"
    @MainActor static var footer: MonarchSiteFooter {
        MonarchSiteFooter(
            columns: [
                MonarchSiteFooterColumn(
                    "Product",
                    links: [MonarchSiteLink("Editor", destination: URL(string: "https://example.com"))]
                ),
                MonarchSiteFooterColumn(
                    "Company",
                    links: [MonarchSiteLink("About Rook", destination: URL(string: "https://example.com"))]
                ),
            ],
            legal: "© 2026 Rook",
            legalLinks: [MonarchSiteLink("Privacy", destination: URL(string: "https://example.com/privacy"))]
        )
    }

    static let fixtures = [
        Fixture("footer") { footer },
        Fixture("footer without a legal line") {
            MonarchSiteFooter(columns: [
                MonarchSiteFooterColumn("Resources", links: [MonarchSiteLink("Guides"), MonarchSiteLink("Changelog")])
            ])
        },
        Fixture("shell with a menu, actions and the footer") {
            MonarchSiteShell(
                "Rook Example",
                navigation: [
                    MonarchSiteNavItem(
                        "Product",
                        links: [
                            MonarchSiteLink("Editor", description: "Pages and blocks"),
                            MonarchSiteLink("Search", description: "Find any page"),
                        ]
                    ),
                    MonarchSiteNavItem("Pricing", destination: URL(string: "https://example.com/pricing")),
                ],
                footer: footer
            ) {
                MonarchButton("Get started", variant: .primary, size: .sm) {}
            } content: {
                Text("Content")
            }
            .frame(height: MonarchTokens.Space.space24 * 4)
        },
    ]
}

@Suite("MonarchSiteShell")
struct MonarchSiteShellTests {
    @Test("A nav item is a link or a submenu")
    func navItems() {
        let link = MonarchSiteNavItem("Pricing", destination: URL(string: "https://example.com/pricing"))
        #expect(link.links.isEmpty)
        #expect(link.destination != nil)
        let menu = MonarchSiteNavItem("Product", links: [MonarchSiteLink("Editor")])
        #expect(menu.destination == nil)
        #expect(menu.links.map(\.label) == ["Editor"])
    }

    @Test("The menu button uses the standard menu symbol and the built-in name")
    func menu() {
        #expect(MonarchSiteShellTokens.menuSymbol == "line.3.horizontal")
        #expect(MonarchSiteShellStrings.menu == "Menu")
    }

    @Test("Footer links are muted and titles default")
    func colours() {
        #expect(MonarchSiteShellTokens.mutedColor == \MonarchColorTokens.textMuted)
        #expect(MonarchSiteShellTokens.titleColor == \MonarchColorTokens.textDefault)
    }
}

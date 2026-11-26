import MonarchUI
import SwiftUI

struct MonarchSiteShellSection: CataloguePage {
    static let title = "Site shell"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Header and footer")
        MonarchSiteShell(
            "Rook Example",
            navigation: [
                MonarchSiteNavItem(
                    "Product",
                    links: [
                        MonarchSiteLink(
                            "Editor",
                            destination: URL(string: "https://example.com"),
                            description: "Pages and blocks"
                        ),
                        MonarchSiteLink(
                            "Search",
                            destination: URL(string: "https://example.com"),
                            description: "Find any page"
                        ),
                    ]
                ),
                MonarchSiteNavItem("Pricing", destination: URL(string: "https://example.com/pricing")),
            ],
            footer: MonarchSiteFooter(
                columns: [
                    MonarchSiteFooterColumn("Product", links: [MonarchSiteLink("Editor")]),
                    MonarchSiteFooterColumn("Company", links: [MonarchSiteLink("About Rook")]),
                ],
                legal: "© 2026 Rook",
                legalLinks: [MonarchSiteLink("Privacy")]
            )
        ) {
            MonarchButton("Log in", variant: .ghost, size: .sm) {}
            MonarchButton("Get started", variant: .primary, size: .sm) {}
        } content: {
            Text("The page content.").monarchTextStyle(.body)
        }
        .frame(height: theme.spacing.space24 * 5)
    }
}

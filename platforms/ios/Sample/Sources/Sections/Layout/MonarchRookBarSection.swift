import MonarchUI
import SwiftUI

struct MonarchRookBarSection: CataloguePage {
    static let title = "Rook bar"

    private let items = [
        MonarchRookBarItem(
            "products",
            label: "Products",
            groups: [
                MonarchRookBarGroup(
                    "Sales & Support",
                    description: "Win customers, then look after them.",
                    links: [
                        MonarchRookBarLink(
                            "Pipeline",
                            destination: URL(string: "https://example.com"),
                            description: "Leads, deals and invoices."
                        ),
                        MonarchRookBarLink(
                            "Help desk",
                            destination: URL(string: "https://desk.example.com"),
                            description: "Support with a branded portal.",
                            isExternal: true
                        ),
                    ]
                )
            ]
        ),
        MonarchRookBarItem("pricing", label: "Pricing", destination: URL(string: "https://example.com/pricing")),
    ]

    var body: some View {
        CatalogueCaption("Signed out: tap the menu button")
        MonarchRookBar(
            "Northwind",
            items: items,
            account: .signedOut(
                signInURL: URL(string: "https://example.com/login"),
                signUpURL: URL(string: "https://example.com/signup")
            )
        )

        CatalogueCaption("Signed in")
        MonarchRookBar(
            "Northwind",
            items: items,
            account: .signedIn(name: "Ada Lovelace", dashboardURL: URL(string: "https://example.com"), accountURL: nil)
        ) {}
    }
}

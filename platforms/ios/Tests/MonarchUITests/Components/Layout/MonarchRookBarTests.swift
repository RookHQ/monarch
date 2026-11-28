import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchRookBarFixtures: ComponentFixtures {
    static let id = "rook-bar"
    static let items = [
        MonarchRookBarItem(
            "products",
            label: "Products",
            groups: [
                MonarchRookBarGroup(
                    "Sales & Support",
                    description: "Win customers, then look after them.",
                    links: [
                        MonarchRookBarLink("Pipeline", description: "Leads, deals and invoices."),
                        MonarchRookBarLink(
                            "Help desk",
                            description: "Support with a branded portal.",
                            isExternal: true
                        ),
                    ]
                )
            ]
        ),
        MonarchRookBarItem("pricing", label: "Pricing", destination: URL(string: "https://example.com/pricing")),
    ]
    static let fixtures = [
        Fixture("signed out") {
            MonarchRookBar(
                "Northwind",
                items: items,
                account: .signedOut(
                    signInURL: URL(string: "https://example.com/login"),
                    signUpURL: URL(string: "https://example.com/signup")
                )
            )
        },
        Fixture("signed in") {
            MonarchRookBar(
                "Northwind",
                items: items,
                account: .signedIn(
                    name: "Ada Lovelace",
                    dashboardURL: URL(string: "https://example.com"),
                    accountURL: nil
                )
            ) {}
        },
        Fixture("no account controls, a long brand") {
            MonarchRookBar("Northwind Professional Services", items: items)
        },
        Fixture("menu sheet content") {
            MonarchRookBarMenu(
                brand: "Northwind",
                items: items,
                account: .signedOut(signInURL: URL(string: "https://example.com/login"), signUpURL: nil),
                labels: MonarchRookBarLabels(),
                onSignOut: nil
            ) {}
            .frame(height: MonarchTokens.Space.space24 * 4)
        },
    ]
}

@Suite("MonarchRookBar")
struct MonarchRookBarTests {
    @Test("The letter mark is the brand's first letter")
    func initial() {
        #expect(MonarchRookBarTokens.initial("Northwind") == "N")
        #expect(MonarchRookBarTokens.initial("atlas") == "A")
        #expect(MonarchRookBarTokens.initial("").isEmpty)
    }

    @Test("A link row is read as its title, description and meta line")
    func accessibilityLabel() {
        let link = MonarchRookBarLink("Quarterly product update", meta: "Press · Feb 18, 2026")
        #expect(MonarchRookBarTokens.accessibilityLabel(link) == "Quarterly product update, Press · Feb 18, 2026")
        let app = MonarchRookBarLink("Pipeline", description: "Leads, deals and invoices.")
        #expect(MonarchRookBarTokens.accessibilityLabel(app) == "Pipeline, Leads, deals and invoices.")
    }

    @Test("Menus have groups; links have a destination")
    func items() {
        #expect(!MonarchRookBarFixtures.items[0].groups.isEmpty)
        #expect(MonarchRookBarFixtures.items[1].groups.isEmpty)
        #expect(MonarchRookBarFixtures.items[1].destination != nil)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchRookBarStrings.navigation == "Rook")
        #expect(MonarchRookBarStrings.openMenu == "Open menu")
        #expect(MonarchRookBarStrings.closeMenu == "Close menu")
        #expect(MonarchRookBarStrings.signIn == "Log in")
        #expect(MonarchRookBarStrings.signUp == "Sign up")
        #expect(MonarchRookBarStrings.dashboard == "Open dashboard")
        #expect(MonarchRookBarStrings.account == "Account settings")
        #expect(MonarchRookBarStrings.signOut == "Sign out")
        #expect(MonarchRookBarStrings.accountMenu == "Account menu")
    }
}

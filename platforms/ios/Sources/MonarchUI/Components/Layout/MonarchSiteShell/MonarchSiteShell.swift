import SwiftUI

/// The frame of a product's public or about screens in an app: a navigation bar titled with the brand, the site's
/// links in a standard menu and the account actions, then the scrolling content and a ``MonarchSiteFooter``.
///
/// ```swift
/// MonarchSiteShell(
///     "Rook Example",
///     navigation: [
///         MonarchSiteNavItem("Product", links: [MonarchSiteLink("Editor", destination: editorURL, description: "Pages and blocks")]),
///         MonarchSiteNavItem("Pricing", destination: pricingURL),
///     ],
///     footer: MonarchSiteFooter(columns: columns, legal: "© 2026 Rook")
/// ) {
///     MonarchButton("Get started", variant: .primary, size: .sm) { signUp() }
/// } content: {
///     MonarchMarketingHero("Write it once. Find it whenever.")
/// }
/// ```
///
/// The shell is a `NavigationStack`, so pushed screens get the system back button. The links are a `Menu` in the
/// bar, with a submenu per group, rather than a row of links that would not fit a phone.
///
/// Accessibility: the brand is the navigation title, read as the screen's heading. The menu button is named "Menu"; submenus and links are standard
/// menu items read with their descriptions. The actions stay separate buttons in the bar.
public struct MonarchSiteShell<Actions: View, Content: View>: View {
    private let brand: String
    private let navigation: [MonarchSiteNavItem]
    private let footer: MonarchSiteFooter?
    private let menuLabel: String?
    private let actions: Actions
    private let content: Content

    @Environment(\.monarchTheme) private var theme
    @Environment(\.openURL) private var openURL

    /// A site shell.
    ///
    /// - Parameters:
    ///   - brand: The product or company name, the navigation title.
    ///   - navigation: The site's links and submenus, in order.
    ///   - footer: The footer under the content.
    ///   - menuLabel: Replaces the built-in spoken name "Menu" of the menu button.
    ///   - actions: Buttons at the trailing edge of the bar, such as Log in and Get started.
    ///   - content: The screen's content, in a vertical scroll view.
    public init(
        _ brand: String,
        navigation: [MonarchSiteNavItem] = [],
        footer: MonarchSiteFooter? = nil,
        menuLabel: String? = nil,
        @ViewBuilder actions: () -> Actions,
        @ViewBuilder content: () -> Content
    ) {
        self.brand = brand
        self.navigation = navigation
        self.footer = footer
        self.menuLabel = menuLabel
        self.actions = actions()
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: MonarchSiteShellTokens.footerSpacing) {
                    content
                    if let footer {
                        footer
                    }
                }
                .padding(MonarchSiteShellTokens.footerPadding)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .background(theme.colors.bgCanvas)
            .navigationTitle(brand)
            .toolbar {
                ToolbarItemGroup(placement: .primaryAction) {
                    // Bar buttons keep their whole titles; the system would otherwise shorten them.
                    actions.fixedSize()
                    if !navigation.isEmpty { menu }
                }
            }
        }
    }

    private var menu: some View {
        Menu {
            ForEach(navigation) { item in
                if item.links.isEmpty {
                    Button(item.label) {
                        if let destination = item.destination { openURL(destination) }
                    }
                } else {
                    Menu {
                        ForEach(item.links) { link in
                            Button {
                                if let destination = link.destination { openURL(destination) }
                            } label: {
                                Text(link.label)
                                if let description = link.description { Text(description) }
                            }
                        }
                    } label: {
                        Label(item.label, systemImage: MonarchSiteShellTokens.submenuSymbol)
                    }
                }
            }
        } label: {
            Label(menuLabel ?? MonarchSiteShellStrings.menu, systemImage: MonarchSiteShellTokens.menuSymbol)
                .labelStyle(.iconOnly)
        }
    }
}

extension MonarchSiteShell where Actions == EmptyView {
    /// A site shell without actions in the bar.
    ///
    /// - Parameters:
    ///   - brand: The product or company name, the navigation title.
    ///   - navigation: The site's links and submenus, in order.
    ///   - footer: The footer under the content.
    ///   - menuLabel: Replaces the built-in spoken name "Menu" of the menu button.
    ///   - content: The screen's content, in a vertical scroll view.
    public init(
        _ brand: String,
        navigation: [MonarchSiteNavItem] = [],
        footer: MonarchSiteFooter? = nil,
        menuLabel: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.init(brand, navigation: navigation, footer: footer, menuLabel: menuLabel) {
            EmptyView()
        } content: {
            content()
        }
    }
}

#if DEBUG
    #Preview("MonarchSiteShell") {
        MonarchPreviewMatrix {
            MonarchSiteShell(
                "Rook Example",
                navigation: [
                    MonarchSiteNavItem("Product", links: [MonarchSiteLink("Editor", description: "Pages and blocks")]),
                    MonarchSiteNavItem("Pricing", destination: nil),
                ],
                footer: MonarchSiteFooter(
                    columns: [MonarchSiteFooterColumn("Product", links: [MonarchSiteLink("Editor")])],
                    legal: "© 2026 Rook",
                    legalLinks: [MonarchSiteLink("Privacy")]
                )
            ) {
                MonarchButton("Get started", variant: .primary, size: .sm) {}
            } content: {
                Text("Content")
            }
        }
    }
#endif

import SwiftUI

/// The Rook company bar for the public and signed-out screens of a Rook app: the brand, the account controls and
/// a menu button that opens every Rook menu (products, services, pricing, news) in a full-height sheet.
///
/// ```swift
/// MonarchRookBar(
///     "Northwind",
///     items: [
///         MonarchRookBarItem("products", label: "Products", groups: productSections),
///         MonarchRookBarItem("pricing", label: "Pricing", destination: pricingURL),
///     ],
///     account: .signedOut(signInURL: signInURL, signUpURL: signUpURL)
/// )
/// ```
///
/// Place it at the top of the screen with `.safeAreaInset(edge: .top)`. Every app, link and post is data from
/// the app; the bar never fetches. It is scoped to Rook's own apps: other apps use ``MonarchSiteShell``.
///
/// Accessibility: the bar is a container named "Rook" (or `labels.navigation`). The brand is a heading. The menu
/// button is named "Open menu"; the sheet has a "Close menu" button, a collapsible group per menu and the account
/// controls at the end. A signed-in person's avatar is a menu named "Account menu".
public struct MonarchRookBar: View {
    private let brand: String
    private let mark: Image?
    private let items: [MonarchRookBarItem]
    private let account: MonarchRookBarAccount?
    private let labels: MonarchRookBarLabels
    private let onSignOut: (@MainActor () -> Void)?

    @State private var isMenuShown = false
    @Environment(\.monarchTheme) private var theme
    @Environment(\.openURL) private var openURL
    @ScaledMetric(relativeTo: .body) private var scale: CGFloat = 1

    /// A Rook bar.
    ///
    /// - Parameters:
    ///   - brand: The app's name, shown beside its mark.
    ///   - mark: The app's mark. Without it the first letter of the brand is shown in a tile.
    ///   - items: The menus and links, in order.
    ///   - account: Who is signed in. Without it no account controls are shown.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSignOut: Signs the person out. Without it Sign out is not shown.
    public init(
        _ brand: String,
        mark: Image? = nil,
        items: [MonarchRookBarItem],
        account: MonarchRookBarAccount? = nil,
        labels: MonarchRookBarLabels = MonarchRookBarLabels(),
        onSignOut: (@MainActor () -> Void)? = nil
    ) {
        self.brand = brand
        self.mark = mark
        self.items = items
        self.account = account
        self.labels = labels
        self.onSignOut = onSignOut
    }

    /// The content of the view.
    public var body: some View {
        HStack(spacing: MonarchRookBarTokens.spacing) {
            brandView
            Spacer(minLength: 0)
            accountControls
            Button {
                isMenuShown = true
            } label: {
                Label(labels.openMenu ?? MonarchRookBarStrings.openMenu, systemImage: MonarchRookBarTokens.menuSymbol)
                    .labelStyle(.iconOnly)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                    .monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, MonarchRookBarTokens.horizontalPadding)
        .padding(.vertical, MonarchRookBarTokens.verticalPadding)
        .frame(maxWidth: .infinity)
        .background(theme.colors[role: MonarchRookBarTokens.fill])
        .overlay(alignment: .bottom) { MonarchDivider() }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.navigation ?? MonarchRookBarStrings.navigation)
        .sheet(isPresented: $isMenuShown) {
            MonarchRookBarMenu(
                brand: brand,
                items: items,
                account: account,
                labels: labels,
                onSignOut: onSignOut
            ) {
                isMenuShown = false
            }
        }
    }

    private var brandView: some View {
        HStack(spacing: MonarchRookBarTokens.brandSpacing) {
            Group {
                if let mark {
                    mark.resizable().scaledToFit()
                } else {
                    Text(MonarchRookBarTokens.initial(brand))
                        .monarchTextStyle(.label, weight: MonarchRookBarTokens.brandWeight)
                        .foregroundStyle(theme.colors.textDefault)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(theme.colors[role: MonarchRookBarTokens.markFill], in: MonarchShapes.control)
                }
            }
            .frame(width: MonarchRookBarTokens.markSize * scale, height: MonarchRookBarTokens.markSize * scale)
            .accessibilityHidden(true)
            Text(brand)
                .monarchTextStyle(.body, weight: MonarchRookBarTokens.brandWeight)
                .foregroundStyle(theme.colors.textDefault)
                .lineLimit(1)
                .accessibilityAddTraits(.isHeader)
        }
    }

    @ViewBuilder private var accountControls: some View {
        switch account {
        case .signedOut(let signInURL, let signUpURL):
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchRookBarTokens.spacing) {
                    if let signInURL {
                        MonarchButton(labels.signIn ?? MonarchRookBarStrings.signIn, variant: .ghost, size: .sm) {
                            openURL(signInURL)
                        }
                    }
                    signUpButton(signUpURL)
                }
                signUpButton(signUpURL)
            }
        case .signedIn(let name, let dashboardURL, let accountURL):
            Menu {
                Text(name)
                if let dashboardURL {
                    Button(labels.dashboard ?? MonarchRookBarStrings.dashboard) { openURL(dashboardURL) }
                }
                if let accountURL {
                    Button(labels.account ?? MonarchRookBarStrings.account) { openURL(accountURL) }
                }
                if let onSignOut {
                    Button(labels.signOut ?? MonarchRookBarStrings.signOut, role: .destructive) { onSignOut() }
                }
            } label: {
                MonarchAvatar(image: nil, name: name, size: .sm, isDecorative: true)
                    .monarchMinimumHitTarget()
            }
            .accessibilityLabel(labels.accountMenu ?? MonarchRookBarStrings.accountMenu)
            .accessibilityValue(name)
        case nil:
            EmptyView()
        }
    }

    @ViewBuilder private func signUpButton(_ url: URL?) -> some View {
        if let url {
            MonarchButton(labels.signUp ?? MonarchRookBarStrings.signUp, variant: .primary, size: .sm) {
                openURL(url)
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchRookBar") {
        MonarchPreviewMatrix {
            VStack {
                MonarchRookBar(
                    "Northwind",
                    items: [MonarchRookBarItem("pricing", label: "Pricing", destination: nil)],
                    account: .signedOut(
                        signInURL: URL(string: "https://example.com/login"),
                        signUpURL: URL(string: "https://example.com/signup")
                    )
                )
                MonarchRookBar(
                    "Northwind",
                    items: [],
                    account: .signedIn(name: "Ada Lovelace", dashboardURL: nil, accountURL: nil)
                ) {}
            }
        }
    }
#endif

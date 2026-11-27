import SwiftUI

/// Site-style navigation for a product's public or help screens: a row of entries, where an entry with groups
/// opens a panel of grouped links with symbols, descriptions and badges, and an optional featured card.
///
/// ```swift
/// MonarchMegaMenu(items: [
///     MonarchMegaMenuItem("product", label: "Product", groups: [
///         MonarchMegaMenuGroup("Plan", links: [
///             MonarchMegaMenuLink("Projects", description: "Track work from idea to launch", systemImage: "square.grid.2x2"),
///         ]),
///     ]),
///     MonarchMegaMenuItem("support", label: "Support", destination: URL(string: "https://example.com/support")),
/// ]) { link in
///     navigate(to: link)
/// }
/// ```
///
/// On a phone the panel is a popover under its entry, as on iPad; it closes when a link is chosen or with a tap
/// outside. The top-level navigation of an app itself is a `TabView`; use this for a band of destinations that
/// needs descriptions, such as a help centre or a product tour.
///
/// Accessibility: each entry is a button; an entry with a panel has the hint "Explore" and the value of its state
/// through the popover. Section headings have the header trait. Each link is one button read as its title, its
/// badge and its description; an external link is a link and shows an outward arrow.
public struct MonarchMegaMenu: View {
    private let items: [MonarchMegaMenuItem]
    private let openItem: Binding<String?>?
    private let exploreLabel: String?
    private let onNavigate: (@MainActor (MonarchMegaMenuLink) -> Void)?

    @State private var storedOpenItem: String?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.openURL) private var openURL

    /// A mega menu.
    ///
    /// - Parameters:
    ///   - items: The top-level entries, in order.
    ///   - openItem: The id of the entry whose panel is open. Without it the menu keeps its own.
    ///   - exploreLabel: Replaces the built-in "Explore" action of the featured card.
    ///   - onNavigate: Called with the chosen link. Without it the link's destination is opened with `openURL`.
    public init(
        items: [MonarchMegaMenuItem],
        openItem: Binding<String?>? = nil,
        exploreLabel: String? = nil,
        onNavigate: (@MainActor (MonarchMegaMenuLink) -> Void)? = nil
    ) {
        self.items = items
        self.openItem = openItem
        self.exploreLabel = exploreLabel
        self.onNavigate = onNavigate
    }

    /// The content of the view.
    public var body: some View {
        MonarchFlowLayout(spacing: MonarchMegaMenuTokens.itemSpacing) {
            ForEach(items) { item in
                if item.isMenu {
                    trigger(item)
                } else {
                    Button {
                        if let destination = item.destination { openURL(destination) }
                    } label: {
                        Text(item.label)
                            .monarchTextStyle(.label)
                            .foregroundStyle(theme.colors[role: MonarchMegaMenuTokens.triggerColor(isOpen: false)])
                            .padding(.horizontal, MonarchMegaMenuTokens.itemPadding)
                            .monarchMinimumHitTarget()
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(.isLink)
                }
            }
        }
    }

    private var current: String? { openItem?.wrappedValue ?? storedOpenItem }

    private func setOpen(_ id: String?) {
        if let openItem {
            openItem.wrappedValue = id
        } else {
            storedOpenItem = id
        }
    }

    private func trigger(_ item: MonarchMegaMenuItem) -> some View {
        let isOpen = current == item.id
        return Button {
            setOpen(isOpen ? nil : item.id)
        } label: {
            HStack(spacing: MonarchMegaMenuTokens.triggerSpacing) {
                Text(item.label).monarchTextStyle(.label)
                MonarchIcon(systemName: MonarchMegaMenuTokens.chevronSymbol, size: .sm)
                    .rotationEffect(isOpen ? MonarchMegaMenuTokens.openRotation : .zero)
            }
            .foregroundStyle(theme.colors[role: MonarchMegaMenuTokens.triggerColor(isOpen: isOpen)])
            .padding(.horizontal, MonarchMegaMenuTokens.itemPadding)
            .monarchMinimumHitTarget()
            .background(isOpen ? theme.colors[role: MonarchMegaMenuTokens.openFill] : .clear, in: MonarchShapes.control)
        }
        .buttonStyle(.plain)
        .accessibilityHint(exploreLabel ?? MonarchMegaMenuStrings.explore)
        .popover(
            isPresented: Binding(
                get: { current == item.id },
                set: { isShown in if !isShown, current == item.id { setOpen(nil) } }
            )
        ) {
            panel(item)
                .presentationCompactAdaptation(.popover)
                .presentationBackground(theme.colors[role: MonarchMegaMenuTokens.panelFill])
        }
    }

    private func panel(_ item: MonarchMegaMenuItem) -> some View {
        VStack(alignment: .leading, spacing: MonarchMegaMenuTokens.sectionSpacing) {
            ForEach(item.groups) { group in
                VStack(alignment: .leading, spacing: MonarchMegaMenuTokens.linkSpacing) {
                    Text(group.heading)
                        .textCase(.uppercase)
                        .monarchTextStyle(.caption, weight: MonarchMegaMenuTokens.headingWeight)
                        .foregroundStyle(theme.colors[role: MonarchMegaMenuTokens.supportingColor])
                        .accessibilityAddTraits(.isHeader)
                    ForEach(group.links) { link in row(link) }
                }
            }
            if let featured = item.featured { featuredCard(featured) }
        }
        .padding(MonarchMegaMenuTokens.panelPadding)
        .frame(
            idealWidth: MonarchMegaMenuTokens.panelWidth,
            maxWidth: MonarchMegaMenuTokens.panelWidth,
            alignment: .leading
        )
        .fixedSize(horizontal: false, vertical: true)
    }

    private func choose(_ link: MonarchMegaMenuLink) {
        setOpen(nil)
        if let onNavigate {
            onNavigate(link)
        } else if let destination = link.destination {
            openURL(destination)
        }
    }

    private func row(_ link: MonarchMegaMenuLink) -> some View {
        Button {
            choose(link)
        } label: {
            HStack(alignment: .top, spacing: MonarchMegaMenuTokens.linkIconSpacing) {
                if let systemImage = link.systemImage { tile(systemImage) }
                VStack(alignment: .leading, spacing: MonarchMegaMenuTokens.linkSpacing) {
                    HStack(spacing: MonarchMegaMenuTokens.badgeSpacing) {
                        Text(link.label)
                            .monarchTextStyle(.bodyStrong)
                            .foregroundStyle(theme.colors.textDefault)
                        if let badge = link.badge { MonarchBadge(badge, tone: .accent, size: .sm) }
                        if link.isExternal {
                            MonarchIcon(systemName: MonarchMegaMenuTokens.externalSymbol, size: .sm)
                                .foregroundStyle(theme.colors[role: MonarchMegaMenuTokens.supportingColor])
                        }
                    }
                    if let description = link.description {
                        Text(description)
                            .monarchTextStyle(.bodySmall)
                            .foregroundStyle(theme.colors[role: MonarchMegaMenuTokens.supportingColor])
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                Spacer(minLength: 0)
            }
            .padding(.vertical, MonarchMegaMenuTokens.linkPadding)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchMegaMenuTokens.accessibilityLabel(link))
        .accessibilityAddTraits(link.isExternal ? [.isButton, .isLink] : .isButton)
    }

    private func tile(_ systemImage: String) -> some View {
        MonarchIcon(systemName: systemImage, size: .sm)
            .foregroundStyle(theme.colors[role: MonarchMegaMenuTokens.tileForeground])
            .frame(width: MonarchMegaMenuTokens.tileSize, height: MonarchMegaMenuTokens.tileSize)
            .background(theme.colors[role: MonarchMegaMenuTokens.tileFill], in: MonarchShapes.control)
    }

    private func featuredCard(_ featured: MonarchMegaMenuLink) -> some View {
        Button {
            choose(featured)
        } label: {
            VStack(alignment: .leading, spacing: MonarchMegaMenuTokens.featuredSpacing) {
                HStack(alignment: .top) {
                    if let systemImage = featured.systemImage {
                        tile(systemImage)
                            .background(theme.colors.bgSurface, in: MonarchShapes.control)
                    }
                    Spacer(minLength: 0)
                    if let badge = featured.badge { MonarchBadge(badge, tone: .accent, size: .sm) }
                }
                Text(featured.label)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                if let description = featured.description {
                    Text(description)
                        .monarchTextStyle(.bodySmall)
                        .foregroundStyle(theme.colors[role: MonarchMegaMenuTokens.supportingColor])
                        .fixedSize(horizontal: false, vertical: true)
                }
                HStack(spacing: MonarchMegaMenuTokens.triggerSpacing) {
                    Text(exploreLabel ?? MonarchMegaMenuStrings.explore)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors.textDefault)
                    MonarchIcon(systemName: MonarchMegaMenuTokens.exploreSymbol, size: .sm)
                        .flipsForRightToLeftLayoutDirection(true)
                        .foregroundStyle(theme.colors[role: MonarchMegaMenuTokens.tileForeground])
                }
            }
            .padding(MonarchMegaMenuTokens.featuredPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.colors[role: MonarchMegaMenuTokens.featuredFill], in: MonarchShapes.container)
            .contentShape(MonarchShapes.container)
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchMegaMenuTokens.accessibilityLabel(featured))
        .accessibilityHint(exploreLabel ?? MonarchMegaMenuStrings.explore)
        .accessibilityAddTraits(.isButton)
    }
}

#if DEBUG
    #Preview("MonarchMegaMenu") {
        MonarchPreviewMatrix {
            MonarchMegaMenu(items: [
                MonarchMegaMenuItem(
                    "product",
                    label: "Product",
                    groups: [
                        MonarchMegaMenuGroup(
                            "Plan",
                            links: [MonarchMegaMenuLink("Projects", description: "Track work from idea to launch")]
                        )
                    ]
                ),
                MonarchMegaMenuItem("support", label: "Support", destination: URL(string: "https://example.com")),
            ])
        }
    }
#endif

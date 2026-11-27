import SwiftUI

/// The frame every dashboard widget sits in: a title with an optional
/// period, header actions, an overflow menu and a View all button, and its
/// own loading, empty and error states, so one failing widget never blocks
/// the rest of the screen.
///
/// ```swift
/// MonarchWidget(
///     "Team load",
///     period: "Last 30 days",
///     state: model.state,
///     updatedAt: model.refreshedAt,
///     onRetry: { model.reload() },
///     onViewAll: { path.append(Route.team) },
///     menu: {
///         Button("Refresh", systemImage: "arrow.clockwise") { model.reload() }
///         Button("Remove from dashboard", systemImage: "trash", role: .destructive) { remove() }
///     },
///     content: { TeamLoadChart(model.rows) }
/// )
/// ```
///
/// The app fetches the data; the widget shows where it stands through
/// `state`.
///
/// Accessibility: the title is a heading, so VoiceOver's Headings rotor
/// moves between widgets. The loading placeholder is one element named
/// "Loading" and the title. The menu button is named "More actions for" and
/// the title, and View all "View all:" and the title. A failure is announced
/// when it appears.
public struct MonarchWidget<Content: View, Actions: View, MenuItems: View>: View {
    private let title: String
    private let description: String?
    private let period: String?
    private let systemImage: String?
    private let variant: MonarchWidgetVariant
    private let state: MonarchWidgetState
    private let isRetrying: Bool
    private let updatedAt: Date?
    private let footer: String?
    private let labels: MonarchWidgetLabels
    private let onRetry: (@MainActor () -> Void)?
    private let onViewAll: (@MainActor () -> Void)?
    private let hasActions: Bool
    private let hasMenu: Bool
    private let actions: Actions
    private let menuItems: MenuItems
    private let content: Content

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var skeletonLineHeight = MonarchWidgetTokens.skeletonLineHeight

    /// A widget with header actions and an overflow menu.
    ///
    /// - Parameters:
    ///   - title: The widget's title, such as "Team load". It is a heading.
    ///   - description: A line under the title.
    ///   - period: The period the figures cover, beside the title, such as "Last 30 days".
    ///   - systemImage: A decorative SF Symbol before the title.
    ///   - variant: A card, or plain inside another surface.
    ///   - state: Ready, loading, empty or failed.
    ///   - isRetrying: Whether a retry after a failure is running.
    ///   - updatedAt: When the data was last refreshed, shown in the footer
    ///     as "Updated 5 minutes ago" and kept current.
    ///   - footer: A note under the body, such as what the figures include.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Adds Try again to a failure.
    ///   - onViewAll: Adds a View all button to the header, which opens the full list or report.
    ///   - actions: Controls in the header before the menu, such as a small segmented Picker.
    ///   - menu: The items of the overflow menu: `Button`, `Toggle`, `Divider`.
    ///     Give a destructive item `role: .destructive` and confirm it.
    ///   - content: The body.
    public init(
        _ title: String,
        description: String? = nil,
        period: String? = nil,
        systemImage: String? = nil,
        variant: MonarchWidgetVariant = .card,
        state: MonarchWidgetState = .ready,
        isRetrying: Bool = false,
        updatedAt: Date? = nil,
        footer: String? = nil,
        labels: MonarchWidgetLabels = MonarchWidgetLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onViewAll: (@MainActor () -> Void)? = nil,
        @ViewBuilder actions: () -> Actions,
        @ViewBuilder menu: () -> MenuItems,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.description = description
        self.period = period
        self.systemImage = systemImage
        self.variant = variant
        self.state = state
        self.isRetrying = isRetrying
        self.updatedAt = updatedAt
        self.footer = footer
        self.labels = labels
        self.onRetry = onRetry
        self.onViewAll = onViewAll
        self.hasActions = Actions.self != EmptyView.self
        self.hasMenu = MenuItems.self != EmptyView.self
        self.actions = actions()
        self.menuItems = menu()
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        Group {
            switch variant {
            case .card:
                if showsFooter {
                    MonarchCard(elevation: .none) {
                        stack
                    } footer: {
                        footerView
                    }
                } else {
                    MonarchCard(elevation: .none) { stack }
                }
            case .plain:
                VStack(alignment: .leading, spacing: MonarchWidgetTokens.sectionSpacing) {
                    stack
                    if showsFooter {
                        MonarchDivider()
                        footerView
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .accessibilityElement(children: .contain)
        .onChange(of: state) { old, new in
            if let message = MonarchWidgetTokens.announcement(from: old, to: new, labels: labels) {
                AccessibilityNotification.Announcement(message).post()
            }
        }
    }

    private var stack: some View {
        VStack(alignment: .leading, spacing: MonarchWidgetTokens.sectionSpacing) {
            header
            bodyContent
        }
    }

    private var showsFooter: Bool {
        MonarchWidgetTokens.showsFooter(state: state, footer: footer, updatedAt: updatedAt)
    }

    // MARK: Header

    private var hasHeaderControls: Bool { hasActions || hasMenu || onViewAll != nil }

    private var header: some View {
        VStack(alignment: .leading, spacing: MonarchWidgetTokens.headerSpacing) {
            if !hasHeaderControls {
                heading
            } else if MonarchWidgetTokens.stacksActions(at: dynamicTypeSize) {
                VStack(alignment: .leading, spacing: MonarchWidgetTokens.headerSpacing) {
                    heading
                    controls
                }
            } else {
                HStack(alignment: .firstTextBaseline, spacing: MonarchWidgetTokens.titleSpacing) {
                    heading.frame(maxWidth: .infinity, alignment: .leading)
                    controls
                }
            }
            if let description {
                mutedText(description, style: .caption)
            }
        }
    }

    private var heading: some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchWidgetTokens.titleSpacing) {
            if let systemImage {
                MonarchIcon(systemName: systemImage, size: .sm)
                    .foregroundStyle(theme.colors[role: MonarchWidgetTokens.muted])
            }
            Text(title)
                .monarchTextStyle(.label, weight: MonarchWidgetTokens.titleWeight)
                .foregroundStyle(theme.colors[role: MonarchWidgetTokens.titleColor])
                .accessibilityAddTraits(.isHeader)
            if let period {
                mutedText(period, style: .caption)
            }
        }
    }

    private var controls: some View {
        HStack(spacing: MonarchWidgetTokens.actionSpacing) {
            actions
            if let onViewAll {
                Button(action: onViewAll) {
                    Label {
                        Text(MonarchWidgetTokens.viewAllTitle(labels: labels))
                    } icon: {
                        MonarchIcon(systemName: MonarchWidgetTokens.viewAllSymbol, size: .sm)
                    }
                    .labelStyle(MonarchWidgetTrailingIconLabelStyle())
                    // The link colour, as the web's View all is.
                    .foregroundStyle(theme.colors.textLink)
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
                .accessibilityLabel(MonarchWidgetTokens.viewAllLabel(title: title, labels: labels))
            }
            if hasMenu {
                Menu {
                    menuItems
                } label: {
                    Label(
                        MonarchWidgetTokens.menuLabel(title: title, labels: labels),
                        systemImage: MonarchWidgetTokens.menuSymbol
                    )
                    .labelStyle(.iconOnly)
                }
                .menuStyle(.button)
                .buttonStyle(.monarch(.ghost, size: .sm))
            }
        }
    }

    // MARK: Body

    @ViewBuilder private var bodyContent: some View {
        switch state {
        case .ready:
            content
        case .loading:
            skeleton
        case .empty:
            MonarchEmptyState(MonarchWidgetTokens.emptyTitle(state, labels: labels) ?? "", size: .sm)
        case .failed:
            let message = MonarchWidgetTokens.errorMessage(state, labels: labels) ?? ""
            if let onRetry {
                MonarchInlineErrorRetry(message, isRetrying: isRetrying, onRetry: onRetry)
            } else {
                MonarchInlineNotice(message, tone: .danger, variant: .plain)
            }
        }
    }

    private var skeleton: some View {
        VStack(alignment: .leading, spacing: MonarchWidgetTokens.skeletonSpacing) {
            ForEach(MonarchWidgetTokens.skeletonWidths.indices, id: \.self) { index in
                GeometryReader { proxy in
                    MonarchShapes.mark
                        .fill(theme.colors[role: MonarchWidgetTokens.skeletonFill])
                        .frame(width: proxy.size.width * MonarchWidgetTokens.skeletonWidths[index])
                }
                .frame(height: skeletonLineHeight)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchWidgetTokens.loadingLabel(title: title, labels: labels))
    }

    // MARK: Footer

    private var footerView: some View {
        VStack(alignment: .trailing, spacing: MonarchWidgetTokens.headerSpacing) {
            if let footer {
                mutedText(footer, style: .caption)
            }
            if let updatedAt {
                MonarchRelativeTimeClock(dates: [updatedAt]) { current in
                    mutedText(
                        MonarchWidgetTokens.updatedText(updatedAt, now: current, locale: locale, labels: labels),
                        style: .caption
                    )
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
    }

    private func mutedText(_ text: String, style: MonarchTextStyle) -> some View {
        Text(text)
            .monarchTextStyle(style)
            .foregroundStyle(theme.colors[role: MonarchWidgetTokens.muted])
    }
}

/// The title, then the icon: "View all ›".
private struct MonarchWidgetTrailingIconLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: MonarchWidgetTokens.actionSpacing) {
            configuration.title
            configuration.icon
        }
    }
}

extension MonarchWidget where Actions == EmptyView {
    /// A widget with an overflow menu and no other header actions.
    ///
    /// - Parameters:
    ///   - title: The widget's title. It is a heading.
    ///   - description: A line under the title.
    ///   - period: The period the figures cover, beside the title.
    ///   - systemImage: A decorative SF Symbol before the title.
    ///   - variant: A card, or plain inside another surface.
    ///   - state: Ready, loading, empty or failed.
    ///   - isRetrying: Whether a retry after a failure is running.
    ///   - updatedAt: When the data was last refreshed.
    ///   - footer: A note under the body.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Adds Try again to a failure.
    ///   - onViewAll: Adds a View all button to the header.
    ///   - menu: The items of the overflow menu.
    ///   - content: The body.
    public init(
        _ title: String,
        description: String? = nil,
        period: String? = nil,
        systemImage: String? = nil,
        variant: MonarchWidgetVariant = .card,
        state: MonarchWidgetState = .ready,
        isRetrying: Bool = false,
        updatedAt: Date? = nil,
        footer: String? = nil,
        labels: MonarchWidgetLabels = MonarchWidgetLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onViewAll: (@MainActor () -> Void)? = nil,
        @ViewBuilder menu: () -> MenuItems,
        @ViewBuilder content: () -> Content
    ) {
        self.init(
            title,
            description: description,
            period: period,
            systemImage: systemImage,
            variant: variant,
            state: state,
            isRetrying: isRetrying,
            updatedAt: updatedAt,
            footer: footer,
            labels: labels,
            onRetry: onRetry,
            onViewAll: onViewAll,
            actions: { EmptyView() },
            menu: menu,
            content: content
        )
    }
}

extension MonarchWidget where Actions == EmptyView, MenuItems == EmptyView {
    /// A widget without header actions or a menu.
    ///
    /// - Parameters:
    ///   - title: The widget's title. It is a heading.
    ///   - description: A line under the title.
    ///   - period: The period the figures cover, beside the title.
    ///   - systemImage: A decorative SF Symbol before the title.
    ///   - variant: A card, or plain inside another surface.
    ///   - state: Ready, loading, empty or failed.
    ///   - isRetrying: Whether a retry after a failure is running.
    ///   - updatedAt: When the data was last refreshed.
    ///   - footer: A note under the body.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Adds Try again to a failure.
    ///   - onViewAll: Adds a View all button to the header.
    ///   - content: The body.
    public init(
        _ title: String,
        description: String? = nil,
        period: String? = nil,
        systemImage: String? = nil,
        variant: MonarchWidgetVariant = .card,
        state: MonarchWidgetState = .ready,
        isRetrying: Bool = false,
        updatedAt: Date? = nil,
        footer: String? = nil,
        labels: MonarchWidgetLabels = MonarchWidgetLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        onViewAll: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.init(
            title,
            description: description,
            period: period,
            systemImage: systemImage,
            variant: variant,
            state: state,
            isRetrying: isRetrying,
            updatedAt: updatedAt,
            footer: footer,
            labels: labels,
            onRetry: onRetry,
            onViewAll: onViewAll,
            actions: { EmptyView() },
            menu: { EmptyView() },
            content: content
        )
    }
}

#if DEBUG
    #Preview("MonarchWidget") {
        MonarchPreviewMatrix {
            VStack {
                MonarchWidget(
                    "Team load",
                    period: "Last 30 days",
                    updatedAt: .now.addingTimeInterval(-300),
                    onViewAll: {},
                    menu: { Button("Refresh", systemImage: "arrow.clockwise") {} },
                    content: { MonarchMetric("Open tickets", value: 128) }
                )
                MonarchWidget("Approvals", state: .loading) { EmptyView() }
                MonarchWidget("Revenue", state: .failed(), onRetry: {}) { EmptyView() }
                MonarchWidget("Mentions", variant: .plain, state: .empty()) { EmptyView() }
            }
        }
    }
#endif

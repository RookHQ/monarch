import MonarchTokens
import SwiftUI

/// A titled card for one section of a screen or dashboard: a header with
/// actions, the content, an optional footer bar, and built-in loading, empty
/// and error states. Optionally collapsible.
///
/// ```swift
/// MonarchSectionCard("Invoices", meta: "12 this month", state: invoices == nil ? .loading : .ready) {
///     ForEach(invoices ?? []) { InvoiceRow($0) }
/// } actions: {
///     MonarchButton("New", size: .sm) { create() }
/// } footer: {
///     Button("View all") { showAll() }.buttonStyle(.monarchLink(.standalone))
/// }
/// ```
///
/// It has the surface, border, radius and padding of ``MonarchCard``. The
/// header uses the type of ``MonarchSectionHeader``; the states are
/// ``MonarchLoadingState``, ``MonarchEmptyState`` and ``MonarchErrorState``.
/// For an empty state with its own actions, keep `state` at `.ready` and put
/// a ``MonarchEmptyState`` in the content.
///
/// Accessibility: the title has the header trait. A collapsible title is a
/// button whose value is "Expanded" or "Collapsed"; while busy the value
/// adds "Loading" and the progress indicator is hidden. A tone other than
/// neutral adds the tone's icon, read as the tone's name. Actions stay
/// separate elements and move under the title at the accessibility text sizes.
public struct MonarchSectionCard<Content: View, Actions: View, Footer: View>: View {
    private let title: String
    private let description: String?
    private let meta: String?
    private let systemImage: String?
    private let tone: MonarchTone
    private let state: MonarchSectionCardState
    private let emptyMessage: String?
    private let errorMessage: String?
    private let isBusy: Bool
    private let isRetrying: Bool
    private let isFlush: Bool
    private let isCollapsible: Bool
    private let labels: MonarchSectionCardLabels
    private let onRetry: (@MainActor () -> Void)?
    private let content: Content
    private let actions: Actions
    private let footer: Footer
    /// A count or short text in a pill after the title, set by components inside the module.
    fileprivate var titleBadge: String?
    /// A count or short text in a pill under the description, as the web shows a badge as the meta.
    fileprivate var metaBadge: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var isExpanded: Bool

    /// A section card with actions and a footer.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - description: A sentence under the title that explains the section.
    ///   - meta: A short secondary line: a count, a date range, "Updated 5 min ago".
    ///   - systemImage: An SF Symbol before the title. Decorative.
    ///   - tone: `danger` for a destructive zone: the border and title take the tone, with its icon.
    ///   - state: What the body shows: the content, skeleton rows, an empty state or an error state.
    ///   - emptyMessage: The message of the empty state.
    ///   - errorMessage: The message of the error state.
    ///   - isBusy: Whether the content is refreshing in place: a progress indicator after the title.
    ///   - isRetrying: Whether the error state's retry is running.
    ///   - isFlush: Whether the body has no padding, for a list or table that runs to the edges.
    ///   - isCollapsible: Whether the title shows and hides the body.
    ///   - isInitiallyExpanded: Whether a collapsible card starts open.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Shows a retry button in the error state.
    ///   - content: The body, laid out in a leading-aligned vertical stack.
    ///   - actions: Buttons or a menu at the trailing edge of the header.
    ///   - footer: The content of the footer bar: secondary actions, a summary, a pager.
    public init(
        _ title: String,
        description: String? = nil,
        meta: String? = nil,
        systemImage: String? = nil,
        tone: MonarchTone = .neutral,
        state: MonarchSectionCardState = .ready,
        emptyMessage: String? = nil,
        errorMessage: String? = nil,
        isBusy: Bool = false,
        isRetrying: Bool = false,
        isFlush: Bool = false,
        isCollapsible: Bool = false,
        isInitiallyExpanded: Bool = true,
        labels: MonarchSectionCardLabels = MonarchSectionCardLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> Content,
        @ViewBuilder actions: () -> Actions,
        @ViewBuilder footer: () -> Footer
    ) {
        self.title = title
        self.description = description
        self.meta = meta
        self.systemImage = systemImage
        self.tone = tone
        self.state = state
        self.emptyMessage = emptyMessage
        self.errorMessage = errorMessage
        self.isBusy = isBusy
        self.isRetrying = isRetrying
        self.isFlush = isFlush
        self.isCollapsible = isCollapsible
        self.labels = labels
        self.onRetry = onRetry
        self.content = content()
        self.actions = actions()
        self.footer = footer()
        _isExpanded = State(initialValue: isInitiallyExpanded)
    }

    /// The card with a short text, such as a count, in a pill after the title.
    func monarchTitleBadge(_ text: String?) -> Self {
        var copy = self
        copy.titleBadge = text
        return copy
    }

    /// The card with a short text, such as a count, in a pill where the meta line sits.
    public func monarchMetaBadge(_ text: String?) -> Self {
        var copy = self
        copy.metaBadge = text
        return copy
    }

    private var hasActions: Bool { Actions.self != EmptyView.self }
    private var hasFooter: Bool { Footer.self != EmptyView.self }

    /// The content of the view.
    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchSectionCardTokens.cornerRadius, style: .continuous)
        let showsBody = MonarchSectionCardTokens.showsBody(isCollapsible: isCollapsible, isExpanded: isExpanded)
        VStack(alignment: .leading, spacing: 0) {
            header
                .padding(MonarchSectionCardTokens.padding)
            if showsBody {
                stateBody
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, MonarchSectionCardTokens.bodyPadding(isFlush: isFlush))
                    .padding(.bottom, MonarchSectionCardTokens.bodyPadding(isFlush: isFlush))
                if hasFooter {
                    Rectangle()
                        .fill(theme.colors[role: MonarchCardTokens.border])
                        .frame(height: MonarchSectionCardTokens.borderWidth)
                        .accessibilityHidden(true)
                    HStack(spacing: MonarchSectionCardTokens.contentSpacing) { footer }
                        .frame(maxWidth: .infinity, alignment: .trailing)
                        .padding(MonarchSectionCardTokens.padding)
                        .background(theme.colors.bgSubtle)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: MonarchCardTokens.background]))
        .clipShape(shape)
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchSectionCardTokens.border(tone)],
                lineWidth: MonarchSectionCardTokens.borderWidth
            )
        )
        .monarchAnimation(.fast, value: isExpanded)
        .accessibilityElement(children: .contain)
    }

    @ViewBuilder private var header: some View {
        if !hasActions {
            titleArea
        } else if MonarchSectionHeaderTokens.stacksActions(at: dynamicTypeSize) {
            VStack(alignment: .leading, spacing: MonarchSectionHeaderTokens.stackedSpacing) {
                titleArea
                actionRow
            }
        } else {
            // The actions sit at the end of the title row, as on the web; the title and its description wrap
            // beside them, and at the larger text sizes the actions go under (`stacksActions`).
            // When the title would be squeezed into a narrow column the actions go under it instead.
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .top, spacing: MonarchSectionCardTokens.actionSpacing) {
                    titleArea.frame(
                        minWidth: MonarchSectionHeaderTokens.leastTitleWidth,
                        idealWidth: MonarchSectionHeaderTokens.leastTitleWidth,
                        maxWidth: .infinity,
                        alignment: .leading
                    )
                    actionRow.fixedSize(horizontal: true, vertical: false)
                }
                VStack(alignment: .leading, spacing: MonarchSectionHeaderTokens.stackedSpacing) {
                    titleArea
                    actionRow
                }
            }
        }
    }

    private var actionRow: some View {
        HStack(spacing: MonarchSectionHeaderTokens.actionSpacing) { actions }
    }

    @ViewBuilder private var titleArea: some View {
        if isCollapsible {
            Button {
                isExpanded.toggle()
            } label: {
                titleText(showsChevron: true)
                    .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .combine)
            .accessibilityValue(titleValue)
            .accessibilityAddTraits(.isHeader)
        } else {
            titleText(showsChevron: false)
                .accessibilityElement(children: .combine)
                .accessibilityValue(titleValue)
                .accessibilityAddTraits(.isHeader)
        }
    }

    private var titleValue: String {
        MonarchSectionCardTokens.titleValue(
            isCollapsible: isCollapsible,
            isExpanded: isExpanded,
            isBusy: MonarchSectionCardTokens.isBusy(isBusy, state: state),
            expanded: labels.expanded ?? MonarchSectionCardStrings.expanded,
            collapsed: labels.collapsed ?? MonarchSectionCardStrings.collapsed,
            loading: labels.loading ?? MonarchSectionCardStrings.loading
        )
    }

    private func titleText(showsChevron: Bool) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchSectionCardTokens.iconSpacing) {
            icon
            VStack(alignment: .leading, spacing: MonarchSectionCardTokens.textSpacing) {
                HStack(spacing: MonarchSectionCardTokens.iconSpacing) {
                    Text(title)
                        .monarchTextStyle(
                            MonarchSectionHeaderTokens.titleStyle(.md),
                            weight: MonarchSectionHeaderTokens.titleWeight
                        )
                        .foregroundStyle(theme.colors[role: MonarchSectionCardTokens.titleColor(tone)])
                    if let titleBadge, !titleBadge.isEmpty {
                        MonarchBadge(titleBadge)
                    }
                    if isBusy {
                        ProgressView()
                            .controlSize(.small)
                            .accessibilityHidden(true)
                    }
                }
                if let description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(
                            MonarchSectionHeaderTokens.descriptionStyle,
                            weight: MonarchSectionHeaderTokens.descriptionWeight
                        )
                        .foregroundStyle(theme.colors[role: MonarchSectionHeaderTokens.supportingColor])
                }
                if let metaBadge, !metaBadge.isEmpty {
                    MonarchBadge(metaBadge)
                } else if let meta, !meta.isEmpty {
                    Text(meta)
                        .monarchTextStyle(MonarchSectionHeaderTokens.metaStyle)
                        .foregroundStyle(theme.colors[role: MonarchSectionHeaderTokens.supportingColor])
                }
            }
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            if showsChevron {
                MonarchIcon(systemName: "chevron.down", size: .sm)
                    .rotationEffect(isExpanded ? .degrees(180) : .zero)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
    }

    @ViewBuilder private var icon: some View {
        if let symbol = MonarchSectionCardTokens.symbol(tone, override: systemImage) {
            // The assistant glyph is drawn in the AI colour, as the web draws the Bot in an AI section.
            let color =
                symbol == MonarchAiAgentAvatarTokens.botSymbol && tone == .neutral
                ? theme.colors.aiStrong : theme.colors[role: MonarchSectionCardTokens.titleColor(tone)]
            if tone != .neutral, let spoken = labels.tone ?? MonarchToneStrings.name(tone) {
                MonarchIcon(systemName: symbol, label: spoken).foregroundStyle(color).iconTile(theme)
            } else {
                MonarchIcon(systemName: symbol).foregroundStyle(
                    tone == .neutral && symbol != MonarchAiAgentAvatarTokens.botSymbol ? theme.colors.textMuted : color
                ).iconTile(theme)
            }
        }
    }

    @ViewBuilder private var stateBody: some View {
        switch state {
        case .ready:
            VStack(alignment: .leading, spacing: MonarchSectionCardTokens.contentSpacing) { content }
        case .loading:
            MonarchLoadingState(
                labels.loading ?? MonarchSectionCardStrings.loading,
                variant: .rows,
                rows: MonarchSectionCardTokens.loadingRows
            )
            .padding(.horizontal, isFlush ? MonarchSectionCardTokens.padding : 0)
        case .empty:
            MonarchEmptyState(
                labels.emptyTitle ?? MonarchSectionCardStrings.emptyTitle,
                message: emptyMessage,
                size: .sm
            )
        case .error:
            MonarchErrorState(
                labels.errorTitle ?? MonarchSectionCardStrings.errorTitle,
                message: errorMessage,
                size: .sm,
                isRetrying: isRetrying,
                onRetry: onRetry
            )
        }
    }
}

extension View {
    /// The web's icon tile: a subtle square behind the section icon.
    fileprivate func iconTile(_ theme: MonarchTheme) -> some View {
        frame(width: 28, height: 28)
            .background(
                RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
                    .fill(theme.colors.bgSubtle)
            )
    }
}

extension MonarchSectionCard where Footer == EmptyView {
    /// A section card with actions and no footer.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - description: A sentence under the title that explains the section.
    ///   - meta: A short secondary line: a count, a date range, "Updated 5 min ago".
    ///   - systemImage: An SF Symbol before the title. Decorative.
    ///   - tone: `danger` for a destructive zone: the border and title take the tone, with its icon.
    ///   - state: What the body shows: the content, skeleton rows, an empty state or an error state.
    ///   - emptyMessage: The message of the empty state.
    ///   - errorMessage: The message of the error state.
    ///   - isBusy: Whether the content is refreshing in place: a progress indicator after the title.
    ///   - isRetrying: Whether the error state's retry is running.
    ///   - isFlush: Whether the body has no padding, for a list or table that runs to the edges.
    ///   - isCollapsible: Whether the title shows and hides the body.
    ///   - isInitiallyExpanded: Whether a collapsible card starts open.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Shows a retry button in the error state.
    ///   - content: The body, laid out in a leading-aligned vertical stack.
    ///   - actions: Buttons or a menu at the trailing edge of the header.
    public init(
        _ title: String,
        description: String? = nil,
        meta: String? = nil,
        systemImage: String? = nil,
        tone: MonarchTone = .neutral,
        state: MonarchSectionCardState = .ready,
        emptyMessage: String? = nil,
        errorMessage: String? = nil,
        isBusy: Bool = false,
        isRetrying: Bool = false,
        isFlush: Bool = false,
        isCollapsible: Bool = false,
        isInitiallyExpanded: Bool = true,
        labels: MonarchSectionCardLabels = MonarchSectionCardLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> Content,
        @ViewBuilder actions: () -> Actions
    ) {
        self.init(
            title,
            description: description,
            meta: meta,
            systemImage: systemImage,
            tone: tone,
            state: state,
            emptyMessage: emptyMessage,
            errorMessage: errorMessage,
            isBusy: isBusy,
            isRetrying: isRetrying,
            isFlush: isFlush,
            isCollapsible: isCollapsible,
            isInitiallyExpanded: isInitiallyExpanded,
            labels: labels,
            onRetry: onRetry,
            content: content,
            actions: actions,
            footer: { EmptyView() }
        )
    }
}

extension MonarchSectionCard where Actions == EmptyView {
    /// A section card with content and a footer bar, and no header actions.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - description: A sentence under the title that explains the section.
    ///   - meta: A short secondary line: a count, a date range, "Updated 5 min ago".
    ///   - systemImage: An SF Symbol before the title. Decorative.
    ///   - tone: `danger` for a destructive zone: the border and title take the tone, with its icon.
    ///   - state: What the body shows: the content, skeleton rows, an empty state or an error state.
    ///   - emptyMessage: The message of the empty state.
    ///   - errorMessage: The message of the error state.
    ///   - isBusy: Whether the content is refreshing in place: a progress indicator after the title.
    ///   - isRetrying: Whether the error state's retry is running.
    ///   - isFlush: Whether the body has no padding, for a list or table that runs to the edges.
    ///   - isCollapsible: Whether the title shows and hides the body.
    ///   - isInitiallyExpanded: Whether a collapsible card starts open.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Shows a retry button in the error state.
    ///   - content: The body, laid out in a leading-aligned vertical stack.
    ///   - footer: The content of the footer bar: secondary actions, a summary, a pager.
    public init(
        _ title: String,
        description: String? = nil,
        meta: String? = nil,
        systemImage: String? = nil,
        tone: MonarchTone = .neutral,
        state: MonarchSectionCardState = .ready,
        emptyMessage: String? = nil,
        errorMessage: String? = nil,
        isBusy: Bool = false,
        isRetrying: Bool = false,
        isFlush: Bool = false,
        isCollapsible: Bool = false,
        isInitiallyExpanded: Bool = true,
        labels: MonarchSectionCardLabels = MonarchSectionCardLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> Content,
        @ViewBuilder footer: () -> Footer
    ) {
        self.init(
            title,
            description: description,
            meta: meta,
            systemImage: systemImage,
            tone: tone,
            state: state,
            emptyMessage: emptyMessage,
            errorMessage: errorMessage,
            isBusy: isBusy,
            isRetrying: isRetrying,
            isFlush: isFlush,
            isCollapsible: isCollapsible,
            isInitiallyExpanded: isInitiallyExpanded,
            labels: labels,
            onRetry: onRetry,
            content: content,
            actions: { EmptyView() },
            footer: footer
        )
    }
}

extension MonarchSectionCard where Actions == EmptyView, Footer == EmptyView {
    /// A section card with a title and content only.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - description: A sentence under the title that explains the section.
    ///   - meta: A short secondary line: a count, a date range, "Updated 5 min ago".
    ///   - systemImage: An SF Symbol before the title. Decorative.
    ///   - tone: `danger` for a destructive zone: the border and title take the tone, with its icon.
    ///   - state: What the body shows: the content, skeleton rows, an empty state or an error state.
    ///   - emptyMessage: The message of the empty state.
    ///   - errorMessage: The message of the error state.
    ///   - isBusy: Whether the content is refreshing in place: a progress indicator after the title.
    ///   - isRetrying: Whether the error state's retry is running.
    ///   - isFlush: Whether the body has no padding, for a list or table that runs to the edges.
    ///   - isCollapsible: Whether the title shows and hides the body.
    ///   - isInitiallyExpanded: Whether a collapsible card starts open.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Shows a retry button in the error state.
    ///   - content: The body, laid out in a leading-aligned vertical stack.
    public init(
        _ title: String,
        description: String? = nil,
        meta: String? = nil,
        systemImage: String? = nil,
        tone: MonarchTone = .neutral,
        state: MonarchSectionCardState = .ready,
        emptyMessage: String? = nil,
        errorMessage: String? = nil,
        isBusy: Bool = false,
        isRetrying: Bool = false,
        isFlush: Bool = false,
        isCollapsible: Bool = false,
        isInitiallyExpanded: Bool = true,
        labels: MonarchSectionCardLabels = MonarchSectionCardLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.init(
            title,
            description: description,
            meta: meta,
            systemImage: systemImage,
            tone: tone,
            state: state,
            emptyMessage: emptyMessage,
            errorMessage: errorMessage,
            isBusy: isBusy,
            isRetrying: isRetrying,
            isFlush: isFlush,
            isCollapsible: isCollapsible,
            isInitiallyExpanded: isInitiallyExpanded,
            labels: labels,
            onRetry: onRetry,
            content: content,
            actions: { EmptyView() },
            footer: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchSectionCard") {
        MonarchPreviewMatrix {
            VStack {
                MonarchSectionCard("Invoices", meta: "12 this month") {
                    Text("Invoice 1042").monarchTextStyle(.body)
                } actions: {
                    MonarchButton("New", size: .sm) {}
                } footer: {
                    Text("Updated 5 min ago").monarchTextStyle(.caption)
                }
                MonarchSectionCard("Team", state: .loading) { EmptyView() }
                MonarchSectionCard("Danger zone", tone: .danger, isCollapsible: true) {
                    MonarchButton("Delete workspace", variant: .danger) {}
                }
            }
        }
    }
#endif

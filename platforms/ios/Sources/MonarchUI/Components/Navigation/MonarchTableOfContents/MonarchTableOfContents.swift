import SwiftUI

/// In-page navigation: a list of the page's headings that marks the section
/// being read and scrolls to a section when it is chosen.
///
/// ```swift
/// ScrollViewReader { proxy in
///     ScrollView {
///         ForEach(sections) { section in
///             Text(section.title)
///                 .monarchTableOfContentsTarget(section.id)
///             // section content
///         }
///     }
///     .monarchTableOfContentsSpy(selection: $current)
///     .sheet(isPresented: $showsContents) {
///         MonarchTableOfContents(entries, selection: $current) { id in
///             showsContents = false
///             proxy.scrollTo(id, anchor: .top)
///         }
///     }
/// }
/// ```
///
/// The list is a plain stack, so put it in a sheet or a sidebar. Mark each
/// heading with `monarchTableOfContentsTarget(_:)`, which also gives it the id
/// for `ScrollViewProxy.scrollTo`, and add `monarchTableOfContentsSpy` to the
/// scroll view to keep `selection` on the section being read.
///
/// Accessibility: the list is a container named by its title, which has the
/// header trait. Each entry is a button; the current one has the value
/// "Current section" and is also marked by a line and a heavier weight, not by
/// colour alone. Entries wrap at large text sizes and have a touch target of
/// at least 44 points.
public struct MonarchTableOfContents<ID: Hashable>: View {
    private let items: [MonarchTableOfContentsItem<ID>]
    @Binding private var selection: ID?
    private let title: String?
    private let showsTitle: Bool
    private let currentLabel: String?
    private let action: @MainActor (ID) -> Void

    @Environment(\.monarchTheme) private var theme

    /// A table of contents.
    ///
    /// - Parameters:
    ///   - items: The entries, in page order.
    ///   - selection: The section being read. Choosing an entry sets it.
    ///   - title: Replaces the built-in title "On this page".
    ///   - showsTitle: Whether the title is shown. It still names the list when hidden.
    ///   - currentLabel: Replaces the built-in "Current section", read as the value of the current entry.
    ///   - action: Scrolls to the chosen section, for example with `ScrollViewProxy.scrollTo`.
    public init(
        _ items: [MonarchTableOfContentsItem<ID>],
        selection: Binding<ID?>,
        title: String? = nil,
        showsTitle: Bool = true,
        currentLabel: String? = nil,
        action: @escaping @MainActor (ID) -> Void
    ) {
        self.items = items
        _selection = selection
        self.title = title
        self.showsTitle = showsTitle
        self.currentLabel = currentLabel
        self.action = action
    }

    /// The content of the view.
    public var body: some View {
        let resolvedTitle = MonarchTableOfContentsTokens.title(title)
        let depths = MonarchTableOfContentsTokens.depths(levels: items.map(\.level))
        VStack(alignment: .leading, spacing: MonarchTableOfContentsTokens.titleSpacing) {
            if showsTitle {
                Text(resolvedTitle)
                    .monarchTextStyle(MonarchTableOfContentsTokens.titleStyle)
                    .textCase(.uppercase)
                    .fontWeight(MonarchTableOfContentsTokens.emphasisWeight)
                    .foregroundStyle(theme.colors[role: MonarchTableOfContentsTokens.titleColor])
                    .accessibilityAddTraits(.isHeader)
            }
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    let isCurrent = item.id == selection
                    Button {
                        selection = item.id
                        action(item.id)
                    } label: {
                        MonarchTableOfContentsEntry(title: item.title, isCurrent: isCurrent, depth: depths[index])
                    }
                    .buttonStyle(.plain)
                    .accessibilityValue(
                        MonarchTableOfContentsTokens.value(isCurrent: isCurrent, currentLabel: currentLabel)
                    )
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(resolvedTitle)
    }
}

/// One entry. A separate view, so it reads whether it is enabled.
private struct MonarchTableOfContentsEntry: View {
    let title: String
    let isCurrent: Bool
    let depth: Int

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let colors = MonarchTableOfContentsTokens.colors(isCurrent: isCurrent, isEnabled: isEnabled)
        HStack(spacing: MonarchTableOfContentsTokens.markerSpacing) {
            Rectangle()
                .fill(theme.colors[role: colors.marker])
                .frame(width: MonarchTableOfContentsTokens.markerWidth)
            Text(title)
                .monarchTextStyle(MonarchTableOfContentsTokens.textStyle)
                .fontWeight(isCurrent ? MonarchTableOfContentsTokens.emphasisWeight : nil)
                .foregroundStyle(theme.colors[role: colors.text])
                .multilineTextAlignment(.leading)
                .padding(.leading, CGFloat(depth) * MonarchTableOfContentsTokens.indent)
                .padding(.vertical, MonarchTableOfContentsTokens.verticalPadding)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .fixedSize(horizontal: false, vertical: true)
        .monarchMinimumHitTarget()
        .contentShape(Rectangle())
    }
}

#if DEBUG
    #Preview("MonarchTableOfContents") {
        MonarchPreviewMatrix {
            MonarchTableOfContents(
                [
                    MonarchTableOfContentsItem("Overview", id: 1),
                    MonarchTableOfContentsItem("Billing", id: 2),
                    MonarchTableOfContentsItem("Invoices", id: 3, level: 3),
                    MonarchTableOfContentsItem("Payment methods", id: 4, level: 3),
                    MonarchTableOfContentsItem("Members", id: 5),
                ],
                selection: .constant(3)
            ) { _ in }
        }
    }
#endif

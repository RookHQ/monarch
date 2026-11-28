import SwiftUI

/// A row of a list: a leading icon, a title with supporting text, and a
/// trailing view or a chevron, with a hairline divider under it.
///
/// Use it as the content of a `Button`, a `NavigationLink`, or a row of a
/// `List` (add `monarchListRowStyle()` there, so the list draws no separator
/// or fill of its own):
///
/// ```swift
/// List {
///     ForEach(members) { member in
///         MonarchListRow(member.name, subtitle: member.role, systemImage: "person")
///             .monarchListRowStyle()
///     }
/// }
/// .listStyle(.plain)
/// .monarchCanvas()
/// ```
///
/// Accessibility: the title, the supporting text and the trailing text are
/// read as one element. The icon and chevron are decorative. The row is at
/// least 44 points high and grows with Dynamic Type.
public struct MonarchListRow<Trailing: View>: View {
    private let title: String
    private let subtitle: String?
    private let systemImage: String?
    private let showsChevron: Bool
    private let showsDivider: Bool
    private let trailing: Trailing

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .body) private var leadingWidth = MonarchListRowTokens.leadingWidth

    /// A list row with a trailing view.
    ///
    /// - Parameters:
    ///   - title: The title, already localised.
    ///   - subtitle: Supporting text under the title.
    ///   - systemImage: An SF Symbol before the text.
    ///   - showsChevron: Adds a chevron at the end, for a row that opens something.
    ///   - showsDivider: Draws the hairline under the row.
    ///   - trailing: A view at the end, such as a badge or a value.
    public init(
        _ title: String,
        subtitle: String? = nil,
        systemImage: String? = nil,
        showsChevron: Bool = false,
        showsDivider: Bool = true,
        @ViewBuilder trailing: () -> Trailing
    ) {
        self.title = title
        self.subtitle = subtitle
        self.systemImage = systemImage
        self.showsChevron = showsChevron
        self.showsDivider = showsDivider
        self.trailing = trailing()
    }

    /// The content of the view.
    public var body: some View {
        HStack(spacing: MonarchListRowTokens.spacing) {
            if let systemImage {
                MonarchIcon(systemName: systemImage, size: .md)
                    .foregroundStyle(theme.colors[role: MonarchListRowTokens.iconColor(isEnabled: isEnabled)])
                    .frame(width: leadingWidth)
            }
            VStack(alignment: .leading, spacing: MonarchListRowTokens.textSpacing) {
                Text(title)
                    .monarchTextStyle(.body, weight: .medium)
                    .foregroundStyle(theme.colors[role: MonarchListRowTokens.titleColor(isEnabled: isEnabled)])
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .monarchTextStyle(.label, weight: .regular)
                        .foregroundStyle(theme.colors[role: MonarchListRowTokens.supportingColor(isEnabled: isEnabled)])
                }
                // At the accessibility text sizes the trailing view goes under the text, so it does not
                // squeeze the title into a word-by-word column.
                if dynamicTypeSize.isAccessibilitySize { trailing }
            }
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            if !dynamicTypeSize.isAccessibilitySize { trailing }
            if showsChevron {
                MonarchIcon(systemName: MonarchListRowTokens.chevronSymbol, size: .sm)
                    .foregroundStyle(theme.colors[role: MonarchListRowTokens.supportingColor(isEnabled: isEnabled)])
                    .flipsForRightToLeftLayoutDirection(true)
            }
        }
        .padding(.horizontal, MonarchListRowTokens.horizontalPadding)
        .padding(.vertical, MonarchListRowTokens.verticalPadding)
        .frame(maxWidth: .infinity, minHeight: MonarchListRowTokens.minHeight, alignment: .leading)
        .contentShape(Rectangle())
        .overlay(alignment: .bottom) {
            if showsDivider {
                Rectangle()
                    .fill(theme.colors[role: MonarchListRowTokens.dividerColor])
                    .frame(height: MonarchListRowTokens.dividerWidth)
                    .padding(
                        .leading,
                        MonarchListRowTokens.dividerLeadingInset(
                            hasIcon: systemImage != nil,
                            leadingWidth: leadingWidth
                        )
                    )
            }
        }
        .accessibilityElement(children: .combine)
    }
}

extension MonarchListRow where Trailing == EmptyView {
    /// A list row with no trailing view.
    ///
    /// - Parameters:
    ///   - title: The title, already localised.
    ///   - subtitle: Supporting text under the title.
    ///   - systemImage: An SF Symbol before the text.
    ///   - showsChevron: Adds a chevron at the end, for a row that opens something.
    ///   - showsDivider: Draws the hairline under the row.
    public init(
        _ title: String,
        subtitle: String? = nil,
        systemImage: String? = nil,
        showsChevron: Bool = false,
        showsDivider: Bool = true
    ) {
        self.init(
            title,
            subtitle: subtitle,
            systemImage: systemImage,
            showsChevron: showsChevron,
            showsDivider: showsDivider
        ) { EmptyView() }
    }
}

extension View {
    /// Makes a `List` row draw nothing of its own: no inset, no separator and
    /// no fill, so a ``MonarchListRow`` (which draws its own padding and
    /// divider) sits on the list background.
    public func monarchListRowStyle() -> some View {
        listRowInsets(EdgeInsets())
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
    }
}

#if DEBUG
    #Preview("MonarchListRow") {
        MonarchPreviewMatrix {
            VStack(spacing: 0) {
                MonarchListRow("Ana Lima", subtitle: "Owner", systemImage: "person", showsChevron: true)
                MonarchListRow("Notifications", systemImage: "bell") {
                    MonarchBadge("3", tone: .info)
                }
                MonarchListRow("Billing", subtitle: "Plan and invoices", showsDivider: false).disabled(true)
            }
        }
    }
#endif

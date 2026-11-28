import SwiftUI

/// A trail of the levels above the current screen in a deep hierarchy, such
/// as folders, with the current level last.
///
/// ```swift
/// MonarchBreadcrumb(
///     [
///         MonarchBreadcrumbItem("Drive"),
///         MonarchBreadcrumbItem("Projects"),
///         MonarchBreadcrumbItem("Monarch"),
///         MonarchBreadcrumbItem("Specs"),
///         MonarchBreadcrumbItem("Navigation"),
///     ]
/// ) { item in
///     path.removeLast(...)
/// }
/// ```
///
/// Ordinary screens use the navigation bar title and back button instead.
/// When there are more levels than `maxVisible`, the first level stays, the
/// middle ones collapse into a menu and the last ones stay. When the trail is
/// wider than the screen it scrolls sideways and starts scrolled to the
/// current level.
///
/// Accessibility: the trail is a container named "Breadcrumb". Each ancestor
/// is a link-styled button; the last level is text whose value is "Current
/// page". The separators are hidden. The collapsed-levels button is named
/// "Show 3 more levels" and its menu disables levels without a screen. Every
/// button has a touch target of at least 44 points and the text scales with
/// Dynamic Type. The separators point in the reading direction.
public struct MonarchBreadcrumb: View {
    private let items: [MonarchBreadcrumbItem]
    private let maxVisible: Int
    private let labels: MonarchBreadcrumbLabels
    private let action: @MainActor (MonarchBreadcrumbItem) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A breadcrumb trail.
    ///
    /// - Parameters:
    ///   - items: The levels, from the root to the current screen.
    ///   - maxVisible: The most levels shown before the middle ones collapse into a menu. At least 3.
    ///   - labels: Replacements for the built-in spoken strings.
    ///   - action: Goes to the level that was chosen. It is never called for the last level or
    ///     a level that is not linked.
    public init(
        _ items: [MonarchBreadcrumbItem],
        maxVisible: Int = 4,
        labels: MonarchBreadcrumbLabels = MonarchBreadcrumbLabels(),
        action: @escaping @MainActor (MonarchBreadcrumbItem) -> Void
    ) {
        self.items = items
        self.maxVisible = maxVisible
        self.labels = labels
        self.action = action
    }

    /// The content of the view.
    public var body: some View {
        ViewThatFits(in: .horizontal) {
            trail(maxVisible: maxVisible)
            // Too wide for the screen: fold more of the middle into the menu before the trail has to scroll.
            trail(maxVisible: 3)
            ScrollViewReader { proxy in
                ScrollView(.horizontal, showsIndicators: false) {
                    trail(maxVisible: 3)
                }
                .onAppear { proxy.scrollTo(items.count - 1, anchor: .trailing) }
                .onChange(of: items) { _, new in proxy.scrollTo(new.count - 1, anchor: .trailing) }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(MonarchBreadcrumbTokens.label(labels))
    }

    private func trail(maxVisible: Int) -> some View {
        let segments = MonarchBreadcrumbTokens.segments(count: items.count, maxVisible: maxVisible)
        return HStack(spacing: MonarchBreadcrumbTokens.spacing) {
            ForEach(Array(segments.enumerated()), id: \.offset) { offset, segment in
                switch segment {
                case .item(let index):
                    level(at: index).id(index)
                case .collapsed(let indices):
                    collapsed(indices)
                }
                if offset < segments.count - 1 {
                    MonarchIcon(systemName: MonarchBreadcrumbTokens.separatorSymbol, size: .sm)
                        .imageScale(.small)
                        .foregroundStyle(theme.colors[role: MonarchBreadcrumbTokens.separatorColor])
                }
            }
        }
        .monarchTextStyle(MonarchBreadcrumbTokens.textStyle)
        .lineLimit(1)
        .fixedSize()
    }

    @ViewBuilder
    private func level(at index: Int) -> some View {
        let item = items[index]
        let kind = MonarchBreadcrumbTokens.kind(index: index, count: items.count, isLinked: item.isLinked)
        switch kind {
        case .link:
            // A plain button, not the link style: the link style's 44 point
            // minimum width spaces short levels further apart than long
            // ones. The touch area grows past the text instead (rule: 44
            // points of height, the width of the text plus the outset).
            Button {
                action(item)
            } label: {
                Text(item.title)
                    .foregroundStyle(theme.colors[role: MonarchBreadcrumbTokens.textColor(kind)])
                    .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                    .contentShape(Rectangle().inset(by: -MonarchBreadcrumbTokens.hitOutset))
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(.isLink)
        case .text, .current:
            Text(item.title)
                .fontWeight(kind == .current ? MonarchBreadcrumbTokens.currentWeight : nil)
                .foregroundStyle(theme.colors[role: MonarchBreadcrumbTokens.textColor(kind)])
                .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                .accessibilityValue(MonarchBreadcrumbTokens.value(kind, labels: labels))
        }
    }

    private func collapsed(_ indices: [Int]) -> some View {
        Menu {
            ForEach(indices, id: \.self) { index in
                let item = items[index]
                Button(item.title) { action(item) }
                    .disabled(!item.isLinked)
            }
        } label: {
            MonarchIcon(
                systemName: MonarchBreadcrumbTokens.moreSymbol,
                label: MonarchBreadcrumbTokens.moreLabel(count: indices.count, labels: labels, locale: locale),
                size: .sm
            )
            .foregroundStyle(theme.colors[role: MonarchBreadcrumbTokens.textColor(.link)])
            .frame(minHeight: MonarchAccessibility.minimumHitTarget)
            .contentShape(Rectangle().inset(by: -MonarchBreadcrumbTokens.hitOutset))
        }
        .buttonStyle(.plain)
    }
}

#if DEBUG
    #Preview("MonarchBreadcrumb") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchBreadcrumb([MonarchBreadcrumbItem("Drive"), MonarchBreadcrumbItem("Projects")]) { _ in }
                MonarchBreadcrumb(
                    ["Drive", "Projects", "Monarch", "Specs", "Navigation", "Breadcrumb"].map {
                        MonarchBreadcrumbItem($0)
                    }
                ) { _ in }
                MonarchBreadcrumb(
                    [
                        MonarchBreadcrumbItem("Drive"),
                        MonarchBreadcrumbItem("Shared with me", isLinked: false),
                        MonarchBreadcrumbItem("Quarterly report"),
                    ]
                ) { _ in }
            }
        }
    }
#endif

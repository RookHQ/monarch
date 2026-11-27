import SwiftUI

/// An overlapping row of avatars with a "+N" tile for the ones not shown.
///
/// ```swift
/// MonarchAvatarGroup("Assignees", people, id: \.id, maxVisible: 3, size: .sm) { person in
///     MonarchAvatar(url: person.photoURL, name: person.name, size: .sm)
/// }
/// ```
///
/// Give the avatars in `content` the same size and shape as the group. A
/// "+1" tile would take the place of the avatar it hides, so with one avatar
/// too many all of them are shown.
///
/// Accessibility: the group is a container named by `label`, or by the
/// number of people ("4 people") when it has no label; VoiceOver reads each
/// avatar by its own name, then the tile as "2 more".
public struct MonarchAvatarGroup<Data: RandomAccessCollection, ID: Hashable, Content: View>: View {
    private let label: String?
    private let data: Data
    private let id: KeyPath<Data.Element, ID>
    private let maxVisible: Int?
    private let size: MonarchAvatarSize
    private let shape: MonarchAvatarShape
    private let overflowLabel: (@Sendable (Int) -> String)?
    private let content: (Data.Element) -> Content

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric private var side: CGFloat
    @ScaledMetric private var squareRadius: CGFloat

    /// An avatar group.
    ///
    /// - Parameters:
    ///   - label: Names the group for VoiceOver, such as "Assignees". Already localised.
    ///   - data: The people or entities, in order.
    ///   - id: The key path to a stable identity of an element.
    ///   - maxVisible: Show at most this many avatars, then a "+N" tile. All by default.
    ///   - size: The size of the avatars, for the overlap and the tile.
    ///   - shape: The shape of the avatars, for the ring and the tile.
    ///   - overflowLabel: Replaces the built-in spoken "N more" of the tile.
    ///   - content: The avatar of one element.
    public init(
        _ label: String,
        _ data: Data,
        id: KeyPath<Data.Element, ID>,
        maxVisible: Int? = nil,
        size: MonarchAvatarSize = .md,
        shape: MonarchAvatarShape = .circle,
        overflowLabel: (@Sendable (Int) -> String)? = nil,
        @ViewBuilder content: @escaping (Data.Element) -> Content
    ) {
        self.init(
            label: label,
            data,
            id: id,
            maxVisible: maxVisible,
            size: size,
            shape: shape,
            overflowLabel: overflowLabel,
            content: content
        )
    }

    /// An avatar group without a label, named by its number of people.
    ///
    /// - Parameters:
    ///   - data: The people or entities, in order.
    ///   - id: The key path to a stable identity of an element.
    ///   - maxVisible: Show at most this many avatars, then a "+N" tile. All by default.
    ///   - size: The size of the avatars, for the overlap and the tile.
    ///   - shape: The shape of the avatars, for the ring and the tile.
    ///   - overflowLabel: Replaces the built-in spoken "N more" of the tile.
    ///   - content: The avatar of one element.
    public init(
        _ data: Data,
        id: KeyPath<Data.Element, ID>,
        maxVisible: Int? = nil,
        size: MonarchAvatarSize = .md,
        shape: MonarchAvatarShape = .circle,
        overflowLabel: (@Sendable (Int) -> String)? = nil,
        @ViewBuilder content: @escaping (Data.Element) -> Content
    ) {
        self.init(
            label: nil,
            data,
            id: id,
            maxVisible: maxVisible,
            size: size,
            shape: shape,
            overflowLabel: overflowLabel,
            content: content
        )
    }

    private init(
        label: String?,
        _ data: Data,
        id: KeyPath<Data.Element, ID>,
        maxVisible: Int?,
        size: MonarchAvatarSize,
        shape: MonarchAvatarShape,
        overflowLabel: (@Sendable (Int) -> String)?,
        content: @escaping (Data.Element) -> Content
    ) {
        self.label = label
        self.data = data
        self.id = id
        self.maxVisible = maxVisible
        self.size = size
        self.shape = shape
        self.overflowLabel = overflowLabel
        self.content = content
        _side = ScaledMetric(wrappedValue: MonarchAvatarTokens.side(size), relativeTo: .body)
        _squareRadius = ScaledMetric(wrappedValue: MonarchAvatarTokens.squareRadius(size), relativeTo: .body)
    }

    /// The content of the view.
    public var body: some View {
        let visible = MonarchAvatarTokens.visibleCount(total: data.count, maxVisible: maxVisible)
        let hidden = data.count - visible
        HStack(spacing: -side * MonarchAvatarTokens.overlapRatio(size)) {
            ForEach(data.prefix(visible), id: id) { element in
                ringed(content(element))
            }
            if hidden > 0 {
                ringed(overflowTile(hidden))
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(MonarchAvatarTokens.groupLabel(label, count: data.count))
    }

    private var outline: RoundedRectangle {
        RoundedRectangle(
            cornerRadius: shape == .circle ? side : squareRadius + MonarchAvatarTokens.ringWidth,
            style: .continuous
        )
    }

    private func ringed(_ view: some View) -> some View {
        view
            .padding(MonarchAvatarTokens.ringWidth)
            .background(outline.fill(theme.colors.bgSurface))
    }

    private func overflowTile(_ hidden: Int) -> some View {
        Text(MonarchAvatarStrings.moreShort(hidden))
            .monarchTextStyle(MonarchAvatarTokens.initialsStyle(size))
            .lineLimit(1)
            .minimumScaleFactor(MonarchAvatarTokens.initialsMinimumScale)
            .foregroundStyle(theme.colors.textMuted)
            .frame(width: side, height: side)
            .background(theme.colors.bgMuted)
            .clipShape(
                RoundedRectangle(cornerRadius: shape == .circle ? side / 2 : squareRadius, style: .continuous)
            )
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(overflowLabel?(hidden) ?? MonarchAvatarStrings.more(hidden))
            .accessibilityAddTraits(.isImage)
    }
}

extension MonarchAvatarGroup where Data.Element: Identifiable, ID == Data.Element.ID {
    /// An avatar group of identifiable elements.
    ///
    /// - Parameters:
    ///   - label: Names the group for VoiceOver, such as "Assignees". Already localised.
    ///   - data: The people or entities, in order.
    ///   - maxVisible: Show at most this many avatars, then a "+N" tile. All by default.
    ///   - size: The size of the avatars, for the overlap and the tile.
    ///   - shape: The shape of the avatars, for the ring and the tile.
    ///   - overflowLabel: Replaces the built-in spoken "N more" of the tile.
    ///   - content: The avatar of one element.
    public init(
        _ label: String,
        _ data: Data,
        maxVisible: Int? = nil,
        size: MonarchAvatarSize = .md,
        shape: MonarchAvatarShape = .circle,
        overflowLabel: (@Sendable (Int) -> String)? = nil,
        @ViewBuilder content: @escaping (Data.Element) -> Content
    ) {
        self.init(
            label,
            data,
            id: \.id,
            maxVisible: maxVisible,
            size: size,
            shape: shape,
            overflowLabel: overflowLabel,
            content: content
        )
    }

    /// An identifiable avatar group without a label, named by its number of people.
    ///
    /// - Parameters:
    ///   - data: The people or entities, in order.
    ///   - maxVisible: Show at most this many avatars, then a "+N" tile. All by default.
    ///   - size: The size of the avatars, for the overlap and the tile.
    ///   - shape: The shape of the avatars, for the ring and the tile.
    ///   - overflowLabel: Replaces the built-in spoken "N more" of the tile.
    ///   - content: The avatar of one element.
    public init(
        _ data: Data,
        maxVisible: Int? = nil,
        size: MonarchAvatarSize = .md,
        shape: MonarchAvatarShape = .circle,
        overflowLabel: (@Sendable (Int) -> String)? = nil,
        @ViewBuilder content: @escaping (Data.Element) -> Content
    ) {
        self.init(
            data,
            id: \.id,
            maxVisible: maxVisible,
            size: size,
            shape: shape,
            overflowLabel: overflowLabel,
            content: content
        )
    }
}

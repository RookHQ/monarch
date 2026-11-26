import SwiftUI

/// A picture of a person or an entity, with initials or a symbol when there
/// is no picture, and an optional presence dot.
///
/// ```swift
/// MonarchAvatar(url: person.photoURL, name: person.name, status: .online)
/// MonarchAvatar(image: nil, name: "Rook Labs", systemImage: "building.2", shape: .square)
/// ```
///
/// Without an image, while it loads and when it fails, the avatar shows the
/// initials of `name` on a colour picked from `seed` (the name by default),
/// so the same person has the same colour everywhere. Pass a stable id as
/// the seed so a renamed person keeps their colour. With `systemImage`, or
/// when the name has no letters or digits, a symbol is shown instead.
///
/// Accessibility: the avatar is one image element named by `name`, and by
/// the presence when there is one ("Ana Silva, Online"); the dot itself is
/// not read separately. Pass `isDecorative` when the name is written next to
/// the avatar, so it is not read twice. The avatar scales with Dynamic Type.
public struct MonarchAvatar: View {
    private let image: Image?
    private let url: URL?
    private let name: String
    private let seed: String?
    private var usesAiTone = false
    private let systemImage: String?
    private let shape: MonarchAvatarShape
    private let size: MonarchAvatarSize
    private let status: MonarchPresenceStatus?
    private let isDecorative: Bool
    private let statusLabel: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.redactionReasons) private var redactionReasons
    @ScaledMetric private var side: CGFloat
    @ScaledMetric private var squareRadius: CGFloat

    /// An avatar with an image the app already has.
    ///
    /// - Parameters:
    ///   - image: The picture, or `nil` for the fallback.
    ///   - name: The person or entity, already localised. Read by VoiceOver and used for the initials.
    ///   - seed: Picks the fallback colour. The name by default; pass a stable id.
    ///   - systemImage: An SF Symbol shown instead of the initials, for entities.
    ///   - shape: A circle for people, a rounded square for entities.
    ///   - size: The side before Dynamic Type.
    ///   - status: A presence dot at the bottom trailing corner.
    ///   - isDecorative: Hides the avatar from VoiceOver, when the name is written beside it.
    ///   - statusLabel: Replaces the built-in spoken name of the presence.
    public init(
        image: Image?,
        name: String,
        seed: String? = nil,
        systemImage: String? = nil,
        shape: MonarchAvatarShape = .circle,
        size: MonarchAvatarSize = .md,
        status: MonarchPresenceStatus? = nil,
        isDecorative: Bool = false,
        statusLabel: String? = nil
    ) {
        self.image = image
        self.url = nil
        self.name = name
        self.seed = seed
        self.systemImage = systemImage
        self.shape = shape
        self.size = size
        self.status = status
        self.isDecorative = isDecorative
        self.statusLabel = statusLabel
        _side = ScaledMetric(wrappedValue: MonarchAvatarTokens.side(size), relativeTo: .body)
        _squareRadius = ScaledMetric(wrappedValue: MonarchAvatarTokens.squareRadius(size), relativeTo: .body)
    }

    /// An avatar loaded from a URL with `AsyncImage`.
    ///
    /// - Parameters:
    ///   - url: The picture's address, or `nil` for the fallback. The fallback shows while it loads and if it fails.
    ///   - name: The person or entity, already localised. Read by VoiceOver and used for the initials.
    ///   - seed: Picks the fallback colour. The name by default; pass a stable id.
    ///   - systemImage: An SF Symbol shown instead of the initials, for entities.
    ///   - shape: A circle for people, a rounded square for entities.
    ///   - size: The side before Dynamic Type.
    ///   - status: A presence dot at the bottom trailing corner.
    ///   - isDecorative: Hides the avatar from VoiceOver, when the name is written beside it.
    ///   - statusLabel: Replaces the built-in spoken name of the presence.
    public init(
        url: URL?,
        name: String,
        seed: String? = nil,
        systemImage: String? = nil,
        shape: MonarchAvatarShape = .circle,
        size: MonarchAvatarSize = .md,
        status: MonarchPresenceStatus? = nil,
        isDecorative: Bool = false,
        statusLabel: String? = nil
    ) {
        self.image = nil
        self.url = url
        self.name = name
        self.seed = seed
        self.systemImage = systemImage
        self.shape = shape
        self.size = size
        self.status = status
        self.isDecorative = isDecorative
        self.statusLabel = statusLabel
        _side = ScaledMetric(wrappedValue: MonarchAvatarTokens.side(size), relativeTo: .body)
        _squareRadius = ScaledMetric(wrappedValue: MonarchAvatarTokens.squareRadius(size), relativeTo: .body)
    }

    /// The same avatar with the AI accent instead of a hashed hue: an agent's face, as on the web.
    func aiTone() -> MonarchAvatar {
        var copy = self
        copy.usesAiTone = true
        return copy
    }

    /// The content of the view.
    public var body: some View {
        let outline = RoundedRectangle(
            cornerRadius: shape == .circle ? side / 2 : squareRadius,
            style: .continuous
        )
        picture
            .frame(width: side, height: side)
            .clipShape(outline)
            .overlay { if usesAiTone { outline.strokeBorder(theme.colors.aiBorder, lineWidth: 1) } }
            .overlay(alignment: .bottomTrailing) {
                if let status {
                    MonarchPresenceDot(
                        status,
                        size: MonarchAvatarTokens.dotSize(size),
                        isDecorative: true,
                        isOverlay: true
                    )
                    .offset(
                        x: MonarchAvatarTokens.dotOutset(size)
                            * MonarchPresenceDotTokens.diameter(MonarchAvatarTokens.dotSize(size)),
                        y: MonarchAvatarTokens.dotOutset(size)
                            * MonarchPresenceDotTokens.diameter(MonarchAvatarTokens.dotSize(size))
                    )
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(
                MonarchAvatarTokens.accessibilityLabel(name: name, status: status, statusLabel: statusLabel)
            )
            .accessibilityAddTraits(.isImage)
            .accessibilityHidden(isDecorative)
    }

    @ViewBuilder private var picture: some View {
        if let image {
            MonarchRemoteImage(image: image) { placeholder }
        } else {
            MonarchRemoteImage(url: url) { placeholder }
        }
    }

    private var placeholder: some View {
        let hashed = MonarchAvatarTokens.hueFallback(seed: seed ?? name, isDark: theme.isDark)
        let colors = usesAiTone ? (background: theme.colors.aiSubtle, foreground: theme.colors.aiStrong) : hashed
        let initials = MonarchAvatarTokens.initials(name)
        // In a skeleton (`redacted(reason: .placeholder)`) the avatar is a
        // plain muted disc, the colour of the other placeholder shapes.
        let isPlaceholder = redactionReasons.contains(.placeholder)
        return ZStack {
            Rectangle()
                .fill(
                    isPlaceholder ? theme.colors[role: MonarchAvatarTokens.placeholderFill] : colors.background
                )
            if !isPlaceholder {
                Group {
                    if systemImage == nil, MonarchAvatarTokens.hasReadableInitials(initials) {
                        Text(initials)
                            .monarchTextStyle(MonarchAvatarTokens.initialsStyle(size), weight: .semibold)
                            .lineLimit(1)
                            .minimumScaleFactor(MonarchAvatarTokens.initialsMinimumScale)
                    } else {
                        MonarchIcon(
                            systemName: systemImage ?? MonarchAvatarTokens.personSymbol,
                            size: MonarchAvatarTokens.iconSize(size)
                        )
                    }
                }
                .foregroundStyle(theme.isHighContrast ? theme.colors.textDefault : colors.foreground)
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchAvatar") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                HStack {
                    ForEach(MonarchAvatarSize.allCases, id: \.self) { size in
                        MonarchAvatar(image: nil, name: "Ana Silva", size: size, status: .online)
                    }
                    MonarchAvatar(image: nil, name: "Rook Labs", systemImage: "building.2", shape: .square)
                    MonarchAvatar(image: nil, name: "", status: .offline)
                }
                MonarchAvatarGroup(
                    "Assignees",
                    ["Ana Silva", "Ben Okafor", "Chen Wei", "Dara Kim", "Eli Roth"],
                    id: \.self,
                    maxVisible: 3
                ) {
                    MonarchAvatar(image: nil, name: $0)
                }
            }
        }
    }
#endif

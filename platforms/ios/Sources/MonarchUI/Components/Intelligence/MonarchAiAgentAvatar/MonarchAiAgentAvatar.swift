import SwiftUI

/// The picture of an AI agent: a rounded square with a bot badge, so it is
/// never taken for a person's round avatar, and a ring that shows what the
/// agent is doing.
///
/// ```swift
/// MonarchAiAgentAvatar(MonarchAiAgentRef(name: "Research agent", codename: "Ra"), state: .working)
/// ```
///
/// It is ``MonarchAvatar`` with the square shape. Without a picture it shows
/// the initials of the agent's codename, else of its name, else a chip
/// symbol. Working draws a turning ring, waiting a dashed warning ring and
/// error a danger ring with a danger badge.
///
/// Accessibility: the avatar is one image element read as the name, "AI
/// agent" and the state unless idle ("Research agent, AI agent, Working"),
/// so the state never depends on the ring's colour or motion. Pass
/// `isDecorative` when the name is written beside it: the avatar is then
/// hidden, except that a state other than idle is still read. With Reduce
/// Motion the working ring stands still. It scales with Dynamic Type.
public struct MonarchAiAgentAvatar: View {
    private let agent: MonarchAiAgentRef
    private let image: Image?
    private let usesImage: Bool
    private let state: MonarchAiAgentState
    private let size: MonarchAvatarSize
    private let showsState: Bool
    private let isDecorative: Bool
    private let labels: MonarchAiAgentAvatarLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric private var badgeSide: CGFloat
    @ScaledMetric private var ringRadius: CGFloat
    @ScaledMetric(relativeTo: .body) private var ringWidth = MonarchAiAgentAvatarTokens.ringWidth
    @ScaledMetric(relativeTo: .body) private var ringGap = MonarchAiAgentAvatarTokens.ringGap

    /// An agent avatar that loads the agent's `avatar` URL with `AsyncImage`.
    ///
    /// - Parameters:
    ///   - agent: The agent. `name` names the avatar, `codename` feeds the
    ///     initials and `avatar` is the picture's http or https address.
    ///   - state: What the agent is doing.
    ///   - size: The side of the picture before Dynamic Type.
    ///   - showsState: Whether the state is written beside the picture.
    ///   - isDecorative: Hides the avatar from VoiceOver, except for a state
    ///     other than idle, when the name is written beside it.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ agent: MonarchAiAgentRef,
        state: MonarchAiAgentState = .idle,
        size: MonarchAvatarSize = .md,
        showsState: Bool = false,
        isDecorative: Bool = false,
        labels: MonarchAiAgentAvatarLabels = MonarchAiAgentAvatarLabels()
    ) {
        self.init(agent, image: nil, usesImage: false, state, size, showsState, isDecorative, labels)
    }

    /// An agent avatar with a picture the app already has.
    ///
    /// - Parameters:
    ///   - agent: The agent. `name` names the avatar and `codename` feeds the initials.
    ///   - image: The picture, or `nil` for the initials.
    ///   - state: What the agent is doing.
    ///   - size: The side of the picture before Dynamic Type.
    ///   - showsState: Whether the state is written beside the picture.
    ///   - isDecorative: Hides the avatar from VoiceOver, except for a state
    ///     other than idle, when the name is written beside it.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ agent: MonarchAiAgentRef,
        image: Image?,
        state: MonarchAiAgentState = .idle,
        size: MonarchAvatarSize = .md,
        showsState: Bool = false,
        isDecorative: Bool = false,
        labels: MonarchAiAgentAvatarLabels = MonarchAiAgentAvatarLabels()
    ) {
        self.init(agent, image: image, usesImage: true, state, size, showsState, isDecorative, labels)
    }

    private init(
        _ agent: MonarchAiAgentRef,
        image: Image?,
        usesImage: Bool,
        _ state: MonarchAiAgentState,
        _ size: MonarchAvatarSize,
        _ showsState: Bool,
        _ isDecorative: Bool,
        _ labels: MonarchAiAgentAvatarLabels
    ) {
        self.agent = agent
        self.image = image
        self.usesImage = usesImage
        self.state = state
        self.size = size
        self.showsState = showsState
        self.isDecorative = isDecorative
        self.labels = labels
        _badgeSide = ScaledMetric(wrappedValue: MonarchAiAgentAvatarTokens.badgeSide(size), relativeTo: .body)
        _ringRadius = ScaledMetric(wrappedValue: MonarchAiAgentAvatarTokens.ringRadius(size), relativeTo: .body)
    }

    /// The content of the view.
    public var body: some View {
        let label = MonarchAiAgentAvatarTokens.accessibilityLabel(
            name: agent.name,
            state: state,
            isDecorative: isDecorative,
            labels: labels
        )
        HStack(spacing: MonarchAiAgentAvatarTokens.stateSpacing) {
            picture
            if showsState {
                Text(MonarchAiAgentAvatarTokens.stateName(state, labels: labels))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label ?? "")
        .accessibilityAddTraits(isDecorative ? [] : .isImage)
        .accessibilityHidden(label == nil)
    }

    private var picture: some View {
        avatar
            .padding(ringGap + ringWidth)
            .overlay { ring }
            .overlay(alignment: .bottomTrailing) {
                badge.offset(x: badgeOutset, y: badgeOutset)
            }
    }

    @ViewBuilder private var avatar: some View {
        let name = MonarchAiAgentAvatarTokens.initialsSource(agent)
        let symbol = MonarchAiAgentAvatarTokens.fallbackSymbol(agent)
        let seed = agent.id ?? agent.name
        if usesImage {
            MonarchAvatar(
                image: image,
                name: name,
                seed: seed,
                systemImage: symbol,
                shape: .square,
                size: size,
                isDecorative: true
            )
            .aiTone()
        } else {
            MonarchAvatar(
                url: MonarchAiAgentAvatarTokens.imageURL(agent),
                name: name,
                seed: seed,
                systemImage: symbol,
                shape: .square,
                size: size,
                isDecorative: true
            )
            .aiTone()
        }
    }

    @ViewBuilder private var ring: some View {
        let shape = RoundedRectangle(cornerRadius: ringRadius, style: .continuous)
        switch MonarchAiAgentAvatarTokens.ring(state) {
        case .none:
            EmptyView()
        case .solid(let role):
            shape.strokeBorder(theme.colors[role: role], lineWidth: ringWidth)
        case .dashed(let role):
            shape.strokeBorder(
                theme.colors[role: role],
                style: StrokeStyle(lineWidth: ringWidth, dash: MonarchAiAgentAvatarTokens.dash)
            )
        case .moving(let strong, let faint):
            if reduceMotion {
                shape.strokeBorder(theme.colors[role: strong], lineWidth: ringWidth)
            } else {
                TimelineView(.animation) { context in
                    let angle = MonarchAiAgentAvatarTokens.ringAngle(at: context.date.timeIntervalSinceReferenceDate)
                    shape.strokeBorder(
                        AngularGradient(
                            colors: [theme.colors[role: faint], theme.colors[role: strong], theme.colors[role: faint]],
                            center: .center,
                            angle: angle
                        ),
                        lineWidth: ringWidth
                    )
                }
            }
        }
    }

    /// How far the badge moves out over the corner, so it never sits on the initials.
    private var badgeOutset: CGFloat {
        badgeSide * MonarchAiAgentAvatarTokens.badgeOutset(size)
    }

    private var badge: some View {
        let colors = MonarchAiAgentAvatarTokens.badge(state)
        let glyph = badgeSide * MonarchAiAgentAvatarTokens.badgeGlyphRatio
        return MonarchAiBotGlyphShape()
            .stroke(
                style: StrokeStyle(
                    lineWidth: glyph * MonarchAiBotGlyphTokens.strokeRatio,
                    lineCap: .round,
                    lineJoin: .round
                )
            )
            .frame(width: glyph, height: glyph)
            .foregroundStyle(theme.colors[role: colors.glyph])
            .frame(width: badgeSide, height: badgeSide)
            .background(Circle().fill(theme.colors[role: colors.fill]))
            .padding(MonarchAiAgentAvatarTokens.badgeRingWidth)
            .background(Circle().fill(theme.colors.bgSurface))
            .accessibilityHidden(true)
    }
}

#if DEBUG
    #Preview("MonarchAiAgentAvatar") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                HStack {
                    ForEach(MonarchAvatarSize.allCases, id: \.self) { size in
                        MonarchAiAgentAvatar(MonarchAiAgentRef(name: "Research agent"), size: size)
                    }
                }
                ForEach(MonarchAiAgentState.allCases, id: \.self) { state in
                    MonarchAiAgentAvatar(
                        MonarchAiAgentRef(name: "Billing agent", codename: "Bo"),
                        state: state,
                        size: .lg,
                        showsState: true
                    )
                }
            }
        }
    }
#endif

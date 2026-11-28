import SwiftUI

/// One participant in a call: their video or avatar, name, microphone,
/// raised hand, connection quality and reactions, with Pin and full-screen
/// buttons.
///
/// ```swift
/// MonarchCallTile(
///     participant.name,
///     isPinned: $isPinned,
///     isMicOff: participant.isMuted,
///     isSpeaking: participant.isSpeaking,
///     quality: participant.quality
/// ) {
///     RemoteVideoView(track: participant.videoTrack)
/// }
/// ```
///
/// The video is a slot: put the view of the product's call SDK in it, for
/// example a `UIViewRepresentable` around the SDK's renderer. Monarch does
/// not capture or decode video. Without video, or with `showsVideo` false,
/// the avatar is shown. Your own camera (`isSelf`) is mirrored, a shared
/// screen (`isScreen`) never is. The tile is 16 by 9 at its width and grows
/// taller when the text needs more room.
///
/// A reaction floats up for three seconds after it was sent, and stands
/// still with Reduce Motion. Pin appears when `isPinned` is given, full
/// screen when `onFullScreen` is given; neither on the `sm` filmstrip size.
///
/// Accessibility: the tile is one element named by the participant, "Ana
/// (you)", whose value lists what is true now: "Speaking, Muted, Hand
/// raised, Poor connection". Speaking is also a thicker border, the muted
/// microphone and the hand are symbols, and quality is the number of bars,
/// so nothing depends on colour. Pin and full screen are separate buttons
/// with 44-point targets; a reaction is announced.
public struct MonarchCallTile<Video: View>: View {
    private let name: String
    private let avatarURL: URL?
    private let isPinned: Binding<Bool>?
    private let showsVideo: Bool
    private let isSelf: Bool
    private let isMicOff: Bool
    private let isSpeaking: Bool
    private let isHandRaised: Bool
    private let isScreen: Bool
    private let status: String?
    private let statusTone: MonarchTone
    private let quality: MonarchCallQuality?
    private let reaction: MonarchCallReaction?
    private let size: MonarchCallTileSize
    private let labels: MonarchCallTileLabels
    private let onFullScreen: (@MainActor () -> Void)?
    private let video: Video

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// A call tile with a video slot.
    ///
    /// - Parameters:
    ///   - name: The participant's display name, already localised.
    ///   - isPinned: Whether the participant is pinned to the main spot. Adds the Pin button.
    ///   - avatarURL: The picture shown without video, loaded with `AsyncImage`.
    ///   - showsVideo: Whether the video is shown. Pass false while connecting or when the camera is off.
    ///   - isSelf: This is you: adds "(you)" and mirrors the video.
    ///   - isMicOff: Shows the muted microphone.
    ///   - isSpeaking: Highlights the tile with a green border.
    ///   - isHandRaised: Shows the raised hand badge.
    ///   - isScreen: The video is a shared screen: fitted and never mirrored.
    ///   - status: A short status in the name bar, such as "Reconnecting…".
    ///   - statusTone: The colour of the status.
    ///   - quality: The connection quality bars. None when `nil`.
    ///   - reaction: A reaction that floats up for three seconds after it was sent.
    ///   - size: `sm` for the filmstrip, `md` in a grid, `lg` for the main speaker.
    ///   - labels: Replacements for the built-in strings.
    ///   - onFullScreen: Shows the participant full screen. Adds the button.
    ///   - video: The participant's video, from the product's call SDK.
    public init(
        _ name: String,
        isPinned: Binding<Bool>? = nil,
        avatarURL: URL? = nil,
        showsVideo: Bool = true,
        isSelf: Bool = false,
        isMicOff: Bool = false,
        isSpeaking: Bool = false,
        isHandRaised: Bool = false,
        isScreen: Bool = false,
        status: String? = nil,
        statusTone: MonarchTone = .neutral,
        quality: MonarchCallQuality? = nil,
        reaction: MonarchCallReaction? = nil,
        size: MonarchCallTileSize = .md,
        labels: MonarchCallTileLabels = MonarchCallTileLabels(),
        onFullScreen: (@MainActor () -> Void)? = nil,
        @ViewBuilder video: () -> Video
    ) {
        self.name = name
        self.isPinned = isPinned
        self.avatarURL = avatarURL
        self.showsVideo = showsVideo
        self.isSelf = isSelf
        self.isMicOff = isMicOff
        self.isSpeaking = isSpeaking
        self.isHandRaised = isHandRaised
        self.isScreen = isScreen
        self.status = status
        self.statusTone = statusTone
        self.quality = quality
        self.reaction = reaction
        self.size = size
        self.labels = labels
        self.onFullScreen = onFullScreen
        self.video = video()
    }

    private typealias Tokens = MonarchCallTileTokens
    private var displayName: String { Tokens.displayName(name, isSelf: isSelf, labels: labels) }
    private var hasVideo: Bool { showsVideo && Video.self != EmptyView.self }

    /// The content of the view.
    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: Tokens.cornerRadius, style: .continuous)
        MonarchCallTileLayout {
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top) {
                    if isHandRaised { handBadge }
                    Spacer(minLength: 0)
                    actions
                }
                Spacer(minLength: Tokens.padding(size))
                if !hasVideo {
                    // Between the top row and the name bar, so a short tile never draws the label over the face.
                    avatar
                        .frame(maxWidth: .infinity)
                    Spacer(minLength: Tokens.padding(size))
                }
                nameBar
            }
            .padding(Tokens.padding(size))
        }
        .background { picture.accessibilityHidden(true) }
        .overlay { floatingReaction }
        .clipShape(shape)
        .overlay {
            if isSpeaking {
                shape.strokeBorder(theme.colors[role: Tokens.speakingBorder], lineWidth: Tokens.speakingBorderWidth)
                    .accessibilityHidden(true)
            }
        }
        .monarchAnimation(.fast, value: isSpeaking)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(displayName)
        .onChange(of: reaction) { _, new in
            guard let new, Tokens.isReactionVisible(new, at: Date()) else { return }
            AccessibilityNotification.Announcement(Tokens.reactedAnnouncement(name, new.emoji, labels: labels)).post()
        }
    }

    @ViewBuilder private var picture: some View {
        ZStack {
            Rectangle().fill(theme.colors[role: Tokens.background])
            if hasVideo {
                video
                    .scaleEffect(x: Tokens.mirrors(isSelf: isSelf, isScreen: isScreen) ? -1 : 1, y: 1)
            }
        }
    }

    private var avatar: some View {
        MonarchAvatar(url: avatarURL, name: name, size: Tokens.avatarSize(size), isDecorative: true)
            .overlay {
                if isSpeaking {
                    Circle()
                        .strokeBorder(
                            theme.colors[role: Tokens.speakingRing],
                            lineWidth: Tokens.speakingRingWidth
                        )
                        .padding(-Tokens.speakingRingWidth)
                }
            }
            .padding(Tokens.speakingRingWidth)
    }

    private var infoValue: String {
        Tokens.accessibilityValue(
            isSpeaking: isSpeaking,
            isMicOff: isMicOff,
            isHandRaised: isHandRaised,
            isScreen: isScreen,
            status: status,
            quality: quality,
            labels: labels
        )
    }

    private var handBadge: some View {
        MonarchBadge(
            labels.handRaised ?? MonarchCallTileStrings.handRaised,
            tone: .warning,
            variant: .solid,
            systemImage: Tokens.handSymbol
        )
        .accessibilityHidden(true)
    }

    @ViewBuilder private var actions: some View {
        if Tokens.showsActions(size, hasPin: isPinned != nil, hasFullScreen: onFullScreen != nil) {
            HStack(spacing: 0) {
                if let isPinned {
                    actionButton(
                        isPinned.wrappedValue ? Tokens.unpinSymbol : Tokens.pinSymbol,
                        label: isPinned.wrappedValue
                            ? labels.unpin?(name) ?? MonarchCallTileStrings.unpin(name)
                            : labels.pin?(name) ?? MonarchCallTileStrings.pin(name)
                    ) {
                        isPinned.wrappedValue.toggle()
                    }
                    .accessibilityAddTraits(isPinned.wrappedValue ? [.isToggle, .isSelected] : [.isToggle])
                }
                if let onFullScreen {
                    actionButton(
                        Tokens.fullScreenSymbol,
                        label: labels.fullScreen?(name) ?? MonarchCallTileStrings.fullScreen(name),
                        action: onFullScreen
                    )
                }
            }
        }
    }

    private func actionButton(
        _ symbol: String,
        label: String,
        action: @escaping @MainActor () -> Void
    ) -> some View {
        Button(action: action) {
            MonarchIcon(systemName: symbol, size: .sm)
                .foregroundStyle(theme.colors[role: Tokens.barForeground])
                .padding(Tokens.padding(.sm))
                .background(Circle().fill(theme.colors[role: Tokens.barBackground]))
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .help(label)
    }

    private var nameBar: some View {
        HStack(spacing: Tokens.barSpacing) {
            if isMicOff {
                MonarchIcon(systemName: Tokens.mutedSymbol, size: .sm)
            }
            if isScreen {
                MonarchIcon(systemName: Tokens.screenSymbol, size: .sm)
            }
            Text(displayName)
                .monarchTextStyle(Tokens.barStyle(size), weight: .semibold)
            if let status, !status.isEmpty {
                Text(status)
                    .monarchTextStyle(Tokens.barStyle(size))
                    .foregroundStyle(theme.colors[role: Tokens.statusColor(statusTone)])
            }
            if let quality {
                MonarchCallQualityBars(quality: quality, label: Tokens.qualityLabel(quality, labels: labels))
            }
        }
        .foregroundStyle(theme.colors[role: Tokens.barForeground])
        .padding(.horizontal, Tokens.padding(size))
        .padding(.vertical, Tokens.barSpacing)
        .background(
            RoundedRectangle(cornerRadius: Tokens.cornerRadius / 2, style: .continuous)
                .fill(theme.colors[role: Tokens.barBackground])
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(displayName)
        .accessibilityValue(infoValue)
        .accessibilitySortPriority(1)
    }

    @ViewBuilder private var floatingReaction: some View {
        if let reaction {
            TimelineView(.explicit(Tokens.reactionSchedule(reaction))) { context in
                let isVisible = Tokens.isReactionVisible(reaction, at: context.date)
                ZStack {
                    if isVisible {
                        Text(reaction.emoji)
                            .monarchTextStyle(.display)
                            .transition(
                                reduceMotion
                                    ? AnyTransition.opacity
                                    : AnyTransition.opacity.combined(with: .offset(y: Tokens.reactionRise))
                            )
                    }
                }
                .monarchAnimation(.slow, easing: .enter, value: isVisible)
            }
            .id(reaction)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
        }
    }
}

extension MonarchCallTile where Video == EmptyView {
    /// A call tile that shows the avatar, for a participant without video.
    ///
    /// - Parameters:
    ///   - name: The participant's display name, already localised.
    ///   - isPinned: Whether the participant is pinned to the main spot. Adds the Pin button.
    ///   - avatarURL: The picture, loaded with `AsyncImage`.
    ///   - isSelf: This is you: adds "(you)".
    ///   - isMicOff: Shows the muted microphone.
    ///   - isSpeaking: Highlights the tile with a green border.
    ///   - isHandRaised: Shows the raised hand badge.
    ///   - status: A short status in the name bar, such as "Reconnecting…".
    ///   - statusTone: The colour of the status.
    ///   - quality: The connection quality bars. None when `nil`.
    ///   - reaction: A reaction that floats up for three seconds after it was sent.
    ///   - size: `sm` for the filmstrip, `md` in a grid, `lg` for the main speaker.
    ///   - labels: Replacements for the built-in strings.
    ///   - onFullScreen: Shows the participant full screen. Adds the button.
    public init(
        _ name: String,
        isPinned: Binding<Bool>? = nil,
        avatarURL: URL? = nil,
        isSelf: Bool = false,
        isMicOff: Bool = false,
        isSpeaking: Bool = false,
        isHandRaised: Bool = false,
        status: String? = nil,
        statusTone: MonarchTone = .neutral,
        quality: MonarchCallQuality? = nil,
        reaction: MonarchCallReaction? = nil,
        size: MonarchCallTileSize = .md,
        labels: MonarchCallTileLabels = MonarchCallTileLabels(),
        onFullScreen: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            name,
            isPinned: isPinned,
            avatarURL: avatarURL,
            showsVideo: false,
            isSelf: isSelf,
            isMicOff: isMicOff,
            isSpeaking: isSpeaking,
            isHandRaised: isHandRaised,
            isScreen: false,
            status: status,
            statusTone: statusTone,
            quality: quality,
            reaction: reaction,
            size: size,
            labels: labels,
            onFullScreen: onFullScreen,
            video: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchCallTile") {
        MonarchPreviewMatrix {
            VStack {
                MonarchCallTile(
                    "Ana Silva",
                    isPinned: .constant(true),
                    isMicOff: true,
                    isSpeaking: true,
                    isHandRaised: true,
                    quality: .poor,
                    onFullScreen: {}
                )
                HStack {
                    ForEach(MonarchCallTileSize.allCases, id: \.self) { size in
                        MonarchCallTile("Ben", isSelf: true, status: "Reconnecting…", statusTone: .warning, size: size)
                    }
                }
            }
        }
    }
#endif

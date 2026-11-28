import SwiftUI

/// The call itself: a header with the title, a running timer and the head
/// count, everyone's ``MonarchCallTile`` in a grid or a spotlight with a
/// filmstrip, connection notices and the controls.
///
/// ```swift
/// MonarchCallStage(
///     "Weekly planning",
///     participants: call.participants,
///     startedAt: call.startedAt,
///     mode: call.layout,
///     pinnedID: $call.pinnedID,
///     activeSpeakerID: call.activeSpeakerID,
///     video: { participant in RemoteVideoView(track: call.track(participant.id)) },
///     controls: { MonarchCallControls(isMicOn: $call.isMicOn, onLeave: { call.leave() }) }
/// )
/// ```
///
/// The stage takes participants, video views and callbacks only; the call
/// SDK stays in the product, and `video` returns its renderer for each
/// participant. The layout comes from
/// ``MonarchCallStageLayout/automatic(selfID:ids:mode:pinnedID:sharerID:activeSpeakerID:)``
/// unless `layout` is given: a pin wins, then someone else's shared screen,
/// then the speaker in speaker mode, then the other person in a one-to-one
/// call. The grid has at most two columns on a narrow screen or a compact
/// surface; on a wide full stage the filmstrip stands beside the main tile.
/// The stage grows with its tiles; give it a frame to fill a screen.
///
/// Accessibility: the stage is a container named by its title (or "Call");
/// the title is a heading. The timer is the system's timer text, read as
/// "Call duration" with the time as its value. The head count, a new
/// error, the reconnecting notice and the muted hint are announced when they
/// change. Header buttons are icon buttons with names and 44-point targets.
/// Tiles keep their own behaviour (see ``MonarchCallTile``).
@_spi(Experimental) public struct MonarchCallStage<Video: View, Controls: View>: View {
    private typealias Tokens = MonarchCallStageTokens

    private let title: String?
    private let participants: [MonarchCallStageParticipant]
    private let startedAt: Date?
    private let mode: MonarchCallControlsLayout
    private let pinnedID: Binding<String?>?
    private let activeSpeakerID: String?
    private let layout: MonarchCallStageLayout?
    private let surface: MonarchCallStageSurface
    private let isLowData: Bool
    private let isReconnecting: Bool
    private let error: String?
    private let isSpeakingWhileMuted: Bool
    private let labels: MonarchCallStageLabels
    private let onSurfaceChange: (@MainActor (MonarchCallStageSurface) -> Void)?
    private let onMinimize: (@MainActor () -> Void)?
    private let onPictureInPicture: (@MainActor () -> Void)?
    private let onFullScreen: (@MainActor (String) -> Void)?
    private let video: (MonarchCallStageParticipant) -> Video
    private let controls: Controls

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.horizontalSizeClass) private var sizeClass

    /// A call stage with video and controls.
    ///
    /// - Parameters:
    ///   - title: The heading, such as the meeting name, already localised. Also names the stage.
    ///   - participants: Everyone on the call, you included (`isSelf`), in display order.
    ///   - startedAt: When the call started. Shows a running timer.
    ///   - mode: The layout chosen in the call controls.
    ///   - pinnedID: The pinned participant, shown large. Adds Pin buttons on the full stage.
    ///   - activeSpeakerID: Who is talking, for speaker mode.
    ///   - layout: Replaces the automatic layout.
    ///   - surface: The full stage, a docked panel or a picture in picture window.
    ///   - isLowData: Shows the "Low data" badge.
    ///   - isReconnecting: Shows the offline notice while the connection recovers.
    ///   - error: A problem in the product's words, such as a connection that failed.
    ///   - isSpeakingWhileMuted: Shows the "You're muted" hint.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSurfaceChange: Adds Shrink (to `docked`) on the stage and Expand (to `stage`) when docked.
    ///   - onMinimize: Adds Minimize when docked.
    ///   - onPictureInPicture: Adds the picture in picture button, except inside it.
    ///   - onFullScreen: Adds full screen to the tiles of the full stage; receives the participant id.
    ///   - video: The video view of a participant, from the product's call SDK.
    ///   - controls: The controls row, usually ``MonarchCallControls``.
    public init(
        _ title: String? = nil,
        participants: [MonarchCallStageParticipant],
        startedAt: Date? = nil,
        mode: MonarchCallControlsLayout = .automatic,
        pinnedID: Binding<String?>? = nil,
        activeSpeakerID: String? = nil,
        layout: MonarchCallStageLayout? = nil,
        surface: MonarchCallStageSurface = .stage,
        isLowData: Bool = false,
        isReconnecting: Bool = false,
        error: String? = nil,
        isSpeakingWhileMuted: Bool = false,
        labels: MonarchCallStageLabels = MonarchCallStageLabels(),
        onSurfaceChange: (@MainActor (MonarchCallStageSurface) -> Void)? = nil,
        onMinimize: (@MainActor () -> Void)? = nil,
        onPictureInPicture: (@MainActor () -> Void)? = nil,
        onFullScreen: (@MainActor (String) -> Void)? = nil,
        @ViewBuilder video: @escaping (MonarchCallStageParticipant) -> Video,
        @ViewBuilder controls: () -> Controls
    ) {
        self.title = title
        self.participants = participants
        self.startedAt = startedAt
        self.mode = mode
        self.pinnedID = pinnedID
        self.activeSpeakerID = activeSpeakerID
        self.layout = layout
        self.surface = surface
        self.isLowData = isLowData
        self.isReconnecting = isReconnecting
        self.error = error
        self.isSpeakingWhileMuted = isSpeakingWhileMuted
        self.labels = labels
        self.onSurfaceChange = onSurfaceChange
        self.onMinimize = onMinimize
        self.onPictureInPicture = onPictureInPicture
        self.onFullScreen = onFullScreen
        self.video = video
        self.controls = controls()
    }

    private var isNarrow: Bool { sizeClass != .regular }
    private var reconnectingText: String { labels.reconnecting ?? MonarchCallStageStrings.reconnecting }
    private var mutedText: String { labels.mutedHint ?? MonarchCallStageStrings.mutedHint }

    private var snapshot: Tokens.Snapshot {
        Tokens.Snapshot(
            headCount: Tokens.headCount(participants, locale: locale, labels: labels),
            reconnecting: isReconnecting ? reconnectingText : nil,
            error: error.flatMap { $0.isEmpty ? nil : $0 },
            mutedHint: isSpeakingWhileMuted ? mutedText : nil
        )
    }

    /// The content of the view.
    public var body: some View {
        let current = snapshot
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            header(headCount: current.headCount)
            if isReconnecting {
                MonarchCalloutBanner(message: reconnectingText, systemImage: Tokens.offlineSymbol, tone: .warning)
            }
            if let error = current.error {
                MonarchCalloutBanner(message: error, tone: .danger)
            }
            tiles
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            if isSpeakingWhileMuted {
                MonarchInlineNotice(mutedText, systemImage: Tokens.mutedSymbol, tone: .warning)
            }
            controls
                .frame(maxWidth: .infinity)
        }
        .padding(Tokens.padding(surface))
        .background(
            RoundedRectangle(cornerRadius: Tokens.cornerRadius, style: .continuous)
                .fill(theme.colors[role: Tokens.background])
        )
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title ?? labels.region ?? MonarchCallStageStrings.region)
        .onChange(of: current) { old, new in
            if let text = Tokens.announcement(from: old, to: new) {
                AccessibilityNotification.Announcement(text).post()
            }
        }
    }

    // MARK: Header

    private func header(headCount: String) -> some View {
        HStack(alignment: .top, spacing: Tokens.spacing) {
            VStack(alignment: .leading, spacing: 0) {
                if let title, !title.isEmpty {
                    Text(title)
                        .monarchTextStyle(.label, weight: .semibold)
                        .foregroundStyle(theme.colors[role: Tokens.titleRole])
                        .accessibilityAddTraits(.isHeader)
                }
                MonarchFlowLayout(spacing: Tokens.metaSpacing) {
                    if let startedAt {
                        // Minutes with two digits, "04:00", as the web writes the running time.
                        TimelineView(.periodic(from: startedAt, by: 1)) { context in
                            Text(
                                MonarchRecordingIndicatorTokens.timeText(
                                    seconds: MonarchRecordingIndicatorTokens.seconds(
                                        since: startedAt,
                                        elapsed: nil,
                                        now: context.date
                                    ),
                                    locale: locale
                                )
                            )
                        }
                        .monospacedDigit()
                        .accessibilityLabel(labels.duration ?? MonarchCallStageStrings.duration)
                        .accessibilityAddTraits(.updatesFrequently)
                    }
                    Text(headCount)
                    if isLowData {
                        MonarchBadge(labels.lowData ?? MonarchCallStageStrings.lowData)
                    }
                }
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.metaRole])
            }
            Spacer(minLength: 0)
            HStack(spacing: 0) {
                ForEach(
                    Tokens.tools(
                        surface: surface,
                        hasPictureInPicture: onPictureInPicture != nil,
                        hasSurfaceChange: onSurfaceChange != nil,
                        hasMinimize: onMinimize != nil
                    ),
                    id: \.self
                ) { tool in
                    toolButton(tool)
                }
            }
        }
    }

    private func toolButton(_ tool: Tokens.Tool) -> some View {
        let label = Tokens.toolLabel(tool, labels: labels)
        return Button {
            switch tool {
            case .pictureInPicture: onPictureInPicture?()
            case .shrink: onSurfaceChange?(.docked)
            case .expand: onSurfaceChange?(.stage)
            case .minimize: onMinimize?()
            }
        } label: {
            MonarchIcon(systemName: Tokens.toolSymbol(tool), size: .md)
                .foregroundStyle(theme.colors[role: Tokens.toolRole])
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .help(label)
    }

    // MARK: Tiles

    @ViewBuilder private var tiles: some View {
        switch Tokens.layout(
            layout,
            participants: participants,
            mode: mode,
            pinnedID: pinnedID?.wrappedValue,
            activeSpeakerID: activeSpeakerID
        ) {
        case .grid(let ids, let columns):
            let count = Tokens.columns(columns, surface: surface, isNarrow: isNarrow)
            LazyVGrid(
                columns: Array(
                    repeating: GridItem(.flexible(), spacing: Tokens.spacing, alignment: .top),
                    count: count
                ),
                spacing: Tokens.spacing
            ) {
                ForEach(ids, id: \.self) { id in
                    tile(id, size: .md)
                }
            }
        case .spotlight(let main, let strip):
            if Tokens.stripIsBeside(surface: surface, isNarrow: isNarrow) {
                HStack(alignment: .top, spacing: Tokens.spacing) {
                    tile(main, size: .lg)
                    if !strip.isEmpty {
                        VStack(spacing: Tokens.spacing) {
                            ForEach(strip, id: \.self) { id in tile(id, size: .sm) }
                        }
                        .frame(width: Tokens.stripTileWidth)
                    }
                }
            } else {
                VStack(spacing: Tokens.spacing) {
                    tile(main, size: .lg)
                    if !strip.isEmpty {
                        ScrollView(.horizontal) {
                            HStack(spacing: Tokens.spacing) {
                                ForEach(strip, id: \.self) { id in
                                    tile(id, size: .sm).frame(width: Tokens.stripTileWidth)
                                }
                            }
                        }
                        .scrollIndicators(.hidden)
                    }
                }
            }
        }
    }

    @ViewBuilder private func tile(_ id: String, size: MonarchCallTileSize) -> some View {
        if let person = participants.first(where: { $0.id == id }) {
            let actions = Tokens.showsTileActions(surface)
            MonarchCallTile(
                person.name,
                isPinned: actions ? pinBinding(person.id) : nil,
                avatarURL: person.avatarURL,
                showsVideo: person.showsVideo,
                isSelf: person.isSelf,
                isMicOff: person.isMicOff,
                isSpeaking: person.isSpeaking,
                isHandRaised: person.isHandRaised,
                isScreen: person.isScreen,
                status: person.status,
                statusTone: person.statusTone,
                quality: person.quality,
                reaction: person.reaction,
                size: size,
                labels: labels.tile,
                onFullScreen: actions ? fullScreen(person.id) : nil,
                video: { video(person) }
            )
        }
    }

    private func pinBinding(_ id: String) -> Binding<Bool>? {
        guard let pinnedID else { return nil }
        return Binding(
            get: { pinnedID.wrappedValue == id },
            set: { pinnedID.wrappedValue = $0 ? id : nil }
        )
    }

    private func fullScreen(_ id: String) -> (@MainActor () -> Void)? {
        guard let onFullScreen else { return nil }
        return { onFullScreen(id) }
    }
}

extension MonarchCallStage where Video == EmptyView {
    /// A call stage that shows avatars, for an audio call or before video arrives.
    ///
    /// - Parameters:
    ///   - title: The heading, already localised. Also names the stage.
    ///   - participants: Everyone on the call, you included, in display order.
    ///   - startedAt: When the call started. Shows a running timer.
    ///   - mode: The layout chosen in the call controls.
    ///   - pinnedID: The pinned participant. Adds Pin buttons on the full stage.
    ///   - activeSpeakerID: Who is talking, for speaker mode.
    ///   - layout: Replaces the automatic layout.
    ///   - surface: The full stage, a docked panel or a picture in picture window.
    ///   - isLowData: Shows the "Low data" badge.
    ///   - isReconnecting: Shows the offline notice.
    ///   - error: A problem in the product's words.
    ///   - isSpeakingWhileMuted: Shows the "You're muted" hint.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSurfaceChange: Adds Shrink and Expand.
    ///   - onMinimize: Adds Minimize when docked.
    ///   - onPictureInPicture: Adds the picture in picture button.
    ///   - onFullScreen: Adds full screen to the tiles of the full stage.
    ///   - controls: The controls row, usually ``MonarchCallControls``.
    public init(
        _ title: String? = nil,
        participants: [MonarchCallStageParticipant],
        startedAt: Date? = nil,
        mode: MonarchCallControlsLayout = .automatic,
        pinnedID: Binding<String?>? = nil,
        activeSpeakerID: String? = nil,
        layout: MonarchCallStageLayout? = nil,
        surface: MonarchCallStageSurface = .stage,
        isLowData: Bool = false,
        isReconnecting: Bool = false,
        error: String? = nil,
        isSpeakingWhileMuted: Bool = false,
        labels: MonarchCallStageLabels = MonarchCallStageLabels(),
        onSurfaceChange: (@MainActor (MonarchCallStageSurface) -> Void)? = nil,
        onMinimize: (@MainActor () -> Void)? = nil,
        onPictureInPicture: (@MainActor () -> Void)? = nil,
        onFullScreen: (@MainActor (String) -> Void)? = nil,
        @ViewBuilder controls: () -> Controls
    ) {
        self.init(
            title,
            participants: participants,
            startedAt: startedAt,
            mode: mode,
            pinnedID: pinnedID,
            activeSpeakerID: activeSpeakerID,
            layout: layout,
            surface: surface,
            isLowData: isLowData,
            isReconnecting: isReconnecting,
            error: error,
            isSpeakingWhileMuted: isSpeakingWhileMuted,
            labels: labels,
            onSurfaceChange: onSurfaceChange,
            onMinimize: onMinimize,
            onPictureInPicture: onPictureInPicture,
            onFullScreen: onFullScreen,
            video: { _ in EmptyView() },
            controls: controls
        )
    }
}

extension MonarchCallStage where Video == EmptyView, Controls == EmptyView {
    /// A call stage with avatars and no controls, for a preview or a picture in picture window.
    ///
    /// - Parameters:
    ///   - title: The heading, already localised. Also names the stage.
    ///   - participants: Everyone on the call, you included, in display order.
    ///   - startedAt: When the call started. Shows a running timer.
    ///   - mode: The layout chosen in the call controls.
    ///   - activeSpeakerID: Who is talking, for speaker mode.
    ///   - layout: Replaces the automatic layout.
    ///   - surface: The full stage, a docked panel or a picture in picture window.
    ///   - isLowData: Shows the "Low data" badge.
    ///   - isReconnecting: Shows the offline notice.
    ///   - error: A problem in the product's words.
    ///   - isSpeakingWhileMuted: Shows the "You're muted" hint.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ title: String? = nil,
        participants: [MonarchCallStageParticipant],
        startedAt: Date? = nil,
        mode: MonarchCallControlsLayout = .automatic,
        activeSpeakerID: String? = nil,
        layout: MonarchCallStageLayout? = nil,
        surface: MonarchCallStageSurface = .stage,
        isLowData: Bool = false,
        isReconnecting: Bool = false,
        error: String? = nil,
        isSpeakingWhileMuted: Bool = false,
        labels: MonarchCallStageLabels = MonarchCallStageLabels()
    ) {
        self.init(
            title,
            participants: participants,
            startedAt: startedAt,
            mode: mode,
            activeSpeakerID: activeSpeakerID,
            layout: layout,
            surface: surface,
            isLowData: isLowData,
            isReconnecting: isReconnecting,
            error: error,
            isSpeakingWhileMuted: isSpeakingWhileMuted,
            labels: labels,
            controls: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchCallStage") {
        let team = [
            MonarchCallStageParticipant("me", name: "Sam Lee", isSelf: true),
            MonarchCallStageParticipant("robin", name: "Robin Park", isSpeaking: true, quality: .good),
            MonarchCallStageParticipant("alex", name: "Alex Chen", isMicOff: true, quality: .fair),
            MonarchCallStageParticipant("jordan", name: "Jordan Diaz", isHandRaised: true),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchCallStage("Weekly planning", participants: team, isLowData: true) {
                    MonarchCallControls(isMicOn: .constant(true), onLeave: {})
                }
                MonarchCallStage("Robin Park", participants: Array(team.prefix(2)), surface: .docked)
                MonarchCallStage(participants: [team[0]], isReconnecting: true, isSpeakingWhileMuted: true)
            }
        }
    }
#endif

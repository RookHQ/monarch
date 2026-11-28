import SwiftUI

/// The screen before a call: see yourself, check the microphone picks you
/// up, choose devices, and join with the microphone or camera off.
///
/// ```swift
/// MonarchPreJoinLobby(
///     isMicOn: $call.isMicOn,
///     isCameraOn: $call.isCameraOn,
///     audioLevel: meter.level,
///     participants: call.people,
///     isJoining: call.isJoining,
///     onCancel: { dismiss() },
///     preview: { CameraPreview(session: capture.session) },
///     devices: {
///         MonarchDevicePicker(
///             microphone: $call.microphoneID,
///             camera: $call.cameraID,
///             microphones: devices.microphones,
///             cameras: devices.cameras,
///             size: .sm
///         )
///     },
///     onJoin: { call.join() }
/// )
/// ```
///
/// Monarch never captures media. The camera preview is a slot the app
/// fills, for example with a view hosting `AVCaptureVideoPreviewLayer`; it
/// shows while the camera is on and ready, and a placeholder shows
/// otherwise. The app measures the microphone (for example with
/// `AVAudioRecorder` metering) and passes `audioLevel`, shown with
/// ``MonarchAudioLevelMeter`` while the microphone is on. The toggles are
/// ``MonarchCallControls``. The device choice is a second slot, usually a
/// ``MonarchDevicePicker``. Present the lobby in a sheet or push it; pass
/// `isTitleHidden` when the navigation bar already shows a title.
///
/// Accessibility: the title is a header. The preview is a container named
/// "Your camera preview" whose placeholder is read as text. The microphone
/// and camera are standard toggles, read as on or off. The level meter is
/// named "Microphone level". The participants are read as one element. A
/// problem is an icon and text. Join and Cancel are standard buttons of at
/// least 44 points, and stack vertically when they do not fit side by side.
@_spi(Experimental) public struct MonarchPreJoinLobby<Preview: View, Devices: View>: View {
    private let title: String?
    @Binding private var isMicOn: Bool
    private let isCameraOn: Binding<Bool>?
    private let audioLevel: Double
    private let participants: [MonarchPresencePerson]
    private let error: String?
    private let isCameraStarting: Bool
    private let isJoining: Bool
    private let joinTitle: String?
    private let isTitleHidden: Bool
    private let labels: MonarchPreJoinLobbyLabels
    private let onCancel: (@MainActor () -> Void)?
    private let preview: Preview
    private let devices: Devices
    private let onJoin: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchPreJoinLobbyTokens
    private typealias Strings = MonarchPreJoinLobbyStrings

    /// A pre-join lobby with a device choice.
    ///
    /// - Parameters:
    ///   - title: The heading. "Join the call" by default.
    ///   - isMicOn: Whether to join with the microphone on.
    ///   - isCameraOn: Whether to join with the camera on. Shows the camera toggle; leave out for audio calls.
    ///   - audioLevel: The microphone level from 0 to 1, measured by the app.
    ///   - participants: People already in the call.
    ///   - error: A problem getting the preview, such as blocked access.
    ///   - isCameraStarting: The camera is on but its preview is not ready yet.
    ///   - isJoining: Shows progress on the join button and disables Cancel.
    ///   - joinTitle: The join button's title, such as "Start call". "Join now" by default.
    ///   - isTitleHidden: Hides the heading, for a navigation bar that already has one.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCancel: Adds a Cancel button.
    ///   - preview: The app's camera preview, shown while the camera is on and ready.
    ///   - devices: Device choices under the preview, usually a ``MonarchDevicePicker``.
    ///   - onJoin: Joins with the chosen settings.
    public init(
        _ title: String? = nil,
        isMicOn: Binding<Bool>,
        isCameraOn: Binding<Bool>? = nil,
        audioLevel: Double = 0,
        participants: [MonarchPresencePerson] = [],
        error: String? = nil,
        isCameraStarting: Bool = false,
        isJoining: Bool = false,
        joinTitle: String? = nil,
        isTitleHidden: Bool = false,
        labels: MonarchPreJoinLobbyLabels = MonarchPreJoinLobbyLabels(),
        onCancel: (@MainActor () -> Void)? = nil,
        @ViewBuilder preview: () -> Preview,
        @ViewBuilder devices: () -> Devices,
        onJoin: @escaping @MainActor () -> Void
    ) {
        self.title = title
        self._isMicOn = isMicOn
        self.isCameraOn = isCameraOn
        self.audioLevel = audioLevel
        self.participants = participants
        self.error = error
        self.isCameraStarting = isCameraStarting
        self.isJoining = isJoining
        self.joinTitle = joinTitle
        self.isTitleHidden = isTitleHidden
        self.labels = labels
        self.onCancel = onCancel
        self.preview = preview()
        self.devices = devices()
        self.onJoin = onJoin
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            if !isTitleHidden {
                Text(title ?? labels.title ?? Strings.title)
                    .monarchTextStyle(.heading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
            }
            previewArea
            if let error {
                MonarchInlineNotice(error, tone: .danger)
            }
            devices
            if let text = Tokens.alreadyInText(participants.count, labels: labels) {
                HStack(spacing: Tokens.innerSpacing) {
                    // As on the web, the people already in the call show without presence dots.
                    MonarchAvatarGroup(
                        MonarchPresenceAvatarsTokens.label(
                            count: participants.count,
                            labels: MonarchPresenceAvatarsLabels()
                        ),
                        participants,
                        maxVisible: Tokens.maxVisibleParticipants,
                        size: .sm
                    ) { person in
                        MonarchAvatar(url: person.avatarURL, name: person.name, seed: person.seed, size: .sm)
                    }
                    Text(text)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors[role: Tokens.muted])
                }
                .accessibilityElement(children: .combine)
            }
            actions
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var previewArea: some View {
        let state = Tokens.preview(
            hasError: error != nil,
            isCameraOn: isCameraOn?.wrappedValue,
            isCameraStarting: isCameraStarting
        )
        return ZStack {
            Color.clear.aspectRatio(Tokens.previewAspectRatio, contentMode: .fit)
            if state == .video {
                preview
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let text = Tokens.placeholderText(state, labels: labels) {
                Text(text)
                    .monarchTextStyle(.body)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(theme.colors[role: Tokens.muted])
                    .padding(Tokens.previewPadding)
                    .accessibilityElement(children: .combine)
            }
        }
        .frame(maxWidth: .infinity)
        .overlay(alignment: .topLeading) {
            // As on the web, the microphone level is a short bar in the corner of the preview.
            if Tokens.showsMeter(isMicOn: isMicOn) {
                MonarchAudioLevelMeter(
                    Tokens.clampedLevel(audioLevel),
                    label: labels.micLevel ?? Strings.micLevel,
                    variant: .bar
                )
                .frame(width: 56)
                .padding(theme.spacing.space3)
            }
        }
        .overlay(alignment: .bottom) {
            MonarchCallControls(
                isMicOn: $isMicOn,
                isCameraOn: isCameraOn,
                isMinimal: true,
                labels: MonarchCallControlsLabels(toolbar: labels.controls ?? Strings.controls)
            )
            .padding(theme.spacing.space3)
        }
        .background(theme.colors[role: Tokens.previewFill])
        .clipShape(MonarchShapes.container)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.preview ?? Strings.preview)
    }

    private var actions: some View {
        ViewThatFits(in: .horizontal) {
            HStack {
                Spacer(minLength: 0)
                cancelButton(fullWidth: false)
                joinButton(fullWidth: false)
            }
            VStack {
                joinButton(fullWidth: true)
                cancelButton(fullWidth: true)
            }
        }
    }

    @ViewBuilder private func cancelButton(fullWidth: Bool) -> some View {
        if let onCancel {
            MonarchButton(labels.cancel ?? Strings.cancel, variant: .ghost, fullWidth: fullWidth) {
                onCancel()
            }
            .disabled(isJoining)
        }
    }

    private func joinButton(fullWidth: Bool) -> some View {
        MonarchButton(
            Tokens.joinTitle(isJoining: isJoining, joinTitle: joinTitle, labels: labels),
            variant: .primary,
            isLoading: isJoining,
            fullWidth: fullWidth
        ) {
            onJoin()
        }
    }
}

extension MonarchPreJoinLobby where Devices == EmptyView {
    /// A pre-join lobby without a device choice.
    ///
    /// - Parameters:
    ///   - title: The heading. "Join the call" by default.
    ///   - isMicOn: Whether to join with the microphone on.
    ///   - isCameraOn: Whether to join with the camera on. Shows the camera toggle; leave out for audio calls.
    ///   - audioLevel: The microphone level from 0 to 1, measured by the app.
    ///   - participants: People already in the call.
    ///   - error: A problem getting the preview, such as blocked access.
    ///   - isCameraStarting: The camera is on but its preview is not ready yet.
    ///   - isJoining: Shows progress on the join button and disables Cancel.
    ///   - joinTitle: The join button's title, such as "Start call". "Join now" by default.
    ///   - isTitleHidden: Hides the heading, for a navigation bar that already has one.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCancel: Adds a Cancel button.
    ///   - preview: The app's camera preview, shown while the camera is on and ready.
    ///   - onJoin: Joins with the chosen settings.
    public init(
        _ title: String? = nil,
        isMicOn: Binding<Bool>,
        isCameraOn: Binding<Bool>? = nil,
        audioLevel: Double = 0,
        participants: [MonarchPresencePerson] = [],
        error: String? = nil,
        isCameraStarting: Bool = false,
        isJoining: Bool = false,
        joinTitle: String? = nil,
        isTitleHidden: Bool = false,
        labels: MonarchPreJoinLobbyLabels = MonarchPreJoinLobbyLabels(),
        onCancel: (@MainActor () -> Void)? = nil,
        @ViewBuilder preview: () -> Preview,
        onJoin: @escaping @MainActor () -> Void
    ) {
        self.init(
            title,
            isMicOn: isMicOn,
            isCameraOn: isCameraOn,
            audioLevel: audioLevel,
            participants: participants,
            error: error,
            isCameraStarting: isCameraStarting,
            isJoining: isJoining,
            joinTitle: joinTitle,
            isTitleHidden: isTitleHidden,
            labels: labels,
            onCancel: onCancel,
            preview: preview,
            devices: { EmptyView() },
            onJoin: onJoin
        )
    }
}

extension MonarchPreJoinLobby where Preview == EmptyView, Devices == EmptyView {
    /// A pre-join lobby for an audio call: no camera preview and no device choice.
    ///
    /// - Parameters:
    ///   - title: The heading. "Join the call" by default.
    ///   - isMicOn: Whether to join with the microphone on.
    ///   - audioLevel: The microphone level from 0 to 1, measured by the app.
    ///   - participants: People already in the call.
    ///   - error: A problem with the microphone, such as blocked access.
    ///   - isJoining: Shows progress on the join button and disables Cancel.
    ///   - joinTitle: The join button's title, such as "Start call". "Join now" by default.
    ///   - isTitleHidden: Hides the heading, for a navigation bar that already has one.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCancel: Adds a Cancel button.
    ///   - onJoin: Joins with the chosen settings.
    public init(
        _ title: String? = nil,
        isMicOn: Binding<Bool>,
        audioLevel: Double = 0,
        participants: [MonarchPresencePerson] = [],
        error: String? = nil,
        isJoining: Bool = false,
        joinTitle: String? = nil,
        isTitleHidden: Bool = false,
        labels: MonarchPreJoinLobbyLabels = MonarchPreJoinLobbyLabels(),
        onCancel: (@MainActor () -> Void)? = nil,
        onJoin: @escaping @MainActor () -> Void
    ) {
        self.init(
            title,
            isMicOn: isMicOn,
            audioLevel: audioLevel,
            participants: participants,
            error: error,
            isJoining: isJoining,
            joinTitle: joinTitle,
            isTitleHidden: isTitleHidden,
            labels: labels,
            onCancel: onCancel,
            preview: { EmptyView() },
            devices: { EmptyView() },
            onJoin: onJoin
        )
    }
}

#if DEBUG
    #Preview("MonarchPreJoinLobby") {
        MonarchPreviewMatrix {
            MonarchPreJoinLobby(
                isMicOn: .constant(true),
                isCameraOn: .constant(false),
                audioLevel: 0.6,
                participants: [
                    MonarchPresencePerson(id: "1", name: "Ana Lopez"),
                    MonarchPresencePerson(id: "2", name: "Ben Okafor"),
                ],
                onCancel: {},
                preview: { EmptyView() },
                onJoin: {}
            )
            MonarchPreJoinLobby(isMicOn: .constant(false), error: "Microphone access is blocked.", isJoining: true) {}
        }
    }
#endif

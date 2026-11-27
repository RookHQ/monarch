import MonarchTokens
import SwiftUI

/// The surface for a live, two-way spoken conversation with an AI assistant:
/// a calm central visual, always-visible state text, live captions, and
/// explicit Mute, Interrupt, Captions and End controls.
///
/// ```swift
/// .fullScreenCover(isPresented: $isTalking) {
///     MonarchAiVoiceMode(
///         state: call.state,
///         level: call.level,
///         agent: MonarchAiAgentRef(name: "Ada"),
///         captions: conversation.messages,
///         elapsedSeconds: call.seconds,
///         onMuteChange: { call.setMuted($0) },
///         onInterrupt: { call.interrupt() },
///         onEnd: { call.end(); isTalking = false }
///     )
/// }
/// ```
///
/// It is UI only. The app owns the microphone, the audio, the connection and
/// the audio session, and reports what happens through `state`, `level`,
/// `isMuted` and `captions`; nothing is recorded, sent or ended without a
/// button press. The audio contract:
///
/// - Configure `AVAudioSession` (for example `.playAndRecord` with the
///   `.voiceChat` mode) before reporting `.connecting`, and deactivate it after `.ended`.
/// - On an interruption (`AVAudioSession.interruptionNotification`), such as
///   a phone call, pause and report `.muted` or `.reconnecting`; when it ends,
///   resume and report the live state again, or report `.ended`.
/// - On a route change (`AVAudioSession.routeChangeNotification`), such as
///   headphones being unplugged, keep the call and keep reporting the state.
/// - Report `.error` with `error` when the call cannot continue.
///
/// Captions are the conversation's own ``MonarchAiConversationMessage``
/// values: user and assistant messages, oldest first. A message whose text
/// is still streaming is an interim caption: shown and updated in place, but
/// hidden from VoiceOver. When its text is done it is announced once.
///
/// Present it with `.fullScreenCover` at size `md`, or place the `compact`
/// size inside an assistant panel.
///
/// Accessibility: the surface is a container named "Voice conversation with"
/// the assistant's name. The state is always written, never shown by the
/// visual alone, and each change is announced except flips between
/// listening and speaking. Mute and Captions are toggles with on and off
/// values; Magic Tap (two-finger double-tap) toggles mute. The visual is
/// decorative and holds still with Reduce Motion. When Interrupt goes away
/// while VoiceOver is on it, focus moves to Mute.
public struct MonarchAiVoiceMode: View {
    private let state: MonarchAiVoiceModeState
    private let level: Double
    private let agent: MonarchAiAgentRef?
    private let captions: [MonarchAiConversationMessage]
    private let captionsVisible: Binding<Bool>?
    private let isMuted: Bool?
    private let elapsedSeconds: Int?
    private let error: String?
    private let size: MonarchAiVoiceModeSize
    private let labels: MonarchAiVoiceModeLabels
    private let onMuteChange: (@MainActor (Bool) -> Void)?
    private let onInterrupt: (@MainActor () -> Void)?
    private let onEnd: (@MainActor () -> Void)?
    private let onRestart: (@MainActor () -> Void)?
    private let onRetry: (@MainActor () -> Void)?

    @State private var ownCaptionsVisible = true
    @State private var announced: Set<String> = []
    @AccessibilityFocusState private var focus: MonarchAiVoiceModeFocus?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @ScaledMetric private var captionsHeight: CGFloat
    @ScaledMetric(relativeTo: .body) private var avatarSize = MonarchAiVoiceModeTokens.avatarSize

    /// A voice conversation surface.
    ///
    /// - Parameters:
    ///   - state: Where the call is. The app reports it.
    ///   - level: The live audio level, 0 through 1, of whoever is talking.
    ///     It drives the visual while listening or speaking, and not with Reduce Motion.
    ///   - agent: The assistant. Its name labels the surface and its captions; its avatar is shown beside it.
    ///   - captions: The conversation's messages, oldest first. User and
    ///     assistant text is shown; streaming text is an interim caption.
    ///   - captionsVisible: Whether captions show. Without a binding the
    ///     surface keeps its own value, on at first: captions are an accessibility need.
    ///   - isMuted: Whether the microphone is muted. By default, only in the `muted` state.
    ///   - elapsedSeconds: How long the call has run. Shown as a clock, and as the duration once ended.
    ///   - error: What went wrong, shown on the `error` state. A general sentence by default.
    ///   - size: Full size, or compact for a panel.
    ///   - labels: Replacements for the built-in strings.
    ///   - onMuteChange: Called with the wanted value when the person presses
    ///     Mute or uses Magic Tap. Shows Mute while the call is live.
    ///   - onInterrupt: Shows Interrupt while the assistant thinks or speaks.
    ///   - onEnd: Shows End while the call is live. Only this button ends the call.
    ///   - onRestart: Shows Start again on the `ended` state.
    ///   - onRetry: Shows Retry on the `error` state.
    public init(
        state: MonarchAiVoiceModeState,
        level: Double = 0,
        agent: MonarchAiAgentRef? = nil,
        captions: [MonarchAiConversationMessage] = [],
        captionsVisible: Binding<Bool>? = nil,
        isMuted: Bool? = nil,
        elapsedSeconds: Int? = nil,
        error: String? = nil,
        size: MonarchAiVoiceModeSize = .md,
        labels: MonarchAiVoiceModeLabels = MonarchAiVoiceModeLabels(),
        onMuteChange: (@MainActor (Bool) -> Void)? = nil,
        onInterrupt: (@MainActor () -> Void)? = nil,
        onEnd: (@MainActor () -> Void)? = nil,
        onRestart: (@MainActor () -> Void)? = nil,
        onRetry: (@MainActor () -> Void)? = nil
    ) {
        self.state = state
        self.level = level
        self.agent = agent
        self.captions = captions
        self.captionsVisible = captionsVisible
        self.isMuted = isMuted
        self.elapsedSeconds = elapsedSeconds
        self.error = error
        self.size = size
        self.labels = labels
        self.onMuteChange = onMuteChange
        self.onInterrupt = onInterrupt
        self.onEnd = onEnd
        self.onRestart = onRestart
        self.onRetry = onRetry
        _captionsHeight = ScaledMetric(
            wrappedValue: MonarchAiVoiceModeTokens.captionsHeight(size),
            relativeTo: .body
        )
    }

    private var showsCaptions: Binding<Bool> {
        captionsVisible ?? $ownCaptionsVisible
    }

    private var agentName: String { MonarchAiVoiceModeTokens.agentName(agent, labels: labels) }
    private var errorText: String { error ?? labels.errorFallback ?? MonarchAiVoiceModeStrings.errorFallback }

    /// The content of the view.
    public var body: some View {
        let lines = MonarchAiVoiceModeTokens.captions(captions, agentName: agentName, labels: labels)
        let isLive = MonarchAiVoiceModeTokens.isLive(state)
        VStack(spacing: MonarchAiVoiceModeTokens.spacing(size)) {
            if size == .md { Spacer(minLength: 0) }
            MonarchAiVoiceModeVisual(state: state, level: level, size: size)
            summary
            if showsCaptions.wrappedValue && (isLive || !lines.isEmpty) {
                captionsLog(lines)
            }
            controls
            if size == .md { Spacer(minLength: 0) }
        }
        .padding(MonarchAiVoiceModeTokens.padding(size))
        .frame(maxWidth: .infinity, maxHeight: size == .md ? .infinity : nil)
        .background {
            if size == .md {
                theme.colors.bgCanvas.ignoresSafeArea()
            } else {
                MonarchShapes.container.fill(theme.colors.bgSurface)
                MonarchShapes.container.strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(MonarchAiVoiceModeTokens.surfaceLabel(agentName: agentName, labels: labels))
        .modifier(
            MonarchAiVoiceModeMagicTap(isEnabled: isLive && onMuteChange != nil) {
                onMuteChange?(!MonarchAiVoiceModeTokens.isMuted(state, isMuted: isMuted))
            }
        )
        .onAppear { announced = Set(lines.filter(\.isFinal).map(\.id)) }
        .onChange(of: state) { old, new in
            if let text = MonarchAiVoiceModeTokens.stateAnnouncement(
                from: old,
                to: new,
                errorText: errorText,
                labels: labels
            ) {
                AccessibilityNotification.Announcement(text).post()
            }
            if !MonarchAiVoiceModeTokens.canInterrupt(new, hasHandler: onInterrupt != nil), focus == .interrupt {
                focus = .mute
            }
        }
        .onChange(of: lines) { _, lines in
            for line in MonarchAiVoiceModeTokens.unannounced(lines, announced: announced) {
                announced.insert(line.id)
                if showsCaptions.wrappedValue {
                    AccessibilityNotification.Announcement(line.accessibilityLabel).post()
                }
            }
        }
    }

    private var summary: some View {
        let isMuted = MonarchAiVoiceModeTokens.isMuted(state, isMuted: isMuted)
        let duration = elapsedSeconds.map { MonarchAiVoiceModeTokens.duration($0, locale: locale) }
        return VStack(spacing: MonarchAiVoiceModeTokens.lineSpacing) {
            HStack(spacing: MonarchAiVoiceModeTokens.lineSpacing) {
                avatar
                Text(agentName)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                if MonarchAiVoiceModeTokens.isLive(state), let duration {
                    Text(duration)
                        .monarchTextStyle(.label)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textMuted)
                        .accessibilityLabel(MonarchAiVoiceModeTokens.elapsedLabel(duration, labels: labels))
                }
            }
            Text(MonarchAiVoiceModeTokens.stateText(state, labels: labels))
                .monarchTextStyle(MonarchAiVoiceModeTokens.stateStyle(size))
                .foregroundStyle(theme.colors.textDefault)
            if isMuted && state != .muted {
                Label(
                    labels.microphoneMuted ?? MonarchAiVoiceModeStrings.microphoneMuted,
                    systemImage: "mic.slash"
                )
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            }
            if state == .error {
                Text(errorText)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.dangerStrong)
            }
            if state == .ended, let duration {
                Text(MonarchAiVoiceModeTokens.lasted(duration, labels: labels))
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .multilineTextAlignment(.center)
        .fixedSize(horizontal: false, vertical: true)
    }

    @ViewBuilder
    private var avatar: some View {
        if let agent, let avatar = agent.avatar, let url = URL(string: avatar) {
            MonarchRemoteImage(url: url) {
                MonarchIcon(systemName: "cpu", size: .sm)
            }
            .frame(width: avatarSize, height: avatarSize)
            .clipShape(Circle())
            .accessibilityHidden(true)
        } else if let agent {
            // Without a picture the assistant is its initials with the AI mark, as on the web.
            MonarchAiAgentAvatar(agent, size: .xs, isDecorative: true)
        }
    }

    private func captionsLog(_ lines: [MonarchAiVoiceModeTokens.Caption]) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: MonarchAiVoiceModeTokens.lineSpacing) {
                if lines.isEmpty {
                    Text(labels.captionsEmpty ?? MonarchAiVoiceModeStrings.captionsEmpty)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
                ForEach(lines) { line in
                    caption(line)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .defaultScrollAnchor(.bottom)
        .frame(maxHeight: captionsHeight)
        // Up to four lines the panel takes their height, so no line is cut at the top; more lines scroll.
        .fixedSize(horizontal: false, vertical: lines.count < 5)
        .padding(MonarchTokens.Space.space3)
        .background(MonarchShapes.control.fill(theme.colors.bgSubtle))
        .overlay(
            MonarchShapes.control.strokeBorder(theme.colors.borderSubtle, lineWidth: MonarchShapes.borderWidth)
        )
    }

    private func caption(_ line: MonarchAiVoiceModeTokens.Caption) -> some View {
        var speaker = AttributedString(line.speaker)
        speaker.inlinePresentationIntent = .stronglyEmphasized
        speaker.foregroundColor = line.isUser ? theme.colors.textMuted : theme.colors.aiStrong
        var text = AttributedString(" " + line.text)
        text.foregroundColor = theme.colors.textDefault
        return Text(speaker + text)
            .monarchTextStyle(.body)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityLabel(line.accessibilityLabel)
            .accessibilityHidden(!line.isFinal)
    }

    @ViewBuilder
    private var controls: some View {
        let buttonSize = MonarchAiVoiceModeTokens.buttonSize(size)
        if MonarchAiVoiceModeTokens.isLive(state) {
            let isMuted = MonarchAiVoiceModeTokens.isMuted(state, isMuted: isMuted)
            MonarchFlowLayout(spacing: MonarchAiVoiceModeTokens.controlSpacing, justification: .center) {
                if let onMuteChange {
                    Toggle(isOn: Binding(get: { isMuted }, set: { onMuteChange($0) })) {
                        Label(labels.mute ?? MonarchAiVoiceModeStrings.mute, systemImage: isMuted ? "mic.slash" : "mic")
                    }
                    .toggleStyle(.monarchButton(.outline, size: buttonSize))
                    .accessibilityFocused($focus, equals: .mute)
                }
                if let onInterrupt, MonarchAiVoiceModeTokens.canInterrupt(state, hasHandler: true) {
                    MonarchButton(
                        labels.interrupt ?? MonarchAiVoiceModeStrings.interrupt,
                        systemImage: "stop.fill",
                        variant: .secondary,
                        size: buttonSize,
                        action: onInterrupt
                    )
                    .accessibilityFocused($focus, equals: .interrupt)
                }
                Toggle(isOn: showsCaptions) {
                    Label(labels.captions ?? MonarchAiVoiceModeStrings.captions, systemImage: "captions.bubble")
                }
                .toggleStyle(.monarchButton(.ghost, size: buttonSize, tone: .ai))
                if let onEnd {
                    MonarchButton(
                        labels.end ?? MonarchAiVoiceModeStrings.end,
                        systemImage: "xmark",
                        variant: .danger,
                        size: buttonSize,
                        action: onEnd
                    )
                }
            }
        } else if state == .ended, let onRestart {
            MonarchButton(
                labels.restart ?? MonarchAiVoiceModeStrings.restart,
                systemImage: "arrow.clockwise",
                size: buttonSize,
                action: onRestart
            )
        } else if state == .error, let onRetry {
            MonarchButton(
                labels.retry ?? MonarchAiVoiceModeStrings.retry,
                systemImage: "arrow.clockwise",
                size: buttonSize,
                action: onRetry
            )
        }
    }
}

/// Magic Tap toggles mute. The action exists only on iOS; macOS is a build convenience.
private struct MonarchAiVoiceModeMagicTap: ViewModifier {
    let isEnabled: Bool
    let action: @MainActor () -> Void

    func body(content: Content) -> some View {
        #if os(iOS)
            if isEnabled {
                content.accessibilityAction(.magicTap) { action() }
            } else {
                content
            }
        #else
            content
        #endif
    }
}

/// The controls VoiceOver focus is moved between.
private enum MonarchAiVoiceModeFocus: Hashable {
    case mute
    case interrupt
}

#if DEBUG
    #Preview("MonarchAiVoiceMode") {
        MonarchPreviewMatrix {
            VStack {
                MonarchAiVoiceMode(
                    state: .speaking,
                    level: 0.6,
                    agent: MonarchAiAgentRef(name: "Ada"),
                    captions: [
                        MonarchAiConversationMessage(
                            id: "1",
                            role: .user,
                            parts: [.text(MonarchAiTextPart(text: "What is on my calendar today?"))],
                            createdAt: "2025-03-10T12:00:00.000Z"
                        ),
                        MonarchAiConversationMessage(
                            id: "2",
                            role: .assistant,
                            parts: [.text(MonarchAiTextPart(text: "You have two meetings", state: .streaming))],
                            createdAt: "2025-03-10T12:00:02.000Z"
                        ),
                    ],
                    elapsedSeconds: 65,
                    size: .compact,
                    onMuteChange: { _ in },
                    onInterrupt: {},
                    onEnd: {}
                )
                MonarchAiVoiceMode(state: .error, size: .compact, onRetry: {})
            }
        }
    }
#endif

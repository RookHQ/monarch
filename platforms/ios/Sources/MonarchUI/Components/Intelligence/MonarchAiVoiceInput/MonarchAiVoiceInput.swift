import SwiftUI

/// A microphone button that dictates into a prompt: press to start and again
/// to stop, or hold to talk.
///
/// ```swift
/// MonarchAiVoiceInput { text, isFinal in
///     if isFinal { prompt += (prompt.isEmpty ? "" : " ") + text }
/// }
/// ```
///
/// While listening it shows a level meter, the status ("Listening…") and the
/// words as they are recognised. When access is off it shows a notice with
/// guidance and an Open Settings action; other failures show their own
/// message. It only hands text back through `onTranscript` and never sends
/// anything. It draws nothing when the recogniser is not available for the
/// language.
///
/// The default recogniser is ``MonarchAiSystemSpeechRecognizer`` (the Speech
/// framework), which needs `NSMicrophoneUsageDescription` and
/// `NSSpeechRecognitionUsageDescription` in the app's `Info.plist`. Pass any
/// ``MonarchAiSpeechRecognizer`` to use another engine.
///
/// Accessibility: in `toggle` mode the button is a standard `Toggle`, so
/// VoiceOver reads it as on while listening. In `pushToTalk` mode, which
/// VoiceOver users cannot hold, a double-tap starts and a second double-tap
/// stops. The escape gesture stops listening. "Listening…", "Stopped
/// listening." and failure messages are announced. The level meter is
/// decorative; it holds still with Reduce Motion. The transcript is read with
/// the prefix "Transcript".
@_spi(Experimental) public struct MonarchAiVoiceInput: View {
    private let locale: Locale?
    private let mode: MonarchAiVoiceInputMode
    private let size: MonarchButtonSize
    private let showsLevel: Bool
    private let showsTranscript: Bool
    private let recognizer: any MonarchAiSpeechRecognizer
    private let labels: MonarchAiVoiceInputLabels
    private let onStateChange: (@MainActor (MonarchAiVoiceInputState) -> Void)?
    private let onTranscript: @MainActor (_ text: String, _ isFinal: Bool) -> Void

    @State private var model = MonarchAiVoiceInputModel()
    @Environment(\.monarchTheme) private var theme
    @Environment(\.openURL) private var openURL
    @Environment(\.isEnabled) private var isEnabled

    /// A voice input.
    ///
    /// - Parameters:
    ///   - locale: The language of the speech. The device language by default.
    ///   - mode: Press to start and stop, or hold to talk.
    ///   - size: The size of the button.
    ///   - showsLevel: Whether a level meter is shown while listening.
    ///   - showsTranscript: Whether the words are shown under the button as they are recognised.
    ///   - recognizer: The speech engine. The Speech framework by default.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStateChange: Called with every change of state.
    ///   - onTranscript: Called with recognised text. Guesses come with
    ///     `isFinal` false and may change; each settled phrase comes once with
    ///     `isFinal` true. Put final text into the prompt; never send it automatically.
    public init(
        locale: Locale? = nil,
        mode: MonarchAiVoiceInputMode = .toggle,
        size: MonarchButtonSize = .md,
        showsLevel: Bool = true,
        showsTranscript: Bool = true,
        recognizer: any MonarchAiSpeechRecognizer = MonarchAiSystemSpeechRecognizer(),
        labels: MonarchAiVoiceInputLabels = MonarchAiVoiceInputLabels(),
        onStateChange: (@MainActor (MonarchAiVoiceInputState) -> Void)? = nil,
        onTranscript: @escaping @MainActor (_ text: String, _ isFinal: Bool) -> Void
    ) {
        self.locale = locale
        self.mode = mode
        self.size = size
        self.showsLevel = showsLevel
        self.showsTranscript = showsTranscript
        self.recognizer = recognizer
        self.labels = labels
        self.onStateChange = onStateChange
        self.onTranscript = onTranscript
    }

    /// The content of the view.
    public var body: some View {
        if recognizer.isAvailable(for: locale) {
            VStack(alignment: .leading, spacing: MonarchAiVoiceInputTokens.stackSpacing) {
                HStack(spacing: MonarchAiVoiceInputTokens.controlSpacing) {
                    button
                    if model.isActive && showsLevel {
                        MonarchAudioLevelMeter(
                            model.level,
                            size: .sm,
                            segments: MonarchAiVoiceInputTokens.meterSegments,
                            isDecorative: true
                        )
                    }
                    if let status = MonarchAiVoiceInputTokens.statusText(model.state, labels: labels) {
                        Text(status)
                            .monarchTextStyle(MonarchAiVoiceInputTokens.statusStyle)
                            .foregroundStyle(theme.colors.textMuted)
                            .accessibilityHidden(true)
                    }
                }
                if showsTranscript, !model.preview.isEmpty {
                    Text(model.preview)
                        .monarchTextStyle(MonarchAiVoiceInputTokens.transcriptStyle)
                        .foregroundStyle(theme.colors.textDefault)
                        .fixedSize(horizontal: false, vertical: true)
                        .accessibilityLabel(MonarchAiVoiceInputTokens.transcriptLabel(model.preview, labels: labels))
                }
                if let failure = model.failure, !model.isActive {
                    notice(failure)
                }
            }
            .onChange(of: isEnabled) { _, enabled in
                if !enabled { model.stop() }
            }
            .onDisappear { model.cancel() }
        }
    }

    @ViewBuilder
    private var button: some View {
        let title = MonarchAiVoiceInputTokens.buttonTitle(mode: mode, labels: labels)
        let symbol =
            model.isActive ? MonarchAiVoiceInputTokens.listeningSymbol : MonarchAiVoiceInputTokens.microphoneSymbol
        switch mode {
        case .toggle:
            Toggle(isOn: Binding(get: { model.isActive }, set: { $0 ? start() : model.stop() })) {
                Label(title, systemImage: symbol)
            }
            .toggleStyle(.monarchButton(.outline, size: size))
            .accessibilityAction(.escape) { model.stop() }
        case .pushToTalk:
            MonarchAiVoiceInputHoldButton(
                title: title,
                systemImage: symbol,
                hint: labels.holdHint ?? MonarchAiVoiceInputStrings.holdHint,
                size: size,
                isActive: model.isActive,
                onPress: start,
                onRelease: { model.stop() },
                onToggle: { model.isActive ? model.stop() : start() }
            )
            .accessibilityAction(.escape) { model.stop() }
        }
    }

    private func notice(_ failure: MonarchAiSpeechError) -> some View {
        let openURL = openURL
        let offersSettings = MonarchAiVoiceInputTokens.offersSettings(failure)
        let settingsURL = offersSettings ? MonarchSettingsURL.appSettings : nil
        var openSettings: (@MainActor @Sendable () -> Void)?
        if let settingsURL { openSettings = { openURL(settingsURL) } }
        return MonarchInlineNotice(
            MonarchAiVoiceInputTokens.message(failure, labels: labels),
            tone: MonarchAiVoiceInputTokens.tone(failure),
            actionTitle: openSettings == nil ? nil : (labels.openSettings ?? MonarchAiVoiceInputStrings.openSettings),
            action: openSettings
        )
    }

    private func start() {
        guard isEnabled else { return }
        model.labels = labels
        model.onTranscript = onTranscript
        model.onStateChange = onStateChange
        model.start(with: recognizer, locale: locale, continuous: mode == .toggle)
    }
}

#if DEBUG
    /// A recogniser for the preview that hears one phrase.
    private final class MonarchAiVoiceInputPreviewRecognizer: MonarchAiSpeechRecognizer {
        func isAvailable(for locale: Locale?) -> Bool { true }
        func start(locale: Locale?, continuous: Bool) -> AsyncThrowingStream<MonarchAiSpeechEvent, any Error> {
            AsyncThrowingStream { continuation in
                continuation.yield(.started)
                continuation.yield(.final("Book a table for two"))
                continuation.finish()
            }
        }
        func stop() {}
    }

    #Preview("MonarchAiVoiceInput") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiVoiceInput(recognizer: MonarchAiVoiceInputPreviewRecognizer()) { _, _ in }
                MonarchAiVoiceInput(mode: .pushToTalk, size: .sm, recognizer: MonarchAiVoiceInputPreviewRecognizer()) {
                    _,
                    _ in
                }
            }
        }
    }
#endif

@preconcurrency import AVFoundation
import Foundation
@preconcurrency import Speech

/// Speech recognition with Apple's Speech framework: `SFSpeechRecognizer`
/// fed from an `AVAudioEngine` microphone tap.
///
/// ```swift
/// MonarchAiVoiceInput(recognizer: MonarchAiSystemSpeechRecognizer(prefersOnDeviceRecognition: true)) { text, isFinal in
///     if isFinal { prompt += text }
/// }
/// ```
///
/// The app's `Info.plist` must contain `NSMicrophoneUsageDescription` and
/// `NSSpeechRecognitionUsageDescription`; without them the system ends the
/// app when access is requested. Permission is asked on the first start.
///
/// The recogniser differs from the web's Web Speech API: languages are those
/// of `SFSpeechRecognizer.supportedLocales()`; recognition on Apple's servers
/// is limited in length per request and needs a network, while on-device
/// recognition (when `supportsOnDeviceRecognition` is true for the language)
/// has no such limit. On iOS 26 an app can conform its own type to
/// ``MonarchAiSpeechRecognizer`` with `SpeechAnalyzer` instead.
///
/// On iOS the audio session is set to record while listening and
/// deactivated afterwards, so other audio is ducked and then resumes.
@MainActor
@_spi(Experimental) public final class MonarchAiSystemSpeechRecognizer: MonarchAiSpeechRecognizer {
    private let prefersOnDeviceRecognition: Bool
    private var engine: AVAudioEngine?
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private var stopRequested = false
    private var generation = 0

    /// A recogniser.
    ///
    /// - Parameter prefersOnDeviceRecognition: Keep the audio on the device
    ///   when the language supports it. Speech is then never sent to Apple.
    public init(prefersOnDeviceRecognition: Bool = false) {
        self.prefersOnDeviceRecognition = prefersOnDeviceRecognition
    }

    /// Whether the Speech framework has a recogniser for the language.
    public func isAvailable(for locale: Locale?) -> Bool {
        SFSpeechRecognizer(locale: locale ?? .current) != nil
    }

    /// Asks for permission, opens the microphone and recognises.
    public func start(locale: Locale?, continuous: Bool) -> AsyncThrowingStream<MonarchAiSpeechEvent, any Error> {
        teardown()
        generation += 1
        let id = generation
        stopRequested = false
        return AsyncThrowingStream { continuation in
            let task = Task { @MainActor [weak self] in
                guard let self else { return continuation.finish() }
                do {
                    try await self.run(locale: locale, continuous: continuous, id: id, continuation: continuation)
                } catch {
                    if self.generation == id { self.teardown() }
                    continuation.finish(throwing: error)
                }
            }
            continuation.onTermination = { _ in
                task.cancel()
                Task { @MainActor [weak self] in
                    guard let self, self.generation == id else { return }
                    self.teardown()
                }
            }
        }
    }

    /// Stops the microphone; the last words arrive and the stream finishes.
    public func stop() {
        stopRequested = true
        guard let engine else { return }
        engine.stop()
        engine.inputNode.removeTap(onBus: 0)
        request?.endAudio()
    }

    private func run(
        locale: Locale?,
        continuous: Bool,
        id: Int,
        continuation: AsyncThrowingStream<MonarchAiSpeechEvent, any Error>.Continuation
    ) async throws {
        guard await Self.speechAuthorized(), await Self.microphoneAuthorized() else {
            throw MonarchAiSpeechError.denied
        }
        try Task.checkCancellation()
        guard generation == id, !stopRequested else { return continuation.finish() }
        guard let recognizer = SFSpeechRecognizer(locale: locale ?? .current), recognizer.isAvailable else {
            throw MonarchAiSpeechError.unavailable
        }

        #if os(iOS)
            let session = AVAudioSession.sharedInstance()
            do {
                try session.setCategory(.record, mode: .measurement, options: .duckOthers)
                try session.setActive(true, options: .notifyOthersOnDeactivation)
            } catch {
                throw MonarchAiSpeechError.noMicrophone
            }
        #endif

        let engine = AVAudioEngine()
        let input = engine.inputNode
        let format = input.outputFormat(forBus: 0)
        guard format.channelCount > 0, format.sampleRate > 0 else { throw MonarchAiSpeechError.noMicrophone }

        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        // Long dictation, or a short phrase while the button is held.
        request.taskHint = continuous ? .dictation : .search
        if prefersOnDeviceRecognition, recognizer.supportsOnDeviceRecognition {
            request.requiresOnDeviceRecognition = true
        }

        input.installTap(
            onBus: 0,
            bufferSize: 1024,
            format: format,
            block: Self.tap(request: request, continuation: continuation)
        )
        engine.prepare()
        do {
            try engine.start()
        } catch {
            input.removeTap(onBus: 0)
            throw MonarchAiSpeechError.noMicrophone
        }
        self.engine = engine
        self.request = request
        continuation.yield(.started)
        recognitionTask = recognizer.recognitionTask(
            with: request,
            resultHandler: Self.results(
                continuation: continuation,
                finished: { [weak self] in
                    Task { @MainActor [weak self] in
                        guard let self, self.generation == id else { return }
                        self.teardown()
                    }
                }
            )
        )
    }

    /// Releases the microphone, the recognition task and the audio session.
    private func teardown() {
        recognitionTask?.cancel()
        recognitionTask = nil
        if let engine {
            engine.stop()
            engine.inputNode.removeTap(onBus: 0)
        }
        request?.endAudio()
        engine = nil
        request = nil
        #if os(iOS)
            try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        #endif
    }

    // MARK: - Callbacks that run off the main actor

    /// The microphone tap. It runs on the audio thread, so it is built
    /// outside the main actor and touches only the request and the stream.
    nonisolated private static func tap(
        request: SFSpeechAudioBufferRecognitionRequest,
        continuation: AsyncThrowingStream<MonarchAiSpeechEvent, any Error>.Continuation
    ) -> AVAudioNodeTapBlock {
        { buffer, _ in
            request.append(buffer)
            continuation.yield(.level(level(of: buffer)))
        }
    }

    /// The recognition handler, which runs on a queue of the Speech framework.
    nonisolated private static func results(
        continuation: AsyncThrowingStream<MonarchAiSpeechEvent, any Error>.Continuation,
        finished: @escaping @Sendable () -> Void
    ) -> @Sendable (SFSpeechRecognitionResult?, (any Error)?) -> Void {
        { result, error in
            if let result {
                let text = result.bestTranscription.formattedString
                if result.isFinal {
                    continuation.yield(.final(text))
                    continuation.finish()
                    finished()
                    return
                }
                continuation.yield(.partial(text))
            }
            if let error {
                if let mapped = speechError(error) {
                    continuation.finish(throwing: mapped)
                } else {
                    continuation.finish()
                }
                finished()
            }
        }
    }

    /// The root-mean-square level of a buffer, scaled to 0 through 1 as on the web.
    nonisolated static func level(of buffer: AVAudioPCMBuffer) -> Double {
        guard let channel = buffer.floatChannelData?[0], buffer.frameLength > 0 else { return 0 }
        let count = Int(buffer.frameLength)
        var sum: Float = 0
        for index in 0..<count { sum += channel[index] * channel[index] }
        return min(1, Double((sum / Float(count)).squareRoot()) * 4)
    }

    /// Maps a Speech framework error, or `nil` for a cancellation that is not a failure.
    nonisolated static func speechError(_ error: any Error) -> MonarchAiSpeechError? {
        let error = error as NSError
        guard error.domain == "kAFAssistantErrorDomain" else { return .failed }
        switch error.code {
        case 1110: return .noSpeech  // No speech detected.
        case 203, 216, 301: return nil  // The request was cancelled or ended with no result.
        case 1700: return .denied  // Recognition is not authorised.
        case 1101, 1107: return .unavailable  // The service is unreachable.
        default: return .failed
        }
    }

    nonisolated private static func speechAuthorized() async -> Bool {
        switch SFSpeechRecognizer.authorizationStatus() {
        case .authorized: return true
        case .denied, .restricted: return false
        case .notDetermined:
            return await withCheckedContinuation { continuation in
                SFSpeechRecognizer.requestAuthorization { status in
                    continuation.resume(returning: status == .authorized)
                }
            }
        @unknown default: return false
        }
    }

    nonisolated private static func microphoneAuthorized() async -> Bool {
        await AVAudioApplication.requestRecordPermission()
    }
}

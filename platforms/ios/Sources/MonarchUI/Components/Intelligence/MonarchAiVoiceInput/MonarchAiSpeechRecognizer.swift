import Foundation

/// A speech recogniser that ``MonarchAiVoiceInput`` listens with.
///
/// Monarch ships ``MonarchAiSystemSpeechRecognizer``, built on the Speech
/// framework. Conform to this protocol to use another engine: `SpeechAnalyzer`
/// on iOS 26, a recogniser on your server, or a scripted one in previews and
/// tests.
///
/// ```swift
/// final class ScriptedRecognizer: MonarchAiSpeechRecognizer {
///     func isAvailable(for locale: Locale?) -> Bool { true }
///     func start(locale: Locale?, continuous: Bool) -> AsyncThrowingStream<MonarchAiSpeechEvent, any Error> {
///         AsyncThrowingStream { continuation in
///             continuation.yield(.started)
///             continuation.yield(.final("Book a table for two"))
///             continuation.finish()
///         }
///     }
///     func stop() {}
/// }
/// ```
@MainActor
@_spi(Experimental) public protocol MonarchAiSpeechRecognizer: AnyObject {
    /// Whether recognition can run for a language on this device. When it
    /// returns `false` the voice input draws nothing.
    ///
    /// - Parameter locale: The language of the speech, or `nil` for the device language.
    /// - Returns: Whether dictation can be offered.
    func isAvailable(for locale: Locale?) -> Bool

    /// Asks for permission if needed, opens the microphone and starts
    /// recognising.
    ///
    /// The stream yields ``MonarchAiSpeechEvent`` values, finishes after
    /// ``stop()`` once the last words are in, and throws a
    /// ``MonarchAiSpeechError`` when it fails. When the consumer cancels the
    /// stream, stop at once and discard what is pending.
    ///
    /// - Parameters:
    ///   - locale: The language of the speech, or `nil` for the device language.
    ///   - continuous: `true` for press-to-start dictation that runs until
    ///     stopped, `false` for push-to-talk.
    /// - Returns: The events of this listening session.
    func start(locale: Locale?, continuous: Bool) -> AsyncThrowingStream<MonarchAiSpeechEvent, any Error>

    /// Stops listening. The stream delivers the last words and finishes.
    func stop()
}

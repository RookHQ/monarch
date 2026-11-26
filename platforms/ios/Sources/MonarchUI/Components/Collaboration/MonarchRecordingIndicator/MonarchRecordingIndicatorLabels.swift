/// Replacements for the built-in strings of ``MonarchRecordingIndicator``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchRecordingIndicatorLabels: Sendable, Hashable {
    /// The short visible text while recording. Built in: "REC".
    public var rec: String?
    /// The spoken name of the indicator. Built in: "Recording".
    public var recording: String?
    /// The visible and spoken text while paused. Built in: "Paused".
    public var paused: String?
    /// Announced when recording starts. Built in: "Recording started".
    public var started: String?
    /// Announced when recording stops. Built in: "Recording stopped".
    public var stopped: String?
    /// Announced when recording is paused. Built in: "Recording paused".
    public var pausedAnnouncement: String?
    /// Announced when recording resumes. Built in: "Recording resumed".
    public var resumed: String?
    /// The spoken name of the stop button. Built in: "Stop recording".
    public var stop: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        rec: String? = nil,
        recording: String? = nil,
        paused: String? = nil,
        started: String? = nil,
        stopped: String? = nil,
        pausedAnnouncement: String? = nil,
        resumed: String? = nil,
        stop: String? = nil
    ) {
        self.rec = rec
        self.recording = recording
        self.paused = paused
        self.started = started
        self.stopped = stopped
        self.pausedAnnouncement = pausedAnnouncement
        self.resumed = resumed
        self.stop = stop
    }
}

/// Built-in strings of the recording indicator, from the
/// `MonarchRecordingIndicator` strings table.
enum MonarchRecordingIndicatorStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.recordingIndicator." + key, table: "MonarchRecordingIndicator")
    }

    /// The short visible text while recording.
    static var rec: String { string("rec") }
    /// The spoken name of the indicator.
    static var recording: String { string("recording") }
    /// The visible and spoken text while paused.
    static var paused: String { string("paused") }
    /// Announced when recording starts.
    static var started: String { string("started") }
    /// Announced when recording stops.
    static var stopped: String { string("stopped") }
    /// Announced when recording is paused.
    static var pausedAnnouncement: String { string("pausedAnnouncement") }
    /// Announced when recording resumes.
    static var resumed: String { string("resumed") }
    /// The spoken name of the stop button.
    static var stop: String { string("stop") }
}

/// Built-in strings of the stop button, from the `MonarchAiStopButton` strings table.
enum MonarchAiStopButtonStrings {
    /// The title of the Stop button.
    static var stop: String { MonarchStrings.string("monarch.aiStopButton.stop", table: "MonarchAiStopButton") }
    /// Shown and announced once the response has stopped.
    static var stopped: String { MonarchStrings.string("monarch.aiStopButton.stopped", table: "MonarchAiStopButton") }
    /// The title of the button that resumes the response.
    static var resume: String { MonarchStrings.string("monarch.aiStopButton.continue", table: "MonarchAiStopButton") }
}

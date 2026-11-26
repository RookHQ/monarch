/// Built-in strings of the voice input, from the `MonarchAiVoiceInput` strings table.
enum MonarchAiVoiceInputStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiVoiceInput.\(key)", table: "MonarchAiVoiceInput")
    }

    static var dictate: String { string("dictate") }
    static var holdToTalk: String { string("holdToTalk") }
    static var holdHint: String { string("holdHint") }
    static var starting: String { string("starting") }
    static var listening: String { string("listening") }
    static var stopped: String { string("stopped") }
    static var denied: String { string("denied") }
    static var noSpeech: String { string("noSpeech") }
    static var noMicrophone: String { string("noMicrophone") }
    static var unavailable: String { string("unavailable") }
    static var error: String { string("error") }
    static var transcript: String { string("transcript") }
    static var openSettings: String { string("openSettings") }
}

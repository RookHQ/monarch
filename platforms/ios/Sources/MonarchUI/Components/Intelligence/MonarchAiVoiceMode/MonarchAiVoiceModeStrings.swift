/// Built-in strings of the voice mode, from the `MonarchAiVoiceMode` strings table.
enum MonarchAiVoiceModeStrings {
    private static let table = "MonarchAiVoiceMode"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiVoiceMode.\(key)", table: table)
    }

    static func label(_ agent: String) -> String {
        MonarchStrings.string("monarch.aiVoiceMode.label", table: table, agent)
    }

    static func lasted(_ duration: String) -> String {
        MonarchStrings.string("monarch.aiVoiceMode.lasted", table: table, duration)
    }

    static func state(_ state: MonarchAiVoiceModeState) -> String {
        switch state {
        case .connecting: string("state.connecting")
        case .listening: string("state.listening")
        case .thinking: string("state.thinking")
        case .speaking: string("state.speaking")
        case .muted: string("state.muted")
        case .reconnecting: string("state.reconnecting")
        case .ended: string("state.ended")
        case .error: string("state.error")
        }
    }

    static var assistant: String { string("assistant") }
    static var you: String { string("you") }
    static var microphoneMuted: String { string("microphoneMuted") }
    static var mute: String { string("mute") }
    static var interrupt: String { string("interrupt") }
    static var captions: String { string("captions") }
    static var end: String { string("end") }
    static var restart: String { string("restart") }
    static var retry: String { string("retry") }
    static var captionsEmpty: String { string("captionsEmpty") }
    static var elapsed: String { string("elapsed") }
    static var errorFallback: String { string("errorFallback") }
}

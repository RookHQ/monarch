/// Built-in strings of the audio level meter, from the `MonarchAudioLevelMeter` strings table.
enum MonarchAudioLevelMeterStrings {
    /// The spoken name of the meter.
    static var label: String {
        MonarchStrings.string("monarch.audioLevelMeter.label", table: "MonarchAudioLevelMeter")
    }

    /// The spoken value while the microphone is muted.
    static var muted: String {
        MonarchStrings.string("monarch.audioLevelMeter.muted", table: "MonarchAudioLevelMeter")
    }
}

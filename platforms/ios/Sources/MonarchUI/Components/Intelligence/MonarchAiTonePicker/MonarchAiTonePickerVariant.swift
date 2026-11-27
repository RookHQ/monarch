/// How ``MonarchAiTonePicker`` presents the tones.
public enum MonarchAiTonePickerVariant: Sendable, Hashable, CaseIterable {
    /// One row per tone with its description. The default.
    case cards
    /// A standard menu `Picker` on one line, for toolbars and dense forms.
    case compact
}

/// One tone the assistant can write in, for ``MonarchAiTonePicker``.
public struct MonarchAiTone: Sendable, Hashable, Identifiable {
    /// A stable id. It is the value of the picker's selection.
    public let id: String
    /// The name of the tone, already localised.
    public let label: String
    /// One line on how text in this tone reads, already localised.
    public let detail: String

    /// A tone.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - label: The name of the tone, already localised.
    ///   - detail: One line on how text in this tone reads.
    public init(id: String, label: String, detail: String) {
        self.id = id
        self.label = label
        self.detail = detail
    }

    /// The id of the Custom option that ``MonarchAiTonePicker`` adds when it
    /// is given a binding for the custom text.
    public static let customID = "custom"

    /// The built-in tones: Professional, Technical, Casual and Executive,
    /// with ids `professional`, `technical`, `casual` and `executive`.
    public static var defaults: [MonarchAiTone] {
        [
            MonarchAiTone(
                id: "professional",
                label: MonarchAiTonePickerStrings.professional,
                detail: MonarchAiTonePickerStrings.professionalDetail
            ),
            MonarchAiTone(
                id: "technical",
                label: MonarchAiTonePickerStrings.technical,
                detail: MonarchAiTonePickerStrings.technicalDetail
            ),
            MonarchAiTone(
                id: "casual",
                label: MonarchAiTonePickerStrings.casual,
                detail: MonarchAiTonePickerStrings.casualDetail
            ),
            MonarchAiTone(
                id: "executive",
                label: MonarchAiTonePickerStrings.executive,
                detail: MonarchAiTonePickerStrings.executiveDetail
            ),
        ]
    }
}

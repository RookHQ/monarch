/// Built-in strings of ``MonarchAiStructuredAnswer``, from the `MonarchAiStructuredAnswer` strings table.
enum MonarchAiStructuredAnswerStrings {
    private static let table = "MonarchAiStructuredAnswer"

    /// The provenance label.
    static var aiAnswer: String { MonarchStrings.string("monarch.aiStructuredAnswer.aiAnswer", table: table) }
    /// Read for a field still to come.
    static var pending: String { MonarchStrings.string("monarch.aiStructuredAnswer.pending", table: table) }
    /// Read for a field with no value.
    static var empty: String { MonarchStrings.string("monarch.aiStructuredAnswer.empty", table: table) }
    /// Announced when streaming ends.
    static var complete: String { MonarchStrings.string("monarch.aiStructuredAnswer.complete", table: table) }
}

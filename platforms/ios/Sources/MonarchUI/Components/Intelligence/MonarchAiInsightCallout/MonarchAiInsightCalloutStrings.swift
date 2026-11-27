/// Built-in strings of the insight callout, from the `MonarchAiInsightCallout` strings table.
enum MonarchAiInsightCalloutStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiInsightCallout." + key, table: "MonarchAiInsightCallout")
    }

    /// The origin label of a model-made insight.
    static var ai: String { string("ai") }
    /// The origin label of a rule-made insight.
    static var rule: String { string("rule") }
    /// The title of the evidence disclosure.
    static var evidence: String { string("evidence") }
    /// The spoken name of the dismiss button.
    static var dismiss: String { string("dismiss") }
    /// Joins the spoken tone name and the title. %1$@ is the tone, %2$@ the title.
    static func titled(tone: String, title: String) -> String {
        MonarchStrings.string("monarch.aiInsightCallout.titled", table: "MonarchAiInsightCallout", tone, title)
    }
}

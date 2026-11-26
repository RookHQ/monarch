/// Built-in strings of the AI onboarding tour, from the `MonarchAiOnboardingTour` strings table.
enum MonarchAiOnboardingTourStrings {
    private static let table = "MonarchAiOnboardingTour"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiOnboardingTour." + key, table: table)
    }

    /// The eyebrow.
    static var eyebrow: String { string("eyebrow") }
    /// The step position.
    static func stepOf(_ step: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.aiOnboardingTour.stepOf", table: table, step, total)
    }
    /// The back button.
    static var back: String { string("back") }
    /// The next button.
    static var next: String { string("next") }
    /// The last step's button.
    static var finish: String { string("finish") }
    /// The close button.
    static var skip: String { string("skip") }
    /// The opt-out.
    static var disable: String { string("disable") }
}

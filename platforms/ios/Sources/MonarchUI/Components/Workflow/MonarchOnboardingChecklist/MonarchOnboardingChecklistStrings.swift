/// Built-in strings of the onboarding checklist, from the
/// `MonarchOnboardingChecklist` strings table.
enum MonarchOnboardingChecklistStrings {
    private static let table = "MonarchOnboardingChecklist"

    /// The progress text, such as "2 of 5 done".
    static func progress(done: Int, total: Int) -> String {
        MonarchStrings.string("monarch.onboardingChecklist.progress", table: table, done, total)
    }

    /// The spoken name of the progress.
    static var progressLabel: String {
        MonarchStrings.string("monarch.onboardingChecklist.progressLabel", table: table)
    }

    /// The spoken name of the dismiss button.
    static var dismiss: String { MonarchStrings.string("monarch.onboardingChecklist.dismiss", table: table) }
    /// The spoken value of a done step.
    static var done: String { MonarchStrings.string("monarch.onboardingChecklist.done", table: table) }
    /// The spoken value of a step that is not done.
    static var notDone: String { MonarchStrings.string("monarch.onboardingChecklist.notDone", table: table) }
    /// Spoken after the value of the current step.
    static var current: String { MonarchStrings.string("monarch.onboardingChecklist.current", table: table) }
    /// The message when every step is done.
    static var complete: String { MonarchStrings.string("monarch.onboardingChecklist.complete", table: table) }
}

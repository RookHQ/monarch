/// Replacements for the built-in strings of ``MonarchOnboardingChecklist``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchOnboardingChecklistLabels: Sendable {
    /// The visible progress text. Receives the number of done steps and the
    /// number of steps. Built in: "<done> of <total> done".
    public var progress: (@Sendable (Int, Int) -> String)?
    /// The spoken name of the progress. Built in: "Setup progress".
    public var progressLabel: String?
    /// The spoken name of the dismiss button. Built in: "Dismiss checklist".
    public var dismiss: String?
    /// The spoken value of a done step. Built in: "Done".
    public var done: String?
    /// The spoken value of a step that is not done. Built in: "Not done".
    public var notDone: String?
    /// Spoken after the value of the first step that is not done. Built in:
    /// "Current step".
    public var current: String?
    /// Shown when every step is done. Built in: "All done. You are set up."
    public var complete: String?
    /// The spoken value of the expanded checklist. Built in: "Expanded".
    public var expanded: String?
    /// The spoken value of the collapsed checklist. Built in: "Collapsed".
    public var collapsed: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        progress: (@Sendable (Int, Int) -> String)? = nil,
        progressLabel: String? = nil,
        dismiss: String? = nil,
        done: String? = nil,
        notDone: String? = nil,
        current: String? = nil,
        complete: String? = nil,
        expanded: String? = nil,
        collapsed: String? = nil
    ) {
        self.progress = progress
        self.progressLabel = progressLabel
        self.dismiss = dismiss
        self.done = done
        self.notDone = notDone
        self.current = current
        self.complete = complete
        self.expanded = expanded
        self.collapsed = collapsed
    }

    /// The progress text for a count.
    func progressText(done: Int, total: Int) -> String {
        progress?(done, total) ?? MonarchOnboardingChecklistStrings.progress(done: done, total: total)
    }

    /// The spoken name of the progress.
    var resolvedProgressLabel: String { progressLabel ?? MonarchOnboardingChecklistStrings.progressLabel }

    /// The spoken name of the dismiss button.
    var resolvedDismiss: String { dismiss ?? MonarchOnboardingChecklistStrings.dismiss }

    /// The spoken value of a done step.
    var resolvedDone: String { done ?? MonarchOnboardingChecklistStrings.done }

    /// The spoken value of a step that is not done.
    var resolvedNotDone: String { notDone ?? MonarchOnboardingChecklistStrings.notDone }

    /// Spoken after the value of the current step.
    var resolvedCurrent: String { current ?? MonarchOnboardingChecklistStrings.current }

    /// The message when every step is done.
    var resolvedComplete: String { complete ?? MonarchOnboardingChecklistStrings.complete }
}

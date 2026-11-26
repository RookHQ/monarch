/// Replacements for the built-in strings of ``MonarchAiOnboardingTour``.
/// Every value is optional; leave one out to keep the built-in English text.
public struct MonarchAiOnboardingTourLabels: Sendable {
    /// The small label above the step: "AI".
    public var eyebrow: String
    /// The position of the step: "Step 1 of 3".
    public var stepOf: @Sendable (_ step: Int, _ total: Int) -> String
    /// The back button.
    public var back: String
    /// The next button.
    public var next: String
    /// The last step's button.
    public var finish: String
    /// The spoken name of the close button, which skips the tour.
    public var skip: String
    /// The explicit opt-out, shown on every step.
    public var disable: String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - eyebrow: Replaces "AI".
    ///   - stepOf: Replaces "Step … of …".
    ///   - back: Replaces "Back".
    ///   - next: Replaces "Next".
    ///   - finish: Replaces "Got it".
    ///   - skip: Replaces "Skip introduction".
    ///   - disable: Replaces "Turn off AI".
    public init(
        eyebrow: String? = nil,
        stepOf: (@Sendable (_ step: Int, _ total: Int) -> String)? = nil,
        back: String? = nil,
        next: String? = nil,
        finish: String? = nil,
        skip: String? = nil,
        disable: String? = nil
    ) {
        self.eyebrow = eyebrow ?? MonarchAiOnboardingTourStrings.eyebrow
        self.stepOf = stepOf ?? { MonarchAiOnboardingTourStrings.stepOf($0, $1) }
        self.back = back ?? MonarchAiOnboardingTourStrings.back
        self.next = next ?? MonarchAiOnboardingTourStrings.next
        self.finish = finish ?? MonarchAiOnboardingTourStrings.finish
        self.skip = skip ?? MonarchAiOnboardingTourStrings.skip
        self.disable = disable ?? MonarchAiOnboardingTourStrings.disable
    }
}

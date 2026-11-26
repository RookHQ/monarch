import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAiOnboardingTour``.
enum MonarchAiOnboardingTourTokens {
    /// The gap between the header, the stepper, the page and the buttons.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap inside the header and between the buttons.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between a page's symbol, heading and body.
    static let pageSpacing: CGFloat = MonarchTokens.Space.space2
    /// The SF Symbol of the close button.
    static let closeSymbol = "xmark"
    /// The eyebrow and glyph colour.
    static var eyebrowRole: MonarchColorRole { \.aiStrong }

    /// The step shown: the requested one, kept inside the steps.
    static func clamped(_ step: Int, count: Int) -> Int {
        guard count > 0 else { return 0 }
        return min(max(step, 0), count - 1)
    }

    /// Whether the step is the last one.
    static func isLast(_ step: Int, count: Int) -> Bool {
        clamped(step, count: count) == max(count - 1, 0)
    }

    /// Whether the back button is shown: on every step but the first.
    static func showsBack(_ step: Int, count: Int) -> Bool {
        clamped(step, count: count) > 0
    }

    /// The title of the primary button: Next, or Got it on the last step.
    static func primaryTitle(_ step: Int, count: Int, labels: MonarchAiOnboardingTourLabels) -> String {
        isLast(step, count: count) ? labels.finish : labels.next
    }

    /// The steps for the stepper, in order.
    /// The labels are empty: the page below shows the step's title as its heading, so the stepper
    /// shows only "Step 1 of 3" and the title is not written twice.
    static func stepperSteps(_ steps: [MonarchAiOnboardingTourStep]) -> [MonarchStepperStep] {
        steps.enumerated().map { index, step in
            MonarchStepperStep(id: "\(index)-\(step.id)", label: "")
        }
    }

    /// The stepper's labels, with the tour's "Step … of …".
    static func stepperLabels(_ labels: MonarchAiOnboardingTourLabels) -> MonarchStepperLabels {
        MonarchStepperLabels(stepOf: labels.stepOf)
    }
}

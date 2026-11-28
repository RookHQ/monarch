import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchOnboardingChecklist``.
enum MonarchOnboardingChecklistTokens {
    /// The gap between the header, the progress and the steps.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the description.
    static let headingSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the progress bar and the progress text.
    static let progressSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between steps.
    static let stepSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between a step's marker and its text.
    static let markerSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between a step's title, description and action.
    static let stepTextSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below a step.
    static let stepVerticalPadding: CGFloat = MonarchTokens.Space.space2_5
    /// Padding before and after a step.
    static let stepHorizontalPadding: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of the current step's fill and of the complete message.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The diameter of a step marker at the default text size; it scales with Dynamic Type.
    static let markerSize: CGFloat = MonarchTokens.Space.space6
    /// The width of a step marker's ring, as on the web (1.5 pixels).
    static let markerBorderWidth: CGFloat = MonarchTokens.Space.px * 1.5
    /// The gap between the icon and the text of the complete message.
    static let completeIconSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding around the complete message, vertical.
    static let completeVerticalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding around the complete message, horizontal.
    static let completeHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// The SF Symbol of a done step and of the complete message.
    static let doneSymbol = "checkmark"
    /// The SF Symbol of the dismiss button.
    static let dismissSymbol = "xmark"

    /// How many steps are done.
    static func doneCount(_ steps: [MonarchOnboardingStep]) -> Int {
        steps.filter(\.isDone).count
    }

    /// Whether every step is done. An empty checklist is not complete.
    static func isComplete(_ steps: [MonarchOnboardingStep]) -> Bool {
        !steps.isEmpty && steps.allSatisfy(\.isDone)
    }

    /// The index of the first step that is not done, or `nil`.
    static func currentIndex(_ steps: [MonarchOnboardingStep]) -> Int? {
        steps.firstIndex { !$0.isDone }
    }

    /// The total the progress bar measures against: never zero.
    static func progressTotal(_ steps: [MonarchOnboardingStep]) -> Double {
        Double(max(steps.count, 1))
    }

    /// The spoken label of the progress row when it is the collapse button:
    /// the progress name and the progress text.
    static func progressAccessibilityLabel(name: String, text: String) -> String {
        "\(name), \(text)"
    }

    /// The spoken value of a step: done or not done, and "Current step" for
    /// the first step that is not done.
    static func stepAccessibilityValue(
        _ step: MonarchOnboardingStep,
        isCurrent: Bool,
        labels: MonarchOnboardingChecklistLabels
    ) -> String {
        if step.isDone { return labels.resolvedDone }
        return isCurrent ? "\(labels.resolvedNotDone), \(labels.resolvedCurrent)" : labels.resolvedNotDone
    }

    /// The number shown in the marker of a step that is not done. `index` starts at zero.
    static func markerNumber(_ index: Int) -> String {
        String(index + 1)
    }

    /// The colour of a step title.
    static func titleForeground(isDone: Bool) -> MonarchColorRole {
        isDone ? \.textMuted : \.textDefault
    }

    /// The marker's ring colour.
    static func markerBorder(isDone: Bool, isCurrent: Bool) -> MonarchColorRole {
        if isDone { return \.successSolid }
        return isCurrent ? \.accentStrong : \.borderStrong
    }

    /// The marker's fill, or `nil` for none.
    static func markerFill(isDone: Bool) -> MonarchColorRole? {
        isDone ? \.successSolid : nil
    }

    /// The colour of the marker's number or check.
    static func markerForeground(isDone: Bool, isCurrent: Bool) -> MonarchColorRole {
        if isDone { return \.successOnSolid }
        return isCurrent ? \.accentStrong : \.textMuted
    }

    /// The fill behind a step: the current step stands out.
    static func stepBackground(isCurrent: Bool) -> MonarchColorRole? {
        isCurrent ? \.bgSubtle : nil
    }

    /// The fill of the complete message.
    static var completeBackground: MonarchColorRole { \.successSubtle }
    /// The colour of the complete message.
    static var completeForeground: MonarchColorRole { \.successStrong }
}

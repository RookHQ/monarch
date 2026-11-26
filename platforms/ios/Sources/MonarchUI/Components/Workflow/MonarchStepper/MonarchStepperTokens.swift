import MonarchTokens
import SwiftUI

/// The state a step is drawn in.
enum MonarchStepperStatus: Hashable, CaseIterable {
    /// The step is finished.
    case complete
    /// The step the user is on.
    case current
    /// A step that has not been reached.
    case upcoming
    /// The step needs attention.
    case error
}

/// The form a stepper is laid out in.
enum MonarchStepperForm: Hashable {
    /// The row of steps.
    case row
    /// The column of steps.
    case column
    /// The compact summary with a progress bar.
    case compact
    /// The row if it fits the width, the compact summary otherwise.
    case rowOrCompact
}

/// Component tokens and pure logic for ``MonarchStepper``.
enum MonarchStepperTokens {
    /// The diameter of the step marker before Dynamic Type is applied.
    static let markerSize: CGFloat = MonarchTokens.Control.xs
    /// The width of the border of the marker.
    static let markerBorderWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the ring around the marker of the current step.
    static let ringWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// Padding around a step, which is also the room for its pressed state.
    static let itemPadding: CGFloat = MonarchTokens.Space.space1
    /// The gap between the marker and the text of a step.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the name, the description and the optional note.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between steps and the lines that join them, in a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The thickness of the line that joins two steps.
    static let connectorThickness: CGFloat = MonarchTokens.Space.space0_5
    /// The smallest length of the line that joins two steps.
    static let connectorLength: CGFloat = MonarchTokens.Space.space4
    /// The space above and below the line that joins two steps in a column.
    static let connectorMargin: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the summary and the bar of the compact form, and
    /// between the position and the name in the summary.
    static let compactSpacing: CGFloat = MonarchTokens.Space.space2
    /// The type role of the name of a step.
    static let labelStyle: MonarchTextStyle = .label
    /// The type role of the description, the optional note, the number in
    /// the marker and the position in the compact form.
    static let detailStyle: MonarchTextStyle = .caption
    /// The weight of the number in the marker and of the current name.
    static let strongWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The weight of the other names and of the position in the compact form.
    static let regularWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.medium)
    /// What separates the parts of spoken text. VoiceOver pauses at it.
    static let spokenSeparator = ", "

    // MARK: Position and state

    /// An index kept inside the steps: 0 for an empty list.
    static func clamped(_ index: Int, count: Int) -> Int {
        max(0, min(index, count - 1))
    }

    /// The index of the step with an identifier, or the fallback when no
    /// step has it.
    static func index(of id: String?, in steps: [MonarchStepperStep], fallback: Int) -> Int {
        guard let id, let index = steps.firstIndex(where: { $0.id == id }) else { return fallback }
        return index
    }

    /// The furthest step the user has reached: never before the current step.
    static func furthestIndex(_ furthest: Int?, currentIndex: Int, count: Int) -> Int {
        max(currentIndex, clamped(furthest ?? currentIndex, count: count))
    }

    /// The state of a step, from its position and its own flags. An error
    /// comes first, then the current step; a step before the current one,
    /// or one marked complete, is complete.
    static func status(index: Int, currentIndex: Int, step: MonarchStepperStep) -> MonarchStepperStatus {
        if step.hasError { return .error }
        if index == currentIndex { return .current }
        if step.isComplete || index < currentIndex { return .complete }
        return .upcoming
    }

    /// Whether a step is a button: there is an action, the step is not the
    /// current one and not disabled, and it has been reached or is complete.
    static func isSelectable(
        index: Int,
        currentIndex: Int,
        furthestIndex: Int,
        step: MonarchStepperStep,
        hasAction: Bool
    ) -> Bool {
        guard hasAction, !step.isDisabled, index != currentIndex else { return false }
        return index <= furthestIndex || status(index: index, currentIndex: currentIndex, step: step) == .complete
    }

    /// The form to lay the stepper out in.
    static func form(orientation: MonarchStepperOrientation, compact: MonarchStepperCompactMode) -> MonarchStepperForm {
        switch (compact, orientation) {
        case (.always, _): .compact
        case (.automatic, .horizontal): .rowOrCompact
        case (.never, .horizontal): .row
        case (.automatic, .vertical), (.never, .vertical): .column
        }
    }

    // MARK: Appearance

    /// The colour roles of the marker of a step.
    static func marker(for status: MonarchStepperStatus) -> MonarchSurfaceAppearance {
        switch status {
        case .upcoming:
            MonarchSurfaceAppearance(background: \.bgSurface, foreground: \.textMuted, border: \.borderStrong)
        case .current:
            MonarchSurfaceAppearance(background: \.bgSurface, foreground: \.accentStrong, border: \.accentSolid)
        case .complete:
            MonarchSurfaceAppearance(background: \.accentSolid, foreground: \.accentOnSolid, border: \.accentSolid)
        case .error:
            MonarchSurfaceAppearance(background: \.dangerSubtle, foreground: \.dangerStrong, border: \.dangerBorder)
        }
    }

    /// The role of the ring around the marker: only the current step has one.
    static func ring(for status: MonarchStepperStatus) -> MonarchColorRole? {
        status == .current ? \.accentSubtle : nil
    }

    /// The SF Symbol in the marker, or `nil` for the number of the step.
    static func symbol(for status: MonarchStepperStatus) -> String? {
        switch status {
        case .complete: "checkmark"
        case .error: "exclamationmark"
        case .current, .upcoming: nil
        }
    }

    /// The role of the name of a step. A disabled step that would
    /// otherwise be a button is drawn in the disabled colour.
    static func labelColor(for status: MonarchStepperStatus, isUnavailable: Bool) -> MonarchColorRole {
        if isUnavailable { return \.textDisabled }
        return switch status {
        case .current, .complete: \.textDefault
        case .upcoming: \.textMuted
        case .error: \.dangerStrong
        }
    }

    /// The weight of the name of a step: the current one is heavier.
    static func labelWeight(for status: MonarchStepperStatus) -> Font.Weight {
        status == .current ? strongWeight : regularWeight
    }

    /// The role of the line after a step: the accent after a finished step.
    static func connector(after status: MonarchStepperStatus) -> MonarchColorRole {
        status == .complete ? \.accentSolid : \.borderDefault
    }

    // MARK: Text

    /// The position of a step, such as "Step 2 of 5". `position` counts
    /// from 1.
    static func stepOf(_ position: Int, total: Int, labels: MonarchStepperLabels) -> String {
        labels.stepOf?(position, total) ?? MonarchStepperStrings.stepOf(position, total)
    }

    /// The number shown in the marker, in the digits of the locale.
    static func markerText(position: Int, locale: Locale = .autoupdatingCurrent) -> String {
        position.formatted(.number.locale(locale))
    }

    /// The share of the steps that the compact bar fills: the current step
    /// counts as reached. 0 without steps.
    static func fraction(currentIndex: Int, count: Int) -> Double {
        guard count > 0 else { return 0 }
        return Double(clamped(currentIndex, count: count) + 1) / Double(count)
    }

    /// What VoiceOver reads as the label of a step: its position and name.
    static func stepLabel(position: Int, total: Int, label: String, labels: MonarchStepperLabels) -> String {
        [stepOf(position, total: total, labels: labels), label]
            .filter { !$0.isEmpty }
            .joined(separator: spokenSeparator)
    }

    /// What VoiceOver reads as the value of a step: its state, the
    /// optional note and the description. An upcoming step has no state
    /// to read.
    static func stepValue(
        status: MonarchStepperStatus,
        step: MonarchStepperStep,
        isBusy: Bool = false,
        labels: MonarchStepperLabels
    ) -> String {
        let state: String? =
            switch status {
            case .complete: labels.complete ?? MonarchStepperStrings.complete
            case .error: labels.error ?? MonarchStepperStrings.error
            case .current:
                isBusy
                    ? [labels.current ?? MonarchStepperStrings.current, labels.busy ?? MonarchStepperStrings.busy]
                        .joined(separator: spokenSeparator)
                    : labels.current ?? MonarchStepperStrings.current
            case .upcoming: nil
            }
        let optional = step.isOptional ? labels.optional ?? MonarchStepperStrings.optional : nil
        return [state, optional, step.description]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: spokenSeparator)
    }

    /// What VoiceOver reads as the value of the compact form: the position
    /// and the name of the current step.
    static func compactValue(
        currentIndex: Int,
        steps: [MonarchStepperStep],
        isBusy: Bool = false,
        labels: MonarchStepperLabels
    ) -> String {
        guard !steps.isEmpty else { return "" }
        // Past the last step every step is done: the summary names the last one as completed.
        let isFinished = currentIndex >= steps.count
        let shown = min(max(0, currentIndex), steps.count - 1)
        let position = stepLabel(position: shown + 1, total: steps.count, label: steps[shown].label, labels: labels)
        let state =
            isFinished
            ? labels.complete ?? MonarchStepperStrings.complete
            : isBusy ? labels.busy ?? MonarchStepperStrings.busy : nil
        return [position, state].compactMap { $0 }.joined(separator: spokenSeparator)
    }
}

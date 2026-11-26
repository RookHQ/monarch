import SwiftUI

/// Multi-step progress for wizards, onboarding and setup flows.
///
/// ```swift
/// MonarchStepper(steps, current: 1)
/// MonarchStepper(steps, current: 1, furthest: 2, orientation: .vertical) { step, _ in
///     show(step)
/// }
/// ```
///
/// Steps before the current one are finished. With an action, the steps
/// the user has reached become buttons, so they can go back. A horizontal
/// stepper that does not fit the width shows "Step 2 of 5", the name of
/// the step and a progress bar instead.
///
/// Accessibility: the stepper is a container named "Progress". Each step
/// is one element read as its position and name ("Step 2 of 5, Address")
/// with its state as the value ("current step", "completed", "needs
/// attention"); a step that can be selected is a button with a 44-point
/// touch target. The state is also a glyph and a weight, not colour alone.
/// The compact form is one element with the position and name as its value.
public struct MonarchStepper: View {
    private let steps: [MonarchStepperStep]
    private let currentIndex: Int
    private let furthestIndex: Int
    private let orientation: MonarchStepperOrientation
    private let compact: MonarchStepperCompactMode
    private let isFinished: Bool
    private let isBusy: Bool
    private let labels: MonarchStepperLabels
    private let onSelect: (@MainActor (MonarchStepperStep, Int) -> Void)?

    /// A stepper whose current step is given by its position.
    ///
    /// - Parameters:
    ///   - steps: The steps, in order.
    ///   - current: The position of the current step, from 0. It is kept
    ///     inside the steps.
    ///   - furthest: The position of the furthest step the user has
    ///     reached, when they have gone back. Steps up to it can be
    ///     selected. By default the current step.
    ///   - orientation: `horizontal` or `vertical`.
    ///   - compact: When the compact form is shown.
    ///   - isFinished: Every step is done: all markers show the check mark (a step with an error keeps
    ///     its own), no step is current, and the compact form reads the last step as completed with a full bar.
    ///   - isBusy: The current step is working, for example while it saves: its marker shows a progress
    ///     indicator in place of its number and VoiceOver adds "in progress". Ignored when `isFinished`.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSelect: Makes the reached steps buttons. Receives the step
    ///     and its position.
    public init(
        _ steps: [MonarchStepperStep],
        current: Int,
        furthest: Int? = nil,
        orientation: MonarchStepperOrientation = .horizontal,
        compact: MonarchStepperCompactMode = .automatic,
        isFinished: Bool = false,
        isBusy: Bool = false,
        labels: MonarchStepperLabels = MonarchStepperLabels(),
        onSelect: (@MainActor (MonarchStepperStep, Int) -> Void)? = nil
    ) {
        let currentIndex =
            isFinished ? steps.count : MonarchStepperTokens.clamped(current, count: steps.count)
        self.steps = steps
        self.currentIndex = currentIndex
        self.furthestIndex = MonarchStepperTokens.furthestIndex(
            furthest,
            currentIndex: currentIndex,
            count: steps.count
        )
        self.orientation = orientation
        self.compact = compact
        self.isFinished = isFinished
        self.isBusy = isBusy && !isFinished
        self.labels = labels
        self.onSelect = onSelect
    }

    /// A stepper whose current step is given by its identifier.
    ///
    /// - Parameters:
    ///   - steps: The steps, in order.
    ///   - currentID: The identifier of the current step. The first step
    ///     is current when no step has it.
    ///   - furthestID: The identifier of the furthest step the user has
    ///     reached. By default the current step.
    ///   - orientation: `horizontal` or `vertical`.
    ///   - compact: When the compact form is shown.
    ///   - isFinished: Every step is done. See the initializer that takes a position.
    ///   - isBusy: The current step is working. See the initializer that takes a position.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSelect: Makes the reached steps buttons. Receives the step
    ///     and its position.
    public init(
        _ steps: [MonarchStepperStep],
        currentID: String,
        furthestID: String? = nil,
        orientation: MonarchStepperOrientation = .horizontal,
        compact: MonarchStepperCompactMode = .automatic,
        isFinished: Bool = false,
        isBusy: Bool = false,
        labels: MonarchStepperLabels = MonarchStepperLabels(),
        onSelect: (@MainActor (MonarchStepperStep, Int) -> Void)? = nil
    ) {
        let current = MonarchStepperTokens.index(of: currentID, in: steps, fallback: 0)
        self.init(
            steps,
            current: current,
            furthest: MonarchStepperTokens.index(of: furthestID, in: steps, fallback: current),
            orientation: orientation,
            compact: compact,
            isFinished: isFinished,
            isBusy: isBusy,
            labels: labels,
            onSelect: onSelect
        )
    }

    /// The content of the view.
    public var body: some View {
        if !steps.isEmpty {
            switch MonarchStepperTokens.form(orientation: orientation, compact: compact) {
            case .row:
                // Never the compact summary, but a narrow column still shows whole names: the names under their
                // markers when beside them they do not fit.
                ViewThatFits(in: .horizontal) {
                    list(MonarchStepperRow(items: items).fixedSize(horizontal: true, vertical: false))
                    list(MonarchStepperRow(items: items, isStacked: true))
                }
            case .column:
                list(MonarchStepperColumn(items: items))
            case .compact:
                compactForm
            case .rowOrCompact:
                ViewThatFits(in: .horizontal) {
                    list(MonarchStepperRow(items: items))
                    list(MonarchStepperRow(items: items, isStacked: true))
                    compactForm
                }
            }
        }
    }

    private func list(_ content: some View) -> some View {
        content
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.progress ?? MonarchStepperStrings.progress)
    }

    private var compactForm: some View {
        MonarchStepperCompact(steps: steps, currentIndex: currentIndex, isBusy: isBusy, labels: labels)
    }

    private var items: [MonarchStepperItem] {
        steps.indices.map { index in
            let step = steps[index]
            let isSelectable = MonarchStepperTokens.isSelectable(
                index: index,
                currentIndex: currentIndex,
                furthestIndex: furthestIndex,
                step: step,
                hasAction: onSelect != nil
            )
            return MonarchStepperItem(
                step: step,
                index: index,
                total: steps.count,
                status: MonarchStepperTokens.status(index: index, currentIndex: currentIndex, step: step),
                isSelectable: isSelectable,
                isUnavailable: step.isDisabled && onSelect != nil,
                isBusy: isBusy && index == currentIndex,
                labels: labels,
                onSelect: onSelect
            )
        }
    }
}

#if DEBUG
    #Preview("MonarchStepper") {
        MonarchPreviewMatrix {
            let steps = [
                MonarchStepperStep(id: "details", label: "Details"),
                MonarchStepperStep(id: "address", label: "Address", description: "Where we send invoices"),
                MonarchStepperStep(id: "team", label: "Team", isOptional: true),
                MonarchStepperStep(id: "review", label: "Review", hasError: true),
            ]
            VStack(alignment: .leading) {
                MonarchStepper(steps, current: 1, compact: .never)
                MonarchStepper(steps, current: 1, compact: .always)
                MonarchStepper(steps, current: 1, furthest: 2, orientation: .vertical) { _, _ in }
            }
        }
    }
#endif

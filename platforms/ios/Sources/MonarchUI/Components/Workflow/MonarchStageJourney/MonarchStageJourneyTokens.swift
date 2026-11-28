import Foundation
import MonarchTokens
import SwiftUI

/// Where a stage is relative to the current one.
enum MonarchStageJourneyStatus: Sendable, Hashable {
    case done
    case current
    case upcoming
}

/// Consecutive stages that share a group.
struct MonarchStageJourneyGroup: Equatable {
    /// The group's name, or `nil` for stages without one.
    let name: String?
    /// The positions of the group's stages in the journey.
    let indices: [Int]
}

/// Component tokens and pure logic for ``MonarchStageJourney`` and
/// ``MonarchStageHistory``.
enum MonarchStageJourneyTokens {
    /// The space between the strip, the card and the moves.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The space between groups of the strip.
    static let groupSpacing: CGFloat = MonarchTokens.Space.space4
    /// The space between a group's name and its stages.
    static let groupLabelSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The space between stages of a group.
    static let stageSpacing: CGFloat = MonarchTokens.Space.space1
    /// The padding before and after a stage's name.
    static let stagePadding: CGFloat = MonarchTokens.Space.space2_5
    /// The smallest height of a stage.
    static let stageMinHeight: CGFloat = MonarchTokens.Control.xs
    /// The space between the parts of the card.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space2
    /// The padding above and below the card's content.
    static let cardVerticalPadding: CGFloat = MonarchTokens.Space.space3
    /// The padding before and after the card's content.
    static let cardHorizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// The corner radius of the card.
    static let cardCornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The space between the move buttons.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// The space between the entries of a history.
    static let historySpacing: CGFloat = MonarchTokens.Space.space4
    /// The space between the lines of a history entry.
    static let historyLineSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the column of dots in a history.
    static let historyDotColumn: CGFloat = MonarchTokens.Space.space5
    /// The size of a history dot.
    static let historyDotSize: CGFloat = MonarchTokens.Space.space2_5
    /// How far the history line is from the leading edge: under the
    /// middle of the dots.
    static let historyLineInset: CGFloat = (historyDotSize - MonarchTokens.Space.px) / 2
    /// What separates the parts of spoken text.
    static let spokenSeparator = ", "
    /// What separates the actor and the facts of a history entry.
    static let metaSeparator = " · "

    /// The shape of the card.
    static var cardShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: cardCornerRadius, style: .continuous)
    }

    // MARK: Stages

    /// The position of the current stage, or `nil` when no stage has the id.
    static func currentIndex(of stages: [MonarchStageJourneyStage], currentID: String) -> Int? {
        stages.firstIndex { $0.id == currentID }
    }

    /// Where a stage is: before the current one is done, after it upcoming.
    /// Without a current stage every stage is upcoming.
    static func status(at index: Int, currentIndex: Int?) -> MonarchStageJourneyStatus {
        guard let currentIndex else { return .upcoming }
        if index < currentIndex { return .done }
        return index == currentIndex ? .current : .upcoming
    }

    /// The stages in runs of the same group, in order.
    static func groups(_ stages: [MonarchStageJourneyStage]) -> [MonarchStageJourneyGroup] {
        var groups: [MonarchStageJourneyGroup] = []
        for (index, stage) in stages.enumerated() {
            if let last = groups.last, last.name == stage.group {
                groups[groups.count - 1] = MonarchStageJourneyGroup(name: last.name, indices: last.indices + [index])
            } else {
                groups.append(MonarchStageJourneyGroup(name: stage.group, indices: [index]))
            }
        }
        return groups
    }

    /// The colour roles of a stage: accent tint when done, solid accent when
    /// current, solid success or emphasis when the journey ended there,
    /// outlined and muted when upcoming.
    static func stageAppearance(
        status: MonarchStageJourneyStatus,
        terminal: MonarchStageJourneyTerminal?
    ) -> MonarchSurfaceAppearance {
        switch (status, terminal) {
        case (.done, _):
            MonarchSurfaceAppearance(background: \.accentSubtle, foreground: \.textDefault, border: \.accentBorder)
        case (.current, .success):
            MonarchSurfaceAppearance(background: \.successSolid, foreground: \.successOnSolid, border: \.successSolid)
        case (.current, .closed):
            MonarchSurfaceAppearance(background: \.bgEmphasis, foreground: \.textDefault, border: \.borderStrong)
        case (.current, nil):
            MonarchSurfaceAppearance(background: \.accentSolid, foreground: \.accentOnSolid, border: \.accentSolid)
        case (.upcoming, _):
            MonarchSurfaceAppearance(background: nil, foreground: \.textMuted, border: \.borderDefault)
        }
    }

    /// The SF Symbol before a stage's name: a checkmark when done, an
    /// outcome mark for the stage that ended the journey.
    static func stageSymbol(status: MonarchStageJourneyStatus, terminal: MonarchStageJourneyTerminal?) -> String? {
        switch status {
        case .done: "checkmark"
        case .current: terminal.map(outcomeSymbol)
        case .upcoming: nil
        }
    }

    /// The colour role of the done checkmark.
    static var doneSymbol: MonarchColorRole { \.accentStrong }

    /// The weight of a stage's name: medium for the current stage.
    static func stageWeight(status: MonarchStageJourneyStatus) -> Font.Weight {
        MonarchTextStyleSpec.fontWeight(
            status == .current ? MonarchTokens.FontWeight.medium : MonarchTokens.FontWeight.regular
        )
    }

    // MARK: Card

    /// The colour roles of the current-stage card.
    static func cardAppearance(terminal: MonarchStageJourneyTerminal?) -> MonarchSurfaceAppearance {
        switch terminal {
        case .success:
            MonarchSurfaceAppearance(background: \.successSubtle, foreground: \.textDefault, border: \.successBorder)
        case .closed:
            MonarchSurfaceAppearance(background: \.bgSubtle, foreground: \.textDefault, border: \.borderSubtle)
        case nil:
            MonarchSurfaceAppearance(background: \.bgSurface, foreground: \.textDefault, border: \.borderSubtle)
        }
    }

    /// The SF Symbol of an outcome.
    static func outcomeSymbol(_ terminal: MonarchStageJourneyTerminal) -> String {
        switch terminal {
        case .success: "checkmark.circle.fill"
        case .closed: "xmark.circle"
        }
    }

    /// The colour role of the outcome symbol in the card.
    static func outcomeForeground(_ terminal: MonarchStageJourneyTerminal) -> MonarchColorRole {
        terminal == .success ? \.successStrong : \.textMuted
    }

    /// The name of an outcome.
    static func outcomeName(_ terminal: MonarchStageJourneyTerminal, labels: MonarchStageJourneyLabels) -> String {
        switch terminal {
        case .success: MonarchStageJourneyStrings.success(labels)
        case .closed: MonarchStageJourneyStrings.closed(labels)
        }
    }

    /// A relative time in words, such as "3 days ago", from `now`.
    static func relativeText(_ date: Date, now: Date, locale: Locale = .autoupdatingCurrent) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    // MARK: Moves

    /// The moves in the order they are shown: next steps, then early exits,
    /// then the one step back.
    static func orderedMoves(_ transitions: [MonarchStageJourneyTransition]) -> [MonarchStageJourneyTransition] {
        let forward = transitions.filter { $0.kind == .forward }
        let exits = transitions.filter { $0.kind == .exit }
        let back = transitions.first { $0.kind == .back }
        return forward + exits + (back.map { [$0] } ?? [])
    }

    /// The button variant of a move: the first next step is primary, other
    /// next steps secondary, exits and the step back ghost.
    static func buttonVariant(
        for transition: MonarchStageJourneyTransition,
        in transitions: [MonarchStageJourneyTransition]
    ) -> MonarchButtonVariant {
        switch transition.kind {
        case .forward:
            transitions.first { $0.kind == .forward }?.id == transition.id ? .primary : .secondary
        case .exit, .back:
            .ghost
        }
    }

    /// The visible title of a move: the stage, or "Back to …" for a step back.
    static func buttonTitle(for transition: MonarchStageJourneyTransition, labels: MonarchStageJourneyLabels) -> String
    {
        transition.kind == .back
            ? MonarchStageJourneyStrings.backTo(transition.label, labels) : transition.label
    }

    /// The SF Symbol of a move: an undo arrow for a step back.
    static func buttonSymbol(for transition: MonarchStageJourneyTransition) -> String? {
        transition.kind == .back ? "arrow.uturn.backward" : nil
    }

    /// Whether a move's button can be pressed: it is not disabled and no
    /// other move is in progress.
    static func isEnabled(_ transition: MonarchStageJourneyTransition, busyID: String?) -> Bool {
        !transition.isDisabled && (busyID == nil || busyID == transition.id)
    }

    /// The message of a failed move: the error's localized description, or
    /// the built-in message.
    static func failureMessage(for error: any Error, labels: MonarchStageJourneyLabels) -> String {
        MonarchFailureMessage.message(for: error, fallback: MonarchStageJourneyStrings.failed(labels))
    }

    // MARK: Accessibility

    /// What VoiceOver reads as the value of a stage: "done", "current
    /// stage" and the outcome, or nothing for an upcoming stage.
    static func stageAccessibilityValue(
        status: MonarchStageJourneyStatus,
        terminal: MonarchStageJourneyTerminal?,
        labels: MonarchStageJourneyLabels
    ) -> String {
        switch status {
        case .done:
            return MonarchStageJourneyStrings.done(labels)
        case .current:
            let outcome = terminal.map { outcomeName($0, labels: labels) }
            return [MonarchStageJourneyStrings.current(labels), outcome]
                .compactMap { $0 }
                .joined(separator: spokenSeparator)
        case .upcoming:
            return ""
        }
    }

    /// What VoiceOver reads for a move: "Move to Granted" or "Back to
    /// Filed", and a note when the move asks for details.
    static func moveAccessibilityLabel(
        _ transition: MonarchStageJourneyTransition,
        labels: MonarchStageJourneyLabels
    ) -> String {
        let name =
            transition.kind == .back
            ? MonarchStageJourneyStrings.backTo(transition.label, labels)
            : MonarchStageJourneyStrings.moveTo(transition.label, labels)
        guard transition.needsInput else { return name }
        return name + spokenSeparator + MonarchStageJourneyStrings.needsInput(labels)
    }

    /// What VoiceOver reads for the card's heading: "Now: Granted" and the
    /// outcome.
    static func titleAccessibilityLabel(
        stage: String,
        terminal: MonarchStageJourneyTerminal?,
        labels: MonarchStageJourneyLabels
    ) -> String {
        [MonarchStageJourneyStrings.now(stage, labels), terminal.map { outcomeName($0, labels: labels) }]
            .compactMap { $0 }
            .joined(separator: spokenSeparator)
    }

    // MARK: History

    /// The colour role of a history dot: accent, or subtle for a move back.
    static func historyDot(isBackward: Bool) -> MonarchColorRole {
        isBackward ? \.textSubtle : \.accentSolid
    }

    /// The facts line of a history entry: the time, the actor and the facts.
    static func historyMetaText(_ entry: MonarchStageHistoryEntry, time: String) -> String {
        [time, entry.actor, entry.meta]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: metaSeparator)
    }

    /// What VoiceOver reads for a history entry: the move in words, a note
    /// for a move back, the time, the actor, the facts and the note.
    static func historyAccessibilityLabel(
        _ entry: MonarchStageHistoryEntry,
        time: String,
        labels: MonarchStageHistoryLabels
    ) -> String {
        let move =
            entry.from.map { "\($0) \(MonarchStageHistoryStrings.to(labels)) \(entry.to)" }
            ?? MonarchStageHistoryStrings.started(entry.to, labels)
        let back = entry.isBackward ? MonarchStageHistoryStrings.movedBack(labels) : nil
        return [move, back, time, entry.actor, entry.meta, entry.note]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: spokenSeparator)
    }
}

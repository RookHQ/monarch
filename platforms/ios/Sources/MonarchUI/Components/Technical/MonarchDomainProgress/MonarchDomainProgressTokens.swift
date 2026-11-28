import Foundation
import MonarchTokens
import SwiftUI

/// One step with its resolved state and text.
struct MonarchDomainProgressItem: Equatable, Identifiable {
    let step: MonarchDomainStep
    let state: MonarchDomainStepState
    let title: String
    let detail: String

    var id: MonarchDomainStep { step }
}

/// Component tokens and pure logic for ``MonarchDomainProgress``.
enum MonarchDomainProgressTokens {
    /// The gap between the steps, the problems and the status lines.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The space after each step.
    static let stepSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between the marker and the text of a step.
    static let markerSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the title and the detail, and between problems.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The side of the marker before Dynamic Type.
    static let markerSide: CGFloat = MonarchTokens.Space.space6
    /// The width of the marker border and of the line that joins the markers.
    static let lineWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The side of the dot inside the still marker of the current step.
    static let stillDotSide: CGFloat = MonarchTokens.Space.space2
    /// What separates parts of spoken text.
    static let separator = ", "
    /// The arrow between a record type and its value in the DNS line.
    static let arrow = " → "
    /// The separator between record types in the DNS line.
    static let typeSeparator = " · "
    /// The separator between addresses in the DNS line.
    static let listSeparator = ", "
    /// The shortest suggested time between checks.
    static let shortestRecheck: Duration = .seconds(15)
    /// The longest suggested time between checks.
    static let longestRecheck: Duration = .seconds(300)
    /// The time between checks when the product suggests none.
    static let defaultRecheck: Duration = .seconds(30)
    /// The time between checks before the first stage is known.
    static let unknownStageRecheck: Duration = .seconds(15)

    /// The steps in order.
    static let steps: [MonarchDomainStep] = [.dns, .certificate, .live]

    /// The state of a step for a stage, before problems.
    static func baseState(_ step: MonarchDomainStep, stage: MonarchDomainStage) -> MonarchDomainStepState {
        let dnsDone = stage != .dns && stage != .ownership
        let certificateDone = stage == .page || stage == .live
        switch step {
        case .dns: return dnsDone ? .done : .current
        case .certificate: return certificateDone ? .done : (dnsDone ? .current : .todo)
        case .live: return stage == .live ? .done : (stage == .page ? .current : .todo)
        }
    }

    /// The state of each step for a stage and a list of problems. A
    /// blocking problem marks its step (the current one by default) as
    /// needing attention, unless the step is done.
    static func states(
        stage: MonarchDomainStage,
        problems: [MonarchDomainProblem]
    ) -> [MonarchDomainStep: MonarchDomainStepState] {
        var states: [MonarchDomainStep: MonarchDomainStepState] = [:]
        for step in steps { states[step] = baseState(step, stage: stage) }
        let current = steps.first { states[$0] == .current }
        for problem in problems where problem.severity == .blocking {
            guard let step = problem.step ?? current, states[step] != .done else { continue }
            states[step] = .error
        }
        return states
    }

    /// The three steps with their states and text.
    static func items(
        host: String,
        stage: MonarchDomainStage,
        problems: [MonarchDomainProblem],
        labels: MonarchDomainProgressLabels
    ) -> [MonarchDomainProgressItem] {
        let states = states(stage: stage, problems: problems)
        return steps.map { step in
            let state = states[step] ?? .todo
            return MonarchDomainProgressItem(
                step: step,
                state: state,
                title: labels.titleText(step, state: state, stage: stage),
                detail: labels.detailText(step, state: state, host: host)
            )
        }
    }

    /// The position of the step to announce: the first in progress or
    /// needing attention, else the last.
    static func activeIndex(_ items: [MonarchDomainProgressItem]) -> Int {
        items.firstIndex { $0.state == .current || $0.state == .error } ?? max(0, items.count - 1)
    }

    /// The announcement of the active step.
    static func announcement(_ items: [MonarchDomainProgressItem], labels: MonarchDomainProgressLabels) -> String {
        guard !items.isEmpty else { return "" }
        let index = activeIndex(items)
        let item = items[index]
        return labels.announcementText(
            position: index + 1,
            count: items.count,
            title: item.title,
            state: labels.stateText(item.state)
        )
    }

    /// What VoiceOver reads as the value of a step: its state, then its detail.
    static func stepValue(_ item: MonarchDomainProgressItem, labels: MonarchDomainProgressLabels) -> String {
        [labels.stateText(item.state), item.detail].filter { !$0.isEmpty }.joined(separator: separator)
    }

    /// The blocking problems, or else the first informational one.
    static func shownProblems(_ problems: [MonarchDomainProblem]) -> [MonarchDomainProblem] {
        let blocking = problems.filter { $0.severity == .blocking }
        if !blocking.isEmpty { return blocking }
        return problems.first { $0.severity == .info }.map { [$0] } ?? []
    }

    /// The tone of a shown problem: warning when blocking, neutral otherwise.
    static func tone(_ problem: MonarchDomainProblem) -> MonarchTone {
        problem.severity == .blocking ? .warning : .neutral
    }

    /// A DNS snapshot as one line: the first CNAME, or the A and AAAA
    /// addresses, or `empty`.
    static func snapshotText(_ snapshot: MonarchDomainDnsSnapshot, empty: String) -> String {
        if let cname = snapshot.cnames.first { return "CNAME" + arrow + cname }
        if !snapshot.a.isEmpty {
            let v4 = "A" + arrow + snapshot.a.joined(separator: listSeparator)
            guard !snapshot.aaaa.isEmpty else { return v4 }
            return v4 + typeSeparator + "AAAA" + arrow + snapshot.aaaa.joined(separator: listSeparator)
        }
        if !snapshot.aaaa.isEmpty { return "AAAA" + arrow + snapshot.aaaa.joined(separator: listSeparator) }
        return empty
    }

    /// The time before the next check: the suggested delay kept between 15
    /// seconds and 5 minutes (30 seconds when none), 15 seconds before the
    /// stage is known, and `nil` once live or when checking has stopped.
    static func recheckInterval(
        for stage: MonarchDomainStage?,
        suggested: Duration?,
        isStopped: Bool
    ) -> Duration? {
        guard let stage else { return unknownStageRecheck }
        if stage == .live || isStopped { return nil }
        return min(longestRecheck, max(shortestRecheck, suggested ?? defaultRecheck))
    }

    /// The time of the last check relative to `now`, such as "2 minutes ago".
    static func relativeTime(_ date: Date, now: Date, locale: Locale) -> String {
        return MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
    }

    /// The colours of the marker of a state. Each state also has its own
    /// symbol, so colour is not the only signal.
    static func marker(_ state: MonarchDomainStepState) -> MonarchSurfaceAppearance {
        switch state {
        case .done:
            MonarchSurfaceAppearance(background: \.successSolid, foreground: \.successOnSolid, border: \.successSolid)
        case .current:
            MonarchSurfaceAppearance(background: \.accentSolid, foreground: \.accentOnSolid, border: \.accentSolid)
        case .error:
            MonarchSurfaceAppearance(background: \.warningSolid, foreground: \.warningOnSolid, border: \.warningSolid)
        case .todo:
            MonarchSurfaceAppearance(background: \.bgSurface, foreground: \.textMuted, border: \.borderDefault)
        }
    }

    /// The SF Symbol in the marker. The current step shows progress instead.
    static func symbol(_ state: MonarchDomainStepState) -> String? {
        switch state {
        case .done: "checkmark"
        case .error: "exclamationmark"
        case .current, .todo: nil
        }
    }

    /// The colour of a step title: muted before it starts.
    static func titleRole(_ state: MonarchDomainStepState) -> MonarchColorRole {
        state == .todo ? \.textMuted : \.textDefault
    }

    /// The colour of the line after a step: success after a finished step.
    static func connectorRole(after state: MonarchDomainStepState) -> MonarchColorRole {
        state == .done ? \.successSolid : \.borderDefault
    }
}

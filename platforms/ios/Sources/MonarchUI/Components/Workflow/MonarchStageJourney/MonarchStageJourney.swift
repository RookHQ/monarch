import SwiftUI

/// Where an item is in a pipeline or lifecycle and what can happen next:
/// a strip of stages grouped by phase, the current stage with how long it
/// has been there, and buttons for the moves the product allows.
///
/// ```swift
/// MonarchStageJourney(
///     stages,
///     currentID: application.stage,
///     enteredAt: application.stageEnteredAt,
///     transitions: [
///         MonarchStageJourneyTransition(id: "granted", label: "Granted"),
///         MonarchStageJourneyTransition(id: "withdrawn", label: "Withdrawn", kind: .exit),
///     ],
///     onTransition: { move in try await store.move(application, to: move.id) }
/// )
/// ```
///
/// Stages before the current one are done. The product owns the current
/// stage: a move calls `onTransition`, whose button shows a spinner until
/// it returns, and the product changes `currentID`. A thrown error is shown
/// in the card and announced. For the record of past moves, see
/// ``MonarchStageHistory``.
///
/// Accessibility: the strip is a container named "Stages"; each stage is
/// read by name with "done" or "current stage" as its value, and the
/// current stage is selected. A done stage also has a checkmark, so the
/// state is not colour alone. The card's title is a heading. A move is
/// read as "Move to Granted", with a note when it asks for details. A
/// change of stage is announced.
public struct MonarchStageJourney<Content: View>: View {
    private let stages: [MonarchStageJourneyStage]
    private let currentID: String
    private let terminal: MonarchStageJourneyTerminal?
    private let enteredAt: Date?
    private let currentMeta: String?
    private let variant: MonarchStageJourneyVariant
    private let transitions: [MonarchStageJourneyTransition]
    private let now: Date?
    private let labels: MonarchStageJourneyLabels
    private let onTransition: (@MainActor (MonarchStageJourneyTransition) async throws -> Void)?
    private let content: Content

    @State private var busyID: String?
    @State private var failure: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A stage journey with extra content in the current-stage card.
    ///
    /// - Parameters:
    ///   - stages: Every stage, in pipeline order.
    ///   - currentID: The identifier of the current stage.
    ///   - terminal: Whether the current stage ends the journey, as a
    ///     success or closed.
    ///   - enteredAt: When the item entered the current stage, shown as
    ///     "since 3 days ago".
    ///   - currentMeta: A short note shown as a badge next to the current
    ///     stage, such as the phase.
    ///   - variant: `full` for the strip, the card and the moves; `strip`
    ///     for the strip only.
    ///   - transitions: The moves offered from the current stage.
    ///   - now: A fixed reference time for `enteredAt`.
    ///   - labels: Replacements for the built-in strings.
    ///   - onTransition: Called with the chosen move. Without it no moves
    ///     are shown.
    ///   - content: Extra content at the end of the card, such as a
    ///     checklist or a note that moves are locked.
    public init(
        _ stages: [MonarchStageJourneyStage],
        currentID: String,
        terminal: MonarchStageJourneyTerminal? = nil,
        enteredAt: Date? = nil,
        currentMeta: String? = nil,
        variant: MonarchStageJourneyVariant = .full,
        transitions: [MonarchStageJourneyTransition] = [],
        now: Date? = nil,
        labels: MonarchStageJourneyLabels = MonarchStageJourneyLabels(),
        onTransition: (@MainActor (MonarchStageJourneyTransition) async throws -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.stages = stages
        self.currentID = currentID
        self.terminal = terminal
        self.enteredAt = enteredAt
        self.currentMeta = currentMeta
        self.variant = variant
        self.transitions = transitions
        self.now = now
        self.labels = labels
        self.onTransition = onTransition
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        let currentIndex = MonarchStageJourneyTokens.currentIndex(of: stages, currentID: currentID)
        VStack(alignment: .leading, spacing: MonarchStageJourneyTokens.sectionSpacing) {
            MonarchStageJourneyStrip(
                stages: stages,
                currentIndex: currentIndex,
                terminal: terminal,
                labels: labels
            )
            if variant == .full, let currentIndex {
                card(stages[currentIndex])
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onChange(of: currentID) { _, _ in
            failure = nil
            if let index = MonarchStageJourneyTokens.currentIndex(of: stages, currentID: currentID) {
                AccessibilityNotification.Announcement(
                    MonarchStageJourneyStrings.moved(stages[index].label, labels)
                )
                .post()
            }
        }
    }

    private func card(_ stage: MonarchStageJourneyStage) -> some View {
        VStack(alignment: .leading, spacing: MonarchStageJourneyTokens.cardSpacing) {
            heading(stage)
            if let description = stage.description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.label, weight: .regular)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if onTransition != nil, !transitions.isEmpty {
                moves
            }
            if let failure {
                HStack(alignment: .firstTextBaseline, spacing: MonarchStageJourneyTokens.stageSpacing) {
                    MonarchIcon(systemName: "exclamationmark.circle", size: .sm)
                    Text(failure)
                }
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.dangerStrong)
                .accessibilityElement(children: .combine)
            }
            content
        }
        .padding(.vertical, MonarchStageJourneyTokens.cardVerticalPadding)
        .padding(.horizontal, MonarchStageJourneyTokens.cardHorizontalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .monarchSurface(
            MonarchStageJourneyTokens.cardAppearance(terminal: terminal),
            in: MonarchStageJourneyTokens.cardShape
        )
    }

    private func heading(_ stage: MonarchStageJourneyStage) -> some View {
        MonarchFlowLayout(spacing: MonarchStageJourneyTokens.cardSpacing) {
            if let terminal {
                MonarchIcon(systemName: MonarchStageJourneyTokens.outcomeSymbol(terminal))
                    .foregroundStyle(theme.colors[role: MonarchStageJourneyTokens.outcomeForeground(terminal)])
            }
            Text(MonarchStageJourneyStrings.now(stage.label, labels))
                .monarchTextStyle(.label, weight: .semibold)
                .accessibilityLabel(
                    MonarchStageJourneyTokens.titleAccessibilityLabel(
                        stage: stage.label,
                        terminal: terminal,
                        labels: labels
                    )
                )
                .accessibilityAddTraits(.isHeader)
            if let currentMeta, !currentMeta.isEmpty {
                MonarchBadge(currentMeta, tone: .info)
            }
            if let enteredAt {
                since(enteredAt)
            }
        }
    }

    @ViewBuilder private func since(_ enteredAt: Date) -> some View {
        if let now {
            sinceText(enteredAt, now: now)
        } else {
            MonarchRelativeTimeClock(dates: [enteredAt]) { current in
                sinceText(enteredAt, now: current)
            }
        }
    }

    private func sinceText(_ enteredAt: Date, now: Date) -> some View {
        Text(
            MonarchStageJourneyStrings.since(
                MonarchStageJourneyTokens.relativeText(enteredAt, now: now, locale: locale),
                labels
            )
        )
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors.textMuted)
    }

    private var moves: some View {
        VStack(alignment: .leading, spacing: MonarchStageJourneyTokens.cardSpacing) {
            Text(MonarchStageJourneyStrings.nextSteps(labels))
                .monarchTextStyle(.caption, weight: .medium)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityAddTraits(.isHeader)
            MonarchFlowLayout(spacing: MonarchStageJourneyTokens.buttonSpacing) {
                ForEach(MonarchStageJourneyTokens.orderedMoves(transitions)) { transition in
                    MonarchButton(
                        MonarchStageJourneyTokens.buttonTitle(for: transition, labels: labels),
                        systemImage: MonarchStageJourneyTokens.buttonSymbol(for: transition),
                        variant: MonarchStageJourneyTokens.buttonVariant(for: transition, in: transitions),
                        size: .sm,
                        isLoading: busyID == transition.id
                    ) {
                        go(transition)
                    }
                    .disabled(!MonarchStageJourneyTokens.isEnabled(transition, busyID: busyID))
                    .accessibilityLabel(MonarchStageJourneyTokens.moveAccessibilityLabel(transition, labels: labels))
                }
            }
        }
        .padding(.top, MonarchStageJourneyTokens.stageSpacing)
    }

    private func go(_ transition: MonarchStageJourneyTransition) {
        guard let onTransition, busyID == nil else { return }
        busyID = transition.id
        failure = nil
        Task {
            do {
                try await onTransition(transition)
            } catch {
                let message = MonarchStageJourneyTokens.failureMessage(for: error, labels: labels)
                failure = message
                AccessibilityNotification.Announcement(message).post()
            }
            busyID = nil
        }
    }
}

extension MonarchStageJourney where Content == EmptyView {
    /// A stage journey.
    ///
    /// - Parameters:
    ///   - stages: Every stage, in pipeline order.
    ///   - currentID: The identifier of the current stage.
    ///   - terminal: Whether the current stage ends the journey, as a
    ///     success or closed.
    ///   - enteredAt: When the item entered the current stage.
    ///   - currentMeta: A short note shown as a badge next to the current
    ///     stage.
    ///   - variant: `full` or `strip`.
    ///   - transitions: The moves offered from the current stage.
    ///   - now: A fixed reference time for `enteredAt`.
    ///   - labels: Replacements for the built-in strings.
    ///   - onTransition: Called with the chosen move. Without it no moves
    ///     are shown.
    public init(
        _ stages: [MonarchStageJourneyStage],
        currentID: String,
        terminal: MonarchStageJourneyTerminal? = nil,
        enteredAt: Date? = nil,
        currentMeta: String? = nil,
        variant: MonarchStageJourneyVariant = .full,
        transitions: [MonarchStageJourneyTransition] = [],
        now: Date? = nil,
        labels: MonarchStageJourneyLabels = MonarchStageJourneyLabels(),
        onTransition: (@MainActor (MonarchStageJourneyTransition) async throws -> Void)? = nil
    ) {
        self.init(
            stages,
            currentID: currentID,
            terminal: terminal,
            enteredAt: enteredAt,
            currentMeta: currentMeta,
            variant: variant,
            transitions: transitions,
            now: now,
            labels: labels,
            onTransition: onTransition,
            content: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchStageJourney") {
        MonarchPreviewMatrix {
            let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
            let stages = [
                MonarchStageJourneyStage(id: "filed", label: "Filed", group: "Application"),
                MonarchStageJourneyStage(id: "exam", label: "Examination", group: "Application"),
                MonarchStageJourneyStage(id: "granted", label: "Granted", group: "Outcome"),
            ]
            VStack(alignment: .leading) {
                MonarchStageJourney(
                    stages,
                    currentID: "exam",
                    enteredAt: now.addingTimeInterval(-3 * 86400),
                    transitions: [
                        MonarchStageJourneyTransition(id: "granted", label: "Granted"),
                        MonarchStageJourneyTransition(id: "withdrawn", label: "Withdrawn", kind: .exit),
                        MonarchStageJourneyTransition(id: "filed", label: "Filed", kind: .back),
                    ],
                    now: now,
                    onTransition: { _ in }
                )
                MonarchStageJourney(stages, currentID: "granted", terminal: .success, variant: .strip)
                MonarchStageHistory(
                    [
                        MonarchStageHistoryEntry(id: "2", from: "Filed", to: "Examination", at: now, actor: "Alex"),
                        MonarchStageHistoryEntry(id: "1", to: "Filed", at: now.addingTimeInterval(-86400)),
                    ],
                    now: now
                )
            }
        }
    }
#endif

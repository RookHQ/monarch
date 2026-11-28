import SwiftUI

/// Records a decision about an item (keep, review, let lapse; approve,
/// defer, reject) with a reason, next to the evidence for it.
///
/// ```swift
/// MonarchDecisionCard(
///     "Renewal decision",
///     options: [
///         MonarchDecisionOption("Keep", value: "keep", tone: .success),
///         MonarchDecisionOption("Let lapse", value: "lapse", tone: .danger, isReasonRequired: true),
///     ],
///     value: saved?.value,
///     reason: saved?.reason,
///     onDecide: { decision in try await store.save(decision) }
/// )
/// ```
///
/// The card shows the saved decision (`value`, `reason`) and lets people
/// change it. `onDecide` is called with the new decision when Save is
/// activated; while it runs the form is disabled and the button shows
/// progress, and when it throws, the choice is kept and the error is shown.
/// A new saved `value` or `reason` resets the form. Without `onDecide`, or
/// with `isReadOnly`, the card shows the decision as a badge with the
/// reason.
///
/// Accessibility: the title is a heading. Each choice is a button with the
/// selected trait, read as its label and description, inside a group named
/// "Decision"; the selection is a glyph as well as a colour. The note of a
/// newly selected choice, a missing reason, a failed save and a successful
/// save are announced. The time of the decision is read as an absolute date.
/// Every choice is at least 44 points high.
public struct MonarchDecisionCard<Context: View>: View {
    private let title: String?
    private let options: [MonarchDecisionOption]
    private let value: String?
    private let reason: String?
    private let decidedAt: Date?
    private let now: Date?
    private let decidedBy: String?
    private let allowsNone: Bool
    private let isReasonRequired: Bool
    private let isReadOnly: Bool
    private let labels: MonarchDecisionCardLabels
    private let reasonLimit: Int
    private let onDecide: (@MainActor (MonarchDecision) async throws -> Void)?
    private let context: Context

    @State private var choice: String?
    @State private var text: String
    @State private var isSaving = false
    @State private var failure: String?
    @State private var showsMissing = false

    @Environment(\.monarchTheme) private var theme

    /// A decision card with evidence above the choices.
    ///
    /// - Parameters:
    ///   - title: The heading of the card, already localised.
    ///   - options: The choices, in order. Two to five work best.
    ///   - value: The saved decision, or `nil` for none.
    ///   - reason: The saved reason.
    ///   - decidedAt: When the saved decision was made.
    ///   - decidedBy: Who made the saved decision.
    ///   - now: The date the "decided" line is measured from. The current time by default; fix it for a preview or a test.
    ///   - allowsNone: Whether a "No decision" choice that clears the
    ///     decision is offered.
    ///   - isReasonRequired: Whether every choice needs a reason. An option's
    ///     own `isReasonRequired` overrides it.
    ///   - isReadOnly: Whether the decision is shown without the form, for
    ///     people who cannot change it.
    ///   - labels: Replacements for the built-in strings.
    ///   - reasonLimit: The longest reason accepted, in characters.
    ///   - onDecide: Saves the decision. Not called when nothing changed or
    ///     a required reason is missing. Throw to keep the choice and show
    ///     the error.
    ///   - context: Evidence for the decision, shown above the choices:
    ///     usage, risk, cost.
    public init(
        _ title: String? = nil,
        options: [MonarchDecisionOption],
        value: String?,
        reason: String? = nil,
        decidedAt: Date? = nil,
        decidedBy: String? = nil,
        now: Date? = nil,
        allowsNone: Bool = true,
        isReasonRequired: Bool = false,
        isReadOnly: Bool = false,
        labels: MonarchDecisionCardLabels = MonarchDecisionCardLabels(),
        reasonLimit: Int = 500,
        onDecide: (@MainActor (MonarchDecision) async throws -> Void)? = nil,
        @ViewBuilder context: () -> Context
    ) {
        self.title = title
        self.options = options
        self.value = value
        self.reason = reason
        self.decidedAt = decidedAt
        self.now = now
        self.decidedBy = decidedBy
        self.allowsNone = allowsNone
        self.isReasonRequired = isReasonRequired
        self.isReadOnly = isReadOnly
        self.labels = labels
        self.reasonLimit = reasonLimit
        self.onDecide = onDecide
        self.context = context()
        _choice = State(initialValue: value)
        _text = State(initialValue: reason ?? "")
    }

    /// The content of the view.
    public var body: some View {
        MonarchCard {
            VStack(alignment: .leading, spacing: MonarchDecisionCardTokens.sectionSpacing) {
                if let title, !title.isEmpty {
                    Text(title)
                        .monarchTextStyle(.subheading)
                        .foregroundStyle(theme.colors.textDefault)
                        .accessibilityAddTraits(.isHeader)
                }
                context
                if isReadOnly || onDecide == nil {
                    readOnly
                } else {
                    form
                }
            }
        }
        .onChange(of: value) { _, newValue in
            choice = newValue
            showsMissing = false
        }
        .onChange(of: reason) { _, newReason in
            text = newReason ?? ""
            showsMissing = false
        }
    }

    // MARK: Read-only

    private var readOnly: some View {
        VStack(alignment: .leading, spacing: MonarchDecisionCardTokens.readOnlySpacing) {
            if let saved = MonarchDecisionCardTokens.option(value, in: options) {
                MonarchBadge(saved.label, tone: saved.tone)
            } else {
                Text(labels.undecided ?? MonarchDecisionCardStrings.undecided)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            }
            if let reason, !reason.isEmpty {
                Text(reason)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
            }
            decided
        }
    }

    // MARK: Form

    private var isMissing: Bool {
        MonarchDecisionCardTokens.isReasonMissing(
            choice: choice,
            text: text,
            options: options,
            isReasonRequired: isReasonRequired
        )
    }

    private var isDirty: Bool {
        MonarchDecisionCardTokens.isDirty(choice: choice, text: text, value: value, reason: reason)
    }

    private var form: some View {
        let selected = MonarchDecisionCardTokens.option(choice, in: options)
        return VStack(alignment: .leading, spacing: MonarchDecisionCardTokens.sectionSpacing) {
            choices
            if let note = selected?.note, !note.isEmpty {
                Text(note)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            reasonField
            if let failure {
                HStack(alignment: .firstTextBaseline, spacing: MonarchDecisionCardTokens.textSpacing) {
                    MonarchIcon(systemName: MonarchDecisionCardTokens.errorSymbol, size: .sm)
                    Text(failure).monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors.dangerStrong)
                .accessibilityElement(children: .combine)
            }
            footer
        }
        .onChange(of: choice) { _, newChoice in
            showsMissing = false
            if let note = MonarchDecisionCardTokens.option(newChoice, in: options)?.note, !note.isEmpty {
                AccessibilityNotification.Announcement(note).post()
            }
        }
        .onChange(of: text) { _, newText in
            let limited = MonarchDecisionCardTokens.limited(newText, to: reasonLimit)
            if limited != newText { text = limited }
        }
    }

    /// Choices without descriptions sit side by side as tiles, as on the web; with descriptions they stack.
    private var isTiled: Bool {
        options.count + (allowsNone ? 1 : 0) <= 5
            && options.allSatisfy { $0.description?.isEmpty ?? true }
    }

    @ViewBuilder private var choices: some View {
        if isTiled {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchDecisionCardTokens.choiceSpacing) {
                    tiles
                }
                .fixedSize(horizontal: true, vertical: false)
                // Too narrow for every name on one line: two columns, so that no name breaks inside a word.
                LazyVGrid(
                    columns: Array(
                        repeating: GridItem(.flexible(), spacing: MonarchDecisionCardTokens.choiceSpacing),
                        count: 2
                    ),
                    spacing: MonarchDecisionCardTokens.choiceSpacing
                ) {
                    tiles
                }
            }
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.choices ?? MonarchDecisionCardStrings.choices)
        } else {
            stackedChoices
        }
    }

    @ViewBuilder private var tiles: some View {
        ForEach(options) { option in
            MonarchDecisionChoiceRow(
                label: option.label,
                description: nil,
                isSelected: choice == option.value,
                action: { choice = option.value },
                isTile: true
            )
            .disabled(option.isDisabled || isSaving)
        }
        if allowsNone {
            MonarchDecisionChoiceRow(
                label: labels.none ?? MonarchDecisionCardStrings.none,
                description: nil,
                isSelected: choice == nil,
                action: { choice = nil },
                isTile: true
            )
            .disabled(isSaving)
        }
    }

    private var stackedChoices: some View {
        VStack(alignment: .leading, spacing: MonarchDecisionCardTokens.choiceSpacing) {
            ForEach(options) { option in
                MonarchDecisionChoiceRow(
                    label: option.label,
                    description: option.description,
                    isSelected: choice == option.value
                ) {
                    choice = option.value
                }
                .disabled(option.isDisabled || isSaving)
            }
            if allowsNone {
                MonarchDecisionChoiceRow(
                    label: labels.none ?? MonarchDecisionCardStrings.none,
                    description: nil,
                    isSelected: choice == nil
                ) {
                    choice = nil
                }
                .disabled(isSaving)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.choices ?? MonarchDecisionCardStrings.choices)
    }

    private var reasonField: some View {
        let needsReason = MonarchDecisionCardTokens.needsReason(
            choice: choice,
            options: options,
            isReasonRequired: isReasonRequired
        )
        let left = MonarchDecisionCardTokens.charactersLeft(text: text, limit: reasonLimit)
        return MonarchDecisionReasonField(
            label: needsReason
                ? labels.reason ?? MonarchDecisionCardStrings.reason
                : labels.reasonOptional ?? MonarchDecisionCardStrings.reasonOptional,
            text: $text,
            prompt: labels.reasonPrompt ?? MonarchDecisionCardStrings.reasonPrompt,
            helper: left.map { MonarchDecisionCardTokens.charactersLeftText($0, labels: labels) },
            error: showsMissing && isMissing
                ? labels.reasonRequired ?? MonarchDecisionCardStrings.reasonRequired : nil
        )
        .disabled(isSaving)
    }

    private var footer: some View {
        let button = MonarchButton(
            labels.save ?? MonarchDecisionCardStrings.save,
            variant: .primary,
            size: .sm,
            isLoading: isSaving
        ) {
            save()
        }
        .disabled(!isDirty)
        // One row as on the web: the note gives way and wraps beside the button.
        return HStack(spacing: MonarchDecisionCardTokens.sectionSpacing) {
            decided.layoutPriority(-1)
            Spacer(minLength: 0)
            button.fixedSize()
        }
    }

    private func save() {
        guard let onDecide, !isSaving, isDirty else { return }
        if isMissing {
            showsMissing = true
            return
        }
        let decision = MonarchDecisionCardTokens.decision(choice: choice, text: text)
        failure = nil
        isSaving = true
        Task {
            do {
                try await onDecide(decision)
                AccessibilityNotification.Announcement(labels.saved ?? MonarchDecisionCardStrings.saved).post()
            } catch {
                let message = MonarchDecisionCardTokens.failureMessage(for: error, labels: labels)
                failure = message
                AccessibilityNotification.Announcement(message).post()
            }
            isSaving = false
        }
    }

    // MARK: When and by whom

    @ViewBuilder private var decided: some View {
        if decidedAt != nil || !(decidedBy ?? "").isEmpty {
            TimelineView(MonarchRelativeTimeSchedule(dates: decidedAt.map { [$0] } ?? [])) { timeline in
                let text = MonarchDecisionCardTokens.decidedText(
                    decidedAt: decidedAt,
                    decidedBy: decidedBy,
                    now: now ?? timeline.date,
                    labels: labels
                )
                let spoken = MonarchDecisionCardTokens.decidedAccessibilityLabel(
                    decidedAt: decidedAt,
                    decidedBy: decidedBy,
                    labels: labels
                )
                Text(text ?? "")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityLabel(spoken ?? "")
            }
        }
    }
}

extension MonarchDecisionCard where Context == EmptyView {
    /// A decision card without evidence.
    ///
    /// - Parameters:
    ///   - title: The heading of the card, already localised.
    ///   - options: The choices, in order. Two to five work best.
    ///   - value: The saved decision, or `nil` for none.
    ///   - reason: The saved reason.
    ///   - decidedAt: When the saved decision was made.
    ///   - decidedBy: Who made the saved decision.
    ///   - now: The date the "decided" line is measured from. The current time by default; fix it for a preview or a test.
    ///   - allowsNone: Whether a "No decision" choice that clears the
    ///     decision is offered.
    ///   - isReasonRequired: Whether every choice needs a reason. An option's
    ///     own `isReasonRequired` overrides it.
    ///   - isReadOnly: Whether the decision is shown without the form, for
    ///     people who cannot change it.
    ///   - labels: Replacements for the built-in strings.
    ///   - reasonLimit: The longest reason accepted, in characters.
    ///   - onDecide: Saves the decision. Not called when nothing changed or
    ///     a required reason is missing. Throw to keep the choice and show
    ///     the error.
    public init(
        _ title: String? = nil,
        options: [MonarchDecisionOption],
        value: String?,
        reason: String? = nil,
        decidedAt: Date? = nil,
        decidedBy: String? = nil,
        now: Date? = nil,
        allowsNone: Bool = true,
        isReasonRequired: Bool = false,
        isReadOnly: Bool = false,
        labels: MonarchDecisionCardLabels = MonarchDecisionCardLabels(),
        reasonLimit: Int = 500,
        onDecide: (@MainActor (MonarchDecision) async throws -> Void)? = nil
    ) {
        self.init(
            title,
            options: options,
            value: value,
            reason: reason,
            decidedAt: decidedAt,
            decidedBy: decidedBy,
            now: now,
            allowsNone: allowsNone,
            isReasonRequired: isReasonRequired,
            isReadOnly: isReadOnly,
            labels: labels,
            reasonLimit: reasonLimit,
            onDecide: onDecide
        ) {
            EmptyView()
        }
    }
}

#if DEBUG
    #Preview("MonarchDecisionCard") {
        let options = [
            MonarchDecisionOption("Keep", value: "keep", description: "Renews for another year.", tone: .success),
            MonarchDecisionOption("Let lapse", value: "lapse", note: "Access ends on the renewal date.", tone: .danger),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchDecisionCard("Renewal decision", options: options, value: "keep", onDecide: { _ in })
                MonarchDecisionCard(
                    "Renewal decision",
                    options: options,
                    value: "lapse",
                    reason: "Nobody has used it since March.",
                    decidedBy: "Ana",
                    isReadOnly: true
                )
            }
        }
    }
#endif

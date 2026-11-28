import SwiftUI

/// A spending limit for AI usage: what has been spent against the limit this
/// period, whether that is fine, close or reached, and a form to change the
/// limit, when to warn and what happens at the limit.
///
/// ```swift
/// MonarchAiSpendCap(
///     spent: 1_240,
///     limit: 5_000,
///     currencyCode: "GBP",
///     period: .month,
///     resetsAt: nextBillingDate
/// ) { settings in
///     workspace.saveSpendCap(settings)
/// }
/// ```
///
/// Amounts are integers in `unit`: the currency's minor unit, or micros
/// for usage priced in fractions of a cent, so money never goes through a
/// floating-point sum. The card shows the state beside the title, the
/// amount line, a bar in the state's tone, what is left, when it warns,
/// what happens at the limit and when the period resets, an optional
/// breakdown, and a notice when AI is paused. Edit limit opens a form with
/// ``MonarchCurrencyInput``, a warn-at menu and a ``MonarchRadioCardGroup``
/// for the behaviour; Remove limit asks in a confirmation dialog. A limit
/// below what is already spent is saved only when Save is pressed again.
/// The component never bills, blocks or fetches: it reports decisions
/// through `onSave`, and the app passes the saved settings back.
///
/// Accessibility: the card is a container named by its title, which has the
/// header trait. The state is a word and an icon, never colour alone. The
/// bar is read as "Spent against the limit" with the amount line as its
/// value. A change of state after the card appears is announced once ("AI
/// spending limit: Limit reached. £50.00 of £50.00 this month"), and the
/// below-spent warning is announced when it appears.
@_spi(Experimental) public struct MonarchAiSpendCap: View {
    private let spent: Int
    private let limit: Int?
    private let currencyCode: String
    private let period: MonarchAiSpendCapPeriod
    private let unit: MonarchAiSpendCapUnit
    private let warnAt: Double
    private let behavior: MonarchAiSpendCapBehavior
    private let resetsAt: Date?
    private let breakdown: [MonarchAiSpendCapBreakdownItem]
    private let maxLimit: Int?
    private let warnAtOptions: [Double]
    private let isEditingBinding: Binding<Bool>?
    private let canEdit: Bool
    private let isSaving: Bool
    private let labels: MonarchAiSpendCapLabels
    private let onSave: (@MainActor (MonarchAiSpendCapSettings) -> Void)?
    private let onRequestIncrease: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone
    @State private var ownIsEditing = false

    /// A spending limit.
    ///
    /// - Parameters:
    ///   - spent: Spent so far this period, as an integer in `unit`.
    ///   - limit: The limit as an integer in `unit`, or `nil` when none is set.
    ///   - currencyCode: The ISO 4217 code of every amount, such as "GBP".
    ///   - period: The window the limit applies to.
    ///   - unit: What the integers count: minor units or micros.
    ///   - warnAt: The share of the limit, between 0 and 1, at which the state becomes warning.
    ///   - behavior: What happens at the limit: pause and ask, or keep going and tell.
    ///   - resetsAt: When the period starts again.
    ///   - breakdown: Spending per agent or feature, shown as a list.
    ///   - maxLimit: The highest limit this person may set, as an integer in `unit`.
    ///   - warnAtOptions: The warn-at choices as shares; the current value is always offered.
    ///   - isEditing: Whether the form is open. Kept inside, closed at first, when `nil`.
    ///   - canEdit: Whether this person may change the limit. `false` shows a read-only note.
    ///   - isSaving: Shows Save as busy and locks the form while the app saves.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSave: Runs with the new settings on Save, or with a `nil` limit once removal is confirmed.
    ///   - onRequestIncrease: For people who cannot edit: adds "Request a higher limit".
    public init(
        spent: Int,
        limit: Int?,
        currencyCode: String,
        period: MonarchAiSpendCapPeriod,
        unit: MonarchAiSpendCapUnit = .minor,
        warnAt: Double = 0.8,
        behavior: MonarchAiSpendCapBehavior = .pause,
        resetsAt: Date? = nil,
        breakdown: [MonarchAiSpendCapBreakdownItem] = [],
        maxLimit: Int? = nil,
        warnAtOptions: [Double] = [0.5, 0.75, 0.8, 0.9],
        isEditing: Binding<Bool>? = nil,
        canEdit: Bool = true,
        isSaving: Bool = false,
        labels: MonarchAiSpendCapLabels = MonarchAiSpendCapLabels(),
        onSave: (@MainActor (MonarchAiSpendCapSettings) -> Void)? = nil,
        onRequestIncrease: (@MainActor () -> Void)? = nil
    ) {
        self.spent = spent
        self.limit = limit
        self.currencyCode = currencyCode
        self.period = period
        self.unit = unit
        self.warnAt = warnAt
        self.behavior = behavior
        self.resetsAt = resetsAt
        self.breakdown = breakdown
        self.maxLimit = maxLimit
        self.warnAtOptions = warnAtOptions
        self.isEditingBinding = isEditing
        self.canEdit = canEdit
        self.isSaving = isSaving
        self.labels = labels
        self.onSave = onSave
        self.onRequestIncrease = onRequestIncrease
    }

    private typealias Tokens = MonarchAiSpendCapTokens

    private var isEditing: Binding<Bool> { isEditingBinding ?? $ownIsEditing }

    private var phrases: Tokens.Phrases {
        Tokens.Phrases(currencyCode: currencyCode, unit: unit, period: period, locale: locale, labels: labels)
    }

    private var state: MonarchAiSpendCapState { Tokens.state(spent: spent, limit: limit, warnAt: warnAt) }

    /// The content of the view.
    public var body: some View {
        let phrases = phrases
        let state = state
        let amountLine = phrases.amountLine(spent: spent, limit: limit)
        let editing = canEdit && isEditing.wrappedValue
        let shape = MonarchShapes.container
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            header(phrases: phrases, state: state, editing: editing)
            Text(amountLine)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
            if let limit, limit > 0 {
                ProgressView(value: Double(min(max(0, spent), limit)), total: Double(limit))
                    .progressViewStyle(.monarchLinear(tone: Tokens.barTone(state)))
                    .accessibilityLabel(labels.meterLabel ?? MonarchAiSpendCapStrings.meterLabel)
                    .accessibilityValue(amountLine)
            }
            facts(phrases: phrases)
            if Tokens.isPaused(state, behavior: behavior) {
                pausedNotice(phrases: phrases, editing: editing)
            }
            if !breakdown.isEmpty {
                breakdownList(phrases: phrases)
            }
            if editing {
                MonarchAiSpendCapEditor(
                    spent: max(0, spent),
                    limit: limit,
                    currencyCode: currencyCode,
                    unit: unit,
                    maxLimit: maxLimit,
                    shares: Tokens.warnAtShares(options: warnAtOptions, current: warnAt),
                    warnAt: warnAt,
                    behavior: behavior,
                    isSaving: isSaving,
                    phrases: phrases,
                    onSave: { settings in
                        onSave?(settings)
                        isEditing.wrappedValue = false
                    },
                    onCancel: { isEditing.wrappedValue = false }
                )
            }
            if !canEdit {
                readOnlyNote(paused: Tokens.isPaused(state, behavior: behavior))
            }
        }
        .padding(Tokens.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: Tokens.fill]))
        .overlay(shape.strokeBorder(theme.colors[role: Tokens.border], lineWidth: Tokens.borderWidth))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(phrases.title)
        .onChange(of: state) { _, newState in
            AccessibilityNotification.Announcement(
                phrases.announcement(newState, amountLine: phrases.amountLine(spent: spent, limit: limit))
            )
            .post()
        }
    }

    /// The title and the state on the leading edge, Edit limit (or Set a limit) on the trailing edge, as the web.
    private func header(phrases: Tokens.Phrases, state: MonarchAiSpendCapState, editing: Bool) -> some View {
        let look = Tokens.look(state)
        return HStack(alignment: .top, spacing: Tokens.itemSpacing) {
            MonarchFlowLayout(spacing: Tokens.itemSpacing) {
                Text(phrases.title)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
                MonarchStatusBadge(
                    MonarchStatusDefinition(phrases.state(state), tone: look.tone, systemImage: look.symbol),
                    size: .sm
                )
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if canEdit, !editing {
                MonarchButton(
                    limit == nil
                        ? labels.setLimit ?? MonarchAiSpendCapStrings.setLimit
                        : labels.edit ?? MonarchAiSpendCapStrings.edit,
                    systemImage: Tokens.editSymbol,
                    size: .sm
                ) {
                    isEditing.wrappedValue = true
                }
                .fixedSize()
            }
        }
    }

    @ViewBuilder private func facts(phrases: Tokens.Phrases) -> some View {
        let lines =
            (limit.map { limit in
                [
                    phrases.remainder(spent: spent, limit: limit), phrases.warnsAt(warnAt),
                    phrases.behaviorSummary(behavior),
                ]
            } ?? []) + (resetsAt.map { [phrases.resets($0, timeZone: timeZone)] } ?? [])
        if !lines.isEmpty {
            // One wrapping line of facts, as the web's meta row.
            MonarchFlowLayout(spacing: Tokens.spacing, rowSpacing: Tokens.lineSpacing, alignment: .firstTextBaseline) {
                ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                    Text(line)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
    }

    @ViewBuilder private func pausedNotice(phrases: Tokens.Phrases, editing: Bool) -> some View {
        let message = phrases.pausedBody(limit: limit ?? 0)
        let title = labels.pausedTitle ?? MonarchAiSpendCapStrings.pausedTitle
        if canEdit {
            if editing {
                MonarchInlineNotice(message, title: title, tone: .danger)
            } else {
                MonarchInlineNotice(
                    message,
                    title: title,
                    tone: .danger,
                    actionTitle: labels.raiseLimit ?? MonarchAiSpendCapStrings.raiseLimit,
                    action: { isEditing.wrappedValue = true }
                )
            }
        } else if let onRequestIncrease {
            MonarchInlineNotice(
                message,
                title: title,
                tone: .danger,
                actionTitle: labels.requestIncrease ?? MonarchAiSpendCapStrings.requestIncrease,
                action: onRequestIncrease
            )
        } else {
            MonarchInlineNotice(message, title: title, tone: .danger)
        }
    }

    private func breakdownList(phrases: Tokens.Phrases) -> some View {
        VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            Text(labels.breakdownLabel ?? MonarchAiSpendCapStrings.breakdownLabel)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityAddTraits(.isHeader)
            ForEach(breakdown) { item in
                LabeledContent(item.label, value: phrases.money(max(0, item.spent)))
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
            }
        }
    }

    private func readOnlyNote(paused: Bool) -> some View {
        VStack(alignment: .leading, spacing: Tokens.itemSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: Tokens.itemSpacing) {
                MonarchIcon(systemName: Tokens.lockSymbol, size: .sm)
                Text(labels.readOnly ?? MonarchAiSpendCapStrings.readOnly)
                    .monarchTextStyle(.caption)
            }
            .foregroundStyle(theme.colors.textMuted)
            .accessibilityElement(children: .combine)
            if !paused, let onRequestIncrease {
                MonarchButton(labels.requestIncrease ?? MonarchAiSpendCapStrings.requestIncrease, size: .sm) {
                    onRequestIncrease()
                }
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchAiSpendCap") {
        MonarchPreviewMatrix {
            VStack {
                MonarchAiSpendCap(spent: 1_240, limit: 5_000, currencyCode: "GBP", period: .month)
                MonarchAiSpendCap(spent: 5_210, limit: 5_000, currencyCode: "GBP", period: .month, canEdit: false)
                MonarchAiSpendCap(spent: 900, limit: nil, currencyCode: "USD", period: .day, isEditing: .constant(true))
            }
        }
    }
#endif

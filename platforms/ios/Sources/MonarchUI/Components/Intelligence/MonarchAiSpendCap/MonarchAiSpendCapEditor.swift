import SwiftUI

/// The form that changes the limit. The draft lives here, so it starts from
/// the saved settings each time the form opens.
struct MonarchAiSpendCapEditor: View {
    let spent: Int
    let limit: Int?
    let currencyCode: String
    let unit: MonarchAiSpendCapUnit
    let maxLimit: Int?
    let shares: [Double]
    let warnAt: Double
    let behavior: MonarchAiSpendCapBehavior
    let isSaving: Bool
    let phrases: MonarchAiSpendCapTokens.Phrases
    let onSave: @MainActor (MonarchAiSpendCapSettings) -> Void
    let onCancel: @MainActor () -> Void

    @State private var amount: Double?
    @State private var draftWarnAt: Double
    @State private var draftBehavior: MonarchAiSpendCapBehavior?
    @State private var validation: MonarchAiSpendCapTokens.Validation?
    @State private var warnedBelow: Int?
    @State private var isConfirmingRemoval = false

    private typealias Tokens = MonarchAiSpendCapTokens

    init(
        spent: Int,
        limit: Int?,
        currencyCode: String,
        unit: MonarchAiSpendCapUnit,
        maxLimit: Int?,
        shares: [Double],
        warnAt: Double,
        behavior: MonarchAiSpendCapBehavior,
        isSaving: Bool,
        phrases: MonarchAiSpendCapTokens.Phrases,
        onSave: @escaping @MainActor (MonarchAiSpendCapSettings) -> Void,
        onCancel: @escaping @MainActor () -> Void
    ) {
        self.spent = spent
        self.limit = limit
        self.currencyCode = currencyCode
        self.unit = unit
        self.maxLimit = maxLimit
        self.shares = shares
        self.warnAt = warnAt
        self.behavior = behavior
        self.isSaving = isSaving
        self.phrases = phrases
        self.onSave = onSave
        self.onCancel = onCancel
        _amount = State(initialValue: limit.map { Tokens.toMajor($0, currencyCode: currencyCode, unit: unit) })
        _draftWarnAt = State(initialValue: Tokens.initialWarnAt(shares: shares, current: warnAt))
        _draftBehavior = State(initialValue: behavior)
    }

    private var labels: MonarchAiSpendCapLabels { phrases.labels }

    private var error: String? {
        guard let validation, case .belowSpent = validation else {
            return validation.flatMap {
                phrases.message($0, spent: spent, maxLimit: maxLimit, behavior: draftBehavior ?? behavior)
            }
        }
        return nil
    }

    private var warning: String? {
        guard let validation, case .belowSpent = validation else { return nil }
        return phrases.message(validation, spent: spent, maxLimit: maxLimit, behavior: draftBehavior ?? behavior)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            MonarchCurrencyInput(
                labels.amountLabel ?? MonarchAiSpendCapStrings.amountLabel,
                value: $amount,
                currencyCode: currencyCode,
                allowsNegative: true,
                helper: phrases.amountHint(spent: spent),
                error: error
            )
            .onChange(of: amount) {
                validation = nil
                warnedBelow = nil
            }
            if let warning {
                MonarchInlineNotice(warning, tone: .warning)
            }
            MonarchSelectField(
                labels.warnAtLabel ?? MonarchAiSpendCapStrings.warnAtLabel,
                selection: $draftWarnAt,
                valueText: phrases.warnAtOption(draftWarnAt)
            ) {
                ForEach(shares, id: \.self) { share in
                    Text(phrases.warnAtOption(share)).tag(share)
                }
            }
            MonarchRadioCardGroup(
                labels.behaviorLabel ?? MonarchAiSpendCapStrings.behaviorLabel,
                selection: $draftBehavior,
                options: MonarchAiSpendCapBehavior.allCases.map { option in
                    MonarchRadioCardOption(
                        option,
                        title: phrases.behaviorOption(option),
                        description: phrases.behaviorDescription(option)
                    )
                }
            )
            actions
        }
        .disabled(isSaving)
        .confirmationDialog(
            labels.removeConfirmTitle ?? MonarchAiSpendCapStrings.removeConfirmTitle,
            isPresented: $isConfirmingRemoval,
            titleVisibility: .visible
        ) {
            Button(labels.removeLimit ?? MonarchAiSpendCapStrings.removeLimit, role: .destructive) {
                onSave(MonarchAiSpendCapSettings(limit: nil, warnAt: warnAt, behavior: behavior))
            }
            Button(labels.removeCancel ?? MonarchAiSpendCapStrings.removeCancel, role: .cancel) {}
        } message: {
            Text(labels.removeConfirmBody ?? MonarchAiSpendCapStrings.removeConfirmBody)
        }
    }

    private var actions: some View {
        MonarchFlowLayout(spacing: Tokens.itemSpacing) {
            MonarchButton(
                warnedBelow == nil
                    ? labels.save ?? MonarchAiSpendCapStrings.save
                    : labels.saveAnyway ?? MonarchAiSpendCapStrings.saveAnyway,
                variant: .primary,
                size: .sm,
                isLoading: isSaving
            ) {
                save()
            }
            MonarchButton(labels.cancel ?? MonarchAiSpendCapStrings.cancel, variant: .ghost, size: .sm) {
                onCancel()
            }
            if limit != nil {
                MonarchButton(labels.removeLimit ?? MonarchAiSpendCapStrings.removeLimit, variant: .danger, size: .sm) {
                    isConfirmingRemoval = true
                }
            }
        }
    }

    private func save() {
        let result = Tokens.validate(
            major: amount,
            spent: spent,
            maxLimit: maxLimit,
            warnedBelow: warnedBelow,
            currencyCode: currencyCode,
            unit: unit
        )
        switch result {
        case .valid(let newLimit):
            onSave(MonarchAiSpendCapSettings(limit: newLimit, warnAt: draftWarnAt, behavior: draftBehavior ?? behavior))
        case .belowSpent(let newLimit):
            warnedBelow = newLimit
            validation = result
            if let warning = phrases.message(
                result,
                spent: spent,
                maxLimit: maxLimit,
                behavior: draftBehavior ?? behavior
            ) {
                AccessibilityNotification.Announcement(warning).post()
            }
        default:
            warnedBelow = nil
            validation = result
        }
    }
}

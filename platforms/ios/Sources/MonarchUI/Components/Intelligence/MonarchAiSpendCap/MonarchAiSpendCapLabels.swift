import Foundation

/// Replacements for the built-in strings of ``MonarchAiSpendCap``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiSpendCapLabels: Sendable {
    /// The heading and spoken name. Built in: "AI spending limit".
    public var title: String?
    /// The period in running text. Built in: "today", "this week", "this month".
    public var period: (@Sendable (MonarchAiSpendCapPeriod) -> String)?
    /// The name of a state. Built in: "Within limit", "Close to the limit", "Limit reached", "No limit set".
    public var state: (@Sendable (MonarchAiSpendCapState) -> String)?
    /// The amount line, from the spent amount, the limit and the period. Built in: "£12.40 of £50.00 this month".
    public var spentOfLimit: (@Sendable (String, String, String) -> String)?
    /// The amount line without a limit, from the spent amount and the period. Built in: "£12.40 spent this month".
    public var spentNoLimit: (@Sendable (String, String) -> String)?
    /// The spoken name of the bar. Built in: "Spent against the limit".
    public var meterLabel: String?
    /// What is left, from the amount. Built in: "£37.60 left".
    public var remaining: (@Sendable (String) -> String)?
    /// How far spending is past the limit, from the amount. Built in: "£2.10 over the limit".
    public var over: (@Sendable (String) -> String)?
    /// When the warning starts, from the percentage. Built in: "Warns at 80%".
    public var warnsAt: (@Sendable (String) -> String)?
    /// One line on what happens at the limit. Built in: "At the limit, AI pauses and asks".
    public var behaviorSummary: (@Sendable (MonarchAiSpendCapBehavior) -> String)?
    /// When the period starts again, from the formatted date. Built in: "Resets 1 June".
    public var resets: (@Sendable (String) -> String)?
    /// The heading of the breakdown. Built in: "Where it was spent".
    public var breakdownLabel: String?
    /// The button that opens the form when a limit is set. Built in: "Edit limit".
    public var edit: String?
    /// The button that opens the form without a limit. Built in: "Set a limit".
    public var setLimit: String?
    /// The title of the notice when AI is paused. Built in: "AI is paused.".
    public var pausedTitle: String?
    /// The body of that notice, from the limit and the period.
    public var pausedBody: (@Sendable (String, String) -> String)?
    /// The notice action for people who can edit. Built in: "Raise limit".
    public var raiseLimit: String?
    /// The action for people who cannot edit. Built in: "Request a higher limit".
    public var requestIncrease: String?
    /// Who can change the limit. Built in: "Only a workspace admin can change this limit.".
    public var readOnly: String?
    /// The label of the amount field. Built in: "Limit".
    public var amountLabel: String?
    /// Help under the amount field, from the spent amount and the period. Built in: "£12.40 spent this month so far.".
    public var amountHint: (@Sendable (String, String) -> String)?
    /// The amount is empty. Built in: "Enter an amount. To have no limit, use Remove limit.".
    public var errorRequired: String?
    /// The amount is zero or less. Built in: "Enter an amount greater than zero.".
    public var errorPositive: String?
    /// The amount is above the highest limit, from that limit. Built in: "Enter £500.00 or less.".
    public var errorMax: (@Sendable (String) -> String)?
    /// The amount is below what is already spent, from the spent amount, the period and the behaviour; saving again confirms.
    public var belowSpent: (@Sendable (String, String, MonarchAiSpendCapBehavior) -> String)?
    /// The label of the warn-at choice. Built in: "Warn me at".
    public var warnAtLabel: String?
    /// One warn-at choice, from the percentage. Built in: "80% of the limit".
    public var warnAtOption: (@Sendable (String) -> String)?
    /// The label of the behaviour choice. Built in: "When the limit is reached".
    public var behaviorLabel: String?
    /// The name of a behaviour. Built in: "Pause and ask", "Keep going and tell me".
    public var behaviorOption: (@Sendable (MonarchAiSpendCapBehavior) -> String)?
    /// What a behaviour means.
    public var behaviorDescription: (@Sendable (MonarchAiSpendCapBehavior) -> String)?
    /// The Save button. Built in: "Save".
    public var save: String?
    /// The Save button after the below-spent warning. Built in: "Save anyway".
    public var saveAnyway: String?
    /// The Cancel button. Built in: "Cancel".
    public var cancel: String?
    /// The button that starts removing the limit, and the confirming button. Built in: "Remove limit".
    public var removeLimit: String?
    /// The question before the limit is removed. Built in: "Remove the spending limit?".
    public var removeConfirmTitle: String?
    /// What removing the limit means. Built in: "AI will be able to spend without a cap until a new limit is set.".
    public var removeConfirmBody: String?
    /// Backs out of the removal. Built in: "Keep limit".
    public var removeCancel: String?
    /// Announced when the state changes, from the title, the state and the amount line. Built in: "AI spending limit: Limit reached. £50.00 of £50.00 this month".
    public var announce: (@Sendable (String, String, String) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - title: The heading and spoken name.
    ///   - period: The period in running text.
    ///   - state: The name of a state.
    ///   - spentOfLimit: The amount line, from the spent amount, the limit and the period.
    ///   - spentNoLimit: The amount line without a limit, from the spent amount and the period.
    ///   - meterLabel: The spoken name of the bar.
    ///   - remaining: What is left, from the amount.
    ///   - over: How far spending is past the limit, from the amount.
    ///   - warnsAt: When the warning starts, from the percentage.
    ///   - behaviorSummary: One line on what happens at the limit.
    ///   - resets: When the period starts again, from the formatted date.
    ///   - breakdownLabel: The heading of the breakdown.
    ///   - edit: The button that opens the form when a limit is set.
    ///   - setLimit: The button that opens the form without a limit.
    ///   - pausedTitle: The title of the notice when AI is paused.
    ///   - pausedBody: The body of that notice, from the limit and the period.
    ///   - raiseLimit: The notice action for people who can edit.
    ///   - requestIncrease: The action for people who cannot edit.
    ///   - readOnly: Who can change the limit.
    ///   - amountLabel: The label of the amount field.
    ///   - amountHint: Help under the amount field, from the spent amount and the period.
    ///   - errorRequired: The amount is empty.
    ///   - errorPositive: The amount is zero or less.
    ///   - errorMax: The amount is above the highest limit, from that limit.
    ///   - belowSpent: The amount is below what is already spent, from the spent amount, the period and the behaviour; saving again confirms.
    ///   - warnAtLabel: The label of the warn-at choice.
    ///   - warnAtOption: One warn-at choice, from the percentage.
    ///   - behaviorLabel: The label of the behaviour choice.
    ///   - behaviorOption: The name of a behaviour.
    ///   - behaviorDescription: What a behaviour means.
    ///   - save: The Save button.
    ///   - saveAnyway: The Save button after the below-spent warning.
    ///   - cancel: The Cancel button.
    ///   - removeLimit: The button that starts removing the limit, and the confirming button.
    ///   - removeConfirmTitle: The question before the limit is removed.
    ///   - removeConfirmBody: What removing the limit means.
    ///   - removeCancel: Backs out of the removal.
    ///   - announce: Announced when the state changes, from the title, the state and the amount line.
    public init(
        title: String? = nil,
        period: (@Sendable (MonarchAiSpendCapPeriod) -> String)? = nil,
        state: (@Sendable (MonarchAiSpendCapState) -> String)? = nil,
        spentOfLimit: (@Sendable (String, String, String) -> String)? = nil,
        spentNoLimit: (@Sendable (String, String) -> String)? = nil,
        meterLabel: String? = nil,
        remaining: (@Sendable (String) -> String)? = nil,
        over: (@Sendable (String) -> String)? = nil,
        warnsAt: (@Sendable (String) -> String)? = nil,
        behaviorSummary: (@Sendable (MonarchAiSpendCapBehavior) -> String)? = nil,
        resets: (@Sendable (String) -> String)? = nil,
        breakdownLabel: String? = nil,
        edit: String? = nil,
        setLimit: String? = nil,
        pausedTitle: String? = nil,
        pausedBody: (@Sendable (String, String) -> String)? = nil,
        raiseLimit: String? = nil,
        requestIncrease: String? = nil,
        readOnly: String? = nil,
        amountLabel: String? = nil,
        amountHint: (@Sendable (String, String) -> String)? = nil,
        errorRequired: String? = nil,
        errorPositive: String? = nil,
        errorMax: (@Sendable (String) -> String)? = nil,
        belowSpent: (@Sendable (String, String, MonarchAiSpendCapBehavior) -> String)? = nil,
        warnAtLabel: String? = nil,
        warnAtOption: (@Sendable (String) -> String)? = nil,
        behaviorLabel: String? = nil,
        behaviorOption: (@Sendable (MonarchAiSpendCapBehavior) -> String)? = nil,
        behaviorDescription: (@Sendable (MonarchAiSpendCapBehavior) -> String)? = nil,
        save: String? = nil,
        saveAnyway: String? = nil,
        cancel: String? = nil,
        removeLimit: String? = nil,
        removeConfirmTitle: String? = nil,
        removeConfirmBody: String? = nil,
        removeCancel: String? = nil,
        announce: (@Sendable (String, String, String) -> String)? = nil
    ) {
        self.title = title
        self.period = period
        self.state = state
        self.spentOfLimit = spentOfLimit
        self.spentNoLimit = spentNoLimit
        self.meterLabel = meterLabel
        self.remaining = remaining
        self.over = over
        self.warnsAt = warnsAt
        self.behaviorSummary = behaviorSummary
        self.resets = resets
        self.breakdownLabel = breakdownLabel
        self.edit = edit
        self.setLimit = setLimit
        self.pausedTitle = pausedTitle
        self.pausedBody = pausedBody
        self.raiseLimit = raiseLimit
        self.requestIncrease = requestIncrease
        self.readOnly = readOnly
        self.amountLabel = amountLabel
        self.amountHint = amountHint
        self.errorRequired = errorRequired
        self.errorPositive = errorPositive
        self.errorMax = errorMax
        self.belowSpent = belowSpent
        self.warnAtLabel = warnAtLabel
        self.warnAtOption = warnAtOption
        self.behaviorLabel = behaviorLabel
        self.behaviorOption = behaviorOption
        self.behaviorDescription = behaviorDescription
        self.save = save
        self.saveAnyway = saveAnyway
        self.cancel = cancel
        self.removeLimit = removeLimit
        self.removeConfirmTitle = removeConfirmTitle
        self.removeConfirmBody = removeConfirmBody
        self.removeCancel = removeCancel
        self.announce = announce
    }
}

import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiModelPicker``.
enum MonarchAiModelPickerTokens {
    /// Whether a model can be picked.
    enum OptionState: Equatable {
        /// It can be picked.
        case available
        /// Its key is missing; picking it asks the product to add the key.
        case needsKey
        /// It cannot be picked.
        case blocked
    }

    /// The gap between the parts of the button.
    static let triggerSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between badges.
    static let badgeSpacing: CGFloat = MonarchTokens.Space.space1
    /// The column of the check mark, so names line up.
    static let checkWidth: CGFloat = MonarchTokens.Space.space5
    /// The check of the selected model.
    static let checkSymbol = "checkmark"
    /// The chevron of the button.
    static let chevronSymbol = "chevron.up.chevron.down"
    /// The lock on a model whose key is missing.
    static let lockSymbol = "lock.fill"
    /// The key on a model that uses a configured key.
    static let keySymbol = "key.fill"
    /// The weight of a model's name.
    static let nameWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The width of the dropdown under the trigger.
    static let popoverWidth: CGFloat = 340
    /// The least height of the dropdown.
    static let popoverMinimumHeight: CGFloat = 600
    /// The most height of the dropdown before it scrolls.
    static let popoverMaximumHeight: CGFloat = 600
    /// The symbol repeated for the cost tier.
    static let costSymbol = "$"

    /// Whether a model can be picked, needs a key, or is blocked. A missing
    /// key blocks the model unless the product can add one.
    static func state(_ model: MonarchAiModelOption, canConfigureKey: Bool) -> OptionState {
        if model.isDisabled { return .blocked }
        if model.requiresKey && !model.isKeyConfigured { return canConfigureKey ? .needsKey : .blocked }
        return .available
    }

    /// The models grouped by provider, groups in the order they first appear.
    static func groups(_ models: [MonarchAiModelOption]) -> [(provider: String, models: [MonarchAiModelOption])] {
        var order: [String] = []
        var byProvider: [String: [MonarchAiModelOption]] = [:]
        for model in models {
            if byProvider[model.provider] == nil { order.append(model.provider) }
            byProvider[model.provider, default: []].append(model)
        }
        return order.map { ($0, byProvider[$0] ?? []) }
    }

    /// The cost tier clamped to 1...3, or `nil`.
    static func clampedCost(_ tier: Int?) -> Int? {
        tier.map { min(3, max(1, $0)) }
    }

    /// "200K" for 200 000 tokens, with at most one decimal, as the context chips write it.
    static func compactTokens(_ count: Int, locale: Locale) -> String {
        MonarchAiContextFormat.formatTokens(count, locale: locale)
    }

    /// The details line as shown: "200K context · Fast · $$".
    static func metaText(_ model: MonarchAiModelOption, locale: Locale, labels: MonarchAiModelPickerLabels) -> String? {
        var parts: [String] = []
        if let context = contextText(model, locale: locale, labels: labels) { parts.append(context) }
        if let speed = model.speed { parts.append(speedName(speed, labels: labels)) }
        if let tier = clampedCost(model.costTier) { parts.append(String(repeating: costSymbol, count: tier)) }
        return parts.isEmpty ? nil : parts.joined(separator: MonarchAiModelPickerStrings.separator)
    }

    /// The context part: "200K context".
    static func contextText(_ model: MonarchAiModelOption, locale: Locale, labels: MonarchAiModelPickerLabels)
        -> String?
    {
        guard let tokens = model.contextTokens, tokens > 0 else { return nil }
        let formatted = compactTokens(tokens, locale: locale)
        return labels.context?(formatted) ?? MonarchAiModelPickerStrings.context(formatted)
    }

    /// The name of a capability.
    static func capabilityName(_ capability: MonarchAiModelCapability, labels: MonarchAiModelPickerLabels) -> String {
        labels.capability?(capability) ?? MonarchAiModelPickerStrings.capability(capability)
    }

    /// The name of a speed.
    static func speedName(_ speed: MonarchAiModelSpeed, labels: MonarchAiModelPickerLabels) -> String {
        labels.speed?(speed) ?? MonarchAiModelPickerStrings.speed(speed)
    }

    /// The key badge, or `nil` for a model without its own key.
    static func keyText(_ model: MonarchAiModelOption, labels: MonarchAiModelPickerLabels) -> String? {
        guard model.requiresKey else { return nil }
        return model.isKeyConfigured
            ? labels.ownKey ?? MonarchAiModelPickerStrings.ownKey
            : labels.keyNeeded ?? MonarchAiModelPickerStrings.keyNeeded
    }

    /// What VoiceOver reads as a row's name: the model and its key badge.
    static func rowLabel(_ model: MonarchAiModelOption, labels: MonarchAiModelPickerLabels) -> String {
        [model.name, keyText(model, labels: labels)].compactMap { $0 }.joined(separator: ", ")
    }

    /// What VoiceOver reads after the name: the description, the
    /// capabilities, the context, the speed, the cost in words, and why it
    /// cannot be picked.
    static func rowValue(
        _ model: MonarchAiModelOption,
        state: OptionState,
        locale: Locale,
        labels: MonarchAiModelPickerLabels
    ) -> String {
        var parts: [String] = []
        if let description = model.description, !description.isEmpty { parts.append(description) }
        parts += model.capabilities.map { capabilityName($0, labels: labels) }
        if let context = contextText(model, locale: locale, labels: labels) { parts.append(context) }
        if let speed = model.speed { parts.append(speedName(speed, labels: labels)) }
        if let tier = clampedCost(model.costTier) {
            parts.append(labels.cost?(tier) ?? MonarchAiModelPickerStrings.cost(tier))
        }
        if state == .blocked { parts.append(labels.unavailable ?? MonarchAiModelPickerStrings.unavailable) }
        return parts.joined(separator: ", ")
    }

    /// The hint of a row whose key is missing.
    static func rowHint(state: OptionState, labels: MonarchAiModelPickerLabels) -> String? {
        state == .needsKey ? labels.addKeyHint ?? MonarchAiModelPickerStrings.addKeyHint : nil
    }

    /// The colours of a row's name and details.
    static func textRoles(state: OptionState) -> (name: MonarchColorRole, detail: MonarchColorRole) {
        if state == .blocked {
            let disabled: MonarchColorRole = \.textDisabled
            return (disabled, disabled)
        }
        let name: MonarchColorRole = \.textDefault
        let detail: MonarchColorRole = \.textMuted
        return (name, detail)
    }

    /// What VoiceOver reads as the button's value: the selected model and provider, or the placeholder.
    static func triggerValue(_ selected: MonarchAiModelOption?, labels: MonarchAiModelPickerLabels) -> String {
        guard let selected else { return labels.placeholder ?? MonarchAiModelPickerStrings.placeholder }
        return "\(selected.name), \(selected.provider)"
    }
}

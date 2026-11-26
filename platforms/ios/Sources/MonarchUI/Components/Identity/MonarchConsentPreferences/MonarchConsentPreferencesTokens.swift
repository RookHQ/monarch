import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchConsentPreferences``.
enum MonarchConsentPreferencesTokens {
    /// The gap between the header, the categories, the record and the buttons.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between lines of the header and of a category.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// Vertical padding of a category row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between buttons.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// After this long, the saved time is a date instead of a relative time: seven days.
    static let relativeLimit: TimeInterval = 7 * 24 * 60 * 60
    /// The separator between the policy version and the saved time.
    static let recordSeparator = " · "

    /// Whether a category's switch is locked: required, or sale-or-sharing under Global Privacy Control.
    static func isLocked(_ category: MonarchConsentPreferencesCategory, isGlobalPrivacyControl: Bool) -> Bool {
        category.isRequired || isGlobalPrivacyControlLocked(category, isGlobalPrivacyControl: isGlobalPrivacyControl)
    }

    /// Whether a Global Privacy Control signal keeps a category off.
    static func isGlobalPrivacyControlLocked(
        _ category: MonarchConsentPreferencesCategory,
        isGlobalPrivacyControl: Bool
    ) -> Bool {
        isGlobalPrivacyControl && category.isSaleOrSharing && !category.isRequired
    }

    /// Whether a category is on, from the choices: required ones always, GPC-locked ones never.
    static func isOn(
        _ category: MonarchConsentPreferencesCategory,
        choices: [String: Bool],
        isGlobalPrivacyControl: Bool
    ) -> Bool {
        if category.isRequired { return true }
        if isGlobalPrivacyControlLocked(category, isGlobalPrivacyControl: isGlobalPrivacyControl) { return false }
        return choices[category.id] == true
    }

    /// Every category with its state after a decision. Required categories are
    /// on and GPC-locked ones are off whatever the decision.
    static func choices(
        for method: MonarchConsentMethod,
        categories: [MonarchConsentPreferencesCategory],
        draft: [String: Bool],
        isGlobalPrivacyControl: Bool
    ) -> [String: Bool] {
        var result: [String: Bool] = [:]
        for category in categories {
            let wanted: Bool
            switch method {
            case .acceptAll: wanted = true
            case .custom: wanted = draft[category.id] == true
            default: wanted = false
            }
            result[category.id] = isOn(
                category,
                choices: [category.id: wanted],
                isGlobalPrivacyControl: isGlobalPrivacyControl
            )
        }
        return result
    }

    /// Whether Withdraw consent is shown: in categories mode, when allowed, with a saved choice that is not withdrawn.
    static func showsWithdraw(
        mode: MonarchConsentPreferencesMode,
        allowsWithdraw: Bool,
        record: MonarchConsentRecord?
    ) -> Bool {
        guard mode == .categories, allowsWithdraw, let record else { return false }
        return record.method != .withdrawn
    }

    /// The status text of a category in choice mode.
    static func status(
        _ category: MonarchConsentPreferencesCategory,
        choices: [String: Bool],
        isGlobalPrivacyControl: Bool,
        labels: MonarchConsentPreferencesLabels
    ) -> String {
        if category.isRequired { return labels.alwaysOn ?? MonarchConsentPreferencesStrings.alwaysOn }
        return isOn(category, choices: choices, isGlobalPrivacyControl: isGlobalPrivacyControl)
            ? (labels.on ?? MonarchConsentPreferencesStrings.on)
            : (labels.off ?? MonarchConsentPreferencesStrings.off)
    }

    /// The saved time: relative within seven days, otherwise a medium date.
    static func savedTime(_ date: Date, now: Date, locale: Locale) -> String {
        if abs(now.timeIntervalSince(date)) <= relativeLimit {
            return MonarchRelativeTime.string(date, now: now, locale: locale, allowsFuture: true)
        }
        return date.formatted(Date.FormatStyle(date: .abbreviated, time: .omitted).locale(locale))
    }

    /// The record line: the policy version and the saved time.
    static func recordLine(
        _ record: MonarchConsentRecord,
        now: Date,
        locale: Locale,
        labels: MonarchConsentPreferencesLabels
    ) -> String {
        let time = savedTime(record.date, now: now, locale: locale)
        return [
            labels.policyVersion?(record.policyVersion)
                ?? MonarchConsentPreferencesStrings.policyVersion(record.policyVersion),
            labels.saved?(time) ?? MonarchConsentPreferencesStrings.saved(time),
        ]
        .joined(separator: recordSeparator)
    }

    /// The colour of a status in choice mode: on is success, so it is also the word, not colour alone.
    static func statusRole(isOn: Bool) -> MonarchColorRole {
        isOn ? \.successStrong : \.textMuted
    }
}

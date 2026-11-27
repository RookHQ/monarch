import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure text logic of ``MonarchPaymentMethodCard``.
enum MonarchPaymentMethodCardTokens {
    /// The SF Symbol drawn when no network image is given.
    static let cardSymbol = "creditcard"
    /// The SF Symbol of the expiring and expired badges.
    static let expirySymbol = "exclamationmark.triangle.fill"
    /// The gap between the network image and the text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of text and between the text and the actions.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the name and the badges, and between the actions.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// The type role of the card's name.
    static let nameStyle: MonarchTextStyle = .subheading
    /// The type role of the holder and expiry line.
    static let metaStyle: MonarchTextStyle = .caption
    /// The colour of the holder and expiry line.
    static var metaColor: MonarchColorRole { \.textMuted }
    /// The colour of the network image.
    static var iconColor: MonarchColorRole { \.textMuted }
    /// The fill and text of the default badge, which uses the accent where
    /// the web uses its accent tone.
    static var defaultBadge: MonarchSurfaceAppearance {
        MonarchSurfaceAppearance(background: \.accentSubtle, foreground: \.accentStrong, border: nil)
    }

    /// The display name of a network id such as `visa`, or the given text
    /// when it is not a known id.
    static func brandName(_ brand: String) -> String {
        let id = brand.lowercased()
        return MonarchPaymentMethodCardStrings.brandIDs.contains(id) ? MonarchPaymentMethodCardStrings.brand(id) : brand
    }

    /// The card's name: "Visa ending in 4242".
    static func name(brand: String, last4: String, labels: MonarchPaymentMethodCardLabels) -> String {
        let network = brandName(brand)
        return labels.name?(network, last4) ?? MonarchPaymentMethodCardStrings.name(brand: network, last4: last4)
    }

    /// The expiry date as MM/YY, or `nil` without a month and a year.
    static func expiryDate(month: Int?, year: Int?) -> String? {
        guard let month, let year else { return nil }
        let twoDigitYear = ((year % 100) + 100) % 100
        return String(format: "%02d/%02d", month, twoDigitYear)
    }

    /// The status: the override if there is one, otherwise worked out from
    /// the expiry date, otherwise valid.
    static func status(
        month: Int?,
        year: Int?,
        override: MonarchPaymentMethodCardExpiry?,
        now: Date
    ) -> MonarchPaymentMethodCardExpiry {
        if let override { return override }
        guard let month, let year else { return .valid }
        return MonarchPaymentMethodCardExpiry.status(month: month, year: year, now: now)
    }

    /// The holder and the expiry as separate parts, for the web's side-by-side layout; the date of an expired card
    /// is marked so it can be drawn in the danger colour.
    static func metaParts(
        holderName: String?,
        date: String?,
        status: MonarchPaymentMethodCardExpiry,
        labels: MonarchPaymentMethodCardLabels
    ) -> [(text: String, isExpired: Bool)] {
        var parts: [(text: String, isExpired: Bool)] = []
        if let holderName, !holderName.isEmpty { parts.append((holderName, false)) }
        if let date {
            if status == .expired {
                parts.append((labels.expiredOn?(date) ?? MonarchPaymentMethodCardStrings.expiredOn(date), true))
            } else {
                parts.append((labels.expires?(date) ?? MonarchPaymentMethodCardStrings.expires(date), false))
            }
        }
        return parts
    }

    /// The holder and expiry line, or `nil` when there is neither.
    static func meta(
        holderName: String?,
        date: String?,
        status: MonarchPaymentMethodCardExpiry,
        labels: MonarchPaymentMethodCardLabels
    ) -> String? {
        var parts: [String] = []
        if let holderName, !holderName.isEmpty { parts.append(holderName) }
        if let date {
            if status == .expired {
                parts.append(labels.expiredOn?(date) ?? MonarchPaymentMethodCardStrings.expiredOn(date))
            } else {
                parts.append(labels.expires?(date) ?? MonarchPaymentMethodCardStrings.expires(date))
            }
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    /// The text and tone of the expiry badge, or `nil` for a valid card.
    static func expiryBadge(
        _ status: MonarchPaymentMethodCardExpiry,
        labels: MonarchPaymentMethodCardLabels
    ) -> (text: String, tone: MonarchTone)? {
        switch status {
        case .valid: nil
        case .expiring: (labels.expiringBadge ?? MonarchPaymentMethodCardStrings.expiringBadge, .warning)
        case .expired: (labels.expiredBadge ?? MonarchPaymentMethodCardStrings.expiredBadge, .danger)
        }
    }

    /// The spoken name of an action button: its title, then the card.
    static func actionLabel(_ action: String, name: String, labels: MonarchPaymentMethodCardLabels) -> String {
        labels.actionLabel?(action, name) ?? MonarchPaymentMethodCardStrings.actionLabel(action: action, name: name)
    }

    /// Whether Make default is offered: not on the default card, and only
    /// when the app handles it.
    static func showsMakeDefault(isDefault: Bool, hasAction: Bool) -> Bool {
        !isDefault && hasAction
    }

    /// Whether Make default can be used: an expired card cannot be the default.
    static func canMakeDefault(status: MonarchPaymentMethodCardExpiry) -> Bool {
        status != .expired
    }

    /// The text shown for a failed action.
    static func failure(_ error: any Error, labels: MonarchPaymentMethodCardLabels) -> String {
        MonarchFailureMessage.message(for: error, fallback: labels.failed ?? MonarchPaymentMethodCardStrings.failed)
    }
}

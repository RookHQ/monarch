import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchCookieConsentBanner``.
enum MonarchCookieConsentBannerTokens {
    /// The padding inside the card.
    static let padding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the text and the actions.
    static let spacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between the title and the message.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The elevation of the card.
    static let elevation: MonarchElevationLevel = .lg
    /// The fill of the card.
    static var fill: MonarchColorRole { \.bgRaised }
    /// The border of the card.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The colour of the message.
    static var messageColor: MonarchColorRole { \.textMuted }
    /// The colour of the policy link.
    static var linkColor: MonarchColorRole { \.textLink }

    /// The buttons a regime shows, in order.
    static func choices(
        regime: MonarchConsentRegime,
        isGlobalPrivacyControl: Bool,
        allowsCustomize: Bool
    ) -> [MonarchCookieConsentBannerAction] {
        switch regime {
        case .optIn:
            (allowsCustomize ? [.customize] : []) + [.choice(.rejectAll), .choice(.acceptAll)]
        case .optOut:
            (isGlobalPrivacyControl ? [] : [.choice(.optOutOfSaleOrSharing)]) + [.choice(.acknowledge)]
        case .notice:
            [.choice(.acknowledge)]
        }
    }

    /// The built-in message of a regime.
    static func message(_ regime: MonarchConsentRegime) -> String {
        switch regime {
        case .optIn: MonarchCookieConsentBannerStrings.messageOptIn
        case .optOut: MonarchCookieConsentBannerStrings.messageOptOut
        case .notice: MonarchCookieConsentBannerStrings.messageNotice
        }
    }

    /// The title of a button.
    static func title(_ action: MonarchCookieConsentBannerAction, labels: MonarchCookieConsentBannerLabels) -> String {
        switch action {
        case .customize: labels.customize ?? MonarchCookieConsentBannerStrings.customize
        case .choice(.acceptAll): labels.acceptAll ?? MonarchCookieConsentBannerStrings.acceptAll
        case .choice(.rejectAll): labels.rejectAll ?? MonarchCookieConsentBannerStrings.rejectAll
        case .choice(.optOutOfSaleOrSharing): labels.doNotSell ?? MonarchCookieConsentBannerStrings.doNotSell
        case .choice(.acknowledge): labels.acknowledge ?? MonarchCookieConsentBannerStrings.acknowledge
        }
    }

    /// The variant of a button: Customize is a ghost button, the choices are equal secondary buttons, so
    /// rejecting is as easy as accepting.
    static func variant(_ action: MonarchCookieConsentBannerAction) -> MonarchButtonVariant {
        switch action {
        case .customize: .ghost
        case .choice: .secondary
        }
    }
}

/// One button of the banner.
enum MonarchCookieConsentBannerAction: Hashable {
    /// Opens the per-category choices.
    case customize
    /// Records a choice.
    case choice(MonarchCookieConsentChoice)
}

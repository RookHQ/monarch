import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCookieConsentBannerFixtures: ComponentFixtures {
    static let id = "cookie-consent-banner"
    static let fixtures = [
        Fixture("opt-in with a policy link") {
            MonarchCookieConsentBanner(policyURL: URL(string: "https://example.com/cookies"), onCustomize: {}) { _ in }
        },
        Fixture("opt-out") {
            MonarchCookieConsentBanner(
                regime: .optOut,
                labels: MonarchCookieConsentBannerLabels(title: "Your privacy choices")
            ) { _ in }
        },
        Fixture("opt-out with Global Privacy Control") {
            MonarchCookieConsentBanner(regime: .optOut, isGlobalPrivacyControl: true) { _ in }
        },
        Fixture("notice") {
            MonarchCookieConsentBanner(
                regime: .notice,
                labels: MonarchCookieConsentBannerLabels(title: "About cookies")
            ) { _ in }
        },
    ]
}

@Suite("MonarchCookieConsentBanner")
struct MonarchCookieConsentBannerTests {
    @Test("Opt-in offers Customize, Reject all and Accept all, in that order")
    func optIn() {
        #expect(
            MonarchCookieConsentBannerTokens.choices(
                regime: .optIn,
                isGlobalPrivacyControl: false,
                allowsCustomize: true
            )
                == [.customize, .choice(.rejectAll), .choice(.acceptAll)]
        )
        #expect(
            MonarchCookieConsentBannerTokens.choices(
                regime: .optIn,
                isGlobalPrivacyControl: false,
                allowsCustomize: false
            )
                == [.choice(.rejectAll), .choice(.acceptAll)]
        )
    }

    @Test("Opt-out offers the opt-out and OK, without the opt-out once a signal was honoured")
    func optOut() {
        #expect(
            MonarchCookieConsentBannerTokens.choices(
                regime: .optOut,
                isGlobalPrivacyControl: false,
                allowsCustomize: true
            )
                == [.choice(.optOutOfSaleOrSharing), .choice(.acknowledge)]
        )
        #expect(
            MonarchCookieConsentBannerTokens.choices(
                regime: .optOut,
                isGlobalPrivacyControl: true,
                allowsCustomize: true
            )
                == [.choice(.acknowledge)]
        )
    }

    @Test("A notice offers OK")
    func notice() {
        #expect(
            MonarchCookieConsentBannerTokens.choices(
                regime: .notice,
                isGlobalPrivacyControl: false,
                allowsCustomize: true
            )
                == [.choice(.acknowledge)]
        )
    }

    @Test("Rejecting is as prominent as accepting")
    func equalChoices() {
        #expect(
            MonarchCookieConsentBannerTokens.variant(.choice(.rejectAll))
                == MonarchCookieConsentBannerTokens.variant(.choice(.acceptAll))
        )
        #expect(MonarchCookieConsentBannerTokens.variant(.customize) == .ghost)
    }

    @Test("Titles come from the labels, else the built-in strings")
    func titles() {
        let labels = MonarchCookieConsentBannerLabels(acceptAll: "Allow all")
        #expect(MonarchCookieConsentBannerTokens.title(.choice(.acceptAll), labels: labels) == "Allow all")
        #expect(MonarchCookieConsentBannerTokens.title(.choice(.rejectAll), labels: labels) == "Reject all")
        #expect(MonarchCookieConsentBannerTokens.title(.customize, labels: labels) == "Customize")
        #expect(MonarchCookieConsentBannerTokens.title(.choice(.acknowledge), labels: labels) == "OK")
        #expect(
            MonarchCookieConsentBannerTokens.title(.choice(.optOutOfSaleOrSharing), labels: labels)
                == "Do not sell or share my personal information"
        )
    }

    @Test("Each regime has its own message")
    func messages() {
        let messages = [MonarchConsentRegime.optIn, .optOut, .notice].map(MonarchCookieConsentBannerTokens.message)
        #expect(Set(messages).count == 3)
        #expect(messages[2] == "We use cookies to make this site work and to understand how it is used.")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchCookieConsentBannerStrings.title == "Cookies on this site")
        #expect(MonarchCookieConsentBannerStrings.policy == "Cookie policy")
        #expect(MonarchCookieConsentBannerStrings.announcement != "monarch.cookieConsentBanner.announcement")
        #expect(
            MonarchCookieConsentBannerStrings.globalPrivacyControl != "monarch.cookieConsentBanner.globalPrivacyControl"
        )
    }
}

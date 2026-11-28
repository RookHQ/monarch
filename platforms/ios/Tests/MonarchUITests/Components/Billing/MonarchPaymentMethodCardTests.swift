import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private func date(_ year: Int, _ month: Int, _ day: Int = 15) -> Date {
    var components = DateComponents()
    components.year = year
    components.month = month
    components.day = day
    components.hour = 12
    return Calendar(identifier: .gregorian).date(from: components) ?? .distantPast
}

private let today = date(2_026, 11)

enum MonarchPaymentMethodCardFixtures: ComponentFixtures {
    static let id = "payment-method-card"
    static let fixtures = [
        Fixture("default card, valid") {
            MonarchPaymentMethodCard(
                brand: "visa",
                last4: "4242",
                expiryMonth: 8,
                expiryYear: 2_030,
                holderName: "Ana Silva",
                now: today,
                isDefault: true,
                onMakeDefault: {},
                onRemove: {}
            )
        },
        Fixture("expiring, with both actions") {
            MonarchPaymentMethodCard(
                brand: "mastercard",
                last4: "4444",
                expiryMonth: 12,
                expiryYear: 26,
                now: today,
                onMakeDefault: {},
                onRemove: {}
            )
        },
        Fixture("expired, make default disabled") {
            MonarchPaymentMethodCard(
                brand: "amex",
                last4: "0005",
                expiryMonth: 1,
                expiryYear: 2_025,
                now: today,
                onMakeDefault: {},
                onRemove: {}
            )
        },
        Fixture("no actions, unknown network, brand image") {
            MonarchPaymentMethodCard(
                brand: "Rook Pay",
                last4: "9999",
                brandImage: Image(systemName: "wallet.pass")
            )
        },
        Fixture("remove disabled, extra action, long holder name") {
            MonarchPaymentMethodCard(
                brand: "rupay",
                last4: "1234",
                expiryMonth: 3,
                expiryYear: 2_031,
                holderName: "Maria Fernanda de Oliveira Santos Albuquerque",
                now: today,
                isRemoveDisabled: true,
                onRemove: {},
                actions: { MonarchButton("Edit billing address", variant: .ghost, size: .sm) {} }
            )
        },
        Fixture("custom labels") {
            MonarchPaymentMethodCard(
                brand: "visa",
                last4: "4242",
                expiry: .expiring,
                isDefault: true,
                labels: MonarchPaymentMethodCardLabels(
                    name: { brand, last4 in "\(brand) •••• \(last4)" },
                    defaultBadge: "Primary",
                    expiringBadge: "Renew soon"
                )
            )
        },
    ]
}

@Suite("MonarchPaymentMethodCard")
struct MonarchPaymentMethodCardTests {
    private let labels = MonarchPaymentMethodCardLabels()

    @Test("Expiry: valid to the end of the month, expiring this month or next")
    func expiryStatus() {
        typealias Expiry = MonarchPaymentMethodCardExpiry
        #expect(Expiry.status(month: 11, year: 2_026, now: today) == .expiring)
        #expect(Expiry.status(month: 12, year: 26, now: today) == .expiring)
        #expect(Expiry.status(month: 1, year: 2_027, now: today) == .valid)
        #expect(Expiry.status(month: 10, year: 2_026, now: today) == .expired)
        #expect(Expiry.status(month: 1, year: 2_027, now: today, soonMonths: 2) == .expiring)
        #expect(Expiry.status(month: 11, year: 2_026, now: date(2_026, 11, 30)) == .expiring)
        #expect(Expiry.status(month: 11, year: 2_026, now: date(2_026, 12, 1)) == .expired)
    }

    @Test("An override wins; no date means valid")
    func statusOverride() {
        let tokens = MonarchPaymentMethodCardTokens.self
        #expect(tokens.status(month: 1, year: 2_020, override: .valid, now: today) == .valid)
        #expect(tokens.status(month: nil, year: nil, override: nil, now: today) == .valid)
        #expect(tokens.status(month: 1, year: 2_020, override: nil, now: today) == .expired)
    }

    @Test("Network ids become display names; other text is kept")
    func brandNames() {
        let tokens = MonarchPaymentMethodCardTokens.self
        #expect(tokens.brandName("visa") == "Visa")
        #expect(tokens.brandName("AMEX") == "American Express")
        #expect(tokens.brandName("diners") == "Diners Club")
        #expect(tokens.brandName("Rook Pay") == "Rook Pay")
        #expect(tokens.name(brand: "visa", last4: "4242", labels: labels) == "Visa ending in 4242")
        let custom = MonarchPaymentMethodCardLabels(name: { "\($0) *\($1)" })
        #expect(tokens.name(brand: "jcb", last4: "0000", labels: custom) == "JCB *0000")
    }

    @Test("The expiry line is MM/YY after the holder")
    func meta() {
        let tokens = MonarchPaymentMethodCardTokens.self
        #expect(tokens.expiryDate(month: 8, year: 2_027) == "08/27")
        #expect(tokens.expiryDate(month: 12, year: 5) == "12/05")
        #expect(tokens.expiryDate(month: nil, year: 2_027) == nil)
        #expect(tokens.meta(holderName: "Ana", date: "08/27", status: .valid, labels: labels) == "Ana · Expires 08/27")
        #expect(tokens.meta(holderName: nil, date: "08/24", status: .expired, labels: labels) == "Expired 08/24")
        #expect(tokens.meta(holderName: "", date: nil, status: .valid, labels: labels) == nil)
    }

    @Test("Expiry badges carry text and a tone; a valid card has none")
    func badges() {
        let tokens = MonarchPaymentMethodCardTokens.self
        #expect(tokens.expiryBadge(.valid, labels: labels) == nil)
        #expect(tokens.expiryBadge(.expiring, labels: labels)?.text == "Expires soon")
        #expect(tokens.expiryBadge(.expiring, labels: labels)?.tone == .warning)
        #expect(tokens.expiryBadge(.expired, labels: labels)?.text == "Expired")
        #expect(tokens.expiryBadge(.expired, labels: labels)?.tone == .danger)
        #expect(tokens.defaultBadge.background == \.accentSubtle)
        #expect(tokens.defaultBadge.foreground == \.accentStrong)
    }

    @Test("Action names include the card; Make default follows the state")
    func actions() {
        let tokens = MonarchPaymentMethodCardTokens.self
        #expect(
            tokens.actionLabel("Remove", name: "Visa ending in 4242", labels: labels) == "Remove, Visa ending in 4242"
        )
        #expect(tokens.showsMakeDefault(isDefault: false, hasAction: true))
        #expect(!tokens.showsMakeDefault(isDefault: true, hasAction: true))
        #expect(!tokens.showsMakeDefault(isDefault: false, hasAction: false))
        #expect(tokens.canMakeDefault(status: .expiring))
        #expect(!tokens.canMakeDefault(status: .expired))
    }

    @Test("A failure shows the error's description, or the built-in text")
    func failure() {
        struct Described: LocalizedError {
            var errorDescription: String? { "The card was declined." }
        }
        struct Plain: Error {}
        let tokens = MonarchPaymentMethodCardTokens.self
        #expect(tokens.failure(Described(), labels: labels) == "The card was declined.")
        #expect(tokens.failure(Plain(), labels: labels) == "That did not work. Try again.")
        #expect(tokens.failure(Plain(), labels: MonarchPaymentMethodCardLabels(failed: "No")) == "No")
    }

    @Test("The built-in strings resolve")
    func strings() {
        let strings = MonarchPaymentMethodCardStrings.self
        #expect(strings.removeTitle("Visa ending in 4242") == "Remove Visa ending in 4242?")
        #expect(strings.removeConfirm == "Remove card")
        #expect(strings.cancel == "Cancel")
        #expect(strings.makeDefault == "Make default")
        #expect(strings.remove == "Remove")
        #expect(strings.defaultBadge == "Default")
        #expect(!strings.removeMessage.hasPrefix("monarch."))
        for id in strings.brandIDs {
            #expect(!strings.brand(id).hasPrefix("monarch."))
        }
    }

    @Test("Actions add height to the card")
    @MainActor
    func actionsRow() {
        let bare = Host.layout(MonarchPaymentMethodCard(brand: "visa", last4: "4242"))
        let withActions = Host.layout(
            MonarchPaymentMethodCard(brand: "visa", last4: "4242", onMakeDefault: {}, onRemove: {})
        )
        #expect(withActions.height > bare.height)
    }
}

import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchPhoneInputFixtures: ComponentFixtures {
    static let id = "phone-input"
    static let english = Locale(identifier: "en_US")

    static let fixtures = [
        Fixture("value with helper") {
            MonarchPhoneInput(
                "Mobile number",
                value: .constant("+442079460958"),
                helper: "For delivery updates.",
                locale: english
            )
        },
        Fixture("empty, prompt, required, preferred, error") {
            MonarchPhoneInput(
                "Phone",
                value: .constant(nil),
                prompt: "Number",
                error: "Enter a phone number.",
                defaultCountry: "PT",
                preferred: ["PT", "ES"],
                locale: english,
                isRequired: true
            )
        },
        Fixture("shared calling code, no flags, custom labels") {
            MonarchPhoneInput(
                "Telefone",
                value: .constant("+12425550100"),
                countries: ["BS", "US", "CA"],
                locale: Locale(identifier: "pt_PT"),
                showsFlags: false,
                labels: MonarchPhoneInputLabels(country: "Indicativo", invalid: "Número incompleto.")
            )
        },
        Fixture("long label, Arabic locale") {
            MonarchPhoneInput(
                "The mobile number we send sign-in codes to when you sign in on a new device",
                value: .constant("+201001234567"),
                locale: Locale(identifier: "ar_EG")
            )
        },
        Fixture("disabled") {
            MonarchPhoneInput("Phone", value: .constant("+12025550100"), locale: english).disabled(true)
        },
    ]
}

@Suite("MonarchPhoneInput")
struct MonarchPhoneInputTests {
    private typealias Tokens = MonarchPhoneInputTokens
    private let english = Locale(identifier: "en_US")

    @Test("E.164 joins the calling code and digits and drops one trunk 0")
    func toE164() {
        #expect(MonarchPhoneInput.toE164(dialCode: "+44", national: "020 7946 0958") == "+442079460958")
        #expect(MonarchPhoneInput.toE164(dialCode: "+1", national: "(202) 555-0100") == "+12025550100")
        #expect(MonarchPhoneInput.toE164(dialCode: "+39", national: "06 1234 5678", country: "IT") == "+390612345678")
        #expect(MonarchPhoneInput.toE164(dialCode: "+39", national: "06 1234 5678", country: "it") == "+390612345678")
        #expect(MonarchPhoneInput.toE164(dialCode: "+44", national: "00 1") == "+4401")
        #expect(MonarchPhoneInput.toE164(dialCode: "+44", national: " -() ") == nil)
        #expect(MonarchPhoneInput.toE164(dialCode: "+44", national: "") == nil)
    }

    @Test("Digits from other scripts and full-width digits are read")
    func digits() {
        #expect(Tokens.digits("(٠٢٠) ٧٩٤٦") == "0207946")
        #expect(Tokens.digits("２０２ ５５５") == "202555")
        #expect(MonarchPhoneInput.toE164(dialCode: "+20", national: "٠١٠٠١٢٣٤٥٦٧") == "+201001234567")
    }

    @Test("E.164 validity: plus, 8 to 15 digits, no leading 0")
    func validity() {
        #expect(MonarchPhoneInput.isValidE164("+442079460958"))
        #expect(MonarchPhoneInput.isValidE164("+12345678"))
        #expect(MonarchPhoneInput.isValidE164("+123456789012345"))
        #expect(!MonarchPhoneInput.isValidE164("+1234567"))
        #expect(!MonarchPhoneInput.isValidE164("+1234567890123456"))
        #expect(!MonarchPhoneInput.isValidE164("+0442079460958"))
        #expect(!MonarchPhoneInput.isValidE164("442079460958"))
        #expect(!MonarchPhoneInput.isValidE164("+44 20 7946"))
        #expect(!MonarchPhoneInput.isValidE164("+٤٤٢٠٧٩٤٦٠٩٥٨"))
    }

    @Test("Parsing finds the country, calling code and national digits")
    func parse() {
        #expect(
            MonarchPhoneInput.parse("+44 20 7946 0958")
                == MonarchPhoneInputDetails(country: "GB", dialCode: "+44", national: "2079460958")
        )
        #expect(MonarchPhoneInput.parse("00351 912 345 678")?.country == "PT")
        #expect(MonarchPhoneInput.parse("00351 912 345 678")?.national == "912345678")
        #expect(MonarchPhoneInput.parse("+1 242 555 0100")?.country == "BS")
        #expect(MonarchPhoneInput.parse("+1 416 555 0100", preferred: "CA")?.country == "CA")
        #expect(MonarchPhoneInput.parse("+1 202 555 0100")?.country == "US")
        #expect(MonarchPhoneInput.parse("020 7946 0958") == nil)
        #expect(MonarchPhoneInput.parse("") == nil)
        #expect(MonarchPhoneInputDetails(country: "gb", dialCode: "+44", national: "1").country == "GB")
    }

    @Test("A value is split for display, or kept as digits when no code matches")
    func split() {
        let gb = Tokens.split("+442079460958", country: "US")
        #expect(gb.country == "GB" && gb.national == "2079460958")
        let empty = Tokens.split(nil, country: "PT")
        #expect(empty.country == "PT" && empty.national.isEmpty)
        let local = Tokens.split("2025550100", country: "US")
        #expect(local.country == "US" && local.national == "2025550100")
    }

    @Test("A pasted international number picks its country; short or local text does not")
    func pasted() {
        let paste = Tokens.pasted("+44 20 7946 0958", country: "US")
        #expect(paste?.country == "GB" && paste?.national == "2079460958")
        #expect(Tokens.pasted("0044 20 7946 0958", country: "US")?.country == "GB")
        #expect(Tokens.pasted("+44 20", country: "US") == nil)
        #expect(Tokens.pasted("202 555 0100", country: "US") == nil)
        #expect(Tokens.pasted("+44 20 79a6 0958", country: "US") == nil)
    }

    @Test("Committing a national number writes E.164 for the country")
    func commitNational() {
        let result = Tokens.commit("020 7946 0958", country: "gb")
        #expect(
            result
                == Tokens.Commit(value: "+442079460958", country: "GB", draft: "020 7946 0958", isInvalid: false)
        )
    }

    @Test("Committing an international number changes the country and the draft")
    func commitInternational() {
        let result = Tokens.commit("+351 912 345 678", country: "US")
        #expect(result == Tokens.Commit(value: "+351912345678", country: "PT", draft: "912345678", isInvalid: false))
    }

    @Test("Committing empty text writes nil and is not an error")
    func commitEmpty() {
        let result = Tokens.commit("  ", country: "US")
        #expect(result.value == nil)
        #expect(!result.isInvalid)
        #expect(result.country == "US")
    }

    @Test("Too short, too long and unknown codes are invalid and write nothing")
    func commitInvalid() {
        #expect(Tokens.commit("123", country: "US").isInvalid)
        #expect(Tokens.commit("1234567890123456", country: "US").isInvalid)
        #expect(Tokens.commit("+999 1234 5678", country: "US").isInvalid)
        #expect(Tokens.commit("123", country: "US").draft == "123")
    }

    @Test("A number under a shared calling code moves to the country it belongs to")
    func commitSharedCode() {
        let result = Tokens.commit("242 555 0100", country: "US")
        #expect(result.value == "+12425550100")
        #expect(result.country == "BS")
        let stays = Tokens.commit("416 555 0100", country: "CA")
        #expect(stays.country == "CA")
        #expect(stays.value == "+14165550100")
    }

    @Test("The button shows the flag and calling code; the spoken value names the country")
    func countryText() {
        #expect(Tokens.buttonText(country: "PT", showsFlags: true) == "🇵🇹 +351")
        #expect(Tokens.buttonText(country: "PT", showsFlags: false) == "+351")
        #expect(Tokens.countryValue(country: "PT", locale: english, option: nil) == "Portugal (+351)")
        #expect(Tokens.countryValue(country: "PT", locale: english, option: { "\($1) \($0)" }) == "+351 Portugal")
    }

    @Test("The own invalid message wins over the app's error and can be replaced")
    func activeError() {
        let labels = MonarchPhoneInputLabels()
        #expect(
            Tokens.activeError(isInvalid: true, error: "App error", labels: labels) == MonarchPhoneInputStrings.invalid
        )
        #expect(Tokens.activeError(isInvalid: false, error: "App error", labels: labels) == "App error")
        #expect(Tokens.activeError(isInvalid: false, error: "", labels: labels) == nil)
        let custom = MonarchPhoneInputLabels(invalid: "Número incompleto.")
        #expect(Tokens.activeError(isInvalid: true, error: nil, labels: custom) == "Número incompleto.")
    }

    @Test("Colours follow the text field for every state")
    func colours() {
        #expect(Tokens.border(hasError: false, isEnabled: true) == \.borderInput)
        #expect(Tokens.border(hasError: true, isEnabled: true) == \.dangerStrong)
        #expect(Tokens.border(hasError: true, isEnabled: false) == \.borderSubtle)
        #expect(Tokens.background(isEnabled: true) == \.bgSurface)
        #expect(Tokens.background(isEnabled: false) == \.bgSubtle)
        #expect(Tokens.textColor(isEnabled: true) == \.textDefault)
        #expect(Tokens.textColor(isEnabled: false) == \.textDisabled)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchPhoneInputStrings.country == "Country code")
        #expect(MonarchPhoneInputStrings.invalid == "Enter a complete phone number.")
    }

    #if os(iOS)
        @Test("The row is at least 44 points high")
        @MainActor
        func touchTarget() {
            let size = Host.layout(MonarchPhoneInput("Phone", value: .constant(nil)))
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

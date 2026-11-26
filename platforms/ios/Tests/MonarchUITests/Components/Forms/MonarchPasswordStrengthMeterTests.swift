import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchPasswordStrengthMeterFixtures: ComponentFixtures {
    static let id = "password-strength-meter"
    static let fixtures = [
        Fixture("empty") {
            MonarchPasswordStrengthMeter(password: "")
        },
        Fixture("every strength") {
            VStack(alignment: .leading) {
                MonarchPasswordStrengthMeter(password: "abc")
                MonarchPasswordStrengthMeter(password: "abcdefgh1")
                MonarchPasswordStrengthMeter(password: "Abcdefgh1")
                MonarchPasswordStrengthMeter(password: "Abcdefg1!")
                MonarchPasswordStrengthMeter(password: "Correct horse 9 battery")
            }
        },
        Fixture("without rules") {
            VStack(alignment: .leading) {
                MonarchPasswordStrengthMeter(password: "Abcdefg1!", showsRules: false)
                MonarchPasswordStrengthMeter(password: "Abcdefg1!", rules: [])
            }
        },
        Fixture("custom rules, score and text") {
            MonarchPasswordStrengthMeter(
                password: "hunter2",
                rules: [
                    MonarchPasswordRule(id: "passphrase", label: "Four words or more, separated by spaces, is best") {
                        $0.split(separator: " ").count >= 4
                    }
                ],
                strengthText: { $0 == nil ? "Type a passphrase" : "Easy to guess" },
                rulesLabel: "Advice",
                ruleText: { label, isMet in isMet ? "Done: \(label)" : "To do: \(label)" },
                score: { _ in .weak }
            )
        },
    ]
}

@Suite("MonarchPasswordStrengthMeter")
struct MonarchPasswordStrengthMeterTests {
    private func rule(_ id: String) -> MonarchPasswordRule? {
        MonarchPasswordRule.defaults.first { $0.id == id }
    }

    @Test("The default rules are length, case, number and symbol, with labels from the strings table")
    func defaultRules() {
        #expect(MonarchPasswordRule.defaults.map(\.id) == ["length", "case", "number", "symbol"])
        #expect(
            MonarchPasswordRule.defaults.map(\.label) == [
                "At least 8 characters", "Upper and lower case letters", "A number", "A symbol or space",
            ]
        )
    }

    @Test("Each default rule tests what its label says, in any script")
    func ruleTests() throws {
        let length = try #require(rule("length"))
        #expect(!length.test("1234567"))
        #expect(length.test("12345678"))

        let letterCase = try #require(rule("case"))
        #expect(!letterCase.test("abc"))
        #expect(!letterCase.test("ABC"))
        #expect(letterCase.test("aB"))
        #expect(letterCase.test("жД"))

        let number = try #require(rule("number"))
        #expect(!number.test("abc"))
        #expect(number.test("abc1"))
        #expect(number.test("abc٣"))

        let symbol = try #require(rule("symbol"))
        #expect(!symbol.test("abc123"))
        #expect(symbol.test("abc 123"))
        #expect(symbol.test("abc!"))
    }

    @Test("The default score is the share of rules met, with a step for length")
    func score() {
        #expect(MonarchPasswordStrength.score("") == .veryWeak)
        #expect(MonarchPasswordStrength.score("abc") == .veryWeak)
        #expect(MonarchPasswordStrength.score("abcdefgh1") == .weak)
        #expect(MonarchPasswordStrength.score("Abcdefgh1") == .fair)
        #expect(MonarchPasswordStrength.score("Abcdefg1!") == .good)
        #expect(MonarchPasswordStrength.score("Correct horse 9 battery") == .strong)
        #expect(MonarchPasswordStrength.score("a", rules: []) == .good)
        #expect(MonarchPasswordStrength.score("abcdefghijklmn", rules: []) == .strong)
    }

    @Test("Strengths are ordered from very weak to strong")
    func ordering() {
        #expect(MonarchPasswordStrength.allCases.sorted() == [.veryWeak, .weak, .fair, .good, .strong])
        #expect(MonarchPasswordStrength.weak < .fair)
    }

    @Test("An empty password has no strength, and a custom score replaces the default")
    func strength() {
        typealias Tokens = MonarchPasswordStrengthMeterTokens
        let rules = MonarchPasswordRule.defaults
        #expect(Tokens.strength(password: "", rules: rules, score: nil) == nil)
        #expect(Tokens.strength(password: "", rules: rules, score: { _ in .strong }) == nil)
        #expect(Tokens.strength(password: "Abcdefg1!", rules: rules, score: nil) == .good)
        #expect(Tokens.strength(password: "abc", rules: rules, score: { _ in .strong }) == .strong)
    }

    @Test("The bar fills one segment per step, and at least one once something is typed")
    func segments() {
        typealias Tokens = MonarchPasswordStrengthMeterTokens
        func filled(_ strength: MonarchPasswordStrength?) -> Int {
            (1...Tokens.segmentCount).filter { Tokens.isFilled(segment: $0, strength: strength) }.count
        }
        #expect(filled(nil) == 0)
        #expect(filled(.veryWeak) == 1)
        #expect(filled(.weak) == 1)
        #expect(filled(.fair) == 2)
        #expect(filled(.good) == 3)
        #expect(filled(.strong) == 4)
    }

    @Test("Filled segments take the colour of the strength; the rest are the track")
    func segmentColours() {
        typealias Tokens = MonarchPasswordStrengthMeterTokens
        #expect(Tokens.segmentFill(segment: 1, strength: nil) == \MonarchColorTokens.bgEmphasis)
        #expect(Tokens.segmentFill(segment: 1, strength: .veryWeak) == \MonarchColorTokens.dangerSolid)
        #expect(Tokens.segmentFill(segment: 1, strength: .weak) == \MonarchColorTokens.dangerSolid)
        #expect(Tokens.segmentFill(segment: 2, strength: .weak) == \MonarchColorTokens.bgEmphasis)
        #expect(Tokens.segmentFill(segment: 2, strength: .fair) == \MonarchColorTokens.warningSolid)
        #expect(Tokens.segmentFill(segment: 3, strength: .good) == \MonarchColorTokens.successSolid)
        #expect(Tokens.segmentFill(segment: 4, strength: .good) == \MonarchColorTokens.bgEmphasis)
        #expect(Tokens.segmentFill(segment: 4, strength: .strong) == \MonarchColorTokens.successSolid)
    }

    @Test("The strength is always in words")
    func strengthText() {
        typealias Tokens = MonarchPasswordStrengthMeterTokens
        #expect(Tokens.strengthText(nil) == "Password strength: Not entered")
        #expect(Tokens.strengthText(.veryWeak) == "Password strength: Very weak")
        #expect(Tokens.strengthText(.weak) == "Password strength: Weak")
        #expect(Tokens.strengthText(.fair) == "Password strength: Fair")
        #expect(Tokens.strengthText(.good) == "Password strength: Good")
        #expect(Tokens.strengthText(.strong) == "Password strength: Strong")
    }

    @Test("A rule is read as met or not met, and differs by icon as well as colour")
    func rules() throws {
        typealias Tokens = MonarchPasswordStrengthMeterTokens
        let number = try #require(rule("number"))
        #expect(!Tokens.isMet(number, password: ""))
        #expect(!Tokens.isMet(number, password: "abc"))
        #expect(Tokens.isMet(number, password: "abc1"))
        #expect(Tokens.ruleText("A number", isMet: true) == "Met: A number")
        #expect(Tokens.ruleText("A number", isMet: false) == "Not met: A number")
        #expect(Tokens.ruleSymbol(isMet: true) != Tokens.ruleSymbol(isMet: false))
        #expect(Tokens.ruleForeground(isMet: true) == \MonarchColorTokens.successStrong)
        #expect(Tokens.ruleForeground(isMet: false) == \MonarchColorTokens.textMuted)
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchPasswordStrengthMeterStrings.empty == "Not entered")
        #expect(MonarchPasswordStrengthMeterStrings.rulesLabel == "Password requirements")
        #expect(MonarchPasswordStrength.allCases.map(MonarchPasswordStrengthMeterStrings.level).count == 5)
        #expect(Set(MonarchPasswordStrength.allCases.map(MonarchPasswordStrengthMeterStrings.level)).count == 5)
    }
}

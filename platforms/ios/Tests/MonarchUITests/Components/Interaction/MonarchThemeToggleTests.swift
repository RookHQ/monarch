import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchThemeToggleFixtures: ComponentFixtures {
    static let id = "theme-toggle"
    static let fixtures = [
        Fixture("segmented, every choice and size") {
            VStack {
                ForEach(MonarchThemeToggleSize.allCases, id: \.self) { size in
                    ForEach(MonarchSchemePreference.allCases, id: \.self) { preference in
                        MonarchThemeToggle(selection: .constant(preference), size: size)
                    }
                }
            }
        },
        Fixture("menu, with and without the label") {
            VStack {
                ForEach(MonarchThemeToggleSize.allCases, id: \.self) { size in
                    HStack {
                        MonarchThemeToggle(selection: .constant(.light), variant: .menu, size: size)
                        MonarchThemeToggle(selection: .constant(.dark), variant: .menu, size: size, showsLabel: false)
                        MonarchThemeToggle(selection: .constant(.system), variant: .menu, size: size)
                    }
                }
            }
        },
        Fixture("disabled") {
            VStack {
                MonarchThemeToggle(selection: .constant(.dark)).disabled(true)
                MonarchThemeToggle(selection: .constant(.dark), variant: .menu).disabled(true)
            }
        },
        Fixture("translated, long names") {
            MonarchThemeToggle(
                selection: .constant(.system),
                labels: MonarchThemeToggleLabels(
                    title: "Erscheinungsbild",
                    light: "Hell",
                    dark: "Dunkel",
                    system: "Wie das Gerät eingestellt ist"
                )
            )
        },
    ]
}

@Suite("MonarchThemeToggle")
struct MonarchThemeToggleTests {
    typealias Tokens = MonarchThemeToggleTokens

    @Test("The choices are light, dark and system, in the web's order")
    func order() {
        #expect(Tokens.order == [.light, .dark, .system])
        #expect(Set(Tokens.order).count == MonarchSchemePreference.allCases.count)
    }

    @Test("Every choice has its own symbol")
    func symbols() {
        let symbols = Tokens.order.map(Tokens.systemImage)
        #expect(Set(symbols).count == symbols.count)
        #expect(Tokens.systemImage(.light) == "sun.max")
        #expect(Tokens.systemImage(.dark) == "moon")
    }

    @Test("The built-in names resolve and differ")
    func builtInNames() {
        let names = Tokens.order.map { Tokens.name($0, labels: MonarchThemeToggleLabels()) }
        #expect(names == ["Light", "Dark", "System"])
        #expect(Tokens.title(labels: MonarchThemeToggleLabels()) == "Theme")
    }

    @Test("Overrides replace only the strings given")
    func overrides() {
        let labels = MonarchThemeToggleLabels(title: "Erscheinungsbild", dark: "Dunkel")
        #expect(Tokens.title(labels: labels) == "Erscheinungsbild")
        #expect(Tokens.name(.dark, labels: labels) == "Dunkel")
        #expect(Tokens.name(.light, labels: labels) == "Light")
        #expect(Tokens.name(.system, labels: labels) == "System")
    }

    @Test("The stored text round-trips and matches the web cookie values", arguments: MonarchSchemePreference.allCases)
    func storage(preference: MonarchSchemePreference) {
        let stored = MonarchThemeToggle.storageValue(for: preference)
        #expect(["light", "dark", "system"].contains(stored))
        #expect(MonarchThemeToggle.preference(storageValue: stored) == preference)
    }

    @Test("Unknown stored text gives no choice")
    func unknownStorage() {
        #expect(MonarchThemeToggle.preference(storageValue: nil) == nil)
        #expect(MonarchThemeToggle.preference(storageValue: "") == nil)
        #expect(MonarchThemeToggle.preference(storageValue: "Dark") == nil)
        #expect(MonarchThemeToggle.preference(storageValue: "sepia") == nil)
    }

    @Test("Sizes map to system control sizes and button sizes")
    func sizes() {
        #expect(Tokens.controlSize(.md) == .large)
        #expect(Tokens.controlSize(.sm) == .regular)
        #expect(Tokens.buttonSize(.md) == .md)
        #expect(Tokens.buttonSize(.sm) == .sm)
    }

    #if os(iOS)
        @Test("The menu button keeps a 44-point touch target at the small size")
        @MainActor
        func menuTouchTarget() {
            let size = Host.layout(
                MonarchThemeToggle(selection: .constant(.light), variant: .menu, size: .sm, showsLabel: false)
                    .fixedSize()
            )
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

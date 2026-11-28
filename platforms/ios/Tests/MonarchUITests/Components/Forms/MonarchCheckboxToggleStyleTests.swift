import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCheckboxToggleStyleFixtures: ComponentFixtures {
    static let id = "checkbox"
    static let fixtures = [
        Fixture("states") {
            VStack(alignment: .leading) {
                Toggle("Unchecked", isOn: .constant(false))
                Toggle("Checked", isOn: .constant(true))
                Toggle(sources: [Binding.constant(true), .constant(false)], isOn: \.self) { Text("Mixed") }
                Toggle("Disabled", isOn: .constant(true)).disabled(true)
            }
            .toggleStyle(.monarchCheckbox)
        },
        Fixture("long label") {
            Toggle("I agree to the terms of service and to the privacy policy of this product", isOn: .constant(true))
                .toggleStyle(.monarchCheckbox)
        },
    ]
}

@Suite("MonarchCheckboxToggleStyle")
struct MonarchCheckboxToggleStyleTests {
    @Test("Colours follow the marked and enabled states")
    func appearance() {
        let checked = MonarchCheckboxTokens.appearance(isMarked: true, isEnabled: true)
        #expect(checked.fill == \MonarchColorTokens.accentStrong)
        #expect(checked.glyph == \MonarchColorTokens.accentOnStrong)

        let unchecked = MonarchCheckboxTokens.appearance(isMarked: false, isEnabled: true)
        #expect(unchecked.fill == \MonarchColorTokens.bgSurface)
        #expect(unchecked.border == \MonarchColorTokens.borderInput)

        let disabled = MonarchCheckboxTokens.appearance(isMarked: true, isEnabled: false)
        #expect(disabled.fill == \MonarchColorTokens.accentStrong)
        #expect(disabled.glyph == \MonarchColorTokens.accentOnStrong)
    }

    @Test("The checked and mixed states use different glyphs")
    func glyphs() {
        #expect(MonarchCheckboxTokens.checkedSymbol != MonarchCheckboxTokens.mixedSymbol)
    }
}

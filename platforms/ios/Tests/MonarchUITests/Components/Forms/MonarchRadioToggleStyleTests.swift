import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchRadioToggleStyleFixtures: ComponentFixtures {
    static let id = "radio-group"
    static let fixtures = [
        Fixture("states") {
            VStack(alignment: .leading) {
                Toggle("Monthly", isOn: .constant(true))
                Toggle("Yearly", isOn: .constant(false))
                Toggle("Disabled, chosen", isOn: .constant(true)).disabled(true)
                Toggle("Disabled", isOn: .constant(false)).disabled(true)
            }
            .toggleStyle(.monarchRadio)
        },
        Fixture("long label") {
            Toggle("A choice whose label is long enough to wrap onto a second line on a phone", isOn: .constant(true))
                .toggleStyle(.monarchRadio)
        },
    ]
}

@Suite("MonarchRadioToggleStyle")
struct MonarchRadioToggleStyleTests {
    @Test("The ring is 16 points and the dot is 10")
    func sizes() {
        #expect(MonarchRadioTokens.ringSize == 16)
        #expect(MonarchRadioTokens.dotSize == 10)
        #expect(MonarchRadioTokens.dotSize < MonarchRadioTokens.ringSize)
    }

    @Test("A selected radio has the strong accent border and dot")
    func selected() {
        let selected = MonarchRadioTokens.appearance(isSelected: true, isEnabled: true)
        #expect(selected.border == \MonarchColorTokens.accentStrong)
        #expect(selected.dot == \MonarchColorTokens.accentStrong)
        #expect(selected.fill == \MonarchColorTokens.bgSurface)
    }

    @Test("An unselected radio has the input border")
    func unselected() {
        let unselected = MonarchRadioTokens.appearance(isSelected: false, isEnabled: true)
        #expect(unselected.border == \MonarchColorTokens.borderInput)
    }

    @Test("A disabled radio keeps its enabled colours")
    func disabled() {
        for isSelected in [true, false] {
            let appearance = MonarchRadioTokens.appearance(isSelected: isSelected, isEnabled: false)
            #expect(appearance == MonarchRadioTokens.appearance(isSelected: isSelected, isEnabled: true))
        }
        #expect(MonarchRadioTokens.labelColor(isEnabled: false) == \MonarchColorTokens.textDefault)
    }
}

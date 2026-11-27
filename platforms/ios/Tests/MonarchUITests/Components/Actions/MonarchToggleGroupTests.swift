import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let formatting = [
    MonarchToggleGroupOption("bold", label: "Bold", systemImage: "bold", isIconOnly: true),
    MonarchToggleGroupOption("italic", label: "Italic", systemImage: "italic", isIconOnly: true),
    MonarchToggleGroupOption("underline", label: "Underline", systemImage: "underline", isIconOnly: true),
]

private let statuses = [
    MonarchToggleGroupOption("open", label: "Open", count: "12"),
    MonarchToggleGroupOption("pending", label: "Waiting for the customer to reply", count: "1,204"),
    MonarchToggleGroupOption("closed", label: "Closed", isDisabled: true),
]

enum MonarchToggleGroupFixtures: ComponentFixtures {
    static let id = "toggle-group"
    static let fixtures = [
        Fixture("multiple, icon only, every variant and size") {
            VStack(alignment: .leading) {
                ForEach(MonarchToggleButtonVariant.allCases, id: \.self) { variant in
                    ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                        MonarchToggleGroup(
                            "Formatting",
                            selection: .constant(Set(["bold", "underline"])),
                            options: formatting,
                            variant: variant,
                            size: size
                        )
                    }
                }
            }
        },
        Fixture("single, counts, long label, disabled item") {
            MonarchToggleGroup("Status", selection: .constant(Optional("open")), options: statuses, isRequired: true)
        },
        Fixture("nothing selected, vertical") {
            MonarchToggleGroup("Status", selection: .constant(String?.none), options: statuses, axis: .vertical)
        },
        Fixture("whole group disabled") {
            MonarchToggleGroup("Formatting", selection: .constant(Set(["bold"])), options: formatting)
                .disabled(true)
        },
    ]
}

@Suite("MonarchToggleGroup")
struct MonarchToggleGroupTests {
    @Test("Multiple choice adds and removes values")
    func multiple() {
        let start: Set<String> = ["bold"]
        #expect(MonarchToggleGroupTokens.toggled(start, value: "italic", isOn: true) == ["bold", "italic"])
        #expect(MonarchToggleGroupTokens.toggled(start, value: "bold", isOn: false).isEmpty)
        #expect(MonarchToggleGroupTokens.toggled(start, value: "italic", isOn: false) == ["bold"])
    }

    @Test("Single choice replaces the selection and clears it unless required")
    func single() {
        #expect(MonarchToggleGroupTokens.toggled("list", value: "board", isOn: true, isRequired: false) == "board")
        #expect(MonarchToggleGroupTokens.toggled(String?.none, value: "list", isOn: true, isRequired: true) == "list")
        #expect(MonarchToggleGroupTokens.toggled("list", value: "list", isOn: false, isRequired: false) == nil)
        #expect(MonarchToggleGroupTokens.toggled("list", value: "list", isOn: false, isRequired: true) == "list")
        #expect(MonarchToggleGroupTokens.toggled("list", value: "board", isOn: false, isRequired: false) == "list")
    }

    @Test("VoiceOver reads the label, then the count")
    func accessibilityLabel() {
        #expect(MonarchToggleGroupTokens.accessibilityLabel(label: "Open", count: "12") == "Open, 12")
        #expect(MonarchToggleGroupTokens.accessibilityLabel(label: "Open", count: nil) == "Open")
        #expect(MonarchToggleGroupTokens.accessibilityLabel(label: "Open", count: "") == "Open")
    }

    @Test("An icon-only item without a symbol still shows its text")
    func showsText() {
        #expect(!MonarchToggleGroupTokens.showsText(isIconOnly: true, systemImage: "bold"))
        #expect(MonarchToggleGroupTokens.showsText(isIconOnly: true, systemImage: nil))
        #expect(MonarchToggleGroupTokens.showsText(isIconOnly: false, systemImage: "bold"))
    }

    @Test("The count uses the subtle role, and the disabled role when disabled")
    func countColour() {
        #expect(MonarchToggleGroupTokens.count(isEnabled: true) == \MonarchColorTokens.textSubtle)
        #expect(MonarchToggleGroupTokens.count(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("A horizontal group wraps instead of growing wider than the screen")
    @MainActor
    func wraps() {
        let many = (1...12).map { MonarchToggleGroupOption($0, label: "Option \($0)") }
        let row = Host.layout(MonarchToggleGroup("Options", selection: .constant(Set<Int>()), options: many))
        let one = Host.layout(
            MonarchToggleGroup("Options", selection: .constant(Set<Int>()), options: Array(many.prefix(1)))
        )
        #expect(row.width <= Host.width)
        #expect(row.height > one.height)
    }
}

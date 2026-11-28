import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let segmentedFixtureItems = [
    MonarchSegmentedItem("List", value: "list", systemImage: "list.bullet"),
    MonarchSegmentedItem("Board", value: "board", systemImage: "rectangle.split.3x1", count: "4"),
    MonarchSegmentedItem("Grid", value: "grid", systemImage: "square.grid.2x2", isDisabled: true),
]

enum MonarchSegmentedControlFixtures: ComponentFixtures {
    static let id = "segmented-control"
    static let fixtures = [
        Fixture("sizes") {
            VStack {
                ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                    MonarchSegmentedControl(
                        "View",
                        selection: .constant("board"),
                        items: segmentedFixtureItems,
                        size: size
                    )
                }
            }
        },
        Fixture("icon only, natural width") {
            MonarchSegmentedControl(
                "View",
                selection: .constant("list"),
                items: [
                    MonarchSegmentedItem("List", value: "list", systemImage: "list.bullet", isIconOnly: true),
                    MonarchSegmentedItem("Grid", value: "grid", systemImage: "square.grid.2x2", isIconOnly: true),
                ],
                fillsWidth: false
            )
        },
        Fixture("more segments than fit, disabled control") {
            VStack {
                MonarchSegmentedControl(
                    "Months",
                    selection: .constant(3),
                    items: (1...12).map { MonarchSegmentedItem("Month \($0)", value: $0) }
                )
                MonarchSegmentedControl("View", selection: .constant("list"), items: segmentedFixtureItems)
                    .disabled(true)
                MonarchSegmentedControl("None", selection: .constant("x"), items: [MonarchSegmentedItem<String>]())
            }
        },
    ]
}

@Suite("MonarchSegmentedControl")
struct MonarchSegmentedControlTests {
    @Test("The track is 32, 36 or 40 points; a segment is the track less its padding")
    func heights() {
        #expect(MonarchSegmentedControlTokens.trackHeight(.sm) == 32)
        #expect(MonarchSegmentedControlTokens.trackHeight(.md) == 36)
        #expect(MonarchSegmentedControlTokens.trackHeight(.lg) == 40)
        // The web segment is the track height less 4 (2 points of padding on each side).
        #expect(MonarchSegmentedControlTokens.segmentHeight(.md) == 32)
    }

    @Test("In dark the pill is a step above the track with a stronger hairline")
    func pillAppearance() {
        #expect(MonarchSegmentedControlTokens.pillAppearance(isDark: true).background == \MonarchColorTokens.bgEmphasis)
        #expect(MonarchSegmentedControlTokens.pillAppearance(isDark: false).background == \MonarchColorTokens.bgSurface)
    }

    @Test("The selected pill has a shadow in light and none in dark")
    func pillElevation() {
        #expect(MonarchSegmentedControlTokens.pillElevation(isDark: false) == .xs)
        #expect(MonarchSegmentedControlTokens.pillElevation(isDark: true) == .none)
    }

    @Test("The selected segment has the default text, the others the muted one, a disabled one the disabled one")
    func contentColor() {
        #expect(
            MonarchSegmentedControlTokens.contentColor(isSelected: true, isEnabled: true)
                == \MonarchColorTokens.textDefault
        )
        #expect(
            MonarchSegmentedControlTokens.contentColor(isSelected: false, isEnabled: true)
                == \MonarchColorTokens.textMuted
        )
        #expect(
            MonarchSegmentedControlTokens.contentColor(isSelected: true, isEnabled: false)
                == \MonarchColorTokens.textDisabled
        )
    }

    @Test("The selection is marked by a raised surface, not by colour alone")
    func pill() {
        #expect(MonarchSegmentedControlTokens.pillAppearance(isDark: false).background == \MonarchColorTokens.bgSurface)
        #expect(MonarchSegmentedControlTokens.pillAppearance(isDark: false).border != nil)
        #expect(MonarchSegmentedControlTokens.trackColor == \MonarchColorTokens.bgMuted)
    }

    @Test("The count is read after the title")
    func accessibilityLabel() {
        #expect(MonarchSegmentedControlTokens.accessibilityLabel(title: "Open", count: "12") == "Open, 12")
        #expect(MonarchSegmentedControlTokens.accessibilityLabel(title: "Open", count: "") == "Open")
        #expect(MonarchSegmentedControlTokens.accessibilityLabel(title: "Open", count: nil) == "Open")
    }

    @Test("An icon-only segment needs a symbol, else the title is shown")
    func iconOnly() {
        #expect(MonarchSegmentedControlTokens.showsIconOnly(isIconOnly: true, systemImage: "list.bullet"))
        #expect(!MonarchSegmentedControlTokens.showsIconOnly(isIconOnly: true, systemImage: nil))
        #expect(!MonarchSegmentedControlTokens.showsIconOnly(isIconOnly: false, systemImage: "list.bullet"))
    }

    @Test("A small control uses the caption text, the others the label text")
    func textStyle() {
        #expect(MonarchSegmentedControlTokens.textStyle(.sm) == .caption)
        #expect(MonarchSegmentedControlTokens.textStyle(.md) == .label)
    }

    @Test("A segment is identified by its value")
    func item() {
        let item = MonarchSegmentedItem("Open", value: 3, count: "2", isDisabled: true)
        #expect(item.id == 3)
        #expect(item.count == "2")
        #expect(item.isDisabled)
    }
}

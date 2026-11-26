import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchColorPickerFixtures: ComponentFixtures {
    static let id = "color-picker"
    static let fixtures = [
        Fixture("default swatches, one selected, helper") {
            MonarchColorPicker("Label colour", selection: .constant("#2563eb"), helper: "Shown on the board.")
        },
        Fixture("no selection") {
            MonarchColorPicker("Label colour", selection: .constant(nil))
        },
        Fixture("custom colour that is not a swatch") {
            MonarchColorPicker("Label colour", selection: .constant("#123456"), customLabel: "Any colour")
        },
        Fixture("swatches only") {
            MonarchColorPicker("Label colour", selection: .constant("#dc2626"), allowsCustom: false)
        },
        Fixture("custom row only, custom empty text") {
            MonarchColorPicker("Brand colour", selection: .constant(nil), swatches: [], emptyLabel: "Not set")
        },
        Fixture("own swatches with long names") {
            MonarchColorPicker(
                "A long label that wraps onto a second line at the largest text size",
                selection: .constant("#ffffff"),
                swatches: [
                    MonarchColorSwatch(hex: "#fff", name: "White"),
                    MonarchColorSwatch(hex: "000000", name: "Black"),
                    MonarchColorSwatch(hex: "not a colour", name: "Skipped"),
                ].compactMap { $0 }
            )
        },
        Fixture("disabled") {
            MonarchColorPicker("Label colour", selection: .constant("#16a34a"), helper: "Locked.").disabled(true)
        },
    ]
}

@Suite("MonarchColorPicker")
struct MonarchColorPickerTests {
    @Test("Hex text is normalised to lower-case #rrggbb")
    func normalizedHex() {
        #expect(MonarchColorSwatch.normalizedHex("#2563EB") == "#2563eb")
        #expect(MonarchColorSwatch.normalizedHex("2563eb") == "#2563eb")
        #expect(MonarchColorSwatch.normalizedHex("  #FfF ") == "#ffffff")
        #expect(MonarchColorSwatch.normalizedHex("a1c") == "#aa11cc")
        #expect(MonarchColorSwatch.normalizedHex("") == nil)
        #expect(MonarchColorSwatch.normalizedHex("#") == nil)
        #expect(MonarchColorSwatch.normalizedHex("#12345") == nil)
        #expect(MonarchColorSwatch.normalizedHex("#1234567") == nil)
        #expect(MonarchColorSwatch.normalizedHex("#gggggg") == nil)
        #expect(MonarchColorSwatch.normalizedHex("##123456") == nil)
        #expect(MonarchColorSwatch.normalizedHex("blue") == nil)
    }

    @Test("Hex text and sRGB components convert both ways")
    func components() throws {
        let blue = try #require(MonarchColorPickerTokens.components(hex: "#2563eb"))
        #expect(abs(blue.red - 37.0 / 255) < 0.000_001)
        #expect(abs(blue.green - 99.0 / 255) < 0.000_001)
        #expect(abs(blue.blue - 235.0 / 255) < 0.000_001)
        #expect(MonarchColorPickerTokens.components(hex: "nope") == nil)
        #expect(MonarchColorPickerTokens.hex(red: blue.red, green: blue.green, blue: blue.blue) == "#2563eb")
        #expect(MonarchColorPickerTokens.hex(red: 0, green: 0, blue: 0) == "#000000")
        #expect(MonarchColorPickerTokens.hex(red: 1, green: 1, blue: 1) == "#ffffff")
        #expect(MonarchColorPickerTokens.hex(red: 0.02, green: 0.5, blue: 0.999) == "#0580ff")
    }

    @Test("Components outside 0 to 1 are clamped")
    func clamps() {
        #expect(MonarchColorPickerTokens.hex(red: 1.4, green: -0.2, blue: .nan) == "#ff0000")
    }

    @Test("A colour from the system picker comes back as the same hex text")
    func resolvesSystemColor() throws {
        let environment = EnvironmentValues()
        for swatch in MonarchColorSwatch.defaults {
            let color = try #require(MonarchColorSwatch.color(hex: swatch.hex))
            #expect(MonarchColorPickerTokens.hex(of: color, in: environment) == swatch.hex, "\(swatch.name)")
        }
        #expect(MonarchColorSwatch.color(hex: "nope") == nil)
    }

    @Test("A swatch normalises its colour and refuses text that is not a colour")
    func swatch() throws {
        let white = try #require(MonarchColorSwatch(hex: "FFF", name: "White"))
        #expect(white.hex == "#ffffff")
        #expect(white.id == "#ffffff")
        #expect(white.name == "White")
        #expect(MonarchColorSwatch(hex: "white", name: "White") == nil)
    }

    @Test("The ten default swatches are the web's, each with a name from the strings table")
    func defaults() {
        let swatches = MonarchColorSwatch.defaults
        #expect(swatches.count == 10)
        #expect(Set(swatches.map(\.hex)).count == 10)
        #expect(swatches.first?.name == "Slate")
        #expect(swatches.first?.hex == "#64748b")
        #expect(swatches.last?.name == "Pink")
        #expect(swatches.allSatisfy { !$0.name.isEmpty && !$0.name.hasPrefix("monarch.") })
    }

    @Test("VoiceOver reads a swatch as its name and value")
    func swatchLabel() throws {
        let blue = try #require(MonarchColorSwatch(hex: "#2563EB", name: "Blue"))
        #expect(MonarchColorPickerTokens.swatchLabel(blue) == "Blue, #2563eb")
    }

    @Test("The custom row shows the selected value as text, or that there is none")
    func valueText() {
        #expect(MonarchColorPickerTokens.valueText(selection: "#2563EB", emptyLabel: nil) == "#2563eb")
        #expect(MonarchColorPickerTokens.valueText(selection: nil, emptyLabel: nil) == "No colour selected")
        #expect(MonarchColorPickerTokens.valueText(selection: "junk", emptyLabel: nil) == "No colour selected")
        #expect(MonarchColorPickerTokens.valueText(selection: nil, emptyLabel: "Not set") == "Not set")
        #expect(MonarchColorPickerTokens.selectedHex(" 2563EB ") == "#2563eb")
        #expect(MonarchColorPickerTokens.selectedHex(nil) == nil)
    }

    @Test("Text uses the disabled role while disabled; the selection mark does not depend on the swatch colour")
    func colors() {
        #expect(MonarchColorPickerTokens.title(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchColorPickerTokens.title(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchColorPickerTokens.detail(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchColorPickerTokens.detail(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchColorPickerTokens.ring == \MonarchColorTokens.textDefault)
        #expect(MonarchColorPickerTokens.checkBackground == \MonarchColorTokens.bgInverse)
        #expect(MonarchColorPickerTokens.checkGlyph == \MonarchColorTokens.textInverse)
        #expect(MonarchColorPickerTokens.swatchBorder == \MonarchColorTokens.borderDefault)
    }

    @Test("A swatch cell and the custom row are at least as large as the touch target")
    func hitTarget() {
        // The ten swatches share one row, as on the web, so a cell is 34 points: above the 24-point floor of WCAG 2.2
        // target size, below the 44 points of the custom row.
        #expect(MonarchColorPickerTokens.cellSize >= 24)
        #expect(MonarchColorPickerTokens.customMinHeight >= MonarchAccessibility.minimumHitTarget)
        #expect(MonarchColorPickerTokens.swatchSize < MonarchColorPickerTokens.cellSize)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchColorPickerStrings.custom == "Custom colour")
        #expect(MonarchColorPickerStrings.empty == "No colour selected")
        #expect(MonarchColorPickerStrings.swatchLabel(name: "Red", hex: "#dc2626") == "Red, #dc2626")
        for preset in MonarchColorPickerTokens.defaultSwatches {
            #expect(!MonarchColorPickerStrings.swatchName(preset.key).hasPrefix("monarch."), "\(preset.key)")
        }
    }

    #if os(iOS)
        // The touch target applies on the simulator.
        @Test("One swatch row is at least as tall as the touch target")
        @MainActor
        func swatchRowHeight() {
            let picker = MonarchColorPicker("Colour", selection: .constant(nil), allowsCustom: false)
            #expect(Host.layout(picker).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}

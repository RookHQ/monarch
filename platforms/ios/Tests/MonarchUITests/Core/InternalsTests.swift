import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// The shared internals that component authors build on.
@Suite("Internals")
@MainActor
struct InternalsTests {
    @Test("Every tone has its own palette and symbol")
    func palettes() {
        let palettes = MonarchTone.allCases.map(MonarchTonePalette.palette(for:))
        #expect(MonarchTone.allCases.count == 7)
        #expect(Set(palettes.map(\.subtle)).count == MonarchTone.allCases.count)
        #expect(Set(palettes.map(\.solid)).count == MonarchTone.allCases.count)
        #expect(Set(palettes.map(\.symbol)).count == MonarchTone.allCases.count)
        #expect(MonarchTonePalette.palette(for: .danger).strong == \MonarchColorTokens.dangerStrong)
        #expect(MonarchTonePalette.palette(for: .accent).solid == \MonarchColorTokens.accentSolid)
        #expect(MonarchTonePalette.palette(for: .accent).border == \MonarchColorTokens.accentBorder)
        #expect(MonarchTonePalette.palette(for: .ai).subtle == \MonarchColorTokens.aiSubtle)
        #expect(MonarchTonePalette.palette(for: .ai).onSolid == \MonarchColorTokens.aiOnSolid)
    }

    @Test("Emphasis selects fill, foreground and border", arguments: MonarchTone.allCases)
    func surfaces(tone: MonarchTone) {
        let palette = MonarchTonePalette.palette(for: tone)
        #expect(
            MonarchTonePalette.surface(tone: tone, emphasis: .subtle)
                == MonarchSurfaceAppearance(background: palette.subtle, foreground: palette.strong, border: nil)
        )
        #expect(
            MonarchTonePalette.surface(tone: tone, emphasis: .solid)
                == MonarchSurfaceAppearance(background: palette.solid, foreground: palette.onSolid, border: nil)
        )
        #expect(
            MonarchTonePalette.surface(tone: tone, emphasis: .outline)
                == MonarchSurfaceAppearance(background: nil, foreground: palette.strong, border: palette.border)
        )
    }

    @Test("Tone names come from the MonarchTone strings table")
    func toneNames() {
        #expect(MonarchToneStrings.name(.neutral) == nil)
        #expect(MonarchToneStrings.name(.info) == "Information")
        #expect(MonarchToneStrings.name(.success) == "Success")
        #expect(MonarchToneStrings.name(.warning) == "Warning")
        #expect(MonarchToneStrings.name(.danger) == "Error")
        #expect(MonarchToneStrings.name(.accent) == nil, "the accent is emphasis, not a meaning to announce")
        #expect(MonarchToneStrings.name(.ai) == "AI")
    }

    @Test("A missing key falls back to the key")
    func missingString() {
        #expect(MonarchStrings.string("monarch.none", table: "MonarchTone") == "monarch.none")
    }

    @Test("Shared shapes use the radius tokens")
    func shapes() {
        #expect(MonarchShapes.control.cornerSize.width == 8)
        #expect(MonarchShapes.container.cornerSize.width == 12)
        #expect(MonarchShapes.mark.cornerSize.width == 4)
        #expect(MonarchShapes.borderWidth == 1)
    }

    @Test("A surface and an image placeholder lay out")
    func surfaceAndImage() {
        let surface = Text("Surface")
            .padding()
            .monarchSurface(MonarchTonePalette.surface(tone: .info, emphasis: .outline), in: MonarchShapes.control)
        #expect(Host.layout(surface).height > 0)

        let noSource = MonarchRemoteImage(url: nil) { Text("AB") }.frame(width: 40, height: 40)
        let local = MonarchRemoteImage(image: Image(systemName: "person")) { Text("AB") }.frame(width: 40, height: 40)
        #expect(Host.layout(noSource).height > 0)
        #expect(Host.layout(local).height > 0)
    }
}

import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

@Suite("Brand theme integration")
@MainActor
struct BrandThemeIntegrationTests {
    private static let cases: [(MonarchSchemePreference, MonarchContrastPreference, String)] = [
        (.light, .standard, "light"),
        (.dark, .standard, "dark"),
        (.light, .more, "hc-light"),
        (.dark, .more, "hc-dark"),
    ]

    @Test("A view under monarchTheme(brand:) gets the derived accentSolid in all four themes")
    func modifierAppliesDerivedAccent() {
        let brand = MonarchBrandTheme.derive(brand: "#0A7CFF")
        let probe = ThemeProbe()
        for (scheme, contrast, id) in Self.cases {
            _ = Host.layout(probe.view.monarchTheme(brand: "#0A7CFF", scheme: scheme, contrast: contrast))
            #expect(probe.theme?.id == id)
            #expect(probe.theme?.tokens.colors.accentSolid == brand.mode(themeID: id).accent.solid, "\(id)")
            #expect(probe.theme?.colors.accentSolid == Color(monarch: brand.mode(themeID: id).accent.solid), "\(id)")
        }
    }

    @Test("Invalid brand input falls back to the default accent without failing")
    func invalidFallsBack() {
        let probe = ThemeProbe()
        for (scheme, contrast, id) in Self.cases {
            _ = Host.layout(probe.view.monarchTheme(brand: "not a colour", scheme: scheme, contrast: contrast))
            #expect(
                probe.theme?.tokens
                    == MonarchAppearance(scheme: scheme, contrast: contrast).resolve(
                        systemIsDark: false,
                        systemIncreasesContrast: false
                    ),
                "\(id)"
            )
        }
    }

    @Test("MonarchTheme(tokens:brand:) applies the derived accent", arguments: MonarchTokens.themes)
    func pinnedTheme(tokens: MonarchThemeTokens) {
        let theme = MonarchTheme(tokens: tokens, brand: "#f0a")
        let expected = MonarchBrandTheme.derive(brand: "#f0a").mode(themeID: tokens.id).accent
        #expect(theme.tokens.colors.accentSolid == expected.solid)
        #expect(theme.tokens.colors.accentStrong == expected.strong)
    }
}

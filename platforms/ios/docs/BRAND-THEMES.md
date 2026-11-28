# Brand themes on iOS

Apps that let a customer or a workspace pick a colour can derive a complete,
accessible Monarch accent from that one colour at run time. The engine is
`MonarchBrandTheme` in `MonarchTokens`, a port of the web's `deriveTheme`
(`packages/tokens/src/brand.ts`) with results identical to the web.

## Use

Install the theme with the brand colour instead of a generated accent:

```swift
ContentView()
    .monarchTheme(brand: workspace.brandColor)
```

`scheme`, `contrast` and `typography` work as on `monarchTheme(scheme:contrast:accent:typography:)`.
The theme still follows Dark Mode and Increase Contrast, and `tint` takes the
derived accent.

To pin one theme, for a preview or a screenshot:

```swift
let theme = MonarchTheme(tokens: MonarchTokens.dark, brand: "#0A7CFF")
```

Without SwiftUI, in `MonarchTokens` only:

```swift
let brand = MonarchBrandTheme.derive(brand: "#0A7CFF")
let tokens = MonarchTokens.hcDark.withAccent(brand.accent)
let accent = MonarchAccent(brand: "#0A7CFF")   // the same accent
```

## Public API

```swift
public struct MonarchHSL { h, s, l; init(h:s:l:); var hex: String; var rgba: MonarchRGBA }
public func parseHexColor(_ input: String?) -> MonarchHSL?

public struct MonarchBrandTheme {
    public static let aiMinDistance: Double            // 10
    public static func derive(brand: String, ai: String? = nil) -> MonarchBrandTheme
    public let input: String, brand: String, isValid: Bool
    public let light, dark, hcLight, hcDark: MonarchBrandMode
    public let ai: MonarchBrandAICheck?
    public var modes: [MonarchBrandMode]
    public var accent: MonarchAccent
    public var aiAccent: MonarchAccent?
    public func mode(themeID: String) -> MonarchBrandMode
}
public struct MonarchBrandMode { themeID, accent: MonarchAccentScale, report, ai, aiReport }
public struct MonarchBrandReport { base: String, adjustments: [String], ratios: MonarchBrandRatios }
public struct MonarchBrandRatios { onSolid, strongOnCanvas, strongOnSurface, strongOnSubtle }
public struct MonarchBrandAICheck { input, color, isValid, closest, isDistinct, warnings }
extension MonarchAccent { public init(brand: String) }

// MonarchUI
extension View { public func monarchTheme(brand:scheme:contrast:typography:) -> some View }
extension MonarchTheme { public init(tokens:brand:typography:) }
```

## Guarantees

The accent is derived separately for each of the four themes, with the same
rules as every Rook product accent:

| Pair                                                                                   | Standard | High contrast |
| -------------------------------------------------------------------------------------- | -------- | ------------- |
| `onSolid` on `solid`, `solidHover`, `solidActive`                                      | 4.5:1    | 7:1           |
| `strong` (links, icons, focus ring) on canvas, surface, subtle, muted, accent `subtle` | 4.5:1    | 7:1           |
| `solid` fill against the canvas                                                        | 1.5:1    | 4.5:1         |

Contrast is measured on the colour as the screen paints it (8-bit channels).
Hue and saturation are kept; only lightness moves, so the brand stays
recognisable. A colour that cannot carry white text gets dark text first, then
a darker fill.

The guarantees hold for the colours the engine computes. The published colour
is that colour with its HSL channels rounded to one decimal, exactly as the
web's CSS prints it, and the rounding can cost a few hundredths in rare cases:
`#28af4c` in high-contrast light has `onSolid` on `solid` at 7.06:1 before
rounding and 6.97:1 after, on the web and on native alike.

## The report

Each mode carries a `MonarchBrandReport`:

- `base`: the input as HSL channels, for example `354.5 71.3% 52.2%`;
- `adjustments`: one plain-language line per change, for example
  `strong set to 212.1 100% 43% for 4.5:1 text contrast`. Empty when the
  colour was used as given;
- `ratios`: the measured `onSolid`, `strongOnCanvas`, `strongOnSurface` and
  `strongOnSubtle`, rounded to two decimals.

The texts and numbers are the web's, character for character, so a settings
screen can show the same explanation on every platform.

## Invalid input

`#rgb` and `#rrggbb` are accepted in any case, with or without `#`, with
surrounding white space. Anything else (an empty string, `red`, `#12345`,
`#rrggbbaa`, `rgb(…)`) is invalid:

- `parseHexColor` returns `nil`;
- `derive(brand:)` uses the default accent (Rook Red), sets `isValid` to
  false and keeps the value in `input`; `accent` is `MonarchTokens.Accents.rook`;
- `monarchTheme(brand:)` and `MonarchAccent(brand:)` silently give the
  default accent. Nothing asserts or crashes, so a bad value saved by a
  customer never breaks the app. Check `isValid` to tell the person.

## A custom AI colour

`derive(brand:ai:)` also derives an AI colour for libraries and apps outside
Rook. Rook apps never pass it: AI keeps the shared Monarch purple. The AI
roles get the same guarantees (`mode.ai`, `mode.aiReport`, `aiAccent`) and
`ai` reports the check: the closest of the accent and the status fills in any
theme, whether it is at least `aiMinDistance` (OKLab ΔE ×100) away, and a
warning when it is not. An invalid AI colour gives `isValid == false` and a
warning; the purple is kept.

The theme entry points apply the brand accent only. The AI roles are returned
as data; applying them to a theme is not yet provided.

## Parity with the web

`Tests/Fixtures/brand-theme-vectors.json` is written by
`packages/tokens/scripts/emit-brand-vectors.ts` from the web source:
`parseHexColor` cases, the 256 linearised channel values and 77 `deriveTheme`
results (Rook reds, every product accent, light, dark and grey colours,
saturated yellows and cyans, 3-digit hex, invalid strings, AI colours that are
distinct, too close and invalid). `BrandThemeTests` asserts every value
exactly, except the raw AI distance, which depends on the platform's
`pow` and `cbrt` in its last bits and is compared to 9 decimals.
After a change to the token source, regenerate it with
`npx tsx packages/tokens/scripts/emit-brand-vectors.ts`; `--check` fails when
it is stale.

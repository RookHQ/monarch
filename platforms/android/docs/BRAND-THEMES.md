# Brand themes on Android

Apps that let a customer or a workspace pick a colour can derive a complete,
accessible Monarch accent from that one colour at run time. The engine is
`MonarchBrandTheme` in `monarch-tokens` (pure Kotlin, no Android), a port of
the web's `deriveTheme` (`packages/tokens/src/brand.ts`) with results
identical to the web.

## Use

Pass the brand colour instead of a generated accent:

```kotlin
MonarchTheme(brand = workspace.brandColor) {
    // content
}
```

`scheme`, `contrast`, `typography` and `dynamicColor` work as on the `accent`
overload. The theme still follows the system dark theme and contrast
settings, and the Material 3 colour scheme follows the derived accent
(`primary` is `accentStrong`, as for every accent).

To pin one theme, for a preview or a screenshot:

```kotlin
val brand = MonarchBrandTheme.derive("#0A7CFF")
MonarchTheme(tokens = MonarchTokens.dark.withAccent(brand.accent)) { /* … */ }
```

## Public API

```kotlin
// monarch-tokens
public class MonarchHsl(h: Double, s: Double, l: Double) { h; s; l; hex: String; argb: Long }
public fun parseHexColor(input: String?): MonarchHsl?

public class MonarchBrandTheme {
    public val input: String; public val brand: String; public val isValid: Boolean
    public val light, dark, hcLight, hcDark: MonarchBrandMode
    public val ai: MonarchBrandAiCheck?
    public val modes: List<MonarchBrandMode>
    public val accent: MonarchAccent
    public val aiAccent: MonarchAccent?
    public fun mode(themeId: String): MonarchBrandMode
    public companion object {
        public val aiMinDistance: Double            // 10.0
        public fun derive(brand: String, ai: String? = null): MonarchBrandTheme
    }
}
public class MonarchBrandMode(themeId, accent: MonarchAccentScale, report, ai, aiReport)
public class MonarchBrandReport(base: String, adjustments: List<String>, ratios: MonarchBrandRatios)
public class MonarchBrandRatios(onSolid, strongOnCanvas, strongOnSurface, strongOnSubtle)
public class MonarchBrandAiCheck(input, color, isValid, closest, isDistinct, warnings)

// monarch-compose
@Composable public fun MonarchTheme(brand: String, scheme, contrast, typography, dynamicColor, content)
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

- `parseHexColor` returns `null`;
- `MonarchBrandTheme.derive` uses the default accent (Rook Red), sets
  `isValid` to false and keeps the value in `input`; `accent` is
  `MonarchTokens.Accents.rook`;
- `MonarchTheme(brand = …)` silently gives the default accent. Nothing throws,
  so a bad value saved by a customer never breaks the app. Check `isValid` to
  tell the person.

## A custom AI colour

`derive(brand, ai)` also derives an AI colour for libraries and apps outside
Rook. Rook apps never pass it: AI keeps the shared Monarch purple. The AI
roles get the same guarantees (`mode.ai`, `mode.aiReport`, `aiAccent`) and
`ai` reports the check: the closest of the accent and the status fills in any
theme, whether it is at least `aiMinDistance` (OKLab ΔE ×100) away, and a
warning when it is not. An invalid AI colour gives `isValid == false` and a
warning; the purple is kept.

The theme entry point applies the brand accent only. The AI roles are returned
as data; applying them to a theme is not yet provided.

## Parity with the web

`monarch-tokens/src/test/resources/brand-theme-vectors.json` is written by
`packages/tokens/scripts/emit-brand-vectors.ts` from the web source:
`parseHexColor` cases, the 256 linearised channel values and 77 `deriveTheme`
results (Rook reds, every product accent, light, dark and grey colours,
saturated yellows and cyans, 3-digit hex, invalid strings, AI colours that are
distinct, too close and invalid). `MonarchBrandThemeTest` asserts every value
exactly, except the raw AI distance, which depends on the platform's
`pow` and `cbrt` in its last bits and is compared to 9 decimals.
After a change to the token source, regenerate it with
`npx tsx packages/tokens/scripts/emit-brand-vectors.ts`; `--check` fails when
it is stale.

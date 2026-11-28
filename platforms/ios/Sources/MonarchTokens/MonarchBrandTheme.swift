import Foundation

/// A colour in HSL, as the web token source writes it.
///
/// The hue is normalised to 0 until 360, saturation and lightness are clamped
/// to 0 to 100, exactly as `hsl()` in `packages/tokens/src/color.ts`.
public struct MonarchHSL: Sendable, Hashable {
    /// Hue, 0 until 360.
    public let h: Double
    /// Saturation, 0 to 100.
    public let s: Double
    /// Lightness, 0 to 100.
    public let l: Double

    /// A colour from hue, saturation and lightness, normalised as on the web.
    public init(h: Double, s: Double, l: Double) {
        self.h = (h.truncatingRemainder(dividingBy: 360) + 360).truncatingRemainder(dividingBy: 360)
        self.s = clampValue(s, 0, 100)
        self.l = clampValue(l, 0, 100)
    }

    /// The colour as lowercase `#rrggbb`.
    public var hex: String { toHex(self) }

    /// The colour as an opaque ``MonarchRGBA``.
    public var rgba: MonarchRGBA {
        let rgb = toRGB8(self)
        return MonarchRGBA(UInt32(rgb[0] << 16 | rgb[1] << 8 | rgb[2]))
    }
}

/// Parses `#rgb`, `#rrggbb`, `rgb` or `rrggbb` (any case, surrounding white
/// space allowed), like `parseHexColor` on the web.
///
/// - Parameter input: The colour a person or a server supplied.
/// - Returns: The colour, or `nil` for anything else, including named colours
///   and `#rrggbbaa`: a brand colour must be opaque for contrast to mean anything.
public func parseHexColor(_ input: String?) -> MonarchHSL? {
    guard let input else { return nil }
    var v = jsTrim(input)
    if v.first == "#" { v = v.dropFirst() }
    let digits = v.map(hexDigit)
    guard digits.allSatisfy({ $0 != nil }) else { return nil }
    var values = digits.compactMap { $0 }
    if values.count == 3 { values = values.flatMap { [$0, $0] } }
    guard values.count == 6 else { return nil }
    let byte = { (i: Int) in values[i] * 16 + values[i + 1] }
    return fromRGB8(byte(0), byte(2), byte(4))
}

private func hexDigit(_ c: Unicode.Scalar) -> Int? {
    switch c.value {
    case 0x30...0x39: Int(c.value - 0x30)
    case 0x41...0x46: Int(c.value - 0x41 + 10)
    case 0x61...0x66: Int(c.value - 0x61 + 10)
    default: nil
    }
}

/// The contrast ratios measured for one derived intent, rounded to two decimals.
public struct MonarchBrandRatios: Sendable, Hashable {
    /// `onSolid` on `solid`.
    public let onSolid: Double
    /// `strong` on the canvas.
    public let strongOnCanvas: Double
    /// `strong` on the surface.
    public let strongOnSurface: Double
    /// `strong` on the intent's own `subtle` tint.
    public let strongOnSubtle: Double

    /// Measured ratios.
    public init(onSolid: Double, strongOnCanvas: Double, strongOnSurface: Double, strongOnSubtle: Double) {
        self.onSolid = onSolid
        self.strongOnCanvas = strongOnCanvas
        self.strongOnSurface = strongOnSurface
        self.strongOnSubtle = strongOnSubtle
    }
}

/// What the engine changed to meet contrast, and the ratios it measured.
public struct MonarchBrandReport: Sendable, Hashable {
    /// The colour the intent was derived from, as HSL channels (`354.5 71.3% 52.2%`).
    public let base: String
    /// One plain-language note per adjustment. Empty when the colour was used as given.
    public let adjustments: [String]
    /// The measured ratios.
    public let ratios: MonarchBrandRatios

    /// A report.
    public init(base: String, adjustments: [String], ratios: MonarchBrandRatios) {
        self.base = base
        self.adjustments = adjustments
        self.ratios = ratios
    }
}

/// The derived colours of one theme.
public struct MonarchBrandMode: Sendable, Hashable {
    /// `light`, `dark`, `hc-light` or `hc-dark`.
    public let themeID: String
    /// The ten accent roles for this theme.
    public let accent: MonarchAccentScale
    /// What was adjusted in the accent, and the measured ratios.
    public let report: MonarchBrandReport
    /// The AI roles derived from the custom AI colour, or `nil` when none was given or it was invalid.
    public let ai: MonarchAccentScale?
    /// What was adjusted in the AI colour, when there is one.
    public let aiReport: MonarchBrandReport?
}

/// The check of a custom AI colour: valid, and far enough from the accent and the status colours.
public struct MonarchBrandAICheck: Sendable, Hashable {
    /// The colour the AI fill is closest to.
    public struct Closest: Sendable, Hashable {
        /// Perceptual distance (OKLab ΔE ×100).
        public let distance: Double
        /// `accent`, `danger`, `success`, `warning` or `info`.
        public let to: String
        /// The theme where it is closest.
        public let themeID: String
    }

    /// The value that was passed, unchanged.
    public let input: String
    /// The AI colour used, as lowercase `#rrggbb`, or `nil` when `input` was invalid.
    public let color: String?
    /// False when `input` was not a hex colour; the Monarch AI purple is kept.
    public let isValid: Bool
    /// The nearest accent or status fill, in any theme. `nil` when `isValid` is false.
    public let closest: Closest?
    /// True when `closest` is at least ``MonarchBrandTheme/aiMinDistance`` away.
    public let isDistinct: Bool
    /// Plain-language notes for whoever picked the colour. Empty when all is well.
    public let warnings: [String]
}

/// One brand colour in, a complete accessible accent for all four themes out:
/// the native port of `deriveTheme` in `@rookcom/monarch-tokens`, with results
/// identical to the web.
///
/// Guarantees, in every theme: `onSolid` on the solid fills reaches 4.5:1 (7:1
/// in the high-contrast themes); `strong` reaches the same on the canvas,
/// surface, subtle and muted backgrounds and on the accent's own tints; and the
/// fill stays distinguishable from the canvas (1.5:1, 4.5:1 high contrast).
///
/// ```swift
/// let brand = MonarchBrandTheme.derive(brand: "#0A7CFF")
/// let theme = MonarchTokens.dark.withAccent(brand.accent)
/// ```
///
/// Pure: no UI framework, no state, the same input always gives the same output.
public struct MonarchBrandTheme: Sendable, Hashable {
    /// The smallest OKLab distance (×100) the AI fill should keep from the accent and status fills.
    public static let aiMinDistance: Double = 10

    /// The value that was passed, unchanged.
    public let input: String
    /// The colour the theme was derived from, as lowercase `#rrggbb`.
    public let brand: String
    /// False when `input` was not a hex colour and the default accent (Rook Red) was used.
    public let isValid: Bool
    /// The light theme.
    public let light: MonarchBrandMode
    /// The dark theme.
    public let dark: MonarchBrandMode
    /// The high-contrast light theme.
    public let hcLight: MonarchBrandMode
    /// The high-contrast dark theme.
    public let hcDark: MonarchBrandMode
    /// The AI colour check, when an AI colour was passed (even an invalid one).
    public let ai: MonarchBrandAICheck?

    /// The four modes, in the order light, dark, high-contrast light, high-contrast dark.
    public var modes: [MonarchBrandMode] { [light, dark, hcLight, hcDark] }

    /// The derived accent, ready for `withAccent(_:)` and `monarchTheme(accent:)`.
    /// When `isValid` is false this is `MonarchTokens.Accents.rook`, the default accent.
    public var accent: MonarchAccent {
        guard isValid else { return MonarchTokens.Accents.rook }
        return MonarchAccent(
            id: "brand",
            label: "Brand \(brand)",
            light: light.accent,
            dark: dark.accent,
            hcLight: hcLight.accent,
            hcDark: hcDark.accent
        )
    }

    /// The derived AI roles as an accent-shaped value, or `nil` when there is no valid AI colour.
    public var aiAccent: MonarchAccent? {
        guard let l = light.ai, let d = dark.ai, let hl = hcLight.ai, let hd = hcDark.ai else { return nil }
        return MonarchAccent(id: "brand-ai", label: "AI \(ai?.color ?? "")", light: l, dark: d, hcLight: hl, hcDark: hd)
    }

    /// The mode for a theme id (`light`, `dark`, `hc-light`, `hc-dark`); anything else gives `light`.
    public func mode(themeID: String) -> MonarchBrandMode {
        switch themeID {
        case "dark": dark
        case "hc-light": hcLight
        case "hc-dark": hcDark
        default: light
        }
    }

    private static let neighbourNames = [
        "accent": "the brand colour",
        "danger": "the error red",
        "success": "the success green",
        "warning": "the warning amber",
        "info": "the info blue",
    ]

    /// Derives the accent for all four themes from `brand`.
    ///
    /// - Parameters:
    ///   - brand: `#rgb` or `#rrggbb`. Anything else falls back to the default
    ///     accent and sets `isValid` to false, so a bad saved value never breaks the app.
    ///   - ai: An optional custom AI colour, derived with the same guarantees and
    ///     checked for distance from the accent and the status colours.
    /// - Returns: The derived theme and its report.
    public static func derive(brand: String, ai: String? = nil) -> MonarchBrandTheme {
        let parsed = parseHexColor(brand)
        let base = parsed ?? BrandBase.defaultAccent
        let aiBase = ai.flatMap(parseHexColor)
        var closest: MonarchBrandAICheck.Closest?
        let modes = BrandThemeDefinition.all.map { t -> MonarchBrandMode in
            let accent = deriveIntent(base, t)
            let aiIntent = aiBase.map { deriveIntent($0, t) }
            if let aiIntent {
                let fills = [("accent", accent.solid)] + BrandBase.status.map { ($0.0, deriveIntent($0.1, t).solid) }
                for (to, fill) in fills {
                    let distance = colorDistance(aiIntent.solid, fill)
                    if let best = closest, distance >= best.distance { continue }
                    closest = .init(distance: distance, to: to, themeID: t.id)
                }
            }
            return MonarchBrandMode(
                themeID: t.id,
                accent: accent.scale,
                report: accent.report,
                ai: aiIntent?.scale,
                aiReport: aiIntent?.report
            )
        }
        return MonarchBrandTheme(
            input: brand,
            brand: toHex(base),
            isValid: parsed != nil,
            light: modes[0],
            dark: modes[1],
            hcLight: modes[2],
            hcDark: modes[3],
            ai: ai.map { aiCheck($0, aiBase, closest) }
        )
    }

    private static func aiCheck(_ input: String, _ base: MonarchHSL?, _ closest: MonarchBrandAICheck.Closest?)
        -> MonarchBrandAICheck
    {
        guard let base, let closest else {
            return MonarchBrandAICheck(
                input: input,
                color: nil,
                isValid: false,
                closest: nil,
                isDistinct: false,
                warnings: ["\"\(input)\" is not a hex colour, so the Monarch AI purple is used."]
            )
        }
        let distinct = closest.distance >= aiMinDistance
        let warnings =
            distinct
            ? []
            : [
                "The AI colour is too close to \(neighbourNames[closest.to] ?? closest.to) in the \(closest.themeID) "
                    + "theme (distance \(toFixed1(closest.distance)), needs \(jsNumber(aiMinDistance))). "
                    + "People may not tell AI content apart from it. Pick a hue further away; "
                    + "the AI label and icon still show."
            ]
        return MonarchBrandAICheck(
            input: input,
            color: toHex(base),
            isValid: true,
            closest: closest,
            isDistinct: distinct,
            warnings: warnings
        )
    }
}

extension MonarchAccent {
    /// The accent derived from a brand colour (`#rgb` or `#rrggbb`) with
    /// ``MonarchBrandTheme``. Invalid input gives the default accent,
    /// `MonarchTokens.Accents.rook`, without failing.
    public init(brand: String) {
        self = MonarchBrandTheme.derive(brand: brand).accent
    }
}

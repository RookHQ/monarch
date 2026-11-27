// A line-for-line port of the colour maths in packages/tokens/src/color.ts and of
// deriveIntent in packages/tokens/src/themes.ts. The numbers are the web's own; the
// golden vectors in Tests/Fixtures/brand-theme-vectors.json keep the two equal.

import Foundation

// MARK: - JavaScript number semantics

/// `Math.round`: halves round towards positive infinity.
func jsRound(_ x: Double) -> Double {
    let f = x.rounded(.down)
    return x - f >= 0.5 ? f + 1 : f
}

/// `String(n)` for a number with at most one decimal (as `Math.round(n * 10) / 10` gives).
private func oneDecimal(_ n: Double) -> String {
    let k = Int(jsRound(n * 10))
    return k % 10 == 0 ? "\(k / 10)" : "\(k / 10).\(k % 10)"
}

/// `String(n)` for the contrast targets (4.5, 7, 1.5, 3).
func jsNumber(_ n: Double) -> String {
    n == n.rounded(.down) ? String(Int(n)) : String(n)
}

/// `n.toFixed(1)`: rounds the exact binary value, halves away from zero.
func toFixed1(_ n: Double) -> String {
    // printf rounds exact halves to even; toFixed rounds them up. With one decimal,
    // an exact half is only possible when n * 4 is an odd integer (x.25, x.75).
    let quarter = n * 4
    if quarter == quarter.rounded(.down), Int(quarter) % 2 != 0 {
        return String(format: "%.1f", ((n * 10).rounded(.down) + 1) / 10)
    }
    return String(format: "%.1f", n)
}

/// The characters `String.prototype.trim` removes: ECMAScript WhiteSpace and LineTerminator.
private func isJSSpace(_ c: Unicode.Scalar) -> Bool {
    switch c.value {
    case 0x09...0x0D, 0x20, 0xA0, 0x1680, 0x2000...0x200A, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000, 0xFEFF:
        return true
    default:
        return false
    }
}

func jsTrim(_ s: String) -> String.UnicodeScalarView.SubSequence {
    let scalars = s.unicodeScalars
    guard let first = scalars.firstIndex(where: { !isJSSpace($0) }),
        let last = scalars.lastIndex(where: { !isJSSpace($0) })
    else { return scalars[scalars.endIndex...] }
    return scalars[first...last]
}

/// `Math.hypot` as V8 computes it: scaled by the largest value, Kahan-compensated sum.
private func jsHypot(_ values: [Double]) -> Double {
    let abs = values.map { Swift.abs($0) }
    let largest = abs.max() ?? 0
    if largest == 0 { return 0 }
    var sum = 0.0
    var compensation = 0.0
    for v in abs {
        let n = v / largest
        let summand = n * n - compensation
        let preliminary = sum + summand
        compensation = (preliminary - sum) - summand
        sum = preliminary
    }
    return sum.squareRoot() * largest
}

// MARK: - color.ts

func clampValue(_ value: Double, _ low: Double, _ high: Double) -> Double {
    Swift.min(high, Swift.max(low, value))
}

/// `354.5 71.3% 52.2%`, as `toChannels` prints it.
func channels(_ c: MonarchHSL) -> String {
    "\(oneDecimal(c.h)) \(oneDecimal(c.s))% \(oneDecimal(c.l))%"
}

/// The colour exactly as the CSS prints it (each channel rounded to one decimal), as `emitted`.
/// The engine measures and returns emitted colours only, so its guarantees hold for the shipped values.
func emitted(_ c: MonarchHSL) -> MonarchHSL {
    MonarchHSL(h: jsRound(c.h * 10) / 10, s: jsRound(c.s * 10) / 10, l: jsRound(c.l * 10) / 10)
}

func toRGB(_ c: MonarchHSL) -> [Double] {
    let s = c.s / 100
    let l = c.l / 100
    let a = s * Swift.min(l, 1 - l)
    func f(_ n: Double) -> Double {
        let k = (n + c.h / 30).truncatingRemainder(dividingBy: 12)
        return l - a * Swift.max(-1, Swift.min(k - 3, Swift.min(9 - k, 1)))
    }
    return [f(0), f(8), f(4)]
}

/// The 8-bit channels `toHex` prints.
func toRGB8(_ c: MonarchHSL) -> [Int] {
    toRGB(c).map { Int(jsRound($0 * 255)) }
}

func toHex(_ c: MonarchHSL) -> String {
    "#" + toRGB8(c).map { String(format: "%02x", $0) }.joined()
}

func fromRGB8(_ red: Int, _ green: Int, _ blue: Int) -> MonarchHSL {
    let r = Double(red) / 255
    let g = Double(green) / 255
    let b = Double(blue) / 255
    let hi = Swift.max(r, Swift.max(g, b))
    let lo = Swift.min(r, Swift.min(g, b))
    let l = (hi + lo) / 2
    let d = hi - lo
    var h = 0.0
    var s = 0.0
    if d != 0 {
        s = d / (1 - Swift.abs(2 * l - 1))
        if hi == r {
            h = 60 * ((g - b) / d).truncatingRemainder(dividingBy: 6)
        } else if hi == g {
            h = 60 * ((b - r) / d + 2)
        } else {
            h = 60 * ((r - g) / d + 4)
        }
    }
    return MonarchHSL(h: h, s: s * 100, l: l * 100)
}

private func linearise(_ v: Double) -> Double {
    v <= 0.04045 ? v / 12.92 : pow((v + 0.055) / 1.055, 2.4)
}

/// The 256 WCAG linearised channel values, computed once.
let linearChannel: [Double] = (0..<256).map { linearise(Double($0) / 255) }

/// Relative luminance of the colour as a browser paints it (8-bit channels).
func relativeLuminance(_ c: MonarchHSL) -> Double {
    let rgb = toRGB8(c)
    return 0.2126 * linearChannel[rgb[0]] + 0.7152 * linearChannel[rgb[1]] + 0.0722 * linearChannel[rgb[2]]
}

func contrast(_ a: MonarchHSL, _ b: MonarchHSL) -> Double {
    let la = relativeLuminance(a)
    let lb = relativeLuminance(b)
    let (hi, lo) = la > lb ? (la, lb) : (lb, la)
    return (hi + 0.05) / (lo + 0.05)
}

/// OKLab distance ×100, as `colorDistance`.
func colorDistance(_ a: MonarchHSL, _ b: MonarchHSL) -> Double {
    func lab(_ c: MonarchHSL) -> [Double] {
        let v = toRGB(c).map(linearise)
        let (r, g, bl) = (v[0], v[1], v[2])
        let l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * bl)
        let m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * bl)
        let s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * bl)
        return [
            100 * (0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s),
            100 * (1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s),
            100 * (0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s),
        ]
    }
    let x = lab(a)
    let y = lab(b)
    return jsHypot([x[0] - y[0], x[1] - y[1], x[2] - y[2]])
}

/// Moves lightness by half steps until `color` reaches `minRatio` against every
/// colour in `against`. Nil where the web throws (no lightness reaches it).
func ensureContrast(_ color: MonarchHSL, _ against: [MonarchHSL], _ minRatio: Double, darken: Bool) -> MonarchHSL? {
    let step = darken ? -0.5 : 0.5
    var current = emitted(color)
    for _ in 0..<220 {
        if against.allSatisfy({ contrast(current, $0) >= minRatio }) { return current }
        let next = emitted(MonarchHSL(h: current.h, s: current.s, l: current.l + step))
        if next.l == current.l { return nil }
        current = next
    }
    return nil
}

/// `ensureContrast` for the steps the web lets throw. No opaque brand colour
/// reaches that case (checked over the whole sRGB cube at a step of 5); if one
/// ever did, the furthest lightness is used instead of failing the app.
private func ensureOrExtreme(_ color: MonarchHSL, _ against: [MonarchHSL], _ minRatio: Double, darken: Bool)
    -> MonarchHSL
{
    ensureContrast(color, against, minRatio, darken: darken) ?? MonarchHSL(h: color.h, s: color.s, l: darken ? 0 : 100)
}

/// sRGB mix, rounded to 8 bits, as `mixSrgb`.
func mixSRGB(_ a: MonarchHSL, _ b: MonarchHSL, _ p: Double) -> MonarchHSL {
    let x = toRGB(a)
    let y = toRGB(b)
    func m(_ i: Int) -> Int { Int(jsRound((x[i] * p + y[i] * (1 - p)) * 255)) }
    return fromRGB8(m(0), m(1), m(2))
}

/// The colour as the CSS prints it (channels rounded to one decimal).
func cssRGBA(_ c: MonarchHSL) -> MonarchRGBA {
    let rgb = toRGB8(emitted(c))
    return MonarchRGBA(UInt32(rgb[0] << 16 | rgb[1] << 8 | rgb[2]))
}

// MARK: - themes.ts

enum BrandNeutral {
    static let n0 = MonarchHSL(h: 0, s: 0, l: 100)
    static let n25 = MonarchHSL(h: 220, s: 20, l: 99)
    static let n50 = MonarchHSL(h: 220, s: 17, l: 97.5)
    static let n100 = MonarchHSL(h: 220, s: 14, l: 95.5)
    static let n800 = MonarchHSL(h: 221, s: 15, l: 18)
    static let n850 = MonarchHSL(h: 222, s: 16, l: 13.5)
    static let n900 = MonarchHSL(h: 222, s: 18, l: 10)
    static let n950 = MonarchHSL(h: 224, s: 22, l: 6.5)
    static let n1000 = MonarchHSL(h: 0, s: 0, l: 0)
}

/// The parts of a theme definition `deriveIntent` reads.
struct BrandThemeDefinition: Sendable {
    let id: String
    let dark: Bool
    let highContrast: Bool
    let text: Double
    let ui: Double
    let canvas: MonarchHSL
    let surface: MonarchHSL
    let subtle: MonarchHSL
    let muted: MonarchHSL
    let inverse: MonarchHSL
    let textInverse: MonarchHSL

    var canvasFloor: Double { highContrast ? ui : 1.5 }

    static let all: [BrandThemeDefinition] = {
        typealias N = BrandNeutral
        return [
            .init(
                id: "light",
                dark: false,
                highContrast: false,
                text: 4.5,
                ui: 3,
                canvas: N.n25,
                surface: N.n0,
                subtle: N.n50,
                muted: N.n100,
                inverse: N.n900,
                textInverse: N.n0
            ),
            .init(
                id: "dark",
                dark: true,
                highContrast: false,
                text: 4.5,
                ui: 3,
                canvas: N.n950,
                surface: N.n900,
                subtle: N.n850,
                muted: N.n800,
                inverse: N.n50,
                textInverse: N.n900
            ),
            .init(
                id: "hc-light",
                dark: false,
                highContrast: true,
                text: 7,
                ui: 4.5,
                canvas: N.n0,
                surface: N.n0,
                subtle: N.n50,
                muted: N.n100,
                inverse: N.n1000,
                textInverse: N.n0
            ),
            .init(
                id: "hc-dark",
                dark: true,
                highContrast: true,
                text: 7,
                ui: 4.5,
                canvas: N.n1000,
                surface: N.n1000,
                subtle: N.n950,
                muted: N.n900,
                inverse: N.n0,
                textInverse: N.n1000
            ),
        ]
    }()
}

/// The Rook brand colours the status intents and the default accent are derived from.
enum BrandBase {
    static let red = fromRGB8(0xDC, 0x2E, 0x3E)
    static let defaultAccent = red
    static let status: [(String, MonarchHSL)] = [
        ("danger", red),
        ("success", fromRGB8(0x28, 0xAF, 0x4C)),
        ("warning", fromRGB8(0xF4, 0xB4, 0x00)),
        ("info", fromRGB8(0x00, 0x96, 0xD4)),
    ]
}

private let white = BrandNeutral.n0
private let ink = BrandNeutral.n950
/// How far a brand fill may be darkened to keep white text before dark text is used.
private let solidDarkenBudget = 10.0
private let inverseTint = 0.12
private let inverseRaised = 0.09

/// One intent in one theme, in HSL, with its report.
struct DerivedIntent {
    let solid: MonarchHSL
    let solidHover: MonarchHSL
    let solidActive: MonarchHSL
    let onSolid: MonarchHSL
    let subtle: MonarchHSL
    let subtleHover: MonarchHSL
    let strong: MonarchHSL
    let onStrong: MonarchHSL
    let border: MonarchHSL
    let strongInverse: MonarchHSL
    let report: MonarchBrandReport

    var scale: MonarchAccentScale {
        MonarchAccentScale(
            solid: cssRGBA(solid),
            solidHover: cssRGBA(solidHover),
            solidActive: cssRGBA(solidActive),
            onSolid: cssRGBA(onSolid),
            subtle: cssRGBA(subtle),
            subtleHover: cssRGBA(subtleHover),
            strong: cssRGBA(strong),
            onStrong: cssRGBA(onStrong),
            border: cssRGBA(border),
            strongInverse: cssRGBA(strongInverse)
        )
    }
}

/// Steps 1 to 4 of the solid fill in `deriveIntent`.
private func solidFill(_ base: MonarchHSL, _ t: BrandThemeDefinition, _ adjustments: inout [String])
    -> (solid: MonarchHSL, onSolid: MonarchHSL)
{
    let target = t.text
    let floor = t.canvasFloor
    let away = !t.dark
    var solid = base
    if t.dark && base.s < 8 {
        solid = MonarchHSL(h: base.h, s: base.s, l: 94)
        adjustments.append("achromatic brand inverted to a light fill for dark theme")
    }
    if contrast(solid, t.canvas) < floor {
        solid = ensureOrExtreme(solid, [t.canvas], floor, darken: away)
        adjustments.append(
            "solid moved to \(channels(solid)) to stay visible (\(jsNumber(floor)):1) against the canvas"
        )
    }
    var darkened: MonarchHSL?
    if contrast(white, solid) < target,
        let candidate = ensureContrast(solid, [white], target, darken: true),
        solid.l - candidate.l <= solidDarkenBudget
    {
        darkened = candidate
    }
    if contrast(white, solid) >= target {
        return (solid, white)
    }
    if let darkened, contrast(darkened, t.canvas) >= floor {
        adjustments.append(
            "solid darkened \(channels(solid)) → \(channels(darkened)) so white text meets \(jsNumber(target)):1"
        )
        return (darkened, white)
    }
    if contrast(ink, solid) >= target {
        adjustments.append("dark text used on solid (white would fail contrast)")
        return (solid, ink)
    }
    let fg = t.dark ? ink : white
    let moved = ensureOrExtreme(solid, [fg, t.canvas], target, darken: away)
    adjustments.append(
        "solid moved to \(channels(moved)) so \(fg == white ? "white" : "dark") text meets \(jsNumber(target)):1"
    )
    return (moved, fg)
}

/// Hover and active: shift lightness while keeping both guarantees.
private func shade(_ solid: MonarchHSL, _ onSolid: MonarchHSL, _ t: BrandThemeDefinition, _ steps: Double)
    -> MonarchHSL
{
    let prefer: Double = onSolid == white ? -1 : 1
    for magnitude in [steps, steps / 2] {
        for dir in [prefer, -prefer] {
            let c = emitted(MonarchHSL(h: solid.h, s: solid.s, l: solid.l + dir * magnitude))
            if contrast(onSolid, c) >= t.text && contrast(c, t.canvas) >= t.canvasFloor { return c }
        }
    }
    return solid
}

/// `deriveIntent`: the ten roles of one intent in one theme, each meeting the theme's targets.
func deriveIntent(_ input: MonarchHSL, _ t: BrandThemeDefinition) -> DerivedIntent {
    let base = emitted(input)
    let dark = t.dark
    let hc = t.highContrast
    let target = t.text
    let achromatic = base.s < 8
    var adjustments: [String] = []
    let fill = solidFill(base, t, &adjustments)

    let subtle = emitted(
        dark
            ? MonarchHSL(h: base.h, s: base.s * 0.4, l: hc ? 10 : 14)
            : MonarchHSL(h: base.h, s: Swift.min(base.s, 90) * 0.9, l: hc ? 97 : 96.5)
    )
    let subtleHover = emitted(
        dark
            ? MonarchHSL(h: base.h, s: base.s * 0.4, l: hc ? 14 : 18)
            : MonarchHSL(h: base.h, s: Swift.min(base.s, 90) * 0.9, l: hc ? 94 : 93.5)
    )

    let strongSeed = dark && achromatic ? MonarchHSL(h: base.h, s: base.s, l: 80) : base
    let strong = ensureOrExtreme(
        strongSeed,
        [t.canvas, t.surface, t.subtle, t.muted, subtle, subtleHover],
        target,
        darken: !dark
    )
    if Swift.abs(strong.l - strongSeed.l) >= 0.5 {
        adjustments.append("strong set to \(channels(strong)) for \(jsNumber(target)):1 text contrast")
    }
    let onStrong = contrast(ink, strong) > contrast(white, strong) ? ink : white
    let border =
        hc
        ? strong
        : emitted(
            dark ? MonarchHSL(h: base.h, s: base.s * 0.45, l: 32) : MonarchHSL(h: base.h, s: base.s * 0.6, l: 84)
        )
    let inverseCorner = mixSRGB(fill.solid, t.inverse, inverseTint)
    let inverseCard = mixSRGB(t.textInverse, t.inverse, inverseRaised)
    let strongInverse = ensureOrExtreme(
        dark && achromatic ? MonarchHSL(h: base.h, s: base.s, l: 30) : base,
        [t.inverse, inverseCorner, inverseCard],
        target,
        darken: dark
    )

    func round2(_ n: Double) -> Double { jsRound(n * 100) / 100 }
    let report = MonarchBrandReport(
        base: channels(base),
        adjustments: adjustments,
        ratios: MonarchBrandRatios(
            onSolid: round2(contrast(fill.onSolid, fill.solid)),
            strongOnCanvas: round2(contrast(strong, t.canvas)),
            strongOnSurface: round2(contrast(strong, t.surface)),
            strongOnSubtle: round2(contrast(strong, subtle))
        )
    )
    return DerivedIntent(
        solid: fill.solid,
        solidHover: shade(fill.solid, fill.onSolid, t, 5),
        solidActive: shade(fill.solid, fill.onSolid, t, 9),
        onSolid: fill.onSolid,
        subtle: subtle,
        subtleHover: subtleHover,
        strong: strong,
        onStrong: onStrong,
        border: border,
        strongInverse: strongInverse,
        report: report
    )
}

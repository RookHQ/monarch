// A line-for-line port of the colour maths in packages/tokens/src/color.ts and of
// deriveIntent in packages/tokens/src/themes.ts. The numbers are the web's own; the
// golden vectors in src/test/resources/brand-theme-vectors.json keep the two equal.
// The constants are the web's; the role classes mirror its ten-role scale and theme definition.
@file:Suppress("MagicNumber", "LongParameterList", "TooManyFunctions")

package dev.monarchui.tokens

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/* ------------------------------------------------------------------ */
/* JavaScript number semantics                                         */
/* ------------------------------------------------------------------ */

/** `Math.round`: halves round towards positive infinity. */
internal fun jsRound(x: Double): Double {
    val f = floor(x)
    return if (x - f >= 0.5) f + 1 else f
}

/** `String(n)` for a number with at most one decimal (as `Math.round(n * 10) / 10` gives). */
private fun oneDecimal(n: Double): String {
    val k = jsRound(n * 10).toLong()
    return if (k % 10 == 0L) "${k / 10}" else "${k / 10}.${k % 10}"
}

/** `String(n)` for the contrast targets (4.5, 7, 1.5, 3). */
internal fun jsNumber(n: Double): String = if (n == floor(n)) n.toLong().toString() else n.toString()

/** `n.toFixed(1)`: rounds the exact binary value, halves away from zero. */
internal fun toFixed1(n: Double): String = BigDecimal(n).setScale(1, RoundingMode.HALF_UP).toPlainString()

/** The characters `String.prototype.trim` removes: ECMAScript WhiteSpace and LineTerminator. */
private fun isJsSpace(c: Char): Boolean = when (c) {
    '\u0009', '\u000A', '\u000B', '\u000C', '\u000D', ' ', ' ', ' ',
    ' ', ' ', ' ', ' ', '　', '﻿',
    -> true
    else -> c in ' '..' '
}

internal fun jsTrim(s: String): String = s.trim(::isJsSpace)

/** `Math.hypot` as V8 computes it: scaled by the largest value, Kahan-compensated sum. */
private fun jsHypot(a: Double, b: Double, c: Double): Double {
    val values = doubleArrayOf(abs(a), abs(b), abs(c))
    val largest = values.max()
    if (largest == 0.0) return 0.0
    var sum = 0.0
    var compensation = 0.0
    for (v in values) {
        val n = v / largest
        val summand = n * n - compensation
        val preliminary = sum + summand
        compensation = preliminary - sum - summand
        sum = preliminary
    }
    return sqrt(sum) * largest
}

/* ------------------------------------------------------------------ */
/* color.ts                                                            */
/* ------------------------------------------------------------------ */

internal fun hsl(h: Double, s: Double, l: Double): MonarchHsl = MonarchHsl(h, s, l)

internal fun clamp(value: Double, low: Double, high: Double): Double = min(high, max(low, value))

/** `354.5 71.3% 52.2%`, as `toChannels` prints it. */
internal fun channels(c: MonarchHsl): String = "${oneDecimal(c.h)} ${oneDecimal(c.s)}% ${oneDecimal(c.l)}%"

internal fun toRgb(c: MonarchHsl): DoubleArray {
    val s = c.s / 100
    val l = c.l / 100
    val a = s * min(l, 1 - l)
    fun f(n: Double): Double {
        val k = (n + c.h / 30) % 12
        return l - a * max(-1.0, min(k - 3, min(9 - k, 1.0)))
    }
    return doubleArrayOf(f(0.0), f(8.0), f(4.0))
}

/** The 8-bit channels `toHex` prints. */
internal fun toRgb8(c: MonarchHsl): IntArray = toRgb(c).map { jsRound(it * 255).toInt() }.toIntArray()

internal fun toHex(c: MonarchHsl): String = toRgb8(c).joinToString("", prefix = "#") { "%02x".format(it) }

/** `parseHexColor` after trimming and dropping one `#`: three or six hex digits, else null. */
internal fun parseHexDigits(v: String): MonarchHsl? {
    val isHex = v.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    val six = when {
        isHex && v.length == 3 -> v.map { "$it$it" }.joinToString("")
        isHex && v.length == 6 -> v
        else -> null
    } ?: return null
    val n = six.toInt(16)
    return fromRgb8((n shr 16) and 0xFF, (n shr 8) and 0xFF, n and 0xFF)
}

internal fun fromRgb8(red: Int, green: Int, blue: Int): MonarchHsl {
    val r = red / 255.0
    val g = green / 255.0
    val b = blue / 255.0
    val hi = max(r, max(g, b))
    val lo = min(r, min(g, b))
    val l = (hi + lo) / 2
    val d = hi - lo
    var h = 0.0
    var s = 0.0
    if (d != 0.0) {
        s = d / (1 - abs(2 * l - 1))
        h = when (hi) {
            r -> 60 * (((g - b) / d) % 6)
            g -> 60 * ((b - r) / d + 2)
            else -> 60 * ((r - g) / d + 4)
        }
    }
    return hsl(h, s * 100, l * 100)
}

/** The 256 WCAG linearised channel values, computed once. */
internal val linearChannel: DoubleArray = DoubleArray(256) { linearise(it / 255.0) }

private fun linearise(v: Double): Double = if (v <= 0.04045) v / 12.92 else StrictMath.pow((v + 0.055) / 1.055, 2.4)

/** Relative luminance of the colour as a browser paints it (8-bit channels). */
internal fun relativeLuminance(c: MonarchHsl): Double {
    val rgb = toRgb8(c)
    return 0.2126 * linearChannel[rgb[0]] + 0.7152 * linearChannel[rgb[1]] + 0.0722 * linearChannel[rgb[2]]
}

internal fun contrast(a: MonarchHsl, b: MonarchHsl): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val hi = if (la > lb) la else lb
    val lo = if (la > lb) lb else la
    return (hi + 0.05) / (lo + 0.05)
}

/** OKLab distance ×100, as `colorDistance`. */
internal fun colorDistance(a: MonarchHsl, b: MonarchHsl): Double {
    fun lab(c: MonarchHsl): DoubleArray {
        val (r, g, bl) = toRgb(c).map(::linearise)
        val l = StrictMath.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * bl)
        val m = StrictMath.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * bl)
        val s = StrictMath.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * bl)
        return doubleArrayOf(
            100 * (0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s),
            100 * (1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s),
            100 * (0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s),
        )
    }
    val x = lab(a)
    val y = lab(b)
    return jsHypot(x[0] - y[0], x[1] - y[1], x[2] - y[2])
}

/**
 * Moves lightness by half steps until [color] reaches [minRatio] against every
 * colour in [against]. Null where the web throws (no lightness reaches it).
 */
internal fun ensureContrast(
    color: MonarchHsl,
    against: List<MonarchHsl>,
    minRatio: Double,
    darken: Boolean,
): MonarchHsl? {
    val step = if (darken) -0.5 else 0.5
    var current = emitted(color)
    repeat(220) {
        if (against.all { contrast(current, it) >= minRatio }) return current
        val next = emitted(hsl(current.h, current.s, current.l + step))
        if (next.l == current.l) return null
        current = next
    }
    return null
}

/**
 * [ensureContrast] for the steps the web lets throw. No opaque brand colour
 * reaches that case (checked over the whole sRGB cube at a step of 5); if one
 * ever did, the furthest lightness is used instead of failing the app.
 */
private fun ensureOrExtreme(color: MonarchHsl, against: List<MonarchHsl>, minRatio: Double, darken: Boolean) =
    ensureContrast(color, against, minRatio, darken) ?: hsl(color.h, color.s, if (darken) 0.0 else 100.0)

/** sRGB mix, rounded to 8 bits, as `mixSrgb`. */
internal fun mixSrgb(a: MonarchHsl, b: MonarchHsl, p: Double): MonarchHsl {
    val x = toRgb(a)
    val y = toRgb(b)
    fun m(i: Int) = jsRound((x[i] * p + y[i] * (1 - p)) * 255).toInt()
    return fromRgb8(m(0), m(1), m(2))
}

/**
 * The colour exactly as the CSS prints it (each channel rounded to one decimal), as `emitted`.
 * The engine measures and returns emitted colours only, so its guarantees hold for the shipped values.
 */
internal fun emitted(c: MonarchHsl): MonarchHsl =
    hsl(jsRound(c.h * 10) / 10, jsRound(c.s * 10) / 10, jsRound(c.l * 10) / 10)

/** The colour as the CSS prints it (channels rounded to one decimal), as ARGB. */
internal fun cssArgb(c: MonarchHsl): Long {
    val (r, g, b) = toRgb8(emitted(c))
    return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}

/* ------------------------------------------------------------------ */
/* themes.ts                                                           */
/* ------------------------------------------------------------------ */

internal object Neutral {
    val n0 = hsl(0.0, 0.0, 100.0)
    val n25 = hsl(220.0, 20.0, 99.0)
    val n50 = hsl(220.0, 17.0, 97.5)
    val n100 = hsl(220.0, 14.0, 95.5)
    val n800 = hsl(221.0, 15.0, 18.0)
    val n850 = hsl(222.0, 16.0, 13.5)
    val n900 = hsl(222.0, 18.0, 10.0)
    val n950 = hsl(224.0, 22.0, 6.5)
    val n1000 = hsl(0.0, 0.0, 0.0)
}

/** The parts of a theme definition `deriveIntent` reads. */
internal class BrandThemeDefinition(
    val id: String,
    val dark: Boolean,
    val highContrast: Boolean,
    val text: Double,
    val ui: Double,
    val canvas: MonarchHsl,
    val surface: MonarchHsl,
    val subtle: MonarchHsl,
    val muted: MonarchHsl,
    val inverse: MonarchHsl,
    val textInverse: MonarchHsl,
)

internal val brandThemes: List<BrandThemeDefinition> = with(Neutral) {
    listOf(
        BrandThemeDefinition("light", false, false, 4.5, 3.0, n25, n0, n50, n100, n900, n0),
        BrandThemeDefinition("dark", true, false, 4.5, 3.0, n950, n900, n850, n800, n50, n900),
        BrandThemeDefinition("hc-light", false, true, 7.0, 4.5, n0, n0, n50, n100, n1000, n0),
        BrandThemeDefinition("hc-dark", true, true, 7.0, 4.5, n1000, n1000, n950, n900, n0, n1000),
    )
}

/** The Rook brand colours the status intents and the default accent are derived from. */
internal object BrandBase {
    val red = fromRgb8(0xdc, 0x2e, 0x3e)
    val green = fromRgb8(0x28, 0xaf, 0x4c)
    val yellow = fromRgb8(0xf4, 0xb4, 0x00)
    val blue = fromRgb8(0x00, 0x96, 0xd4)
    val defaultAccent = red
    val status = listOf("danger" to red, "success" to green, "warning" to yellow, "info" to blue)
}

private val White = Neutral.n0
private val Ink = Neutral.n950

/** How far a brand fill may be darkened to keep white text before dark text is used. */
private const val SolidDarkenBudget = 10.0
private const val InverseTint = 0.12
private const val InverseRaised = 0.09
private const val SolidCanvasFloor = 1.5

/** One intent in one theme, in HSL, with its report. */
internal class DerivedIntent(
    val solid: MonarchHsl,
    val solidHover: MonarchHsl,
    val solidActive: MonarchHsl,
    val onSolid: MonarchHsl,
    val subtle: MonarchHsl,
    val subtleHover: MonarchHsl,
    val strong: MonarchHsl,
    val onStrong: MonarchHsl,
    val border: MonarchHsl,
    val strongInverse: MonarchHsl,
    val report: MonarchBrandReport,
) {
    fun scale(): MonarchAccentScale = MonarchAccentScale(
        cssArgb(solid),
        cssArgb(solidHover),
        cssArgb(solidActive),
        cssArgb(onSolid),
        cssArgb(subtle),
        cssArgb(subtleHover),
        cssArgb(strong),
        cssArgb(onStrong),
        cssArgb(border),
        cssArgb(strongInverse),
    )
}

private class SolidFill(val solid: MonarchHsl, val onSolid: MonarchHsl)

/** The fill darkened within budget until white text reaches [target], or null. */
private fun darkenedForWhite(solid: MonarchHsl, target: Double): MonarchHsl? {
    if (contrast(White, solid) >= target) return null
    val candidate = ensureContrast(solid, listOf(White), target, darken = true) ?: return null
    return if (solid.l - candidate.l <= SolidDarkenBudget) candidate else null
}

/** Steps 1 to 4 of the solid fill in `deriveIntent`. */
private fun solidFill(base: MonarchHsl, t: BrandThemeDefinition, adjustments: MutableList<String>): SolidFill {
    val target = t.text
    val canvasFloor = if (t.highContrast) t.ui else SolidCanvasFloor
    val away = !t.dark
    var solid = base
    if (t.dark && base.s < 8) {
        solid = hsl(base.h, base.s, 94.0)
        adjustments += "achromatic brand inverted to a light fill for dark theme"
    }
    if (contrast(solid, t.canvas) < canvasFloor) {
        solid = ensureOrExtreme(solid, listOf(t.canvas), canvasFloor, away)
        adjustments += "solid moved to ${channels(solid)} to stay visible " +
            "(${jsNumber(canvasFloor)}:1) against the canvas"
    }
    val darkened = darkenedForWhite(solid, target)
    return when {
        contrast(White, solid) >= target -> SolidFill(solid, White)
        darkened != null && contrast(darkened, t.canvas) >= canvasFloor -> {
            adjustments += "solid darkened ${channels(solid)} → ${channels(darkened)} " +
                "so white text meets ${jsNumber(target)}:1"
            SolidFill(darkened, White)
        }
        contrast(Ink, solid) >= target -> {
            adjustments += "dark text used on solid (white would fail contrast)"
            SolidFill(solid, Ink)
        }
        else -> {
            val fg = if (t.dark) Ink else White
            val moved = ensureOrExtreme(solid, listOf(fg, t.canvas), target, away)
            adjustments += "solid moved to ${channels(moved)} so ${if (fg == White) "white" else "dark"} " +
                "text meets ${jsNumber(target)}:1"
            SolidFill(moved, fg)
        }
    }
}

/** Hover and active: shift lightness while keeping both guarantees. */
private fun shade(fill: SolidFill, t: BrandThemeDefinition, steps: Double): MonarchHsl {
    val canvasFloor = if (t.highContrast) t.ui else SolidCanvasFloor
    val prefer = if (fill.onSolid == White) -1.0 else 1.0
    for (magnitude in listOf(steps, steps / 2)) {
        for (dir in listOf(prefer, -prefer)) {
            val c = emitted(hsl(fill.solid.h, fill.solid.s, fill.solid.l + dir * magnitude))
            if (contrast(fill.onSolid, c) >= t.text && contrast(c, t.canvas) >= canvasFloor) return c
        }
    }
    return fill.solid
}

/** The `subtle` and `subtle-hover` tints. */
private fun tint(base: MonarchHsl, t: BrandThemeDefinition, hover: Boolean): MonarchHsl {
    val hc = t.highContrast
    return if (t.dark) {
        val l = if (hover) (if (hc) 14.0 else 18.0) else (if (hc) 10.0 else 14.0)
        emitted(hsl(base.h, base.s * 0.4, l))
    } else {
        val l = if (hover) (if (hc) 94.0 else 93.5) else (if (hc) 97.0 else 96.5)
        emitted(hsl(base.h, min(base.s, 90.0) * 0.9, l))
    }
}

/** The strong role for text on inverse sections, as `deriveIntent` computes it. */
private fun strongInverse(base: MonarchHsl, solid: MonarchHsl, t: BrandThemeDefinition): MonarchHsl {
    val inverseCorner = mixSrgb(solid, t.inverse, InverseTint)
    val inverseCard = mixSrgb(t.textInverse, t.inverse, InverseRaised)
    return ensureOrExtreme(
        if (t.dark && base.s < 8) hsl(base.h, base.s, 30.0) else base,
        listOf(t.inverse, inverseCorner, inverseCard),
        t.text,
        darken = t.dark,
    )
}

/** `deriveIntent`: the ten roles of one intent in one theme, each meeting the theme's targets. */
internal fun deriveIntent(input: MonarchHsl, t: BrandThemeDefinition): DerivedIntent {
    val base = emitted(input)
    val dark = t.dark
    val hc = t.highContrast
    val target = t.text
    val achromatic = base.s < 8
    val adjustments = mutableListOf<String>()
    val fill = solidFill(base, t, adjustments)

    val subtle = tint(base, t, hover = false)
    val subtleHover = tint(base, t, hover = true)

    val strongSeed = if (dark && achromatic) hsl(base.h, base.s, 80.0) else base
    val strong = ensureOrExtreme(
        strongSeed,
        listOf(t.canvas, t.surface, t.subtle, t.muted, subtle, subtleHover),
        target,
        darken = !dark,
    )
    if (abs(strong.l - strongSeed.l) >= 0.5) {
        adjustments += "strong set to ${channels(strong)} for ${jsNumber(target)}:1 text contrast"
    }
    val onStrong = if (contrast(Ink, strong) > contrast(White, strong)) Ink else White
    val border = when {
        hc -> strong
        dark -> emitted(hsl(base.h, base.s * 0.45, 32.0))
        else -> emitted(hsl(base.h, base.s * 0.6, 84.0))
    }

    fun round2(n: Double) = jsRound(n * 100) / 100
    val report = MonarchBrandReport(
        base = channels(base),
        adjustments = adjustments.toList(),
        ratios = MonarchBrandRatios(
            onSolid = round2(contrast(fill.onSolid, fill.solid)),
            strongOnCanvas = round2(contrast(strong, t.canvas)),
            strongOnSurface = round2(contrast(strong, t.surface)),
            strongOnSubtle = round2(contrast(strong, subtle)),
        ),
    )
    return DerivedIntent(
        solid = fill.solid,
        solidHover = shade(fill, t, 5.0),
        solidActive = shade(fill, t, 9.0),
        onSolid = fill.onSolid,
        subtle = subtle,
        subtleHover = subtleHover,
        strong = strong,
        onStrong = onStrong,
        border = border,
        strongInverse = strongInverse(base, fill.solid, t),
        report = report,
    )
}

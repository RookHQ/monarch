package com.rookhq.monarch.tokens

/**
 * A colour in HSL, as the web token source writes it.
 *
 * The hue is normalised to 0 until 360, saturation and lightness are clamped
 * to 0 to 100, exactly as `hsl()` in `packages/tokens/src/color.ts`.
 */
public class MonarchHsl(h: Double, s: Double, l: Double) {
    /** Hue, 0 until 360. */
    public val h: Double = ((h % 360) + 360) % 360

    /** Saturation, 0 to 100. */
    public val s: Double = clamp(s, 0.0, 100.0)

    /** Lightness, 0 to 100. */
    public val l: Double = clamp(l, 0.0, 100.0)

    /** The colour as lowercase `#rrggbb`. */
    public val hex: String get() = toHex(this)

    /** The colour as opaque ARGB (`0xFFRRGGBB`), the format of the generated tokens. */
    public val argb: Long
        get() {
            val (r, g, b) = toRgb8(this)
            return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
        }

    override fun equals(other: Any?): Boolean = other is MonarchHsl && other.h == h && other.s == s && other.l == l

    override fun hashCode(): Int = (h.hashCode() * 31 + s.hashCode()) * 31 + l.hashCode()

    override fun toString(): String = "MonarchHsl(${channels(this)})"
}

/**
 * Parses `#rgb`, `#rrggbb`, `rgb` or `rrggbb` (any case, surrounding white
 * space allowed), like `parseHexColor` on the web.
 *
 * @param input the colour a person or a server supplied.
 * @return the colour, or `null` for anything else, including named colours and
 * `#rrggbbaa`: a brand colour must be opaque for contrast to mean anything.
 */
public fun parseHexColor(input: String?): MonarchHsl? =
    input?.let { parseHexDigits(jsTrim(it).removePrefix("#")) }

/** The contrast ratios measured for one derived intent, rounded to two decimals. */
public class MonarchBrandRatios(
    /** `onSolid` on `solid`. */
    public val onSolid: Double,
    /** `strong` on the canvas. */
    public val strongOnCanvas: Double,
    /** `strong` on the surface. */
    public val strongOnSurface: Double,
    /** `strong` on the intent's own `subtle` tint. */
    public val strongOnSubtle: Double,
)

/** What the engine changed to meet contrast, and the ratios it measured. */
public class MonarchBrandReport(
    /** The colour the intent was derived from, as HSL channels (`354.5 71.3% 52.2%`). */
    public val base: String,
    /** One plain-language note per adjustment. Empty when the colour was used as given. */
    public val adjustments: List<String>,
    /** The measured ratios. */
    public val ratios: MonarchBrandRatios,
)

/** The derived colours of one theme. */
public class MonarchBrandMode(
    /** `light`, `dark`, `hc-light` or `hc-dark`. */
    public val themeId: String,
    /** The ten accent roles for this theme. */
    public val accent: MonarchAccentScale,
    /** What was adjusted in the accent, and the measured ratios. */
    public val report: MonarchBrandReport,
    /** The AI roles derived from the custom AI colour, or `null` when none was given or it was invalid. */
    public val ai: MonarchAccentScale?,
    /** What was adjusted in the AI colour, when there is one. */
    public val aiReport: MonarchBrandReport?,
)

/** The check of a custom AI colour: valid, and far enough from the accent and the status colours. */
public class MonarchBrandAiCheck(
    /** The value that was passed, unchanged. */
    public val input: String,
    /** The AI colour used, as lowercase `#rrggbb`, or `null` when [input] was invalid. */
    public val color: String?,
    /** False when [input] was not a hex colour; the Monarch AI purple is kept. */
    public val isValid: Boolean,
    /** The nearest accent or status fill, in any theme. `null` when [isValid] is false. */
    public val closest: Closest?,
    /** True when [closest] is at least [MonarchBrandTheme.aiMinDistance] away. */
    public val isDistinct: Boolean,
    /** Plain-language notes for whoever picked the colour. Empty when all is well. */
    public val warnings: List<String>,
) {
    /** The colour the AI fill is closest to. */
    public class Closest(
        /** Perceptual distance (OKLab ΔE ×100). */
        public val distance: Double,
        /** `accent`, `danger`, `success`, `warning` or `info`. */
        public val to: String,
        /** The theme where it is closest. */
        public val themeId: String,
    )
}

/**
 * One brand colour in, a complete accessible accent for all four themes out:
 * the native port of `deriveTheme` in `@rookcom/monarch-tokens`, with results
 * identical to the web.
 *
 * Guarantees, in every theme: `onSolid` on the solid fills reaches 4.5:1 (7:1
 * in the high-contrast themes); `strong` reaches the same on the canvas,
 * surface, subtle and muted backgrounds and on the accent's own tints; and the
 * fill stays distinguishable from the canvas (1.5:1, 4.5:1 high contrast).
 *
 * ```kotlin
 * val brand = MonarchBrandTheme.derive("#0A7CFF")
 * val theme = MonarchTokens.dark.withAccent(brand.accent)
 * ```
 *
 * Pure: no Android, no state, the same input always gives the same output.
 */
public class MonarchBrandTheme internal constructor(
    /** The value that was passed, unchanged. */
    public val input: String,
    /** The colour the theme was derived from, as lowercase `#rrggbb`. */
    public val brand: String,
    /** False when [input] was not a hex colour and the default accent (Rook Red) was used. */
    public val isValid: Boolean,
    /** The light theme. */
    public val light: MonarchBrandMode,
    /** The dark theme. */
    public val dark: MonarchBrandMode,
    /** The high-contrast light theme. */
    public val hcLight: MonarchBrandMode,
    /** The high-contrast dark theme. */
    public val hcDark: MonarchBrandMode,
    /** The AI colour check, when an AI colour was passed (even an invalid one). */
    public val ai: MonarchBrandAiCheck?,
) {
    /** The four modes, in the order light, dark, high-contrast light, high-contrast dark. */
    public val modes: List<MonarchBrandMode> get() = listOf(light, dark, hcLight, hcDark)

    /**
     * The derived accent, ready for `MonarchThemeTokens.withAccent` and the theme entry points.
     * When [isValid] is false this is `MonarchTokens.Accents.rook`, the default accent.
     */
    public val accent: MonarchAccent
        get() = if (!isValid) MonarchTokens.Accents.rook else MonarchAccent(
            "brand",
            "Brand $brand",
            light.accent,
            dark.accent,
            hcLight.accent,
            hcDark.accent,
        )

    /** The derived AI roles as an accent-shaped value, or `null` when there is no valid AI colour. */
    public val aiAccent: MonarchAccent?
        get() {
            val l = light.ai ?: return null
            val d = dark.ai ?: return null
            val hl = hcLight.ai ?: return null
            val hd = hcDark.ai ?: return null
            return MonarchAccent("brand-ai", "AI ${ai?.color}", l, d, hl, hd)
        }

    /** The mode for a theme id (`light`, `dark`, `hc-light`, `hc-dark`); anything else gives [light]. */
    public fun mode(themeId: String): MonarchBrandMode = when (themeId) {
        "dark" -> dark
        "hc-light" -> hcLight
        "hc-dark" -> hcDark
        else -> light
    }

    public companion object {
        /** The smallest OKLab distance (×100) the AI fill should keep from the accent and status fills. */
        // A val, not a const, so the name matches `MonarchBrandTheme.aiMinDistance` on iOS.
        @Suppress("MayBeConst")
        public val aiMinDistance: Double = 10.0

        private val neighbourNames = mapOf(
            "accent" to "the brand colour",
            "danger" to "the error red",
            "success" to "the success green",
            "warning" to "the warning amber",
            "info" to "the info blue",
        )

        /**
         * Derives the accent for all four themes from [brand].
         *
         * @param brand `#rgb` or `#rrggbb`. Anything else falls back to the default
         * accent and sets [isValid] to false, so a bad saved value never breaks the app.
         * @param ai an optional custom AI colour, derived with the same guarantees and
         * checked for distance from the accent and the status colours.
         */
        public fun derive(brand: String, ai: String? = null): MonarchBrandTheme {
            val parsed = parseHexColor(brand)
            val base = parsed ?: BrandBase.defaultAccent
            val aiBase = ai?.let(::parseHexColor)
            var closest: MonarchBrandAiCheck.Closest? = null
            val modes = brandThemes.map { t ->
                val accent = deriveIntent(base, t)
                val aiIntent = aiBase?.let { deriveIntent(it, t) }
                if (aiIntent != null) {
                    val fills = listOf("accent" to accent.solid) +
                        BrandBase.status.map { (id, c) -> id to deriveIntent(c, t).solid }
                    for ((to, fill) in fills) {
                        val distance = colorDistance(aiIntent.solid, fill)
                        val best = closest
                        if (best == null || distance < best.distance) {
                            closest = MonarchBrandAiCheck.Closest(distance, to, t.id)
                        }
                    }
                }
                MonarchBrandMode(t.id, accent.scale(), accent.report, aiIntent?.scale(), aiIntent?.report)
            }
            return MonarchBrandTheme(
                input = brand,
                brand = toHex(base),
                isValid = parsed != null,
                light = modes[0],
                dark = modes[1],
                hcLight = modes[2],
                hcDark = modes[3],
                ai = ai?.let { aiCheck(it, aiBase, closest) },
            )
        }

        private fun aiCheck(input: String, base: MonarchHsl?, closest: MonarchBrandAiCheck.Closest?) =
            if (base == null || closest == null) {
                MonarchBrandAiCheck(
                    input = input,
                    color = null,
                    isValid = false,
                    closest = null,
                    isDistinct = false,
                    warnings = listOf("\"$input\" is not a hex colour, so the Monarch AI purple is used."),
                )
            } else {
                val distinct = closest.distance >= aiMinDistance
                val warnings = if (distinct) {
                    emptyList()
                } else {
                    listOf(
                        "The AI colour is too close to ${neighbourNames[closest.to]} in the ${closest.themeId} " +
                            "theme (distance ${toFixed1(closest.distance)}, needs ${jsNumber(aiMinDistance)}). " +
                            "People may not tell AI content apart from it. Pick a hue further away; " +
                            "the AI label and icon still show.",
                    )
                }
                MonarchBrandAiCheck(input, toHex(base), true, closest, distinct, warnings)
            }
    }
}

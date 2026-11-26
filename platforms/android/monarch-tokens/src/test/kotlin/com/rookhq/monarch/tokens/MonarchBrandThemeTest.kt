package com.rookhq.monarch.tokens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The native engine against the web: every golden vector must match exactly. */
class MonarchBrandThemeTest {
    private val vectors = MiniJson.parse(
        checkNotNull(javaClass.classLoader.getResource("brand-theme-vectors.json")).readText(),
    ).obj()

    private val themes = vectors["themes"].list().map { it.obj() }

    private fun hex(argb: Long) = "#%06X".format(argb and 0xFFFFFF)

    private fun scaleMap(s: MonarchAccentScale) = mapOf(
        "solid" to hex(s.solid),
        "solidHover" to hex(s.solidHover),
        "solidActive" to hex(s.solidActive),
        "onSolid" to hex(s.onSolid),
        "subtle" to hex(s.subtle),
        "subtleHover" to hex(s.subtleHover),
        "strong" to hex(s.strong),
        "onStrong" to hex(s.onStrong),
        "border" to hex(s.border),
        "strongInverse" to hex(s.strongInverse),
    )

    private fun reportMap(r: MonarchBrandReport) = mapOf(
        "base" to r.base,
        "adjustments" to r.adjustments,
        "ratios" to mapOf(
            "onSolid" to r.ratios.onSolid,
            "strongOnCanvas" to r.ratios.strongOnCanvas,
            "strongOnSurface" to r.ratios.strongOnSurface,
            "strongOnSubtle" to r.ratios.strongOnSubtle,
        ),
    )

    private fun aiMap(c: MonarchBrandAiCheck?) = c?.let {
        mapOf(
            "input" to it.input,
            "color" to it.color,
            "valid" to it.isValid,
            "closest" to it.closest?.let { x ->
                mapOf("distance" to jsRound(x.distance * 1e9) / 1e9, "to" to x.to, "theme" to x.themeId)
            },
            "distinct" to it.isDistinct,
            "warnings" to it.warnings,
        )
    }

    /** The native result in the shape of the JSON, so one equality compares every value. */
    private fun resultMap(t: MonarchBrandTheme) = mapOf(
        "input" to t.input,
        "brand" to t.brand,
        "valid" to t.isValid,
        "modes" to t.modes.associate { m ->
            m.themeId to buildMap {
                put("accent", scaleMap(m.accent))
                put("report", reportMap(m.report))
                m.ai?.let { put("ai", scaleMap(it)) }
                m.aiReport?.let { put("aiReport", reportMap(it)) }
            }
        },
        "ai" to aiMap(t.ai),
    )

    @Test
    fun hasEnoughVectors() {
        assertTrue(themes.size >= 40, "${themes.size} theme vectors")
        assertTrue(themes.any { it.containsKey("ai") })
        assertTrue(themes.any { it["result"].obj()["valid"] == false })
    }

    @Test
    fun linearisedChannelsMatchTheWeb() {
        val web = vectors["linear"].list()
        assertEquals(256, web.size)
        // The web writes 12 significant digits; pow may differ between platforms in the last bit.
        web.forEachIndexed { i, v -> assertEquals(v as Double, linearChannel[i], 1e-11 * v + 1e-18, "channel $i") }
    }

    @Test
    fun parseMatchesTheWeb() {
        for (case in vectors["parse"].list().map { it.obj() }) {
            val input = case["input"] as String?
            val parsed = parseHexColor(input)
            if (case["hex"] == null) {
                assertNull(parsed, "parse '$input'")
            } else {
                assertNotNull(parsed, "parse '$input'")
                assertEquals(case["hex"], parsed.hex, "hex of '$input'")
                assertEquals(case["h"], parsed.h, "h of '$input'")
                assertEquals(case["s"], parsed.s, "s of '$input'")
                assertEquals(case["l"], parsed.l, "l of '$input'")
            }
        }
    }

    @Test
    fun everyGoldenVectorMatchesExactly() {
        for (v in themes) {
            val brand = v["brand"] as String
            val ai = v["ai"] as String?
            val native = resultMap(MonarchBrandTheme.derive(brand, ai))
            val web = v["result"].obj()
            assertEquals(web, native, "derive('$brand', ai = $ai)")
        }
    }

    @Test
    fun contrastGuaranteesHoldNatively() {
        val bases = themes.map { it["brand"] as String } + themes.mapNotNull { it["ai"] as String? }
        for (input in bases) {
            val base = parseHexColor(input) ?: BrandBase.defaultAccent
            for (def in brandThemes) {
                val target = def.text
                val floor = if (def.highContrast) def.ui else 1.5
                val i = deriveIntent(base, def)
                for (fill in listOf(i.solid, i.solidHover, i.solidActive)) {
                    assertTrue(contrast(i.onSolid, fill) >= target, "$input ${def.id} on-solid")
                    assertTrue(contrast(fill, def.canvas) >= floor, "$input ${def.id} fill on canvas")
                }
                for (bg in listOf(def.canvas, def.surface, def.subtle, def.muted, i.subtle, i.subtleHover)) {
                    assertTrue(contrast(i.strong, bg) >= target, "$input ${def.id} strong")
                }
                assertTrue(contrast(i.strongInverse, def.inverse) >= target, "$input ${def.id} strong-inverse")
            }
        }
    }

    @Test
    fun defaultBrandReproducesTheGeneratedRookAccent() {
        val derived = MonarchBrandTheme.derive("#dc2e3e").accent
        val rook = MonarchTokens.Accents.rook
        for (id in listOf("light", "dark", "hc-light", "hc-dark")) {
            assertEquals(scaleMap(rook.scale(id)), scaleMap(derived.scale(id)), id)
        }
    }

    @Test
    fun invalidInputFallsBackToTheDefaultAccent() {
        for (input in listOf("", "red", "#12345", "#12345678", "#ggg")) {
            val t = MonarchBrandTheme.derive(input)
            assertFalse(t.isValid, input)
            assertEquals("#dc2e3e", t.brand)
            assertEquals(input, t.input)
            assertEquals(scaleMap(MonarchTokens.Accents.rook.light), scaleMap(t.light.accent))
        }
    }

    @Test
    fun parsesShortAndLongHex() {
        assertEquals("#aabbcc", parseHexColor("#ABC")?.hex)
        assertEquals("#aabbcc", parseHexColor(" abc ")?.hex)
        assertEquals("#0a7cff", parseHexColor("0A7CFF")?.hex)
        assertNull(parseHexColor(null))
        assertNull(parseHexColor("#0a7cff00"))
        assertNull(parseHexColor("rgb(0,0,0)"))
    }

    @Test
    fun aiColourTooCloseIsReported() {
        val t = MonarchBrandTheme.derive("#0a7cff", ai = "#0b7dff")
        val check = assertNotNull(t.ai)
        assertTrue(check.isValid)
        assertFalse(check.isDistinct)
        assertEquals("accent", check.closest?.to)
        assertEquals(1, check.warnings.size)
        assertNotNull(t.aiAccent)

        val none = MonarchBrandTheme.derive("#0a7cff")
        assertNull(none.ai)
        assertNull(none.aiAccent)

        val invalid = assertNotNull(MonarchBrandTheme.derive("#0a7cff", ai = "purple").ai)
        assertFalse(invalid.isValid)
        assertNull(invalid.color)
    }

    @Test
    fun withAccentAppliesTheDerivedAccent() {
        val brand = MonarchBrandTheme.derive("#0A7CFF")
        for (theme in MonarchTokens.themes) {
            val colors = theme.withAccent(brand.accent).colors
            assertEquals(brand.mode(theme.id).accent.solid, colors.accentSolid, theme.id)
            assertEquals(brand.mode(theme.id).accent.strong, colors.accentStrong, theme.id)
        }
    }
}

package dev.monarchui.tokens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class WithAiTest {
    private val themes = listOf(MonarchTokens.light, MonarchTokens.dark, MonarchTokens.hcLight, MonarchTokens.hcDark)
    private val ai = assertNotNull(MonarchBrandTheme.derive("#0a7cff", ai = "#9333ea").aiAccent)
    private val aiFamily = setOf(
        "aiSolid", "aiSolidHover", "aiSolidActive", "aiOnSolid", "aiSubtle", "aiSubtleHover",
        "aiStrong", "aiOnStrong", "aiBorder", "aiStrongInverse", "aiThinking", "aiCitation",
    )

    @Test
    fun replacesTheTenAiRolesAndTheirFollowers() {
        themes.forEach { theme ->
            val scale = ai.scale(theme.id)
            val colors = theme.withAi(ai).colors
            assertEquals(scale.solid, colors.aiSolid, theme.id)
            assertEquals(scale.solidHover, colors.aiSolidHover, theme.id)
            assertEquals(scale.solidActive, colors.aiSolidActive, theme.id)
            assertEquals(scale.onSolid, colors.aiOnSolid, theme.id)
            assertEquals(scale.subtle, colors.aiSubtle, theme.id)
            assertEquals(scale.subtleHover, colors.aiSubtleHover, theme.id)
            assertEquals(scale.strong, colors.aiStrong, theme.id)
            assertEquals(scale.onStrong, colors.aiOnStrong, theme.id)
            assertEquals(scale.border, colors.aiBorder, theme.id)
            assertEquals(scale.strongInverse, colors.aiStrongInverse, theme.id)
            assertEquals(scale.subtle, colors.aiThinking, "${theme.id} thinking follows ai subtle")
            assertEquals(scale.strong, colors.aiCitation, "${theme.id} citation follows ai strong")
        }
    }

    @Test
    fun leavesEveryOtherRoleAlone() {
        themes.forEach { theme ->
            val before = colourRoles(theme.colors)
            val after = colourRoles(theme.withAi(ai).colors)
            before.keys.filterNot { it in aiFamily }.forEach { role ->
                assertEquals(before[role], after[role], "${theme.id}.$role changed")
            }
            // aiTool is the info colour and the diff roles are status colours: not the AI colour.
            assertEquals(theme.colors.aiTool, theme.withAi(ai).colors.aiTool, theme.id)
        }
    }

    @Test
    fun keepsThemeIdentityAndShadows() {
        themes.forEach { theme ->
            val next = theme.withAi(ai)
            assertEquals(theme.id, next.id)
            assertEquals(theme.isDark, next.isDark)
            assertEquals(theme.isHighContrast, next.isHighContrast)
            assertEquals(theme.focusRingWidth, next.focusRingWidth)
            assertSame(theme.shadows, next.shadows)
        }
    }

    @Test
    fun composesWithWithAccent() {
        val brand = MonarchBrandTheme.derive("#0a7cff", ai = "#9333ea")
        themes.forEach { theme ->
            val both = theme.withAccent(brand.accent).withAi(ai).colors
            assertEquals(brand.accent.scale(theme.id).solid, both.accentSolid, theme.id)
            assertEquals(ai.scale(theme.id).solid, both.aiSolid, theme.id)
        }
    }

    @Test
    fun applyingTheBuiltInAiRolesIsANoOp() {
        themes.forEach { theme ->
            val c = theme.colors
            val own = MonarchAccentScale(
                c.aiSolid, c.aiSolidHover, c.aiSolidActive, c.aiOnSolid, c.aiSubtle,
                c.aiSubtleHover, c.aiStrong, c.aiOnStrong, c.aiBorder, c.aiStrongInverse,
            )
            val builtIn = MonarchAccent("ai", "Monarch AI", own, own, own, own)
            assertEquals(colourRoles(c), colourRoles(theme.withAi(builtIn).colors), theme.id)
        }
    }
}

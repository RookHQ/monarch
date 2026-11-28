package com.rookhq.monarch.tokens

import kotlin.test.Test
import kotlin.test.assertEquals

/** C-086: no two neutral roles that sit on each other share a colour. */
class NeutralRolesTest {
    private val themes = listOf(MonarchTokens.light, MonarchTokens.dark, MonarchTokens.hcLight, MonarchTokens.hcDark)

    @Test
    fun subtleDiffersFromSurfaceAndDefaultBorderFromMuted() {
        themes.forEach { theme ->
            val c = theme.colors
            assertDiffers(c.bgSurface, c.bgSubtle, "${theme.id} bgSubtle equals bgSurface")
            assertDiffers(c.bgMuted, c.borderDefault, "${theme.id} borderDefault equals bgMuted")
        }
    }

    @Test
    fun layoutAndWidthTokensMatchTheWeb() {
        assertEquals(704f, MonarchTokens.Width.narrow)
        assertEquals(1152f, MonarchTokens.Width.content)
        assertEquals(1280f, MonarchTokens.Width.wide)
        assertEquals(672f, MonarchTokens.Layout.readingMax)
        assertEquals(1280f, MonarchTokens.Layout.contentMax)
    }
}

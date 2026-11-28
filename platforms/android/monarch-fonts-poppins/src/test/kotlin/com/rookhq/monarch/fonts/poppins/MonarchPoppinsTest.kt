package com.rookhq.monarch.fonts.poppins

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.font.FontListFontFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MonarchPoppinsTest {
    @Test
    fun theFamilyHasRegularMediumAndSemiBold() {
        val family = MonarchPoppins as FontListFontFamily
        val weights = family.fonts.map { it.weight }
        assertTrue(FontWeight.Normal in weights)
        assertTrue(FontWeight.Medium in weights)
        assertTrue(FontWeight.SemiBold in weights)
        assertTrue(family.fonts.all { it is ResourceFont })
    }

    @Test
    fun theTypographyUsesPoppinsAndKeepsMonospaceForCode() {
        val typography = monarchPoppinsTypography()
        assertEquals(MonarchPoppins, typography.body.fontFamily)
        assertEquals(FontFamily.Monospace, typography.code.fontFamily)
    }

    @Test
    fun theLicenceTextShipsWithTheArtifact() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val text = context.resources.openRawResource(R.raw.monarch_poppins_ofl).bufferedReader().readText()
        assertNotNull(text)
        assertTrue(text.contains("SIL OPEN FONT LICENSE Version 1.1"))
        assertTrue(text.contains("The Poppins Project Authors"))
    }
}

package com.rookhq.monarch.fonts.poppins

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.foundation.monarchTypography

/**
 * Poppins as a Compose `FontFamily`: regular (400), medium (500) and semibold (600). Bold maps to semibold, the
 * heaviest face Monarch's type roles use. The font is licensed under the SIL Open Font License 1.1; the
 * licence text is in this artifact as the raw resource `monarch_poppins_ofl`.
 *
 * Monarch's default stays the system font. Pass this to the theme to use Poppins:
 * `MonarchTheme(typography = monarchPoppinsTypography()) { ... }`.
 */
public val MonarchPoppins: FontFamily = FontFamily(
    Font(R.font.monarch_poppins_regular, FontWeight.Normal),
    Font(R.font.monarch_poppins_medium, FontWeight.Medium),
    Font(R.font.monarch_poppins_semibold, FontWeight.SemiBold),
    Font(R.font.monarch_poppins_semibold, FontWeight.Bold),
)

/**
 * The Monarch type roles set in [MonarchPoppins]. Code styles keep the monospace face.
 *
 * @param codeFontFamily the typeface of the code roles.
 */
public fun monarchPoppinsTypography(codeFontFamily: FontFamily = FontFamily.Monospace): MonarchTypography =
    monarchTypography(fontFamily = MonarchPoppins, codeFontFamily = codeFontFamily)

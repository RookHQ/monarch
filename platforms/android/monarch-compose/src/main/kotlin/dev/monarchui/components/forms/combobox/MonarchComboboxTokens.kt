package dev.monarchui.components.forms.combobox

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchCombobox. */
internal object MonarchComboboxTokens {
    /** The most options the automatic presentation shows in a menu; longer lists open a sheet. */
    const val MENU_LIMIT: Int = 8

    /** The height of the menu in steps of the largest spacing token. */
    private const val MENU_MAX_STEPS: Float = 3.5f

    /** The tallest the menu grows before it scrolls, a bounded popover as on the web. */
    val MenuMaxHeight: Dp = (MonarchTokens.Space.space24 * MENU_MAX_STEPS).dp

    /** The least height of a row in the sheet: the touch target. */
    val RowMinHeight: Dp = MonarchTouchTarget.Minimum

    /** The padding of the sheet's content and its rows. */
    val SheetPadding: Dp = MonarchTokens.Space.space4.dp
    val RowVerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the parts of the sheet, and between a row's texts and its mark. */
    val SheetGap: Dp = MonarchTokens.Space.space3.dp
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between an option's label and its description. */
    val ItemTextGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The padding of a group heading or a message in the list. */
    val HeaderHorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val HeaderVerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The presentation used: the given one, or a menu for short lists the app does not search. */
    fun presentation(
        presentation: MonarchComboboxPresentation,
        optionCount: Int,
        searchesInApp: Boolean,
    ): MonarchComboboxPresentation = when {
        presentation != MonarchComboboxPresentation.Automatic -> presentation
        searchesInApp || optionCount > MENU_LIMIT -> MonarchComboboxPresentation.Sheet
        else -> MonarchComboboxPresentation.Menu
    }

    /** The colour of an option's label. */
    fun labelColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of an option's description and hint, of group headings and of list messages. */
    fun secondaryColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the field's text: the chosen label, or the muted placeholder. */
    fun valueColor(chosen: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        chosen -> colors.textDefault
        else -> colors.textMuted
    }

    /** The radio mark of an option: the accent when chosen, the input border otherwise, disabled text when off. */
    fun markColor(selected: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        selected -> colors.accentStrong
        else -> colors.borderInput
    }

    /** The background of the sheet. */
    fun sheetColor(colors: MonarchColors): Color = colors.bgSurface

    /** The colour of the load error. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong

    /** The label of [option], marked with its language when it has one, so TalkBack reads it in that language. */
    fun label(option: MonarchComboboxOption): AnnotatedString {
        val locale = option.labelLocale ?: return AnnotatedString(option.label)
        return buildAnnotatedString {
            withStyle(SpanStyle(localeList = LocaleList(locale.toLanguageTag()))) { append(option.label) }
        }
    }
}

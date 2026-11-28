package com.rookhq.monarch.components.forms.phoneinput

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchPhoneInput. It has no colour decision: both parts use MonarchTextField's roles. */
internal object MonarchPhoneInputTokens {
    /** The space between the country picker and the number: the 16dp between two fields of a form. */
    val Gap: Dp = MonarchTokens.Space.space4.dp

    /** From this font scale the country and the number stack, as half the width cuts the country name. */
    const val StackedFontScale: Float = 1.5f

    /** The left-to-right mark that keeps a number in reading order on a right-to-left screen. */
    private const val LEFT_TO_RIGHT_MARK = "‎"

    /**
     * Shows the number left to right in every layout direction, as the web's
     * `dir="ltr"`: a left-to-right mark before the text, which is not part of the value.
     */
    val LeftToRight: VisualTransformation = VisualTransformation { text ->
        TransformedText(
            AnnotatedString(LEFT_TO_RIGHT_MARK) + text,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset + LEFT_TO_RIGHT_MARK.length

                override fun transformedToOriginal(offset: Int): Int =
                    (offset - LEFT_TO_RIGHT_MARK.length).coerceIn(0, text.length)
            },
        )
    }
}

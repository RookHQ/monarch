package dev.monarchui.components.intelligence.aitonepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import dev.monarchui.R

/**
 * One tone the assistant can write in, offered by `MonarchAiTonePicker`.
 *
 * @property id a stable id, the value of the selection.
 * @property label the visible name, already localised.
 * @property detail one line on how text in this tone reads. Empty shows the label only.
 */
@Immutable
public data class MonarchAiTone(
    public val id: String,
    public val label: String,
    public val detail: String,
) {
    /** The built-in tones and the id of the Custom option. */
    public companion object {
        /** The selection of the Custom option of `MonarchAiTonePicker`. */
        public const val CUSTOM_ID: String = "custom"

        /**
         * The built-in tones, Professional, Technical, Casual and Executive, with their descriptions in the
         * language of the app. Their ids are `professional`, `technical`, `casual` and `executive`.
         */
        @Composable
        public fun defaults(): List<MonarchAiTone> = listOf(
            MonarchAiTone(
                "professional",
                stringResource(R.string.monarch_aitonepicker_professional),
                stringResource(R.string.monarch_aitonepicker_professional_detail),
            ),
            MonarchAiTone(
                "technical",
                stringResource(R.string.monarch_aitonepicker_technical),
                stringResource(R.string.monarch_aitonepicker_technical_detail),
            ),
            MonarchAiTone(
                "casual",
                stringResource(R.string.monarch_aitonepicker_casual),
                stringResource(R.string.monarch_aitonepicker_casual_detail),
            ),
            MonarchAiTone(
                "executive",
                stringResource(R.string.monarch_aitonepicker_executive),
                stringResource(R.string.monarch_aitonepicker_executive_detail),
            ),
        )
    }
}

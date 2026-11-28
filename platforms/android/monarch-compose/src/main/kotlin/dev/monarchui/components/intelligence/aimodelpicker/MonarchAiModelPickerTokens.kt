package dev.monarchui.components.intelligence.aimodelpicker

import android.icu.text.CompactDecimalFormat
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.internal.MonarchNumberText
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Whether a model can be chosen: as is, by adding its key first, or not at all. */
internal enum class MonarchAiModelPickerOptionState { Ok, Key, Blocked }

/** Component tokens and the pure logic of MonarchAiModelPicker. */
internal object MonarchAiModelPickerTokens {
    /** The gap between the heading, the list and Done. */
    val SheetGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the mark and the text of a row, and between badges. */
    val RowGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the lines of a row. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The vertical padding of a row and above a group heading. */
    val RowPadding: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the name and the provider in the button. */
    val ValueGap: Dp = MonarchTokens.Space.space1_5.dp

    private const val LOWEST_TIER = 1
    private const val HIGHEST_TIER = 3

    /** The models grouped by provider, groups in the order their first model appears. */
    fun groups(models: List<MonarchAiModelOption>): List<Pair<String, List<MonarchAiModelOption>>> =
        models.groupBy { it.provider }.toList()

    /** Whether [model] can be chosen, given whether the app can add a missing key. */
    fun state(model: MonarchAiModelOption, canConfigure: Boolean): MonarchAiModelPickerOptionState = when {
        !model.enabled -> MonarchAiModelPickerOptionState.Blocked
        model.requiresKey && !model.keyConfigured ->
            if (canConfigure) MonarchAiModelPickerOptionState.Key else MonarchAiModelPickerOptionState.Blocked
        else -> MonarchAiModelPickerOptionState.Ok
    }

    /** The cost tier from 1 to 3, or `null` without one. */
    fun tier(model: MonarchAiModelOption): Int? = model.costTier?.coerceIn(LOWEST_TIER, HIGHEST_TIER)

    /** A token count in the compact form of [locale], with at most one decimal: 32 768 is "32.8K". */
    fun compact(tokens: Long, locale: Locale): String =
        CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT).apply {
            setSignificantDigitsUsed(false)
            maximumFractionDigits = 1
        }.format(tokens).let(MonarchNumberText::upperScale)

    /** The built-in badge text of a capability. */
    @StringRes
    fun capabilityText(capability: MonarchAiModelCapability): Int = when (capability) {
        MonarchAiModelCapability.Vision -> R.string.monarch_aimodelpicker_capability_vision
        MonarchAiModelCapability.Tools -> R.string.monarch_aimodelpicker_capability_tools
        MonarchAiModelCapability.Reasoning -> R.string.monarch_aimodelpicker_capability_reasoning
        MonarchAiModelCapability.Web -> R.string.monarch_aimodelpicker_capability_web
    }

    /** The built-in text of a speed. */
    @StringRes
    fun speedText(speed: MonarchAiModelSpeed): Int = when (speed) {
        MonarchAiModelSpeed.Fast -> R.string.monarch_aimodelpicker_speed_fast
        MonarchAiModelSpeed.Balanced -> R.string.monarch_aimodelpicker_speed_balanced
        MonarchAiModelSpeed.Slow -> R.string.monarch_aimodelpicker_speed_slow
    }

    /** The built-in spoken text of a cost tier from 1 to 3. */
    @StringRes
    fun costText(tier: Int): Int = when (tier) {
        LOWEST_TIER -> R.string.monarch_aimodelpicker_cost_low
        HIGHEST_TIER -> R.string.monarch_aimodelpicker_cost_high
        else -> R.string.monarch_aimodelpicker_cost_medium
    }

    /** The colour of a row's name: default, or the disabled text role when it cannot be chosen. */
    fun nameColor(state: MonarchAiModelPickerOptionState, colors: MonarchColors): Color =
        if (state == MonarchAiModelPickerOptionState.Blocked) colors.textDisabled else colors.textDefault

    /** The colour of a row's details and of the provider: muted, or disabled when it cannot be chosen. */
    fun detailColor(state: MonarchAiModelPickerOptionState, colors: MonarchColors): Color =
        if (state == MonarchAiModelPickerOptionState.Blocked) colors.textDisabled else colors.textMuted

    /** The colour of the button's placeholder and provider. */
    fun mutedColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled
}

package com.rookhq.monarch.components.technical.lifecyclebadge

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The colours of a lifecycle badge: the capsule and its dot. */
@Immutable
internal data class LifecycleBadgeColors(val surface: MonarchSurfaceColors, val dot: Color)

/** Component tokens of MonarchLifecycleBadge, built from primitives and roles only. */
internal object MonarchLifecycleBadgeTokens {
    val MinHeight: Dp = MonarchTokens.Space.space6.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space2.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space0_5.dp
    val ContentGap: Dp = MonarchTokens.Space.space1_5.dp

    /** 6dp at the default font size; scaled with it. */
    val DotSize: Dp = MonarchTokens.Space.space1_5.dp
    const val FONT_WEIGHT: Int = MonarchTokens.FontWeight.medium

    private val separators = Regex("[_-]+")

    /** The tone of a built-in stage. */
    fun toneOf(lifecycle: MonarchLifecycle): MonarchTone = when (lifecycle) {
        MonarchLifecycle.Planned -> MonarchTone.Neutral
        MonarchLifecycle.Preview -> MonarchTone.Info
        MonarchLifecycle.Stable -> MonarchTone.Success
        MonarchLifecycle.Deprecated -> MonarchTone.Warning
    }

    /** The built-in label of a stage. */
    @StringRes
    fun labelRes(lifecycle: MonarchLifecycle): Int = when (lifecycle) {
        MonarchLifecycle.Planned -> R.string.monarch_lifecyclebadge_planned
        MonarchLifecycle.Preview -> R.string.monarch_lifecyclebadge_preview
        MonarchLifecycle.Stable -> R.string.monarch_lifecyclebadge_stable
        MonarchLifecycle.Deprecated -> R.string.monarch_lifecyclebadge_deprecated
    }

    /** The built-in stage with exactly this key, or `null`. */
    fun lifecycleOf(key: String): MonarchLifecycle? = MonarchLifecycle.entries.firstOrNull { it.key == key }

    /** An unknown key as readable text: "early_access" gives "Early access". */
    fun readableStage(key: String): String =
        key.replace(separators, " ").trim().replaceFirstChar { it.titlecase() }

    /**
     * The colours of a lifecycle badge: the surface with default text, and
     * the tone in the border and the dot only. Neutral uses the default
     * border and the subtle text colour for the dot.
     */
    fun colors(tone: MonarchTone, colors: MonarchColors): LifecycleBadgeColors {
        val palette = MonarchTonePalette.of(tone, colors)
        val neutral = tone == MonarchTone.Neutral
        return LifecycleBadgeColors(
            surface = MonarchSurfaceColors(
                container = colors.bgSurface,
                content = colors.textDefault,
                border = palette.border,
            ),
            dot = if (neutral) colors.textSubtle else palette.strong,
        )
    }
}

package dev.monarchui.components.intelligence.aiclaimsources

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchUtcDates
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and the pure logic of MonarchAiClaimSources. */
internal object MonarchAiClaimSourcesTokens {
    /** The rotation of the expand icon while a claim is closed, so a down chevron points to the start of the text. */
    const val COLLAPSED_ROTATION: Float = -90f

    /** The width of the status stripe at the start of a claim card. */
    val StripeWidth: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the header, the claims and the empty text, and between claims. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the parts of a claim and of a passage. */
    val ItemGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the lines of a header and between badges. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The padding inside a claim's header. */
    val HeaderPadding: Dp = MonarchTokens.Space.space2.dp

    /** The indent of a claim's panel. */
    val PanelIndent: Dp = MonarchTokens.Space.space3.dp

    /** The padding inside a quotation. */
    val QuotePadding: Dp = MonarchTokens.Space.space3.dp

    /** The width of the bar before a quotation and before the selected claim. */
    val BarWidth: Dp = MonarchTokens.Space.space1.dp

    /** The claims a filter shows, in order. */
    fun shown(claims: List<MonarchAiClaim>, filter: MonarchAiClaimSourcesFilter): List<MonarchAiClaim> =
        when (filter) {
            MonarchAiClaimSourcesFilter.All -> claims
            MonarchAiClaimSourcesFilter.NeedsChecking -> claims.filter { it.needsChecking }
        }

    /** [ids] with [id] opened or closed. */
    fun toggled(ids: Set<String>, id: String): Set<String> = if (id in ids) ids - id else ids + id

    /** The tone of a support badge: success, warning, danger or neutral. The level is always also words. */
    fun tone(support: MonarchAiClaimSupport): MonarchTone = when (support) {
        MonarchAiClaimSupport.Supported -> MonarchTone.Success
        MonarchAiClaimSupport.Partial, MonarchAiClaimSupport.Conflicting -> MonarchTone.Warning
        MonarchAiClaimSupport.Unsupported -> MonarchTone.Danger
        MonarchAiClaimSupport.Unchecked -> MonarchTone.Neutral
    }

    /** The colour of a passage's stance text: success or danger strong, muted when not stated. */
    fun stanceColor(stance: MonarchAiClaimEvidenceStance?, colors: MonarchColors): Color = when (stance) {
        MonarchAiClaimEvidenceStance.Supports -> colors.successStrong
        MonarchAiClaimEvidenceStance.Contradicts -> colors.dangerStrong
        null -> colors.textMuted
    }

    /** The bar before a quotation: the stance's border colour, or the strong border when not stated. */
    fun quoteBar(stance: MonarchAiClaimEvidenceStance?, colors: MonarchColors): Color = when (stance) {
        MonarchAiClaimEvidenceStance.Supports -> colors.successBorder
        MonarchAiClaimEvidenceStance.Contradicts -> colors.dangerBorder
        null -> colors.borderStrong
    }

    /** The fill of a quotation. */
    fun quoteBackground(colors: MonarchColors): Color = colors.bgSubtle

    /** The fill of a claim's header: the accent subtle colour when selected, none otherwise. */
    fun headerBackground(selected: Boolean, colors: MonarchColors): Color =
        if (selected) colors.accentSubtle else Color.Transparent

    /** The bar before the selected claim. */
    fun selectedBar(colors: MonarchColors): Color = colors.accentStrong

    /** Muted text: the summary, the counts, the details of a passage, the notes. */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** A publication date in the medium date format of [locale], read in UTC so a date-only value keeps its day. */
    fun publishedDate(millis: Long, locale: Locale): String = MonarchUtcDates.dateText(millis, locale)
}

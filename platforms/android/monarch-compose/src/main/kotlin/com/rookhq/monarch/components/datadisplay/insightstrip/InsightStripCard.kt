package com.rookhq.monarch.components.datadisplay.insightstrip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchTouchTargetOverhang
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeVariant
import com.rookhq.monarch.components.datadisplay.card.MonarchCardTokens
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsible
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchRemoveButton
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * One insight as a `MonarchCard`: the tone and origin badges, the title and
 * the reason as one TalkBack element, then the evidence, the action and the
 * dismiss button as separate elements.
 */
@Composable
internal fun InsightStripCard(
    insight: MonarchInsight,
    labels: MonarchInsightStripLabels,
    onDismiss: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MonarchTheme.colors
    val tokens = MonarchInsightStripTokens
    val palette = MonarchTonePalette.of(insight.tone, colors)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .monarchSurface(
                if (insight.tone == MonarchTone.Neutral) {
                    MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderDefault)
                } else {
                    MonarchSurfaceColors(palette.subtle, colors.textDefault, palette.border)
                },
                MonarchTheme.shapes.lg,
            )
            .padding(MonarchCardTokens.ContentPadding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.CardGap)) {
            Column(modifier = Modifier.semantics(mergeDescendants = true) { }) {
                // As the web: the tone glyph, the title and the dismiss mark on one line, then the origin badge.
                val toneWord = toneLabel(insight.tone, labels)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.BadgeGap),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        Modifier.padding(top = tokens.ReasonGap).semantics { text = AnnotatedString(toneWord) },
                    ) {
                        CompositionLocalProvider(LocalContentColor provides palette.strong) {
                            MonarchDefaultIcons.callout(insight.tone)()
                        }
                    }
                    Text(
                        text = insight.title,
                        modifier = Modifier.weight(1f).padding(top = tokens.ReasonGap),
                        style = tokens.titleStyle(MonarchTheme.typography),
                        color = colors.textDefault,
                    )
                    if (onDismiss != null) {
                        val dismiss = monarchString(labels.dismiss, R.string.monarch_insightstrip_dismiss)
                        MonarchRemoveButton(
                            name = dismiss,
                            text = dismiss,
                            onClick = { onDismiss(insight.id) },
                            enabled = true,
                            icon = MonarchDefaultIcons.Close,
                        )
                    }
                }
                Spacer(Modifier.height(tokens.ReasonGap))
                when (insight.origin) {
                    MonarchInsightOrigin.Ai -> MonarchBadge(
                        text = originLabel(insight.origin, labels),
                        tone = MonarchTone.Ai,
                        leadingIcon = MonarchDefaultIcons.Bot,
                    )
                    MonarchInsightOrigin.Rule -> MonarchBadge(
                        text = originLabel(insight.origin, labels),
                        variant = MonarchBadgeVariant.Outline,
                        leadingIcon = MonarchDefaultIcons.RuleBased,
                    )
                }
                Spacer(Modifier.height(tokens.ReasonGap))
                Text(
                    text = insight.why,
                    style = tokens.reasonStyle(MonarchTheme.typography),
                    color = colors.textMuted,
                )
            }
            if (insight.evidence != null) {
                var open by rememberSaveable(insight.id) { mutableStateOf(false) }
                MonarchCollapsible(
                    expanded = open,
                    onExpandedChange = { open = it },
                    label = monarchString(labels.evidence, R.string.monarch_insightstrip_evidence),
                ) {
                    Text(text = insight.evidence, style = MonarchTheme.typography.caption, color = colors.textMuted)
                }
            }
            val action = insight.onAction
            if (insight.actionLabel != null && action != null) {
                // The 48dp target of the small button spills into the card padding, so the card is as deep
                // under it as above the badges, and the gap above it is the web's 12dp.
                Box(
                    modifier = Modifier
                        .padding(top = tokens.ActionsGap - tokens.CardGap)
                        .monarchTouchTargetOverhang(),
                ) {
                    MonarchButton(text = insight.actionLabel, onClick = action, size = MonarchButtonSize.Sm)
                }
            }
        }
    }
}

@Composable
private fun toneLabel(tone: MonarchTone, labels: MonarchInsightStripLabels): String =
    labels.toneLabel?.invoke(tone) ?: monarchString(
        null,
        when (tone) {
            MonarchTone.Neutral -> R.string.monarch_insightstrip_tone_neutral
            MonarchTone.Info -> R.string.monarch_insightstrip_tone_info
            MonarchTone.Success -> R.string.monarch_insightstrip_tone_success
            MonarchTone.Warning -> R.string.monarch_insightstrip_tone_warning
            MonarchTone.Danger -> R.string.monarch_insightstrip_tone_danger
            MonarchTone.Accent -> R.string.monarch_insightstrip_tone_neutral
            MonarchTone.Ai -> R.string.monarch_internal_tone_ai
        },
    )

@Composable
private fun originLabel(origin: MonarchInsightOrigin, labels: MonarchInsightStripLabels): String = when (origin) {
    MonarchInsightOrigin.Rule -> monarchString(labels.ruleOrigin, R.string.monarch_insightstrip_origin_rule)
    MonarchInsightOrigin.Ai -> monarchString(labels.aiOrigin, R.string.monarch_insightstrip_origin_ai)
}

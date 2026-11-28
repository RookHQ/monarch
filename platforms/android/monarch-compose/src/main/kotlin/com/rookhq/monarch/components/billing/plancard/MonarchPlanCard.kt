package com.rookhq.monarch.components.billing.plancard

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeVariant
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * One plan in a plan picker: its name, price, who it is for, what it
 * includes and a call to action, on a `MonarchCard`.
 *
 * Built from Monarch components: `MonarchCard`, `MonarchBadge` and
 * `MonarchText`. The card shows prices exactly as given and never formats
 * them, and it only presents the plan: an app that sells digital goods must
 * start the purchase with Google Play Billing from the [action]. The name is
 * a heading, the price and period are read as one element, and each feature
 * is one element.
 *
 * @param name the plan name, already localised. A heading.
 * @param modifier the modifier of the card.
 * @param price the price as the product writes it, such as "$12" or "Contact us".
 * @param period text after the price, such as "per seat / month".
 * @param priceNote a line under the price, such as "Billed yearly".
 * @param description one or two sentences on who the plan is for.
 * @param featuresLabel a heading above the features, such as "Everything in Starter, plus".
 * @param features what the plan includes, and with `included = false` what it does not.
 * @param badge a short label beside the name, such as "Recommended".
 * @param footer small print under the features.
 * @param highlighted emphasises the card as the recommended plan: a 1dp accent border, a shadow and an accent badge.
 * @param current adds the "Current plan" badge.
 * @param includedIcon a mark before every included feature, for example a check `MonarchIcon` with a `null`
 * content description. Monarch bundles no icons; without it included features have no mark, and features
 * that are not included always show a dash.
 * @param currentPlanLabel replaces the built-in badge text "Current plan" (`monarch_plancard_current`).
 * @param notIncludedLabel replaces the built-in spoken prefix "Not included:" (`monarch_plancard_not_included`).
 * @param action the call to action, normally one `MonarchButton`. It stays its own element.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchPlanCard(
    name: String,
    modifier: Modifier = Modifier,
    price: String? = null,
    period: String? = null,
    priceNote: String? = null,
    description: String? = null,
    featuresLabel: String? = null,
    features: List<MonarchPlanCardFeature> = emptyList(),
    badge: String? = null,
    footer: String? = null,
    highlighted: Boolean = false,
    current: Boolean = false,
    includedIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Check,
    currentPlanLabel: String? = null,
    notIncludedLabel: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.lg
    val frame = if (highlighted) {
        modifier.border(
            width = MonarchPlanCardTokens.HighlightBorderWidth,
            color = MonarchPlanCardTokens.highlightBorder(colors),
            shape = shape,
        )
    } else {
        modifier
    }
    MonarchCard(modifier = frame.fillMaxWidth(), elevated = highlighted) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchPlanCardTokens.SectionGap)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MonarchPlanCardTokens.BadgeGap, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(MonarchPlanCardTokens.LineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchText(
                    text = name,
                    style = MonarchTextStyle.Subheading,
                    modifier = Modifier.weight(1f, fill = false),
                )
                badge?.let {
                    MonarchBadge(
                        text = it,
                        tone = MonarchPlanCardTokens.badgeTone(highlighted),
                        variant = MonarchPlanCardTokens.BadgeVariant,
                    )
                }
                if (current) {
                    MonarchBadge(
                        text = monarchString(currentPlanLabel, R.string.monarch_plancard_current),
                        variant = MonarchBadgeVariant.Outline,
                    )
                }
            }
            PriceBlock(price, period, priceNote, description)
            action?.let { Box(modifier = Modifier.fillMaxWidth()) { it() } }
            if (features.isNotEmpty()) {
                MonarchDivider(variant = MonarchDividerVariant.Subtle)
                FeatureList(
                    features = features,
                    label = featuresLabel,
                    includedIcon = includedIcon,
                    notIncluded = monarchString(notIncludedLabel, R.string.monarch_plancard_not_included),
                )
            }
            footer?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = colors.textMuted) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PriceBlock(price: String?, period: String?, priceNote: String?, description: String?) {
    val muted = MonarchPlanCardTokens.mutedColor(MonarchTheme.colors)
    if (price != null || period != null || priceNote != null) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchPlanCardTokens.LineGap)) {
            if (price != null || period != null) {
                FlowRow(
                    modifier = Modifier.semantics(mergeDescendants = true) { },
                    horizontalArrangement = Arrangement.spacedBy(MonarchPlanCardTokens.LineGap),
                    itemVerticalAlignment = Alignment.Bottom,
                ) {
                    price?.let { MonarchText(text = it, style = MonarchTextStyle.Title, heading = false) }
                    period?.let { MonarchText(text = it, color = muted) }
                }
            }
            priceNote?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted) }
        }
    }
    description?.let { MonarchText(text = it, color = muted) }
}

@Composable
private fun FeatureList(
    features: List<MonarchPlanCardFeature>,
    label: String?,
    includedIcon: (@Composable () -> Unit)?,
    notIncluded: String,
) {
    val colors = MonarchTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(MonarchPlanCardTokens.FeatureGap)) {
        label?.let { MonarchText(text = it, style = MonarchTextStyle.Label, heading = true) }
        features.forEach { feature ->
            val color = MonarchPlanCardTokens.featureColor(feature.included, colors)
            Row(
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.spacedBy(MonarchPlanCardTokens.MarkGap),
            ) {
                Box(
                    modifier = Modifier.widthIn(min = MonarchPlanCardTokens.MarkWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    if (feature.included) {
                        Box(Modifier.clearAndSetSemantics { }) {
                            CompositionLocalProvider(LocalContentColor provides colors.accentStrong) {
                                includedIcon?.invoke()
                            }
                        }
                    } else {
                        // The dash is read as the "Not included:" prefix, so the difference is not colour alone.
                        Text(
                            text = Dash,
                            modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(notIncluded) },
                            style = MonarchTheme.typography.body,
                            color = color,
                        )
                    }
                }
                // The hint follows the label on the same line in the muted colour, as on the web.
                MonarchText(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = color)) { append(feature.label) }
                        feature.hint?.let {
                            withStyle(SpanStyle(color = colors.textMuted)) { append(" $it") }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** An en dash before a feature that is not included. */
private const val Dash = "–"

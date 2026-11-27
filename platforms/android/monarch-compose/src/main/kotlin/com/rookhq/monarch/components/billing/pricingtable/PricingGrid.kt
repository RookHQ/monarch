package com.rookhq.monarch.components.billing.pricingtable

import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The wide layout: a header per plan, then group headings and one row per
 * feature with a cell per plan. Each cell is read as the plan, the feature and
 * the value: "Team, Single sign-on: Included".
 */
@Composable
internal fun PricingGrid(content: PricingContent) {
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Container
    // One bordered table, as on the web: the header, the group bands and the feature rows share one frame, and the
    // highlighted plan is a tinted column from the top to the bottom.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(MonarchShapeDefaults.BorderWidth, colors.borderDefault, shape),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Spacer(Modifier.weight(MonarchPricingTableTokens.FEATURE_COLUMN_WEIGHT))
            content.plans.forEach { plan ->
                PlanHeader(plan, content, Modifier.weight(1f).planColumn(plan, colors))
            }
        }
        MonarchPricingTableTokens.groups(content.features).forEach { (group, features) ->
            if (group != null) {
                MonarchDivider(variant = MonarchDividerVariant.Subtle)
                MonarchText(
                    text = group,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgSubtle)
                        .padding(
                            horizontal = MonarchPricingTableTokens.HeaderPadding,
                            vertical = MonarchPricingTableTokens.RowGap,
                        ),
                    style = MonarchTextStyle.Caption,
                    color = colors.textMuted,
                    heading = true,
                )
            }
            features.forEach { feature ->
                MonarchDivider(variant = MonarchDividerVariant.Subtle)
                FeatureRow(feature, content)
            }
        }
    }
}

/** The tint of the highlighted plan's column. */
@Composable
private fun Modifier.planColumn(plan: MonarchPricingPlan, colors: com.rookhq.monarch.theme.MonarchColors): Modifier =
    if (plan.highlighted) background(colors.accentSubtle) else this

@Composable
private fun FeatureRow(feature: MonarchPricingFeature, content: PricingContent) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(MonarchPricingTableTokens.FEATURE_COLUMN_WEIGHT)
                .padding(MonarchPricingTableTokens.HeaderPadding)
                .semantics(mergeDescendants = true) { },
        ) {
            MonarchText(text = feature.label)
            if (feature.description != null) {
                MonarchText(
                    text = feature.description,
                    style = MonarchTextStyle.Caption,
                    color = colors.textMuted,
                )
            }
        }
        content.plans.forEach { plan ->
            val value = MonarchPricingTableTokens.value(feature, plan)
            val spoken = stringResource(
                R.string.monarch_pricingtable_cell,
                plan.name,
                feature.label,
                MonarchPricingTableTokens.spoken(value, content.strings.included, content.strings.notIncluded),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .planColumn(plan, colors)
                    .padding(MonarchPricingTableTokens.HeaderPadding)
                    .clearAndSetSemantics { contentDescription = spoken },
                contentAlignment = Alignment.CenterStart,
            ) { PricingValue(value, content) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanHeader(plan: MonarchPricingPlan, content: PricingContent, modifier: Modifier) {
    val colors = MonarchTheme.colors
    val price = if (content.showPrices) MonarchPricingTableTokens.price(plan, content.period) else null
    Column(
        modifier = modifier.padding(MonarchPricingTableTokens.HeaderPadding),
        verticalArrangement = Arrangement.spacedBy(MonarchPricingTableTokens.LineGap),
    ) {
        if (plan.badge != null || plan.current) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchPricingTableTokens.LineGap),
                verticalArrangement = Arrangement.spacedBy(MonarchPricingTableTokens.LineGap),
            ) {
                if (plan.badge != null) MonarchBadge(text = plan.badge, tone = MonarchTone.Accent)
                if (plan.current) {
                    MonarchBadge(text = content.strings.currentPlan, variant = MonarchBadgeVariant.Outline)
                }
            }
        }
        MonarchText(text = plan.name, style = MonarchTextStyle.Subheading)
        if (price?.price != null) {
            Column(modifier = Modifier.semantics(mergeDescendants = true) { }) {
                MonarchText(text = price.price, style = MonarchTextStyle.Heading, heading = false)
                if (price.suffix != null) {
                    MonarchText(text = price.suffix, style = MonarchTextStyle.Caption, color = colors.textMuted)
                }
                if (price.note != null) {
                    MonarchText(text = price.note, style = MonarchTextStyle.Caption, color = colors.textMuted)
                }
            }
        }
        if (plan.description != null) {
            MonarchText(text = plan.description, style = MonarchTextStyle.Caption, color = colors.textMuted)
        }
        content.action?.invoke(plan)
    }
}

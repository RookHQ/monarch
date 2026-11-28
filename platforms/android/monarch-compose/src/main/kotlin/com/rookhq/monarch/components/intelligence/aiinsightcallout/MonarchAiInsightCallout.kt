package com.rookhq.monarch.components.intelligence.aiinsightcallout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsible
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchRemoveButton
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A proactive insight about the current record or screen: what was
 * noticed, why, the evidence and one action, with its origin shown honestly
 * as "AI insight" or "Rule-based".
 *
 * Built by Monarch: a tinted container with a tone bar on the start edge,
 * the title and the origin label in a `FlowRow`, the reason, the evidence in
 * a `MonarchCollapsible`, one small `MonarchButton` and an optional dismiss
 * button. The tone sets the fill, the border, the bar and the icon colour:
 * `Info` for something to know, `Warning` for a risk, `Danger` for
 * something that needs action now, `Success` for good news. Only the AI
 * origin uses the assistant colours. TalkBack reads the tone name before the
 * title ("Warning: Renewal at risk"), so the tone is never colour alone.
 * The callout does not hide itself: remove it in [onDismiss].
 *
 * @param title the insight in one line, already localised.
 * @param why why it was raised, in plain words.
 * @param origin whether a model or a fixed rule raised it. Always shown.
 * @param modifier the modifier of the callout.
 * @param tone how much it matters.
 * @param heading whether the title is a heading for TalkBack navigation.
 * @param actionLabel the label of the one follow-up action. Shown only with [onAction].
 * @param onAction the follow-up action. Shown only with [actionLabel].
 * @param onDismiss adds a dismiss button at the end. The app removes the callout and decides where focus goes.
 * @param icon the tone icon at the start, for example a warning `MonarchIcon` with a `null` content
 * description. It takes the tone's strong colour. Decorative: the tone name is read with the title.
 * @param originIcon a mark before the origin label, for example a bot icon for the AI origin. Decorative.
 * @param dismissIcon the close mark of the dismiss button. Without it the button shows its label as text.
 * @param labels replacements for the built-in strings.
 * @param evidence supporting data (figures, records), shown in a collapsed disclosure. `null` for none.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchAiInsightCallout(
    title: String,
    why: String,
    origin: MonarchAiInsightOrigin,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Info,
    heading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.tone(tone),
    originIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Bot,
    dismissIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchAiInsightCalloutLabels = MonarchAiInsightCalloutLabels(),
    evidence: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = MonarchTheme.colors
    val surface = MonarchAiInsightCalloutTokens.surface(tone, colors)
    val shape = MonarchShapeDefaults.Container
    val toneName = labels.tone ?: MonarchAiInsightCalloutTokens.toneResource(tone)?.let { stringResource(it) }
    val spokenTitle = if (MonarchAiInsightCalloutTokens.namesTone(toneName)) {
        stringResource(R.string.monarch_aiinsightcallout_titled, toneName.orEmpty(), title)
    } else {
        title
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface.fill)
            .drawBehind {
                val width = MonarchAiInsightCalloutTokens.BarWidth.toPx()
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                drawRect(surface.accent, topLeft = Offset(x, 0f), size = Size(width, size.height))
            }
            .border(MonarchShapeDefaults.BorderWidth, surface.border, shape)
            .semantics { isTraversalGroup = true }
            .padding(
                PaddingValues(
                    start = MonarchAiInsightCalloutTokens.StartPadding,
                    top = MonarchAiInsightCalloutTokens.Padding,
                    end = MonarchAiInsightCalloutTokens.Padding,
                    bottom = MonarchAiInsightCalloutTokens.Padding,
                ),
            ),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiInsightCalloutTokens.Gap),
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides surface.accent) {
                Box(Modifier.clearAndSetSemantics { }) { icon() }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MonarchAiInsightCalloutTokens.BodyGap),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchAiInsightCalloutTokens.HeaderGap),
                verticalArrangement = Arrangement.spacedBy(MonarchAiInsightCalloutTokens.OriginGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = spokenTitle
                        if (heading) heading()
                    },
                    style = MonarchTheme.typography.bodyStrong,
                    color = colors.textDefault,
                )
                OriginLabel(origin, labels, originIcon)
            }
            Text(
                text = why,
                style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.Normal),
                color = colors.textDefault,
            )
            if (evidence != null) {
                var expanded by rememberSaveable { mutableStateOf(false) }
                MonarchCollapsible(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    label = monarchString(labels.evidence, R.string.monarch_aiinsightcallout_evidence),
                    content = evidence,
                )
            }
            if (actionLabel != null && onAction != null) {
                MonarchButton(text = actionLabel, onClick = onAction, size = MonarchButtonSize.Sm)
            }
        }
        if (onDismiss != null) {
            val dismissLabel = monarchString(labels.dismiss, R.string.monarch_aiinsightcallout_dismiss)
            MonarchRemoveButton(
                name = dismissLabel,
                text = dismissLabel,
                onClick = onDismiss,
                enabled = true,
                icon = dismissIcon,
            )
        }
    }
}

@Composable
private fun OriginLabel(
    origin: MonarchAiInsightOrigin,
    labels: MonarchAiInsightCalloutLabels,
    icon: (@Composable () -> Unit)?,
) {
    val colors = MonarchAiInsightCalloutTokens.origin(origin, MonarchTheme.colors)
    val text = when (origin) {
        MonarchAiInsightOrigin.Ai -> monarchString(labels.ai, R.string.monarch_aiinsightcallout_ai)
        MonarchAiInsightOrigin.Rule -> monarchString(labels.rule, R.string.monarch_aiinsightcallout_rule)
    }
    Row(
        modifier = Modifier
            .monarchSurface(colors, MonarchShapeDefaults.Mark)
            .padding(
                horizontal = MonarchAiInsightCalloutTokens.OriginHorizontalPadding,
                vertical = MonarchAiInsightCalloutTokens.OriginVerticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(MonarchAiInsightCalloutTokens.OriginGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides colors.content) {
                Box(Modifier.clearAndSetSemantics { }) { icon() }
            }
        }
        Text(text = text, style = MonarchTheme.typography.caption, color = colors.content)
    }
}

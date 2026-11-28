package dev.monarchui.components.forms.passwordstrengthmeter

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Shows how strong a password is while it is typed: a four-part bar, the
 * strength in words ("Password strength: Good") and each requirement as met
 * or not met.
 *
 * Built by Monarch from Compose foundation layouts, because Material 3 has no
 * strength meter. Nothing depends on colour: the strength is a line of text,
 * which TalkBack announces when it changes, and each rule has a filled or an
 * empty mark and is read as "Met: …" or "Not met: …". The bar is decoration
 * and hidden from TalkBack. Place the meter under a `MonarchPasswordInput`.
 *
 * @param password the password to rate. It is only read.
 * @param modifier the modifier of the meter.
 * @param rules the requirements listed under the bar and counted by the default score. An empty list hides the
 * list.
 * @param showRules whether to list the requirements.
 * @param score a custom strength estimator. By default [MonarchPasswordStrength.score] with [rules]. It is not
 * called for an empty password.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchPasswordStrengthMeter(
    password: String,
    modifier: Modifier = Modifier,
    rules: List<MonarchPasswordRule> = MonarchPasswordRule.defaults(),
    showRules: Boolean = true,
    score: ((String) -> MonarchPasswordStrength)? = null,
    labels: MonarchPasswordStrengthMeterLabels = MonarchPasswordStrengthMeterLabels(),
) {
    val strength = when {
        password.isEmpty() -> null
        score != null -> score(password)
        else -> MonarchPasswordStrength.score(password, rules)
    }
    val colors = MonarchTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MonarchPasswordStrengthMeterTokens.Gap),
    ) {
        StrengthBar(strength)
        Text(
            text = labels.strengthText?.invoke(strength) ?: defaultStrengthText(strength),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.caption,
            color = colors.textMuted,
        )
        if (showRules && rules.isNotEmpty()) {
            RuleList(password = password, rules = rules, labels = labels)
        }
    }
}

@Composable
private fun StrengthBar(strength: MonarchPasswordStrength?) {
    val colors = MonarchTheme.colors
    val motion = MonarchTheme.motion
    Row(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(MonarchPasswordStrengthMeterTokens.SegmentGap),
    ) {
        repeat(MonarchPasswordStrengthMeterTokens.SEGMENTS) { index ->
            val fill by animateColorAsState(
                targetValue = MonarchPasswordStrengthMeterTokens.segment(index, strength, colors),
                animationSpec = motion.spec(),
                label = "segment",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(MonarchPasswordStrengthMeterTokens.SegmentHeight)
                    .background(fill, MonarchTheme.shapes.full),
            )
        }
    }
}

@Composable
private fun RuleList(password: String, rules: List<MonarchPasswordRule>, labels: MonarchPasswordStrengthMeterLabels) {
    val listName = monarchString(labels.rulesLabel, R.string.monarch_passwordstrengthmeter_rules)
    Column(
        modifier = Modifier.semantics {
            contentDescription = listName
            collectionInfo = CollectionInfo(rowCount = rules.size, columnCount = 1)
        },
        verticalArrangement = Arrangement.spacedBy(MonarchPasswordStrengthMeterTokens.RuleGap),
    ) {
        rules.forEachIndexed { index, rule ->
            val met = password.isNotEmpty() && rule.test(password)
            RuleRow(index = index, label = rule.label, met = met, labels = labels)
        }
    }
}

@Composable
private fun RuleRow(index: Int, label: String, met: Boolean, labels: MonarchPasswordStrengthMeterLabels) {
    val color = MonarchPasswordStrengthMeterTokens.rule(met, MonarchTheme.colors)
    val spoken = labels.ruleText?.invoke(label, met) ?: if (met) {
        stringResource(R.string.monarch_passwordstrengthmeter_met, label)
    } else {
        stringResource(R.string.monarch_passwordstrengthmeter_unmet, label)
    }
    Row(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = spoken
            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
        },
        horizontalArrangement = Arrangement.spacedBy(MonarchPasswordStrengthMeterTokens.MarkGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Met is a check and not met a dot, as the web, so the state is a shape as well as a colour.
        Box(
            modifier = Modifier.size(MonarchPasswordStrengthMeterTokens.MarkSize),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides color) {
                (if (met) MonarchDefaultIcons.Check else MonarchDefaultIcons.Dot)()
            }
        }
        Text(text = label, style = MonarchTheme.typography.caption, color = color)
    }
}

@Composable
private fun defaultStrengthText(strength: MonarchPasswordStrength?): String {
    val level = stringResource(
        when (strength) {
            null -> R.string.monarch_passwordstrengthmeter_empty
            MonarchPasswordStrength.VeryWeak -> R.string.monarch_passwordstrengthmeter_very_weak
            MonarchPasswordStrength.Weak -> R.string.monarch_passwordstrengthmeter_weak
            MonarchPasswordStrength.Fair -> R.string.monarch_passwordstrengthmeter_fair
            MonarchPasswordStrength.Good -> R.string.monarch_passwordstrengthmeter_good
            MonarchPasswordStrength.Strong -> R.string.monarch_passwordstrengthmeter_strong
        },
    )
    return stringResource(R.string.monarch_passwordstrengthmeter_strength, level)
}

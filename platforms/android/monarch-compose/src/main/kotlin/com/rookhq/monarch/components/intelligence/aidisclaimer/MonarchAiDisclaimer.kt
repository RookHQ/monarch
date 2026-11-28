package com.rookhq.monarch.components.intelligence.aidisclaimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A short reminder that AI output can be wrong, worded and styled for how
 * much a mistake would matter: "AI can make mistakes. Check important info."
 *
 * Built by Monarch: Material 3 has no disclaimer. The low risk is a centred
 * caption in the muted text colour, the medium risk a line at the start in
 * the default text colour, and the high risk a warning `MonarchInlineNotice`,
 * which TalkBack reads with the tone name "Warning" first. The icon and the
 * text are one TalkBack element read as the text.
 *
 * @param modifier the modifier of the disclaimer.
 * @param risk how much harm a wrong answer could do here. Selects the wording and the look.
 * @param text replaces the built-in wording of the risk, already localised. `null` or blank text keeps the
 * built-in strings `monarch_aidisclaimer_low`, `_medium` and `_high`.
 * @param icon an icon before the text, for example a `MonarchIcon` of size `Sm` with a `null` content
 * description. It is tinted by the risk. Drawn by default (the assistant, info or warning by risk); `null`
 * draws none. The text carries the meaning.
 */
@Composable
public fun MonarchAiDisclaimer(
    modifier: Modifier = Modifier,
    risk: MonarchAiDisclaimerRisk = MonarchAiDisclaimerRisk.Low,
    text: String? = null,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.disclaimer(risk),
) {
    val message = MonarchAiDisclaimerTokens.textOrNull(text)
        ?: monarchString(null, MonarchAiDisclaimerTokens.message(risk))
    if (risk == MonarchAiDisclaimerRisk.High) {
        MonarchInlineNotice(message = message, modifier = modifier, tone = MonarchTone.Warning, icon = icon)
        return
    }
    val colors = MonarchTheme.colors
    val low = risk == MonarchAiDisclaimerRisk.Low
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(
            MonarchAiDisclaimerTokens.IconGap,
            if (low) Alignment.CenterHorizontally else Alignment.Start,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides MonarchAiDisclaimerTokens.iconColor(risk, colors)) {
                Box(Modifier.clearAndSetSemantics { }) { icon() }
            }
        }
        Text(
            text = message,
            modifier = Modifier.weight(1f, fill = !low),
            style = MonarchTheme.typography.caption,
            color = MonarchAiDisclaimerTokens.textColor(risk, colors),
            textAlign = if (low) TextAlign.Center else TextAlign.Start,
        )
    }
}

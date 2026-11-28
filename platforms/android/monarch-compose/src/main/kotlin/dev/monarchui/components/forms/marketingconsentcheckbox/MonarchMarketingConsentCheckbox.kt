package dev.monarchui.components.forms.marketingconsentcheckbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.theme.MonarchTheme

/**
 * An opt-in to marketing messages: one checkbox, or one per channel, with the
 * notes that belong to it. Every change reports the exact wording shown, so
 * the app can keep proof of consent.
 *
 * Built from `MonarchCheckboxRow` (the Material 3 `Checkbox` as one row). With
 * one channel the question is the checkbox label and the notes are its
 * description; with several the question is a heading above one checkbox per
 * channel.
 *
 * Consent must be an active choice: start [selection] empty, and pass a
 * stored choice only on a settings screen where the person changes a choice
 * made earlier.
 *
 * @param selection the channels the person agreed to.
 * @param onConsentChange called on every change with each channel's state, the wording shown and the time. Store
 * [MonarchMarketingConsent.granted] as the new selection.
 * @param modifier the modifier of the group.
 * @param channels the channels to ask about, in order. One checkbox each when there are several.
 * @param enabled whether the person can change the choice and open the policy.
 * @param label the question. Built in: "Send me product news by email" for one channel (by the channel's own
 * word for another channel), "Send me product news" for several.
 * @param description what is sent and how often, for example "Product updates and offers, about twice a month."
 * @param showUnsubscribeNote shows the note that consent can be withdrawn.
 * @param policyTitle the text of a link to the privacy policy. Shown only with [onPolicyClick].
 * @param onPolicyClick opens the privacy policy. Monarch does not navigate; the app opens the page.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchMarketingConsentCheckbox(
    selection: Set<MonarchMarketingChannel>,
    onConsentChange: (MonarchMarketingConsent) -> Unit,
    modifier: Modifier = Modifier,
    channels: List<MonarchMarketingChannel> = listOf(MonarchMarketingChannel.Email),
    enabled: Boolean = true,
    label: String? = null,
    description: String? = null,
    showUnsubscribeNote: Boolean = true,
    policyTitle: String? = null,
    onPolicyClick: (() -> Unit)? = null,
    labels: MonarchMarketingConsentCheckboxLabels = MonarchMarketingConsentCheckboxLabels(),
) {
    val shown = channels.distinct()
    val several = shown.size > 1
    val question = label ?: defaultQuestion(shown)
    val note = if (showUnsubscribeNote) {
        labels.unsubscribeNote ?: stringResource(R.string.monarch_marketingconsentcheckbox_unsubscribe)
    } else {
        null
    }
    val channelLabels = shown.associateWith { labels.channelLabels[it] ?: defaultChannelLabel(it) }
    val wording = MonarchMarketingConsentCheckboxTokens.wording(
        listOf(question) + (if (several) shown.map { channelLabels.getValue(it) } else emptyList()) +
            listOf(description, note),
    )
    val change: (MonarchMarketingChannel, Boolean) -> Unit = { channel, checked ->
        val next = shown.associateWith { if (it == channel) checked else it in selection }
        onConsentChange(MonarchMarketingConsent(channels = next, text = wording, at = System.currentTimeMillis()))
    }
    val notes = listOfNotNull(description, note)
    val colors = MonarchTheme.colors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MonarchMarketingConsentCheckboxTokens.Gap),
    ) {
        if (!several) {
            val channel = shown.firstOrNull() ?: MonarchMarketingChannel.Email
            MonarchCheckboxRow(
                checked = channel in selection,
                onCheckedChange = { change(channel, it) },
                label = question,
                enabled = enabled,
                description = notes.takeIf { it.isNotEmpty() }?.joinToString("\n"),
            )
        } else {
            MonarchText(
                text = question,
                style = MonarchTextStyle.Label,
                color = MonarchMarketingConsentCheckboxTokens.question(enabled, colors),
                heading = true,
            )
            notes.forEach { text ->
                MonarchText(
                    text = text,
                    style = MonarchTextStyle.Caption,
                    color = MonarchMarketingConsentCheckboxTokens.note(enabled, colors),
                )
            }
            shown.forEach { channel ->
                MonarchCheckboxRow(
                    checked = channel in selection,
                    onCheckedChange = { change(channel, it) },
                    label = channelLabels.getValue(channel),
                    enabled = enabled,
                )
            }
        }
        if (policyTitle != null && onPolicyClick != null) {
            PolicyLink(title = policyTitle, enabled = enabled, onClick = onPolicyClick)
        }
    }
}

@Composable
private fun PolicyLink(title: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = title,
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchTheme.shapes.sm)
            .clickable(enabled = enabled, onClick = onClick),
        style = MonarchTheme.typography.body.copy(textDecoration = TextDecoration.Underline),
        color = MonarchMarketingConsentCheckboxTokens.link(enabled, MonarchTheme.colors),
    )
}

@Composable
private fun defaultQuestion(channels: List<MonarchMarketingChannel>): String = stringResource(
    if (channels.size > 1) {
        R.string.monarch_marketingconsentcheckbox_question_several
    } else {
        when (channels.firstOrNull() ?: MonarchMarketingChannel.Email) {
            MonarchMarketingChannel.Email -> R.string.monarch_marketingconsentcheckbox_question_email
            MonarchMarketingChannel.Sms -> R.string.monarch_marketingconsentcheckbox_question_sms
            MonarchMarketingChannel.Phone -> R.string.monarch_marketingconsentcheckbox_question_phone
            MonarchMarketingChannel.Post -> R.string.monarch_marketingconsentcheckbox_question_post
        }
    },
)

@Composable
private fun defaultChannelLabel(channel: MonarchMarketingChannel): String = stringResource(
    when (channel) {
        MonarchMarketingChannel.Email -> R.string.monarch_marketingconsentcheckbox_channel_email
        MonarchMarketingChannel.Sms -> R.string.monarch_marketingconsentcheckbox_channel_sms
        MonarchMarketingChannel.Phone -> R.string.monarch_marketingconsentcheckbox_channel_phone
        MonarchMarketingChannel.Post -> R.string.monarch_marketingconsentcheckbox_channel_post
    },
)

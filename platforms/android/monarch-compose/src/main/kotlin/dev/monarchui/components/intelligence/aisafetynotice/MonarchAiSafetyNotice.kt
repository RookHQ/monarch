package dev.monarchui.components.intelligence.aisafetynotice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * Says, calmly and specifically, that a safety system blocked a request,
 * changed an answer or removed parts of it, with an optional reason, next
 * steps, an "Edit prompt" button and a link to the content guidelines.
 *
 * Built by Monarch on the surface of `MonarchBanner`, in the warning tone for
 * [MonarchAiSafetyNoticeKind.Blocked] and the info tone otherwise. The banner
 * takes one string and one action, so the notice draws the same surface and
 * adds the reason, the next steps, a `MonarchButton` and a `MonarchLink`.
 * The title, the explanation, the reason and the next steps are one TalkBack
 * element; the actions are separate. The notice does not announce itself:
 * place it where the answer would have been.
 *
 * The link opens through `LocalUriHandler`, which the app can replace, or
 * through [onOpenLearnMore]. Only http, https and mailto links are shown.
 *
 * @param kind what the safety system did. Sets the tone and the built-in title and explanation.
 * @param modifier the modifier of the notice.
 * @param title replaces the title of the kind for this notice.
 * @param message replaces the explanation of the kind for this notice.
 * @param reason why, in plain words, shown after a bold "Reason:" label. Blank text is not shown.
 * @param nextSteps what the person can do next, as a sentence. Blank text is not shown.
 * @param learnMoreUrl adds a "Content guidelines" link. Any scheme but http, https and mailto is dropped.
 * @param icon an icon at the start, in the tone colour, for example a lock as a `MonarchIcon` with a `null`
 * content description. Monarch bundles no icons; each kind has its own title, so the icon is decoration.
 * @param labels replacements for the built-in titles, explanations, "Reason:", "Edit prompt" and
 * "Content guidelines".
 * @param onEditPrompt adds an "Edit prompt" button, usually to put the prompt back in the composer.
 * @param onOpenLearnMore called with the link instead of opening it with `LocalUriHandler`, for example to
 * open it in a Custom Tab.
 */
@Composable
public fun MonarchAiSafetyNotice(
    kind: MonarchAiSafetyNoticeKind,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    reason: String? = null,
    nextSteps: String? = null,
    learnMoreUrl: String? = null,
    icon: (@Composable () -> Unit)? = null,
    labels: MonarchAiSafetyNoticeLabels = MonarchAiSafetyNoticeLabels(),
    onEditPrompt: (() -> Unit)? = null,
    onOpenLearnMore: ((String) -> Unit)? = null,
) {
    val colors = MonarchAiSafetyNoticeTokens.colors(kind, MonarchTheme.colors)
    val link = MonarchAiSafetyNoticeTokens.safeUrl(learnMoreUrl)
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
            .monarchSurface(colors, MonarchShapeDefaults.Container)
            .padding(
                horizontal = MonarchAiSafetyNoticeTokens.HorizontalPadding,
                vertical = MonarchAiSafetyNoticeTokens.VerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(MonarchAiSafetyNoticeTokens.ContentGap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchAiSafetyNoticeTokens.ContentGap),
            verticalAlignment = Alignment.Top,
        ) {
            if (icon != null) CompositionLocalProvider(LocalContentColor provides colors.content, content = icon)
            NoticeText(
                title = title ?: labels.title?.invoke(kind) ?: stringResource(titleResource(kind)),
                message = message ?: labels.message?.invoke(kind) ?: stringResource(messageResource(kind)),
                reasonLabel = monarchString(labels.reason, R.string.monarch_aisafetynotice_reason),
                reason = MonarchAiSafetyNoticeTokens.present(reason),
                nextSteps = MonarchAiSafetyNoticeTokens.present(nextSteps),
            )
        }
        if (onEditPrompt != null || link != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchAiSafetyNoticeTokens.ActionGap),
                verticalArrangement = Arrangement.spacedBy(MonarchAiSafetyNoticeTokens.ActionGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (onEditPrompt != null) {
                    MonarchButton(
                        text = monarchString(labels.editPrompt, R.string.monarch_aisafetynotice_edit_prompt),
                        onClick = onEditPrompt,
                        variant = MonarchButtonVariant.Secondary,
                        size = MonarchButtonSize.Sm,
                        leadingIcon = MonarchDefaultIcons.Pencil,
                    )
                }
                if (link != null) {
                    MonarchLink(
                        text = monarchString(labels.learnMore, R.string.monarch_aisafetynotice_learn_more),
                        onClick = { if (onOpenLearnMore != null) onOpenLearnMore(link) else uriHandler.openUri(link) },
                        variant = MonarchLinkVariant.Standalone,
                        external = onOpenLearnMore == null,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeText(title: String, message: String, reasonLabel: String, reason: String?, nextSteps: String?) {
    val themeColors = MonarchTheme.colors
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(MonarchAiSafetyNoticeTokens.TextGap),
    ) {
        Text(text = title, style = MonarchTheme.typography.bodyStrong, color = themeColors.textDefault)
        Text(
            text = message,
            style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.Normal),
            color = themeColors.textMuted,
        )
        if (reason != null) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = themeColors.textDefault)) {
                        append(reasonLabel)
                    }
                    append(" ")
                    append(reason)
                },
                style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.Normal),
                color = themeColors.textMuted,
            )
        }
        if (nextSteps != null) {
            Text(
                text = nextSteps,
                style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.Normal),
                color = themeColors.textMuted,
            )
        }
    }
}

private fun titleResource(kind: MonarchAiSafetyNoticeKind): Int = when (kind) {
    MonarchAiSafetyNoticeKind.Blocked -> R.string.monarch_aisafetynotice_title_blocked
    MonarchAiSafetyNoticeKind.Modified -> R.string.monarch_aisafetynotice_title_modified
    MonarchAiSafetyNoticeKind.Filtered -> R.string.monarch_aisafetynotice_title_filtered
}

private fun messageResource(kind: MonarchAiSafetyNoticeKind): Int = when (kind) {
    MonarchAiSafetyNoticeKind.Blocked -> R.string.monarch_aisafetynotice_message_blocked
    MonarchAiSafetyNoticeKind.Modified -> R.string.monarch_aisafetynotice_message_modified
    MonarchAiSafetyNoticeKind.Filtered -> R.string.monarch_aisafetynotice_message_filtered
}

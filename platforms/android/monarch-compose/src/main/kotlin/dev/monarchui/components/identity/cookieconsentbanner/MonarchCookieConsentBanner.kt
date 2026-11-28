package dev.monarchui.components.identity.cookieconsentbanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.monarchShadow
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The first-run consent card of an app that uses cookies or similar tracking, for example in web content it shows:
 * a title, the explanation with a link to the policy, and the choices of the regime.
 *
 * A raised card built from `MonarchButton`s, for the bottom of the screen (a `Scaffold`'s `bottomBar`, or above
 * the content). In the opt-in regime "Reject all" and "Accept all" look the same, so neither is pushed, and
 * "Customize" opens the app's own preferences. The card is a pane named by its title, and with [announce] TalkBack
 * reads it when it appears. The app stores the decision and stops showing the card.
 *
 * @param regime the consent rule: opt-in, opt-out or notice.
 * @param onDecide called with the person's choice.
 * @param modifier the modifier of the card.
 * @param onCustomize opens the detailed preferences; `null` hides "Customize". Shown in the opt-in regime.
 * @param onPolicyClick opens the cookie policy; `null` shows no policy link.
 * @param announce whether TalkBack reads the card when it appears.
 * @param labels replacements for the built-in strings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchCookieConsentBanner(
    regime: MonarchCookieConsentRegime,
    onDecide: (MonarchCookieConsentChoice) -> Unit,
    modifier: Modifier = Modifier,
    onCustomize: (() -> Unit)? = null,
    onPolicyClick: (() -> Unit)? = null,
    announce: Boolean = false,
    labels: MonarchCookieConsentBannerLabels = MonarchCookieConsentBannerLabels(),
) {
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Container
    val title = monarchString(labels.title, R.string.monarch_cookieconsentbanner_title)
    val description = monarchString(labels.description, MonarchCookieConsentBannerTokens.description(regime))
    val policy = monarchString(labels.policy, R.string.monarch_cookieconsentbanner_policy)
    val linkStyles = TextLinkStyles(
        SpanStyle(color = MonarchCookieConsentBannerTokens.link(colors), textDecoration = TextDecoration.Underline),
    )
    val text = buildAnnotatedString {
        append(description)
        if (onPolicyClick != null) {
            append(" ")
            withLink(LinkAnnotation.Clickable(PolicyTag, linkStyles) { onPolicyClick() }) { append(policy) }
        }
    }
    val strings = mapOf(
        MonarchCookieConsentChoice.AcceptAll to
            monarchString(labels.acceptAll, R.string.monarch_cookieconsentbanner_accept_all),
        MonarchCookieConsentChoice.RejectAll to
            monarchString(labels.rejectAll, R.string.monarch_cookieconsentbanner_reject_all),
        MonarchCookieConsentChoice.Acknowledge to
            monarchString(labels.acknowledge, R.string.monarch_cookieconsentbanner_acknowledge),
        MonarchCookieConsentChoice.DoNotSell to
            monarchString(labels.doNotSell, R.string.monarch_cookieconsentbanner_do_not_sell),
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .monarchShadow(MonarchTheme.elevation.lg, shape)
            .background(MonarchCookieConsentBannerTokens.fill(colors), shape)
            .border(MonarchShapeDefaults.BorderWidth, MonarchCookieConsentBannerTokens.border(colors), shape)
            .semantics {
                isTraversalGroup = true
                paneTitle = title
                if (announce) liveRegion = LiveRegionMode.Polite
            }
            .padding(MonarchCookieConsentBannerTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(MonarchCookieConsentBannerTokens.Gap),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchCookieConsentBannerTokens.TextGap)) {
            MonarchText(text = title, style = MonarchTextStyle.BodyStrong, heading = true)
            MonarchText(text = text, color = MonarchCookieConsentBannerTokens.description(colors))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchCookieConsentBannerTokens.ActionGap),
            verticalArrangement = Arrangement.spacedBy(MonarchCookieConsentBannerTokens.ActionGap),
        ) {
            if (regime == MonarchCookieConsentRegime.OptIn && onCustomize != null) {
                MonarchButton(
                    text = monarchString(labels.customize, R.string.monarch_cookieconsentbanner_customize),
                    onClick = onCustomize,
                    variant = MonarchCookieConsentBannerTokens.CustomizeVariant,
                )
            }
            MonarchCookieConsentBannerTokens.choices(regime).forEach { choice ->
                MonarchButton(
                    text = strings.getValue(choice),
                    onClick = { onDecide(choice) },
                    variant = MonarchCookieConsentBannerTokens.DecisionVariant,
                )
            }
        }
    }
}

private const val PolicyTag = "policy"

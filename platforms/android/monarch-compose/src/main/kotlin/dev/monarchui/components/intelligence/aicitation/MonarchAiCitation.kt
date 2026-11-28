package dev.monarchui.components.intelligence.aicitation

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.datadisplay.favicon.MonarchFavicon
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.components.overlays.popover.MonarchPopover
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A `[1]` marker after a sentence of an AI answer that opens a card with the cited source: its title, the kind
 * and the site, the quoted passage and a way to open it.
 *
 * Material 3 has no citation, so Monarch builds the marker as small text in the link colour with a 48dp touch
 * target, and the card on `MonarchPopover`. The web opens the card on hover; on a phone a tap opens it and a tap
 * outside or the back gesture closes it. Place the marker after the sentence it backs, for example in a `FlowRow`
 * or a `Row`: a Compose `Text` cannot hold a control. Number a message's sources with
 * [MonarchAiCitationDefaults.sourceNumbers].
 *
 * For TalkBack the marker is a button named "Source 1: <title>"; the brackets are not read. The card's heading is
 * a heading, and its favicon is decoration.
 *
 * @param index the number shown in the marker; 1 shows `[1]`.
 * @param source the cited source part of the message.
 * @param bare shows the number without brackets, as a small pill: `1` instead of `[1]`.
 * @param modifier the modifier of the marker.
 * @param quote the passage the answer relies on, from the citation part. Shown in the card.
 * @param image the source's favicon, loaded by the app from [MonarchAiSourcePart.favicon]. `null` shows a letter
 * tile.
 * @param expanded whether the card is open, to control it from outside. `null` lets the marker keep its own state.
 * @param onExpandedChange called when the marker or a dismissal asks to open or close the card.
 * @param onOpen opens the source inside the app, for example in a preview sheet. Shows "Open source" in the card,
 * which closes the card first. Without it the card links to the source's http, https or mailto address, and
 * shows no action when there is none.
 * @param labels replaces any of the built-in strings.
 * @param interactionSource the interaction source of the marker.
 */
@Composable
public fun MonarchAiCitation(
    index: Int,
    source: MonarchAiSourcePart,
    modifier: Modifier = Modifier,
    quote: String? = null,
    image: Painter? = null,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onOpen: ((MonarchAiSourcePart) -> Unit)? = null,
    labels: MonarchAiCitationLabels = MonarchAiCitationLabels(),
    interactionSource: MutableInteractionSource? = null,
    bare: Boolean = false,
) {
    var ownExpanded by rememberSaveable { mutableStateOf(false) }
    val open = expanded ?: ownExpanded
    val setOpen: (Boolean) -> Unit = { value ->
        if (expanded == null) ownExpanded = value
        onExpandedChange?.invoke(value)
    }
    val markerName = labels.marker?.invoke(index, source.title)
        ?: stringResource(R.string.monarch_aicitation_marker, index, source.title)
    val cardName = labels.card?.invoke(index) ?: stringResource(R.string.monarch_aicitation_card, index)
    Box(modifier = modifier) {
        MonarchText(
            text = AnnotatedString(
                if (bare) index.toString() else MonarchAiCitationTokens.markerText(index),
                SpanStyle(fontWeight = FontWeight.Bold),
            ),
            modifier = Modifier
                .monarchMinimumTouchTarget()
                .monarchFocusRing(MonarchTheme.shapes.sm)
                .clip(MonarchTheme.shapes.sm)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                ) { setOpen(!open) }
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = markerName
                    onClick {
                        setOpen(!open)
                        true
                    }
                }
                .padding(MonarchAiCitationTokens.MarkerPadding),
            style = MonarchTextStyle.Caption,
            color = MonarchAiCitationTokens.marker(MonarchTheme.colors),
        )
        MonarchPopover(expanded = open, onDismissRequest = { setOpen(false) }, label = cardName) {
            Card(cardName, source, quote, image, onOpen, labels, close = { setOpen(false) })
        }
    }
}

@Composable
private fun Card(
    heading: String,
    source: MonarchAiSourcePart,
    quote: String?,
    image: Painter?,
    onOpen: ((MonarchAiSourcePart) -> Unit)?,
    labels: MonarchAiCitationLabels,
    close: () -> Unit,
) {
    val colors = MonarchTheme.colors
    val host = MonarchAiCitationTokens.host(source.url)
    Column(
        modifier = Modifier.widthIn(max = MonarchAiCitationTokens.CardMaxWidth),
        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
    ) {
        MonarchText(text = heading, style = MonarchTextStyle.Subheading, heading = true)
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchAiCitationTokens.SourceGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchFavicon(image = image, name = host ?: source.title)
            MonarchText(text = source.title, style = MonarchTextStyle.BodyStrong)
        }
        MonarchText(
            text = listOfNotNull(kindName(source.kind, labels), host, source.product).joinToString(" · "),
            style = MonarchTextStyle.Caption,
            color = MonarchAiCitationTokens.meta(colors),
        )
        if (!quote.isNullOrBlank()) Quote(quote)
        Action(source, onOpen, labels, close)
    }
}

@Composable
private fun Quote(quote: String) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MonarchTheme.shapes.sm)
            .background(MonarchAiCitationTokens.quoteBackground(colors)),
    ) {
        Box(
            modifier = Modifier
                .width(MonarchAiCitationTokens.QuoteBarWidth)
                .fillMaxHeight()
                .background(MonarchAiCitationTokens.quoteBar(colors)),
        )
        MonarchText(text = quote, modifier = Modifier.padding(MonarchAiCitationTokens.QuotePadding))
    }
}

@Composable
private fun Action(
    source: MonarchAiSourcePart,
    onOpen: ((MonarchAiSourcePart) -> Unit)?,
    labels: MonarchAiCitationLabels,
    close: () -> Unit,
) {
    if (onOpen != null) {
        MonarchButton(
            text = monarchString(labels.open, R.string.monarch_aicitation_open),
            onClick = {
                close()
                onOpen(source)
            },
            size = MonarchButtonSize.Sm,
        )
        return
    }
    val url = MonarchAiCitationTokens.safeUrl(source.url) ?: return
    val uriHandler = LocalUriHandler.current
    MonarchLink(
        text = monarchString(labels.visit, R.string.monarch_aicitation_visit),
        onClick = { uriHandler.openUri(url) },
        variant = MonarchLinkVariant.Standalone,
        external = true,
    )
}

@Composable
private fun kindName(kind: MonarchAiSourceKind, labels: MonarchAiCitationLabels): String =
    labels.kind?.invoke(kind) ?: stringResource(
        when (kind) {
            MonarchAiSourceKind.Web -> R.string.monarch_aicitation_kind_web
            MonarchAiSourceKind.Doc -> R.string.monarch_aicitation_kind_doc
            MonarchAiSourceKind.Record -> R.string.monarch_aicitation_kind_record
            MonarchAiSourceKind.File -> R.string.monarch_aicitation_kind_file
        },
    )

package com.rookhq.monarch.components.collaboration.linkcard

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeVariant
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.datadisplay.favicon.MonarchFavicon
import com.rookhq.monarch.internal.MonarchImage
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A preview of a shared link, from data the app unfurled: picture, site,
 * title and description, opening the link when tapped.
 *
 * A `MonarchCard` the user taps as a whole; TalkBack reads it as one element
 * with the site, the title, the description and "Video" for a video. The link
 * opens through `LocalUriHandler`, which the app can replace, or through
 * [onOpen]. Only http, https and mailto links open; any other link shows the
 * card as plain text. Monarch fetches nothing: the app passes the page's
 * picture and favicon as `Painter`s it loaded with its own pipeline.
 *
 * @param title the page title, up to two lines.
 * @param url the link.
 * @param modifier the modifier of the card.
 * @param description the page description, up to two lines.
 * @param siteName the site above the title. `null` uses the host of [url] without "www.".
 * @param image the page's picture. While it is `null` and [hasImage] is `true`, a muted tile keeps its place.
 * @param hasImage whether the page has a picture. Pass `true` while the picture loads.
 * @param imageAspectRatio the picture's width over its height. It is kept between 1.2 and 2.2, and 2.2 when
 * unknown.
 * @param favicon the site's icon; without it `MonarchFavicon` draws a letter tile.
 * @param video marks the link as a video: a "Video" badge on the picture, also read by TalkBack.
 * @param size `Md` puts the picture above the text, `Sm` a thumbnail beside it.
 * @param onOpen called with [url] instead of opening it with `LocalUriHandler`, for example to open it in
 * a Custom Tab or to log the tap.
 * @param onDismiss shows a remove button, for the author of the message.
 * @param dismissIcon the icon of the remove button, for example a close mark as a `MonarchIcon` with a
 * `null` content description; the button then sits in the top end corner. Without it the button is a small
 * "Remove" button under the card. Monarch bundles no icons.
 * @param dismissLabel replaces the built-in spoken name of the remove button, "Remove the preview of" and
 * the title (`monarch_linkcard_dismiss`).
 * @param removeLabel replaces the built-in text "Remove" of the remove button without an icon
 * (`monarch_linkcard_remove`).
 * @param videoLabel replaces the built-in badge "Video" (`monarch_linkcard_video`).
 * @param interactionSource the source of the press, focus and hover interactions of the card.
 */
@Composable
public fun MonarchLinkCard(
    title: String,
    url: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    siteName: String? = null,
    image: Painter? = null,
    hasImage: Boolean = image != null,
    imageAspectRatio: Float? = null,
    favicon: Painter? = null,
    video: Boolean = false,
    size: MonarchLinkCardSize = MonarchLinkCardSize.Md,
    onOpen: ((String) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    dismissIcon: (@Composable () -> Unit)? = null,
    dismissLabel: String? = null,
    removeLabel: String? = null,
    videoLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val uriHandler = LocalUriHandler.current
    val site = MonarchLinkCardTokens.siteName(siteName, url)
    val videoText = if (video) monarchString(videoLabel, R.string.monarch_linkcard_video) else null
    val dismissName = dismissLabel ?: stringResource(R.string.monarch_linkcard_dismiss, title)
    val reserveCorner = onDismiss != null && dismissIcon != null
    val body: @Composable ColumnScope.() -> Unit = {
        CardBody(
            title = title,
            description = description,
            site = site,
            url = url,
            picture = if (hasImage) image else null,
            hasImage = hasImage,
            imageAspectRatio = imageAspectRatio,
            favicon = favicon,
            videoText = videoText,
            size = size,
            reserveCorner = reserveCorner,
        )
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(MonarchLinkCardTokens.Gap),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (MonarchLinkCardTokens.isSafe(url)) {
                MonarchCard(
                    onClick = { if (onOpen != null) onOpen(url) else uriHandler.openUri(url.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(),
                    interactionSource = interactionSource,
                    content = body,
                )
            } else {
                MonarchCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) { },
                    contentPadding = PaddingValues(),
                    content = body,
                )
            }
            if (onDismiss != null && dismissIcon != null) {
                DismissIconButton(dismissName, onDismiss, dismissIcon)
            }
        }
        if (onDismiss != null && dismissIcon == null) {
            MonarchButton(
                text = monarchString(removeLabel, R.string.monarch_linkcard_remove),
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = dismissName },
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

@Composable
private fun BoxScope.DismissIconButton(name: String, onDismiss: () -> Unit, icon: @Composable () -> Unit) {
    // Over the picture: the surface fill with a hairline, so the button reads on any image.
    MonarchIconButton(
        label = name,
        onClick = onDismiss,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(MonarchLinkCardTokens.CornerInset),
        variant = MonarchButtonVariant.Secondary,
        size = MonarchButtonSize.Sm,
        shape = CircleShape,
    ) {
        Box(Modifier.clearAndSetSemantics { }) { icon() }
    }
}

@Composable
@Suppress("LongParameterList")
private fun CardBody(
    title: String,
    description: String?,
    site: String?,
    url: String,
    picture: Painter?,
    hasImage: Boolean,
    imageAspectRatio: Float?,
    favicon: Painter?,
    videoText: String?,
    size: MonarchLinkCardSize,
    reserveCorner: Boolean,
) {
    val text: @Composable (Modifier, Boolean) -> Unit = { textModifier, withVideo ->
        TextBlock(title, description, site, url, favicon, if (withVideo) videoText else null, textModifier)
    }
    when {
        !hasImage -> text(Modifier.fillMaxWidth().endSpace(reserveCorner), true)
        size == MonarchLinkCardSize.Md -> {
            Picture(
                image = picture,
                videoText = videoText,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(MonarchLinkCardTokens.aspectRatio(imageAspectRatio)),
            )
            text(Modifier.fillMaxWidth(), false)
        }
        else -> Row(modifier = Modifier.fillMaxWidth().endSpace(reserveCorner)) {
            text(Modifier.weight(1f), true)
            Picture(
                image = picture,
                videoText = null,
                modifier = Modifier
                    .padding(MonarchLinkCardTokens.TextPadding)
                    .size(MonarchLinkCardTokens.Thumbnail),
                shape = MonarchShapeDefaults.Control,
            )
        }
    }
}

private fun Modifier.endSpace(reserve: Boolean): Modifier =
    if (reserve) padding(end = MonarchLinkCardTokens.DismissSpace) else this

@Composable
@Suppress("LongParameterList")
private fun TextBlock(
    title: String,
    description: String?,
    site: String?,
    url: String,
    favicon: Painter?,
    videoText: String?,
    modifier: Modifier,
) {
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    Column(
        modifier = modifier.padding(MonarchLinkCardTokens.TextPadding),
        verticalArrangement = Arrangement.spacedBy(MonarchLinkCardTokens.Gap),
    ) {
        if (site != null || videoText != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchLinkCardTokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (site != null) {
                    MonarchFavicon(image = favicon, name = site.ifEmpty { url })
                    Text(
                        text = site,
                        modifier = Modifier.weight(1f, fill = false),
                        style = typography.caption,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (videoText != null) MonarchBadge(text = videoText)
            }
        }
        Text(
            text = title,
            style = typography.bodyStrong,
            color = colors.textDefault,
            maxLines = MonarchLinkCardTokens.MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        if (description != null) {
            Text(
                text = description,
                style = typography.caption,
                color = colors.textMuted,
                maxLines = MonarchLinkCardTokens.MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Picture(
    image: Painter?,
    videoText: String?,
    modifier: Modifier,
    shape: Shape = RectangleShape,
) {
    val placeholder = MonarchLinkCardTokens.placeholderColor(MonarchTheme.colors)
    Box(modifier = modifier) {
        MonarchImage(
            painter = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            shape = shape,
        ) { Box(Modifier.fillMaxSize().background(placeholder)) }
        if (videoText != null) {
            MonarchBadge(
                text = videoText,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(MonarchLinkCardTokens.TextPadding),
                variant = MonarchBadgeVariant.Solid,
            )
        }
    }
}

package dev.monarchui.components.collaboration.postcard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchRelativeClock
import dev.monarchui.theme.MonarchTheme
import java.text.NumberFormat

/**
 * A post in a community feed or search result: author, relative time, pinned
 * and answered markers, tags, title, a short excerpt, and reaction and
 * comment counts.
 *
 * Built on `MonarchCard`. The author line, the title and the excerpt are one
 * button that calls [onClick], so the app opens the post on its own screen;
 * footer [actions] are separate buttons beside it, never inside it. The
 * relative time ("3 hours ago") refreshes every minute unless [now] is given.
 *
 * @param title the post's title.
 * @param author who wrote it.
 * @param onClick opens the post.
 * @param modifier the modifier of the card.
 * @param pinned pinned to the top of its list: "Pinned" in the author line.
 * @param answered a question with an accepted answer: a success "Answered" badge.
 * @param authorImage the author's picture, loaded by the app. `null` shows initials.
 * @param excerpt the plain-text body, shortened to [excerptLength] characters at a word boundary.
 * @param excerptLength the longest excerpt shown.
 * @param publishedAt when it was published, in epoch milliseconds (UTC).
 * @param now the current time in epoch milliseconds, for tests. `null` uses the clock.
 * @param tags markers after the author line: category, topic or a status such as "In review".
 * @param reactionCount the number of reactions; `null` hides it.
 * @param commentCount the number of comments; `null` hides it.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the interaction source of the card's button.
 * @param actions controls in the footer, such as a bookmark button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchPostCard(
    title: String,
    author: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
    answered: Boolean = false,
    authorImage: Painter? = null,
    excerpt: String? = null,
    excerptLength: Int = 220,
    publishedAt: Long? = null,
    now: Long? = null,
    tags: List<MonarchPostCardTag> = emptyList(),
    reactionCount: Int? = null,
    commentCount: Int? = null,
    labels: MonarchPostCardLabels = MonarchPostCardLabels(),
    interactionSource: MutableInteractionSource? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MonarchPostCardTokens
    val text = tokens.textColors(MonarchTheme.colors)
    val shape = MonarchShapeDefaults.Row
    val body = excerpt?.let { tokens.truncateExcerpt(it, excerptLength) }?.takeIf { it.isNotEmpty() }
    MonarchCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .monarchFocusRing(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    role = Role.Button,
                    onClick = onClick,
                ),
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
                verticalArrangement = Arrangement.spacedBy(tokens.InlineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchAvatar(image = authorImage, name = author, size = MonarchAvatarSize.Sm, decorative = true)
                Text(text = author, style = MonarchTheme.typography.label, color = text.meta)
                if (publishedAt != null) {
                    Text(
                        text = "·",
                        modifier = Modifier.clearAndSetSemantics { },
                        style = MonarchTheme.typography.caption,
                        color = text.meta,
                    )
                    Text(
                        text = tokens.relativeTime(
                            publishedAt,
                            rememberMonarchRelativeClock(listOf(publishedAt), fixed = now),
                        ),
                        style = MonarchTheme.typography.caption,
                        color = text.meta,
                    )
                }
                if (pinned) {
                    Text(
                        text = monarchString(labels.pinned, R.string.monarch_postcard_pinned),
                        style = MonarchTheme.typography.caption,
                        color = text.meta,
                    )
                }
                if (answered) {
                    MonarchBadge(
                        text = monarchString(labels.answered, R.string.monarch_postcard_answered),
                        tone = MonarchTone.Success,
                    )
                }
                tags.forEach { tag -> MonarchBadge(text = tag.label, tone = tag.tone) }
            }
            Text(text = title, style = MonarchTheme.typography.subheading, color = text.title)
            if (body != null) {
                Text(text = body, style = tokens.excerptStyle(MonarchTheme.typography), color = text.meta)
            }
        }
        if (reactionCount != null || commentCount != null || actions != null) {
            Footer(reactionCount, commentCount, labels, actions)
        }
    }
}

@Composable
private fun Footer(
    reactionCount: Int?,
    commentCount: Int?,
    labels: MonarchPostCardLabels,
    actions: (@Composable RowScope.() -> Unit)?,
) {
    val tokens = MonarchPostCardTokens
    val color = tokens.textColors(MonarchTheme.colors).meta
    val format = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = tokens.Gap),
        horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { },
            horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (reactionCount != null) {
                val spoken = labels.reactions?.invoke(reactionCount) ?: pluralStringResource(
                    R.plurals.monarch_postcard_reactions,
                    reactionCount,
                    format.format(reactionCount),
                )
                CountMark(MonarchDefaultIcons.ThumbUp, format.format(reactionCount), spoken, color)
            }
            if (commentCount != null) {
                val spoken = labels.comments?.invoke(commentCount) ?: pluralStringResource(
                    R.plurals.monarch_postcard_comments,
                    commentCount,
                    format.format(commentCount),
                )
                CountMark(MonarchDefaultIcons.Comment, format.format(commentCount), spoken, color)
            }
        }
        actions?.invoke(this)
    }
}

/** A glyph with its count, as the web footer; TalkBack reads the full phrase ("12 reactions"). */
@Composable
private fun CountMark(icon: @Composable () -> Unit, count: String, spoken: String, color: Color) {
    Row(
        modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(spoken) },
        horizontalArrangement = Arrangement.spacedBy(MonarchPostCardTokens.InlineGap / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides color) { icon() }
        Text(text = count, style = MonarchTheme.typography.caption, color = color)
    }
}

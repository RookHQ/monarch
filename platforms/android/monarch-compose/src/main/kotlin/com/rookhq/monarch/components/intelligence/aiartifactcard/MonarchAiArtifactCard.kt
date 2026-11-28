package com.rookhq.monarch.components.intelligence.aiartifactcard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiArtifactKind
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiArtifactPart
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The compact card a reply shows for an artifact it made, such as "Q3 summary · Document · v3".
 *
 * A Material 3 `Surface` with an `onClick`, drawn like `MonarchCard`, that
 * the app uses to open the artifact. The card is one button whose name reads
 * the title, the kind, the version in words, the description and the action.
 * The artifact on show is marked [active]: it reads "Showing" instead of
 * "Open", is selected for TalkBack and has a heavier AI border, so the state
 * is not colour alone.
 *
 * @param title the title of the artifact.
 * @param kind what the artifact is, which selects the kind name.
 * @param version the version number.
 * @param onClick called when the person activates the card. Open the artifact here.
 * @param modifier the modifier of the card.
 * @param description secondary text under the title, for example "Updated just now".
 * @param active whether the artifact is the one currently on show.
 * @param enabled whether the person can activate the card.
 * @param icon the kind's icon in a tile before the text, for example a `MonarchIcon` of a document with a
 * `null` content description. Monarch bundles no icons; without one there is no tile.
 * @param trailingIcon an icon after the action, for example a chevron that points to the end, with
 * `mirrorInRtl` set. Decorative.
 * @param kindLabel replaces the built-in name of the kind, for example `monarch_aiartifactcard_document`.
 * @param versionLabel replaces the built-in "v3" shown and "version 3" read. An empty string hides it.
 * @param actionLabel replaces the built-in "Open" or "Showing".
 * @param interactionSource the source of the press, focus and hover interactions of the card.
 */
@Composable
public fun MonarchAiArtifactCard(
    title: String,
    kind: MonarchAiArtifactKind,
    version: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    kindLabel: String? = null,
    versionLabel: String? = null,
    actionLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MonarchAiArtifactCardTokens.colors(active, enabled, MonarchTheme.colors)
    val shape = MonarchTheme.shapes.lg
    val kindName = monarchString(kindLabel, MonarchAiArtifactCardTokens.kindResource(kind))
    val shownVersion = versionLabel ?: stringResource(R.string.monarch_aiartifactcard_version, version)
    val spokenVersion = versionLabel ?: stringResource(R.string.monarch_aiartifactcard_version_long, version)
    val action = actionLabel ?: stringResource(
        if (active) R.string.monarch_aiartifactcard_opened else R.string.monarch_aiartifactcard_open,
    )
    val meta = if (shownVersion.isEmpty()) {
        kindName
    } else {
        stringResource(R.string.monarch_aiartifactcard_meta, kindName, shownVersion)
    }
    val spoken = listOf(title, kindName, spokenVersion, description.orEmpty(), action)
        .filter { it.isNotEmpty() }
        .joinToString(separator = ", ")
    Surface(
        onClick = onClick,
        modifier = modifier
            .monarchFocusRing(shape)
            .semantics {
                contentDescription = spoken
                role = Role.Button
                selected = active
            },
        enabled = enabled,
        shape = shape,
        color = colors.container,
        contentColor = colors.title,
        border = BorderStroke(colors.borderWidth, colors.border),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .clearAndSetSemantics { }
                .heightIn(min = MonarchAiArtifactCardTokens.MinHeight)
                .padding(MonarchAiArtifactCardTokens.Padding),
            horizontalArrangement = Arrangement.spacedBy(MonarchAiArtifactCardTokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) IconTile(icon)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MonarchAiArtifactCardTokens.TextGap),
            ) {
                Text(text = title, style = MonarchTheme.typography.bodyStrong, color = colors.title)
                Text(text = meta, style = MonarchTheme.typography.caption, color = colors.meta)
                if (!description.isNullOrEmpty()) {
                    Text(text = description, style = MonarchTheme.typography.caption, color = colors.meta)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchAiArtifactCardTokens.ActionGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = action, style = MonarchTheme.typography.label, color = colors.action)
                CompositionLocalProvider(LocalContentColor provides colors.action) { trailingIcon?.invoke() }
            }
        }
    }
}

/**
 * The card for an artifact part of a reply, as the web takes it.
 *
 * @param part the artifact part from the message: its title, kind and version.
 * @param onOpen called with the part when the person activates the card.
 * @param modifier the modifier of the card.
 * @param description secondary text under the title.
 * @param active whether the artifact is the one currently on show.
 * @param enabled whether the person can activate the card.
 * @param icon the kind's icon in a tile before the text.
 * @param trailingIcon an icon after the action.
 * @param interactionSource the source of the press, focus and hover interactions of the card.
 */
@Composable
public fun MonarchAiArtifactCard(
    part: MonarchAiArtifactPart,
    onOpen: (MonarchAiArtifactPart) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    MonarchAiArtifactCard(
        title = part.title,
        kind = part.kind,
        version = part.version,
        onClick = { onOpen(part) },
        modifier = modifier,
        description = description,
        active = active,
        enabled = enabled,
        icon = icon,
        trailingIcon = trailingIcon,
        interactionSource = interactionSource,
    )
}

/** The tinted tile with the kind's icon, shared with the artifact canvas header. */
@Composable
internal fun IconTile(icon: @Composable () -> Unit) {
    val (fill, content) = MonarchAiArtifactCardTokens.iconTile(MonarchTheme.colors)
    Box(
        modifier = Modifier
            .size(MonarchAiArtifactCardTokens.IconTile)
            .background(fill, MonarchTheme.shapes.md),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            Box(Modifier.size(MonarchAiArtifactCardTokens.IconSize), contentAlignment = Alignment.Center) { icon() }
        }
    }
}

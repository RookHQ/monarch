package com.rookhq.monarch.components.datadisplay.statusbadge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A status shown as words in a tone, for example "Partial outage", looked up
 * by key in a typed registry.
 *
 * Built by Monarch from a `Row` with a tinted surface: Material 3 has no
 * equivalent. The key is looked up in [registry], then in
 * `LocalMonarchStatusRegistry`, then in the built-in set; a key found nowhere
 * shows a readable label made from the key in the neutral tone, never the raw
 * key. The label is always shown, so colour is never the only signal; the
 * icon and the dot are decoration and hidden from TalkBack. The badge is not
 * interactive.
 *
 * @param status the key: a built-in one (`operational`, `degraded`, `partial`, `down`, `maintenance`,
 * `unknown`, `success`, `warning`, `danger`, `info`, `neutral`) or one from a registry. `null` is unknown.
 * @param modifier the modifier of the badge.
 * @param variant the emphasis.
 * @param size the text size.
 * @param showIcon `false` hides the icon of the status; the label still carries the meaning.
 * @param registry statuses for this badge only, looked up before `LocalMonarchStatusRegistry`.
 * @param label replaces the label of the status, for example with a string the app translated.
 * @param icon replaces the icon of the status. In the dot variant it replaces the dot.
 * @param mergeSemantics reads the badge as one element. Pass `false` when the badge sits in a row or a cell
 * that merges its children itself, so the label is read in the row's order and not as a separate stop.
 */
@Composable
public fun MonarchStatusBadge(
    status: String?,
    modifier: Modifier = Modifier,
    variant: MonarchStatusBadgeVariant = MonarchStatusBadgeVariant.Subtle,
    size: MonarchStatusBadgeSize = MonarchStatusBadgeSize.Md,
    showIcon: Boolean = true,
    registry: MonarchStatusRegistry? = null,
    label: String? = null,
    icon: (@Composable () -> Unit)? = null,
    mergeSemantics: Boolean = true,
) {
    val definition = resolveStatus(status, registry, LocalMonarchStatusRegistry.current)
    MonarchStatusBadge(
        definition = MonarchStatusDefinition(
            label = label ?: definition.label,
            tone = definition.tone,
            icon = icon ?: definition.icon,
            description = definition.description,
        ),
        modifier = modifier,
        variant = variant,
        size = size,
        showIcon = showIcon,
        mergeSemantics = mergeSemantics,
    )
}

/**
 * A status badge from a definition given directly, without a registry.
 *
 * @param definition the label, tone and icon.
 * @param modifier the modifier of the badge.
 * @param variant the emphasis.
 * @param size the text size.
 * @param showIcon `false` hides the icon of the definition; the label still carries the meaning.
 * @param mergeSemantics reads the badge as one element. Pass `false` inside a row that merges its children.
 */
@Composable
public fun MonarchStatusBadge(
    definition: MonarchStatusDefinition,
    modifier: Modifier = Modifier,
    variant: MonarchStatusBadgeVariant = MonarchStatusBadgeVariant.Subtle,
    size: MonarchStatusBadgeSize = MonarchStatusBadgeSize.Md,
    showIcon: Boolean = true,
    mergeSemantics: Boolean = true,
) {
    val themeColors = MonarchTheme.colors
    val colors = MonarchStatusBadgeTokens.colors(definition.tone, variant, themeColors)
    val icon = definition.icon?.takeIf { showIcon }
    val surface = if (variant == MonarchStatusBadgeVariant.Dot) {
        modifier
    } else {
        modifier
            .monarchSurface(colors, MonarchShapeDefaults.Mark)
            .padding(
                horizontal = MonarchStatusBadgeTokens.HorizontalPadding,
                vertical = MonarchStatusBadgeTokens.VerticalPadding,
            )
    }
    Row(
        modifier = if (mergeSemantics) surface.semantics(mergeDescendants = true) { } else surface,
        horizontalArrangement = Arrangement.spacedBy(MonarchStatusBadgeTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            when {
                icon != null -> Box(Modifier.clearAndSetSemantics { }) { icon() }
                variant == MonarchStatusBadgeVariant.Dot && showIcon -> {
                    val dot = MonarchStatusBadgeTokens.DotSize * LocalDensity.current.fontScale
                    val color = MonarchStatusBadgeTokens.dotColor(definition.tone, themeColors)
                    Canvas(Modifier.size(dot)) { drawCircle(color) }
                }
            }
            Text(
                text = definition.label,
                style = MonarchStatusBadgeTokens.textStyle(size, MonarchTheme.typography),
                color = colors.content,
            )
        }
    }
}

/** The definition of [status]: this badge's registry, then the provided one, then the built-in set. */
@Composable
private fun resolveStatus(
    status: String?,
    registry: MonarchStatusRegistry?,
    provided: MonarchStatusRegistry,
): MonarchStatusDefinition {
    val found = registry?.resolve(status) ?: provided.resolve(status)
    val key = status ?: MonarchStatusBadgeTokens.UNKNOWN
    val builtIn = MonarchStatusBadgeTokens.BuiltIn[key]
    val unknown = MonarchStatusBadgeTokens.BuiltIn.getValue(MonarchStatusBadgeTokens.UNKNOWN)
    val unknownLabel = stringResource(unknown.label)
    return when {
        found != null -> found
        builtIn != null -> MonarchStatusDefinition(stringResource(builtIn.label), builtIn.tone)
        else -> MonarchStatusDefinition(MonarchStatusBadgeTokens.formatLabel(key) ?: unknownLabel, MonarchTone.Neutral)
    }
}

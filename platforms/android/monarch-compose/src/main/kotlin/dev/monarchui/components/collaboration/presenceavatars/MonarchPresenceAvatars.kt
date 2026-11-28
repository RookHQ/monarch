package dev.monarchui.components.collaboration.presenceavatars

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.listrow.MonarchListRow
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarGroup
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.identity.presencedot.MonarchPresenceDotTokens
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchPresenceName
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Who is here now: an overlapping stack of avatars with presence dots and a
 * "+N" tile, opening a bottom sheet that lists everyone with their presence
 * and activity.
 *
 * Built by Monarch from `MonarchAvatarGroup`, `MonarchAvatar` and a
 * `MonarchBottomSheet` with `MonarchListRow` rows. Overlapping 24dp avatars cannot each
 * be a 48dp touch target, so when some people are hidden or [onPersonClick] is
 * given, the whole stack is one button that opens the list, and people are
 * chosen in the list. Otherwise the stack is a group whose avatars are read
 * one by one. Presence is always spoken, so it never depends on the dot
 * colour. Nothing is drawn while [people] is empty.
 *
 * @param people who is here, most relevant first.
 * @param modifier the modifier of the stack.
 * @param maxVisible show at most this many avatars, then "+N". With exactly one too many, all are shown.
 * @param size the size of the avatars in the stack.
 * @param onPersonClick makes every person in the list a button, for example to jump to their cursor.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the interaction source of the stack when it opens the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MonarchPresenceAvatars(
    people: List<MonarchPresencePerson>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 4,
    size: MonarchAvatarSize = MonarchAvatarSize.Sm,
    onPersonClick: ((MonarchPresencePerson) -> Unit)? = null,
    labels: MonarchPresenceAvatarsLabels = MonarchPresenceAvatarsLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    if (people.isEmpty()) return
    val tokens = MonarchPresenceAvatarsTokens
    var open by rememberSaveable { mutableStateOf(false) }
    val opensList = tokens.opensList(people.size, maxVisible, onPersonClick != null)
    val texts = people.map { personText(it, labels) }
    val stackLabel = labels.label?.invoke(people.size)
        ?: pluralStringResource(R.plurals.monarch_presenceavatars_label, people.size, people.size)
    val visible = tokens.visibleCount(people.size, maxVisible)
    val hidden = people.size - visible
    val moreText = if (hidden > 0) {
        labels.more?.invoke(hidden) ?: pluralStringResource(R.plurals.monarch_avatar_more, hidden, hidden)
    } else {
        null
    }
    val summary = stringResource(
        R.string.monarch_presenceavatars_summary,
        stackLabel,
        (texts.take(visible) + listOfNotNull(moreText))
            .joinToString(stringResource(R.string.monarch_presenceavatars_summary_separator)),
    )
    val showAll = monarchString(labels.showAll, R.string.monarch_presenceavatars_show_all)
    val shape = MonarchShapeDefaults.Mark
    val stackModifier = if (opensList) {
        modifier
            .monarchMinimumTouchTarget()
            .clip(shape)
            .monarchFocusRing(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = showAll,
            ) { open = true }
            .clearAndSetSemantics {
                contentDescription = summary
                role = Role.Button
                onClick(label = showAll) {
                    open = true
                    true
                }
            }
    } else {
        modifier
    }
    Box(modifier = stackModifier, contentAlignment = Alignment.Center) {
        MonarchAvatarGroup(
            items = people.indices.toList(),
            label = stackLabel,
            maxVisible = maxVisible.coerceAtLeast(0),
            size = size,
            overflowLabel = labels.more,
        ) { index ->
            val person = people[index]
            Box(
                modifier = if (opensList) {
                    Modifier
                } else {
                    Modifier.clearAndSetSemantics {
                        contentDescription = texts[index]
                        role = Role.Image
                    }
                },
            ) {
                MonarchAvatar(
                    image = person.image,
                    name = person.name,
                    size = size,
                    status = person.status,
                    seed = person.seed ?: person.id,
                    decorative = true,
                )
            }
        }
    }
    if (open) {
        PresenceList(
            people = people,
            texts = texts,
            labels = labels,
            onPersonClick = onPersonClick,
            onClose = { open = false },
        )
    }
}

@Composable
private fun PresenceList(
    people: List<MonarchPresencePerson>,
    texts: List<String>,
    labels: MonarchPresenceAvatarsLabels,
    onPersonClick: ((MonarchPresencePerson) -> Unit)?,
    onClose: () -> Unit,
) {
    val tokens = MonarchPresenceAvatarsTokens
    // The header row has its own Done button, so the sheet has no title of its own; the rows bring their padding.
    MonarchBottomSheet(onDismissRequest = onClose, fullWidthContent = true) {
        val sheet = this
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.ListPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = monarchString(labels.list, R.string.monarch_presenceavatars_list),
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MonarchTheme.typography.heading,
                color = MonarchTheme.colors.textDefault,
            )
            MonarchButton(
                text = monarchString(labels.done, R.string.monarch_presenceavatars_done),
                onClick = { sheet.dismiss() },
                variant = MonarchButtonVariant.Ghost,
            )
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(people.size, key = { people[it].id }) { index ->
                val person = people[index]
                PresenceRow(
                    person = person,
                    spoken = texts[index],
                    onClick = onPersonClick?.let { click ->
                        {
                            onClose()
                            click(person)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PresenceRow(person: MonarchPresencePerson, spoken: String, onClick: (() -> Unit)?) {
    val presence = presenceText(person)
    val activity = MonarchPresenceAvatarsTokens.activityText(person.activity)
    val detail = if (activity == null) {
        presence
    } else {
        stringResource(R.string.monarch_presencedot_with_status, presence, activity)
    }
    MonarchListRow(
        headlineContent = { Text(text = person.name, style = MonarchTheme.typography.bodyStrong) },
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = spoken
            if (onClick != null) {
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
            }
        },
        onClick = onClick,
        supportingContent = { Text(text = detail, style = MonarchTheme.typography.caption) },
        leadingContent = {
            MonarchAvatar(
                image = person.image,
                name = person.name,
                size = MonarchAvatarSize.Lg,
                status = person.status,
                seed = person.seed ?: person.id,
                decorative = true,
            )
        },
    )
}

/** The presence word with the custom status: "Online · In a meeting". */
@Composable
private fun presenceText(person: MonarchPresencePerson): String {
    val word = monarchPresenceName(person.status)
    val custom = MonarchPresenceDotTokens.customText(person.customStatus) ?: return word
    return stringResource(R.string.monarch_presencedot_with_status, word, custom)
}

/** "Ana Silva, Online, Editing", from the labels or the built-in strings. */
@Composable
private fun personText(person: MonarchPresencePerson, labels: MonarchPresenceAvatarsLabels): String {
    val presence = presenceText(person)
    val activity = MonarchPresenceAvatarsTokens.activityText(person.activity)
    labels.person?.let { return it(person.name, presence, activity) }
    return if (activity == null) {
        stringResource(R.string.monarch_presenceavatars_person, person.name, presence)
    } else {
        stringResource(R.string.monarch_presenceavatars_person_with_activity, person.name, presence, activity)
    }
}

package dev.monarchui.components.intelligence.aisharedsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.listrow.MonarchListRow
import dev.monarchui.components.datadisplay.tag.MonarchChip
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.theme.MonarchTheme

/** The participant list in a `MonarchBottomSheet`: people (you first), then agents. */
@Composable
internal fun MonarchAiSharedSessionList(model: MonarchAiSharedSessionListModel, onClose: () -> Unit) {
    val tokens = MonarchAiSharedSessionTokens
    val texts = model.texts
    MonarchBottomSheet(onDismissRequest = onClose, fullyExpanded = false, fullWidthContent = true) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.ListPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = texts.listTitle,
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MonarchTheme.typography.heading,
                color = MonarchTheme.colors.textDefault,
            )
            MonarchButton(text = texts.done, onClick = onClose, variant = MonarchButtonVariant.Ghost)
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            group(texts.people, model.groups.people, model)
            group(texts.agents, model.groups.agents, model)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.group(
    title: String,
    members: List<MonarchAiSharedSessionParticipant>,
    model: MonarchAiSharedSessionListModel,
) {
    if (members.isEmpty()) return
    item(key = "heading-$title") {
        Text(
            text = title,
            modifier = Modifier
                .padding(
                    horizontal = MonarchAiSharedSessionTokens.ListPadding,
                    vertical = MonarchAiSharedSessionTokens.Gap,
                )
                .semantics { heading() },
            style = MonarchTheme.typography.caption,
            color = MonarchTheme.colors.textMuted,
        )
    }
    items(members, key = { it.id }) { participant -> ParticipantRow(participant, model) }
}

@Composable
private fun ParticipantRow(participant: MonarchAiSharedSessionParticipant, model: MonarchAiSharedSessionListModel) {
    val texts = model.texts
    val answering = participant.id == model.answeringId
    val doing = if (answering) model.answeringText else participant.activity?.let { texts.activity(it) }
    val details = texts.details(participant, doing).joinToString(texts.separator)
    val onFollowingChange = model.onFollowingChange
    val canFollow = participant.kind == MonarchAiSharedSessionKind.Person && !participant.you
    MonarchListRow(
        headlineContent = { Text(text = texts.displayName(participant)) },
        modifier = Modifier.semantics(mergeDescendants = true) {},
        supportingContent = { Text(text = details) },
        leadingContent = { MonarchAiSharedSessionFace(participant, answering, MonarchAvatarSize.Md) },
        trailingContent = if (canFollow && onFollowingChange != null) {
            {
                val followed = participant.id in model.following
                val name = texts.followLabel(participant.name)
                MonarchChip(
                    text = texts.follow,
                    selected = followed,
                    onSelectedChange = {
                        val id = participant.id
                        onFollowingChange(if (followed) model.following - id else model.following + id)
                    },
                    modifier = Modifier.semantics { contentDescription = name },
                )
            }
        } else {
            null
        },
    )
}

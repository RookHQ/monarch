package dev.monarchui.components.intelligence.aiconversationheader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.forms.inlineedittext.MonarchInlineEditText
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.intelligence.aiagentavatar.MonarchAiAgentAvatar
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The top bar of an AI conversation: its title (often named automatically
 * from the first prompt) with rename, the assistant and the model in use,
 * and the conversation's actions.
 *
 * Built by Monarch as a bar for a `Scaffold`'s `topBar` from a `Row` on the
 * surface colour, a heading, `MonarchAiAgentAvatar` and the app's slots. A
 * Material 3 `TopAppBar` has a fixed height that cuts a second line and a
 * model picker at large font sizes, so this bar is at least 64dp tall and
 * grows. Tapping the title (when [onRename] is set) swaps it for
 * `MonarchInlineEditText`'s editor with Save and Cancel; while [onRename]
 * runs Save shows progress, and when it throws the editor stays open with
 * "Couldn't rename. Try again.". An empty or unchanged title closes the
 * editor without calling it. While [naming] the title says "Naming
 * conversation…"; the title is a polite live region, so the generated name
 * is announced when it arrives.
 *
 * @param title the conversation's title. Empty shows "New conversation".
 * @param modifier the modifier of the bar.
 * @param agent the assistant answering, shown with its avatar under the title.
 * @param agentImage the assistant's picture, loaded by the app. `null` shows its initials.
 * @param naming the title is being generated: shows a placeholder and offers no rename.
 * @param onRename makes the title renamable; called with the trimmed new title. Throw to keep the editor open.
 * @param navigationIcon the back or menu button at the start, such as an `IconButton`.
 * @param renameIcon an icon after the title that shows it can be renamed, such as a pencil. Decorative.
 * @param model the model in use, such as a `MonarchAiModelPicker` or a short text, after the assistant.
 * @param labels replacements for the built-in strings.
 * @param actions the conversation's actions at the end, such as Share and a More menu of `IconButton`s.
 */
@ExperimentalMonarchApi
@Composable
@Suppress("LongParameterList")
public fun MonarchAiConversationHeader(
    title: String,
    modifier: Modifier = Modifier,
    agent: MonarchAiAgentRef? = null,
    agentImage: Painter? = null,
    naming: Boolean = false,
    onRename: (suspend (String) -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    renameIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Pencil,
    model: (@Composable () -> Unit)? = null,
    labels: MonarchAiConversationHeaderLabels = MonarchAiConversationHeaderLabels(),
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MonarchAiConversationHeaderTokens
    val colors = MonarchTheme.colors
    val border = tokens.border(colors)
    val actionsName = monarchString(labels.actions, R.string.monarch_aiconversationheader_actions)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.container(colors))
            .drawBehind {
                val width = tokens.BorderWidth.toPx()
                val y = size.height - width / 2
                drawLine(border, Offset(0f, y), Offset(size.width, y), width)
            }
            .heightIn(min = tokens.MinHeight)
            .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.textDefault) {
            navigationIcon?.invoke()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.InnerGap)) {
                AiConversationHeaderTitle(title, naming, onRename, renameIcon, labels)
                if (agent != null || model != null) AiConversationHeaderMeta(agent, agentImage, model, labels)
            }
            if (actions != null) {
                Row(
                    modifier = Modifier.semantics {
                        isTraversalGroup = true
                        contentDescription = actionsName
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

/** The title, or the rename editor while renaming. */
@Composable
private fun AiConversationHeaderTitle(
    title: String,
    naming: Boolean,
    onRename: (suspend (String) -> Unit)?,
    renameIcon: (@Composable () -> Unit)?,
    labels: MonarchAiConversationHeaderLabels,
) {
    val tokens = MonarchAiConversationHeaderTokens
    var editing by rememberSaveable { mutableStateOf(false) }
    val renamable = tokens.renamable(onRename != null, naming)
    if (editing && renamable && onRename != null) {
        val failed = monarchString(labels.renameFailed, R.string.monarch_aiconversationheader_rename_failed)
        MonarchInlineEditText(
            value = title,
            onSave = { next -> if (next.isNotBlank()) onRename(next) },
            label = monarchString(labels.titleLabel, R.string.monarch_aiconversationheader_title_label),
            modifier = Modifier.fillMaxWidth(),
            startEditing = true,
            errorMessage = { failed },
            onEditingChange = { open -> if (!open) editing = false },
            labels = labels.inlineEdit,
        )
        return
    }
    val untitled = monarchString(labels.untitled, R.string.monarch_aiconversationheader_untitled)
    val namingText = monarchString(labels.naming, R.string.monarch_aiconversationheader_naming)
    val shown = if (naming) namingText else tokens.shownTitle(title, untitled)
    val placeholder = naming || title.isBlank()
    val renameLabel = monarchString(labels.rename, R.string.monarch_aiconversationheader_rename)
    val action = if (renamable) {
        Modifier
            .monarchMinimumTouchTarget()
            .clickable(onClickLabel = renameLabel, role = Role.Button) { editing = true }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .then(action)
            .semantics(mergeDescendants = true) {
                heading()
                liveRegion = LiveRegionMode.Polite
                if (naming) stateDescription = namingText
            },
        horizontalArrangement = Arrangement.spacedBy(MonarchAiConversationHeaderTokens.InnerGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = shown,
            modifier = Modifier.weight(1f, fill = false),
            style = MonarchTheme.typography.subheading,
            color = tokens.titleColor(placeholder, MonarchTheme.colors),
        )
        if (renamable && renameIcon != null) renameIcon()
    }
}

/** The assistant and the model under the title. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiConversationHeaderMeta(
    agent: MonarchAiAgentRef?,
    agentImage: Painter?,
    model: (@Composable () -> Unit)?,
    labels: MonarchAiConversationHeaderLabels,
) {
    val tokens = MonarchAiConversationHeaderTokens
    val metaColor = tokens.metaColor(MonarchTheme.colors)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalArrangement = Arrangement.spacedBy(tokens.InnerGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (agent != null) {
            val locale = LocalConfiguration.current.locales[0]
            val format = stringResource(R.string.monarch_aiconversationheader_agent)
            val spoken = labels.agent?.invoke(agent.name) ?: String.format(locale, format, agent.name)
            Row(
                modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (agentImage != null) {
                    MonarchAiAgentAvatar(
                        agent = agent,
                        image = agentImage,
                        size = MonarchAvatarSize.Sm,
                        decorative = true,
                    )
                } else {
                    // Without a picture the web shows the assistant mark, not the initials.
                    MonarchDefaultIcons.Toned(MonarchTone.Ai, MonarchDefaultIcons.Bot)
                }
                Text(text = agent.name, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.aiStrong)
            }
        }
        if (model != null) {
            CompositionLocalProvider(LocalContentColor provides metaColor) { model() }
        }
    }
}

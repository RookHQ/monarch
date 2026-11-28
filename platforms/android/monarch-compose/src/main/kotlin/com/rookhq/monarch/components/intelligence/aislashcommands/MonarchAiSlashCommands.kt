package com.rookhq.monarch.components.intelligence.aislashcommands

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.rookhq.monarch.R
import com.rookhq.monarch.components.collaboration.slashmenu.MonarchSlashMenuLabels
import com.rookhq.monarch.components.collaboration.slashmenu.MonarchSlashMenuQuery
import com.rookhq.monarch.components.collaboration.slashmenu.MonarchSlashMenuStrip
import com.rookhq.monarch.components.collaboration.slashmenu.MonarchSlashMenuTriggerPosition
import com.rookhq.monarch.internal.monarchString

/**
 * The / command menu of an AI prompt: typing a slash at the start of a word
 * opens a list of commands such as /summarize, filtered by what follows,
 * and picking one replaces the typed command with the text the app returns.
 *
 * A preset of `MonarchSlashMenu`, so it is a strip of Material 3 `ListItem`
 * rows that the app places directly above its prompt field (Compose gives no
 * caret position to anchor a popup to), with focus kept in the field. The
 * menu finds the command in [value] with `MonarchSlashMenuQuery.find`, ranks
 * the commands (names that start with the text, then names that contain it,
 * then descriptions and keywords) and shows each as "/name" with its
 * description. Nothing is drawn while no command is being typed. The number
 * of matches, or that nothing matches, is announced through a polite live
 * region.
 *
 * @param value the prompt field's text and caret.
 * @param onValueChange called with the new text and caret after a command is picked.
 * @param commands every command, in the app's order.
 * @param onSelect called with the picked command. Return the text to put in place of the typed command, such as
 * "/summarize " to keep it in the prompt, or `null` to remove it (for a command that runs at once).
 * @param modifier the modifier of the strip.
 * @param trigger the character that starts a command.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAiSlashCommands(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    commands: List<MonarchAiSlashCommand>,
    onSelect: (MonarchAiSlashCommand) -> String?,
    modifier: Modifier = Modifier,
    trigger: Char = '/',
    labels: MonarchAiSlashCommandsLabels = MonarchAiSlashCommandsLabels(),
) {
    val query = MonarchSlashMenuQuery.find(
        value,
        trigger = trigger,
        position = MonarchSlashMenuTriggerPosition.WordStart,
    )
    val tokens = MonarchAiSlashCommandsTokens
    val matches = remember(commands, query?.text) { tokens.filter(commands, query?.text.orEmpty()) }
    val items = remember(matches, trigger) { matches.map { tokens.item(it, trigger) } }
    val locale = LocalConfiguration.current.locales[0]
    val resources = LocalResources.current
    val emptyFormat = stringResource(R.string.monarch_aislashcommands_empty)
    MonarchSlashMenuStrip(
        query = query,
        items = items,
        onSelect = { item ->
            val command = matches.firstOrNull { it.id == item.id }
            if (command != null && command.enabled && query != null) {
                onValueChange(query.replacing(value, onSelect(command).orEmpty()))
            }
        },
        modifier = modifier,
        groupOrder = emptyList(),
        assistant = true,
        labels = MonarchSlashMenuLabels(
            label = monarchString(labels.label, R.string.monarch_aislashcommands_label),
            empty = labels.empty ?: { typed -> String.format(locale, emptyFormat, typed) },
            count = labels.count
                ?: { n -> resources.getQuantityString(R.plurals.monarch_aislashcommands_count, n, n) },
        ),
    )
}

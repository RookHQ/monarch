package dev.monarchui.components.intelligence.aislashcommands

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of `MonarchAiSlashCommands`. Every
 * `null` keeps the built-in text.
 *
 * @property label the name of the list for TalkBack. Default "Commands".
 * @property empty shown when nothing matches; receives what was typed, with the slash.
 * @property count announced as the list changes; receives the number of commands. Default "3 commands".
 */
@Immutable
public class MonarchAiSlashCommandsLabels(
    public val label: String? = null,
    public val empty: ((typed: String) -> String)? = null,
    public val count: ((count: Int) -> String)? = null,
)

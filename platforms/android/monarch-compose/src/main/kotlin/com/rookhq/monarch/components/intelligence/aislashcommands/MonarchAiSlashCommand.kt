package com.rookhq.monarch.components.intelligence.aislashcommands

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One command of `MonarchAiSlashCommands`.
 *
 * @property id the stable id.
 * @property name what is typed after the slash, such as "summarize". A leading slash is ignored.
 * @property description what the command does, shown under the name.
 * @property icon a decorative icon before the name.
 * @property keywords more words the filter matches, such as "tl;dr" for summarize.
 * @property enabled whether the command can be picked. A command that cannot stays listed.
 */
@Immutable
public class MonarchAiSlashCommand(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val icon: Painter? = null,
    public val keywords: List<String> = emptyList(),
    public val enabled: Boolean = true,
)

package dev.monarchui.components.intelligence.aislashcommands

import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenuItem
import java.util.Locale

/** The pure logic of MonarchAiSlashCommands. */
internal object MonarchAiSlashCommandsTokens {
    private const val RANK_NAME_START = 0
    private const val RANK_NAME = 1
    private const val RANK_OTHER = 2

    /** The name without leading slashes. */
    fun bare(name: String): String = name.trimStart('/')

    /**
     * The commands matching [query]: names that start with it first, then names that contain it, then matches in
     * the description or keywords, each in the app's order. Ignores case; a blank query keeps every command.
     */
    fun filter(commands: List<MonarchAiSlashCommand>, query: String): List<MonarchAiSlashCommand> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return commands
        return commands
            .mapNotNull { command ->
                val name = bare(command.name).lowercase(Locale.ROOT)
                val rest = (listOfNotNull(command.description) + command.keywords).joinToString(" ")
                    .lowercase(Locale.ROOT)
                val rank = when {
                    name.startsWith(q) -> RANK_NAME_START
                    q in name -> RANK_NAME
                    q in rest -> RANK_OTHER
                    else -> null
                }
                rank?.let { it to command }
            }
            .sortedBy { it.first }
            .map { it.second }
    }

    /** A command as an entry of the slash menu, written with [trigger] before its name. */
    fun item(command: MonarchAiSlashCommand, trigger: Char): MonarchSlashMenuItem = MonarchSlashMenuItem(
        id = command.id,
        label = "$trigger${bare(command.name)}",
        description = command.description,
        icon = command.icon,
        keywords = command.keywords,
        enabled = command.enabled,
    )
}

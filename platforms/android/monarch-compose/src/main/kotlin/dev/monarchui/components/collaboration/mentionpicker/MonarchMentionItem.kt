package dev.monarchui.components.collaboration.mentionpicker

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One suggestion of a `MonarchMentionPicker`, from the app's search.
 *
 * @property id a stable id written into the token, such as a user or channel id.
 * @property label the display name, shown in the list and written into the token.
 * @property kind a person or a channel.
 * @property detail a second line: an email address, a role, a member count.
 * @property image the avatar picture of a person, loaded by the app. `null` shows initials.
 * @property enabled `false` shows the suggestion but it cannot be chosen, for example someone without
 * access to the conversation.
 */
@Immutable
public class MonarchMentionItem(
    public val id: String,
    public val label: String,
    public val kind: MonarchMentionKind = MonarchMentionKind.Person,
    public val detail: String? = null,
    public val image: Painter? = null,
    public val enabled: Boolean = true,
) {
    /**
     * The token for this mention, in the web's format: `@[Ana Lopez](u_123)`.
     * Brackets and parentheses are dropped from the label so the token always
     * parses back.
     *
     * @param trigger the trigger character, `@` for people and `#` for channels.
     */
    public fun token(trigger: Char = '@'): String = "$trigger[${label.replace(TokenUnsafe, "")}]($id)"

    private companion object {
        val TokenUnsafe = Regex("[\\[\\]()]")
    }
}

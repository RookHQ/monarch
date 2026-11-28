package dev.monarchui.components.typography.kbd

import androidx.compose.runtime.Immutable

/**
 * Replacements for the names TalkBack reads for the key glyphs of a
 * `MonarchKbd`. Every `null` keeps the built-in string
 * `monarch_kbd_<key>`.
 *
 * @property command the name of ⌘, "Command".
 * @property shift the name of ⇧, "Shift".
 * @property option the name of ⌥, "Option".
 * @property control the name of ⌃, "Control".
 * @property returnKey the name of ↩, "Return".
 * @property escape the name of ⎋, "Escape".
 * @property delete the name of ⌫, "Delete".
 * @property tab the name of ⇥, "Tab".
 * @property space the name of ␣, "Space".
 * @property capsLock the name of ⇪, "Caps Lock".
 * @property upArrow the name of ↑, "Up Arrow".
 * @property downArrow the name of ↓, "Down Arrow".
 * @property leftArrow the name of ←, "Left Arrow".
 * @property rightArrow the name of →, "Right Arrow".
 */
@Immutable
public class MonarchKbdLabels(
    public val command: String? = null,
    public val shift: String? = null,
    public val option: String? = null,
    public val control: String? = null,
    public val returnKey: String? = null,
    public val escape: String? = null,
    public val delete: String? = null,
    public val tab: String? = null,
    public val space: String? = null,
    public val capsLock: String? = null,
    public val upArrow: String? = null,
    public val downArrow: String? = null,
    public val leftArrow: String? = null,
    public val rightArrow: String? = null,
)

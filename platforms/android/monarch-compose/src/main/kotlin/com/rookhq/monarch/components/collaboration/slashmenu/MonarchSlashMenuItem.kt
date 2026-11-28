package com.rookhq.monarch.components.collaboration.slashmenu

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One entry of a `MonarchSlashMenu`, such as "Heading" or "Checklist".
 *
 * @property id a stable id, the list key; the app maps it to what it inserts.
 * @property label the name shown in the menu and read first.
 * @property description what the entry inserts or does, shown under the label and read with it.
 * @property icon a decorative icon before the label, loaded by the app. Monarch bundles no icons.
 * @property group the heading the entry sits under, such as "Text" or "Lists". Entries without one form an
 * unlabelled group.
 * @property keywords extra words the filter matches, such as "h1" or "todo".
 * @property enabled `false` shows the entry but it cannot be chosen, for example one that needs a selection.
 */
@Immutable
public class MonarchSlashMenuItem(
    public val id: String,
    public val label: String,
    public val description: String? = null,
    public val icon: Painter? = null,
    public val group: String? = null,
    public val keywords: List<String> = emptyList(),
    public val enabled: Boolean = true,
) {
    override fun toString(): String = "MonarchSlashMenuItem($id)"
}

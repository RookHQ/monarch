package dev.monarchui.components.identity.workspaceswitcher

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One workspace a `MonarchWorkspaceSwitcher` offers.
 *
 * @property id the stable id.
 * @property name the workspace's name, already localised.
 * @property handle the short handle shown as `@handle` and matched by search.
 * @property logo the workspace's logo, loaded by the app. `null` shows its initial on a tinted tile.
 * @property keywords extra words that search matches but never shows, such as old names.
 */
@Immutable
public class MonarchWorkspaceOption(
    public val id: String,
    public val name: String,
    public val handle: String? = null,
    public val logo: Painter? = null,
    public val keywords: String? = null,
)

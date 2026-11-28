package dev.monarchui.components.intelligence.aipermissionscope

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One app of a `MonarchAiPermissionScope` and what the AI may do in it.
 *
 * @property app the app's display name, for example "Calendar". Also the row's identity, so keep it unique.
 * @property access what the AI may do in the app.
 * @property resources which part of the app the access covers, for example "Shared projects only". Empty text
 * is not shown or read.
 * @property icon the app's icon, loaded by the app. Shown with `MonarchFavicon` and hidden from TalkBack.
 * `null` shows no icon.
 */
@Immutable
public data class MonarchAiPermissionScopeItem(
    public val app: String,
    public val access: MonarchAiAccessLevel,
    public val resources: String? = null,
    public val icon: Painter? = null,
)

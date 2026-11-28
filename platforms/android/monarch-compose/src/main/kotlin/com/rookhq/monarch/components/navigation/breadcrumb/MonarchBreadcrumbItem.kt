package com.rookhq.monarch.components.navigation.breadcrumb

import androidx.compose.runtime.Immutable

/**
 * One level of a `MonarchBreadcrumb`.
 *
 * @property title the level's name, already localised: "Projects".
 * @property id identifies the level to the app, for example a route or a folder id. The title by default.
 * @property linked whether the level has a screen to go to. A level that is not linked is shown as muted
 * text and is disabled in the collapsed menu. The last level is never a link.
 */
@Immutable
public class MonarchBreadcrumbItem(
    public val title: String,
    public val id: String = title,
    public val linked: Boolean = true,
)

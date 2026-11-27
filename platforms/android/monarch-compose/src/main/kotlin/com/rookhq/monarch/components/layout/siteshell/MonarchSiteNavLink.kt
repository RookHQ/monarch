package com.rookhq.monarch.components.layout.siteshell

import androidx.compose.runtime.Immutable

/**
 * A destination in the navigation or the footer of a [MonarchSiteShell].
 *
 * @property label the name of the destination.
 * @property link an app-defined destination, passed to `onNavigate`.
 * @property description a muted line under the label, shown in the navigation drawer.
 */
@Immutable
public class MonarchSiteNavLink(
    public val label: String,
    public val link: String,
    public val description: String? = null,
)

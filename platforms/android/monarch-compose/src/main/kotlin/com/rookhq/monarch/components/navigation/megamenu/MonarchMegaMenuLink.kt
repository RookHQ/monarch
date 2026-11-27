package com.rookhq.monarch.components.navigation.megamenu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One destination of a [MonarchMegaMenuSection].
 *
 * @property label the name of the destination.
 * @property link an app-defined destination, passed to `onNavigate` when the user taps the row.
 * @property description a muted line under the label.
 * @property badge a short accent badge beside the label, such as "New".
 * @property external marks a destination outside the app: an open-outside mark is drawn and TalkBack says so.
 * @property icon an icon in a tile before the label, for example a `MonarchIcon` with a `null` content description.
 */
@Immutable
public class MonarchMegaMenuLink(
    public val label: String,
    public val link: String,
    public val description: String? = null,
    public val badge: String? = null,
    public val external: Boolean = false,
    public val icon: (@Composable () -> Unit)? = null,
)

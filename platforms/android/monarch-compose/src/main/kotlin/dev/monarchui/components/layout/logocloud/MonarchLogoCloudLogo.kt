package dev.monarchui.components.layout.logocloud

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One organisation of a [MonarchLogoCloud].
 *
 * @property name the organisation's name: the wordmark while [image] is `null`, and what TalkBack reads.
 * @property image the logo, loaded by the app. `null` shows the name as a wordmark.
 * @property imageDark a logo for the dark themes, for a logo that disappears on a dark background.
 * @property link an app-defined destination; a logo with a link can be tapped.
 */
@Immutable
public class MonarchLogoCloudLogo(
    public val name: String,
    public val image: Painter? = null,
    public val imageDark: Painter? = null,
    public val link: String? = null,
)

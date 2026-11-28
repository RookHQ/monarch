package dev.monarchui.testing

import androidx.compose.ui.graphics.vector.ImageVector
import dev.monarchui.sampleicons.SampleIcons

/**
 * A real icon (the Material star, outlined) standing in for an app icon in tests, previews and examples that do
 * not care which icon it is. Use `SampleIcons.<Name>` when the icon means something. Monarch bundles no icons.
 */
internal val TestIcon: ImageVector get() = SampleIcons.Placeholder

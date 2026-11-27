package com.rookhq.monarch.testing

import androidx.compose.ui.graphics.vector.ImageVector
import com.rookhq.monarch.sampleicons.SampleIcons

/**
 * A real icon (the Material star, outlined) standing in for an app icon in tests, previews and examples that do
 * not care which icon it is. Use `SampleIcons.<Name>` when the icon means something. Monarch bundles no icons.
 */
internal val TestIcon: ImageVector get() = SampleIcons.Placeholder

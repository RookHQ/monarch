package com.rookhq.monarch.components.overlays.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A hairline between groups of rows in a [MonarchMenu], in the colour of the
 * menu border, with 4dp above and below. Decorative: TalkBack skips it.
 *
 * @param modifier the modifier of the divider.
 */
@Composable
public fun MonarchMenuDivider(modifier: Modifier = Modifier) {
    val colour = MonarchMenuTokens.border(MonarchTheme.colors, MonarchTheme.isDark, MonarchTheme.isHighContrast)
    Box(
        modifier
            .padding(vertical = MonarchMenuTokens.Inset)
            .fillMaxWidth()
            .height(MonarchShapeDefaults.BorderWidth)
            .background(colour),
    )
}

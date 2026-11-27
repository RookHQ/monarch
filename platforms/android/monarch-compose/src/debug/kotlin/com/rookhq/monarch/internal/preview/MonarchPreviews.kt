package com.rookhq.monarch.internal.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchTheme

/** The three previews every Monarch component has: light, dark and the largest font size. */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large font", showBackground = true, fontScale = 2f)
internal annotation class MonarchPreviews

/** The frame of a component preview: the theme, the canvas colour and a padded column. */
@Composable
internal fun PreviewFrame(content: @Composable ColumnScope.() -> Unit) {
    MonarchTheme {
        Surface(color = MonarchTheme.colors.bgCanvas) {
            Column(
                modifier = Modifier.padding(MonarchTheme.spacing.space4),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                content = content,
            )
        }
    }
}

/** A plain square, standing in for an app icon in previews. Monarch does not bundle icons. */
internal val PreviewIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PreviewIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 4f)
            lineTo(20f, 4f)
            lineTo(20f, 20f)
            lineTo(4f, 20f)
            close()
        }
    }.build()
}

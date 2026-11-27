package com.rookhq.monarch.sample.sections.foundations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.monarchShadow
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchBrandTheme
import com.rookhq.monarch.tokens.MonarchTokens

/** The catalogue page of Theme. */
object MonarchThemeSection : CataloguePage {
    override val title = "Theme"

    @Composable
    override fun Content() {
        val colors = MonarchTheme.colors
        CatalogueCaption("Color roles: solid, strong, subtle")
        listOf(
            "Accent" to listOf(colors.accentSolid, colors.accentStrong, colors.accentSubtle),
            "Danger" to listOf(colors.dangerSolid, colors.dangerStrong, colors.dangerSubtle),
            "Success" to listOf(colors.successSolid, colors.successStrong, colors.successSubtle),
            "Warning" to listOf(colors.warningSolid, colors.warningStrong, colors.warningSubtle),
            "Info" to listOf(colors.infoSolid, colors.infoStrong, colors.infoSubtle),
            "Surface" to listOf(colors.bgCanvas, colors.bgSurface, colors.bgMuted),
        ).forEach { (label, swatches) -> SwatchRow(label, swatches) }
        CatalogueCaption("Custom AI color (withAi): solid, strong, subtle")
        SwatchRow("Monarch AI", listOf(colors.aiSolid, colors.aiStrong, colors.aiSubtle))
        val base = MonarchTokens.themes.first {
            it.isDark == MonarchTheme.isDark && it.isHighContrast == MonarchTheme.isHighContrast
        }
        val customAi = remember { MonarchBrandTheme.derive("#0a7cff", ai = "#7c3aed").aiAccent }
        if (customAi != null) {
            MonarchTheme(tokens = base.withAi(customAi)) {
                val ai = MonarchTheme.colors
                SwatchRow("Custom AI", listOf(ai.aiSolid, ai.aiStrong, ai.aiSubtle))
            }
        }
        CatalogueCaption("Elevation: xs to xl")
        val shape = MonarchTheme.shapes.lg
        val elevation = MonarchTheme.elevation
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space6)) {
            listOf(elevation.xs, elevation.sm, elevation.md, elevation.lg, elevation.xl).forEach { shadow ->
                Box(
                    Modifier
                        .size(MonarchTheme.spacing.space10)
                        .monarchShadow(shadow, shape)
                        .background(colors.bgRaised, shape)
                        .clearAndSetSemantics { },
                )
            }
        }
        CatalogueCaption("Material 3 components inside the theme")
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            Button(onClick = {}) { Text("Button") }
            OutlinedButton(onClick = {}) { Text("Outlined") }
        }
    }

    @Composable
    private fun SwatchRow(label: String, swatches: List<Color>) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = label, style = MonarchTextStyle.Label, modifier = Modifier.weight(1f))
            swatches.forEach { color ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(MonarchTheme.spacing.space8)
                        .background(color, MonarchTheme.shapes.sm),
                )
            }
        }
    }
}

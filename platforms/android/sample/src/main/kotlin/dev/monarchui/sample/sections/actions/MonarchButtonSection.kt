package dev.monarchui.sample.sections.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Button. */
object MonarchButtonSection : CataloguePage {
    override val title = "Button"

    @Composable
    override fun Content() {
        var taps by rememberSaveable { mutableIntStateOf(0) }
        var loading by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Variants")
        ButtonRow {
            MonarchButtonVariant.entries.forEach { variant ->
                MonarchButton(text = variant.name, onClick = { taps++ }, variant = variant)
            }
        }
        CatalogueCaption("Sizes, with an icon")
        ButtonRow {
            MonarchButtonSize.entries.forEach { size ->
                MonarchButton(
                    text = "Size ${size.name}",
                    onClick = { taps++ },
                    variant = MonarchButtonVariant.Primary,
                    size = size,
                    leadingIcon = {
                        MonarchIcon(
                            imageVector = SampleIcons.Plus,
                            contentDescription = null,
                            size = MonarchIconSize.Sm,
                        )
                    },
                )
            }
        }
        CatalogueCaption("Disabled and loading")
        ButtonRow {
            MonarchButton(text = "Disabled", onClick = {}, variant = MonarchButtonVariant.Primary, enabled = false)
            MonarchButton(text = "Disabled", onClick = {}, enabled = false)
            MonarchButton(
                text = if (loading) "Saving" else "Tap to load",
                onClick = { loading = true },
                variant = MonarchButtonVariant.Primary,
                loading = loading,
            )
            MonarchButton(text = "Reset", onClick = { loading = false }, variant = MonarchButtonVariant.Ghost)
        }
        CatalogueCaption("Content slot, full width")
        MonarchButton(onClick = { taps++ }, modifier = Modifier.fillMaxWidth(), size = MonarchButtonSize.Lg) {
            MonarchIcon(imageVector = SampleIcons.Plus, contentDescription = null)
            MonarchText(text = "Add invoice")
        }
        MonarchText(text = "Taps: $taps", color = MonarchTheme.colors.textMuted)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ButtonRow(content: @Composable () -> Unit) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) { content() }
    }
}

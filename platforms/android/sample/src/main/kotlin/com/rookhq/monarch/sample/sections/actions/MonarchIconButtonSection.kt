package com.rookhq.monarch.sample.sections.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Icon button. */
object MonarchIconButtonSection : CataloguePage {
    override val title = "Icon button"

    @Composable
    private fun Icons(content: @Composable () -> Unit) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }

    @Composable
    private fun Add(label: String, variant: MonarchButtonVariant = MonarchButtonVariant.Ghost) {
        MonarchIconButton(label = label, onClick = {}, variant = variant) {
            MonarchIcon(imageVector = SampleIcons.Plus, contentDescription = null)
        }
    }

    @Composable
    override fun Content() {
        CatalogueCaption("Sizes")
        Icons {
            MonarchButtonSize.entries.forEach { size ->
                MonarchIconButton(label = "Add, ${size.name}", onClick = {}, size = size) {
                    MonarchIcon(imageVector = SampleIcons.Plus, contentDescription = null)
                }
            }
        }
        CatalogueCaption("Variants, disabled and loading")
        Icons {
            Add("Add, primary", MonarchButtonVariant.Primary)
            Add("Add, secondary", MonarchButtonVariant.Secondary)
            Add("Add, danger", MonarchButtonVariant.Danger)
            MonarchIconButton(label = "Add, disabled", onClick = {}, enabled = false) {
                MonarchIcon(imageVector = SampleIcons.Plus, contentDescription = null)
            }
            MonarchIconButton(label = "Add, loading", onClick = {}, loading = true) {
                MonarchIcon(imageVector = SampleIcons.Plus, contentDescription = null)
            }
        }
    }
}

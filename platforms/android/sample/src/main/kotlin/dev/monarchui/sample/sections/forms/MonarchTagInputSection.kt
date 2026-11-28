package dev.monarchui.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.taginput.MonarchTagInput
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of TagInput. */
object MonarchTagInputSection : CataloguePage {
    override val title = "TagInput"

    private const val MAX_SKILLS = 5

    @Composable
    override fun Content() {
        var skills by rememberSaveable { mutableStateOf(listOf("Research", "TypeScript")) }
        var recipients by rememberSaveable { mutableStateOf(listOf<String>()) }
        val close: @Composable () -> Unit = {
            MonarchIcon(
                imageVector = SampleIcons.Close,
                contentDescription = null,
                size = MonarchIconSize.Sm,
            )
        }
        CatalogueCaption("Comma or Done adds; at most five, with suggestions")
        MonarchTagInput(
            tags = skills,
            onTagsChange = { skills = it },
            label = "Skills",
            removeIcon = close,
            placeholder = "Add a skill",
            helperText = "Separate skills with commas.",
            suggestions = listOf("Accessibility", "Copywriting", "Research", "TypeScript"),
            maxCount = MAX_SKILLS,
            suggestionIcon = {
                MonarchIcon(imageVector = SampleIcons.Plus, contentDescription = null, size = MonarchIconSize.Sm)
            },
        )
        CatalogueCaption("Email addresses, split on commas, spaces and semicolons")
        MonarchTagInput(
            tags = recipients,
            onTagsChange = { recipients = it },
            label = "Recipients",
            removeIcon = close,
            placeholder = "name@example.com",
            separators = setOf(',', ' ', ';'),
            normalize = { it.trim().lowercase() },
            validate = { tag, _ -> if ('@' in tag) null else "Enter an email address like name@example.com." },
        )
        CatalogueCaption("Read only")
        MonarchTagInput(
            tags = listOf("Billing", "Priority"),
            onTagsChange = {},
            label = "Labels",
            removeIcon = close,
            readOnly = true,
        )
    }
}

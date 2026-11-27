package com.rookhq.monarch.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.forms.taginput.MonarchTagInput
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

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

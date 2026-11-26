package com.rookhq.monarch.sample.sections.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNoticeVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of InlineNotice. */
object MonarchInlineNoticeSection : CataloguePage {
    override val title = "InlineNotice"

    @Composable
    override fun Content() {
        MonarchInlineNoticeVariant.entries.forEach { variant ->
            CatalogueCaption(variant.name)
            MonarchTone.entries.forEach { tone ->
                MonarchInlineNotice(
                    title = "${tone.name}.",
                    message = "A notice in this tone.",
                    tone = tone,
                    variant = variant,
                    icon = {
                        MonarchIcon(SampleIcons.Info, contentDescription = null, size = MonarchIconSize.Sm)
                    },
                )
            }
        }
        CatalogueCaption("Action and dismiss")
        var shown by rememberSaveable { mutableStateOf(true) }
        if (shown) {
            MonarchInlineNotice(
                message = "Two people are editing this record.",
                tone = MonarchTone.Warning,
                action = {
                    MonarchButton(
                        text = "Reload",
                        onClick = {},
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                    )
                },
                onDismiss = { shown = false },
            )
        } else {
            MonarchButton(text = "Show the notice again", onClick = { shown = true }, size = MonarchButtonSize.Sm)
        }
    }
}

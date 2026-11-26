package com.rookhq.monarch.sample.sections.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.copyfield.MonarchCopyField
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of CopyField. */
object MonarchCopyFieldSection : CataloguePage {
    override val title = "CopyField"

    @Composable
    override fun Content() {
        var copies by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("A link with a text button")
        MonarchCopyField(value = "https://example.com/hooks/1042", label = "Webhook URL", onCopy = { copies++ })
        CatalogueCaption("A key with an icon button, sensitive")
        MonarchCopyField(
            value = "sk_live_example_1234",
            label = "API key",
            sensitive = true,
            copyIcon = { MonarchIcon(imageVector = SampleIcons.Copy, contentDescription = null) },
            onCopy = { copies++ },
        )
        CatalogueCaption("Multi-line")
        MonarchCopyField(
            value = "<iframe src=\"https://example.com/embed/7\" width=\"600\" height=\"400\"></iframe>",
            label = "Embed code",
            multiline = true,
            helperText = "Paste it into your page.",
            onCopy = { copies++ },
        )
        CatalogueCaption("Disabled")
        MonarchCopyField(value = "https://example.com/feed.ics", label = "Calendar feed", enabled = false)
        MonarchText(text = "Copies: $copies", color = MonarchTheme.colors.textMuted)
    }
}

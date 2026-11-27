package com.rookhq.monarch.sample.sections.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.copybutton.MonarchCopyButton
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of CopyButton. */
object MonarchCopyButtonSection : CataloguePage {
    override val title = "CopyButton"

    @Composable
    override fun Content() {
        var copies by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("With a label")
        MonarchCopyButton(value = "https://example.com/hooks/1042", target = "webhook URL", onCopy = { copies++ })
        CatalogueCaption("In-app confirmation forced on, primary")
        MonarchCopyButton(
            value = "https://example.com/invite/7",
            variant = MonarchButtonVariant.Primary,
            showConfirmation = true,
            target = "invitation link",
            leadingIcon = { MonarchIcon(imageVector = SampleIcons.Copy, contentDescription = null) },
            onCopy = { copies++ },
        )
        CatalogueCaption("Icon only, sensitive")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = "sk_live_••••1234", style = MonarchTextStyle.Code)
            MonarchCopyButton(
                value = "sk_live_example_1234",
                icon = { MonarchIcon(imageVector = SampleIcons.Copy, contentDescription = null) },
                sensitive = true,
                target = "API key",
                onCopy = { copies++ },
            )
        }
        MonarchText(text = "Copies: $copies", color = MonarchTheme.colors.textMuted)
    }
}

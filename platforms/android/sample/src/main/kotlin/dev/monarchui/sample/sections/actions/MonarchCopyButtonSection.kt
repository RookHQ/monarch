package dev.monarchui.sample.sections.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

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

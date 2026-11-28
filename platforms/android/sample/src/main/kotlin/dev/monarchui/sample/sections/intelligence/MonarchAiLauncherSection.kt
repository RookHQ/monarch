package dev.monarchui.sample.sections.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.intelligence.ailauncher.MonarchAiLauncher
import dev.monarchui.components.intelligence.ailauncher.MonarchAiLauncherBadge
import dev.monarchui.components.intelligence.ailauncher.MonarchAiLauncherVariant
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of AiLauncher. */
object MonarchAiLauncherSection : CataloguePage {
    override val title = "AiLauncher"

    private const val UNREAD = 3

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        val icon: @Composable () -> Unit = { MonarchIcon(SampleIcons.Bot, contentDescription = null) }
        CatalogueCaption("Solid, toggles open")
        MonarchAiLauncher(
            open = open,
            onOpenChange = { open = it },
            icon = icon,
            badge = MonarchAiLauncherBadge.Count(UNREAD),
        )
        CatalogueCaption("Badges")
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space6)) {
            MonarchAiLauncher(open = false, onOpenChange = {}, icon = icon, badge = MonarchAiLauncherBadge.Unread)
            MonarchAiLauncher(open = false, onOpenChange = {}, icon = icon, badge = MonarchAiLauncherBadge.Working)
        }
        CatalogueCaption("Subtle, for a toolbar")
        MonarchAiLauncher(
            open = open,
            onOpenChange = { open = it },
            icon = icon,
            variant = MonarchAiLauncherVariant.Subtle,
        )
    }
}

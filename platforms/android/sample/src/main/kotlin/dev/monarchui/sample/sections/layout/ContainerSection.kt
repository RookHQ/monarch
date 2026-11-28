package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/**
 * The catalogue page of Container: widthIn(max =) with gutters by window width.
 *
 * The catalogue app does not depend on the adaptive artifacts, so this page
 * reads the width from BoxWithConstraints; an app uses
 * currentWindowAdaptiveInfoV2() as the inventory example does.
 */
object ContainerSection : CataloguePage {
    override val title = "Container"

    /** The medium window size class starts at 600dp. */
    private val MediumWidth = 600.dp

    /** The web's narrow container: 44rem. */
    private val ReadingWidth = 704.dp

    @Composable
    override fun Content() {
        CatalogueCaption("Reading width, 704dp, with 16dp or 24dp gutters")
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val gutter = if (maxWidth >= MediumWidth) MonarchTheme.spacing.space6 else MonarchTheme.spacing.space4
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(MonarchTheme.spacing.px, MonarchTheme.colors.borderSubtle),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = ReadingWidth)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = gutter, vertical = MonarchTheme.spacing.space4),
                ) {
                    MonarchText(text = "Billing settings", style = MonarchTextStyle.Heading)
                    MonarchText(
                        text = "Change how and when your workspace is billed. On a wide window this text stops " +
                            "at the reading width instead of stretching across the screen.",
                    )
                }
            }
        }
    }
}

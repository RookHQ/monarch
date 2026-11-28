package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.layout.deviceframe.MonarchDeviceFrame
import dev.monarchui.components.layout.deviceframe.MonarchDeviceFrameDevice
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of DeviceFrame. */
object MonarchDeviceFrameSection : CataloguePage {
    override val title = "DeviceFrame"

    private val LINKS = listOf("Portfolio", "Newsletter", "Book a call", "Shop", "Podcast", "Contact")

    @Composable
    override fun Content() {
        CatalogueCaption("Phone, a preview read as one image")
        MonarchDeviceFrame(
            label = "Preview of your link page on a phone",
            modifier = Modifier.width(220.dp),
            statusBarTime = "9:41",
        ) {
            LinkPage()
        }

        CatalogueCaption("Phone, interactive")
        MonarchDeviceFrame(
            label = "Live preview of your link page",
            modifier = Modifier.width(220.dp),
            statusBarTime = "9:41",
            interactive = true,
        ) {
            LinkPage(modifier = Modifier.verticalScroll(rememberScrollState()))
        }

        CatalogueCaption("Tablet, flat")
        MonarchDeviceFrame(
            label = "Preview of your link page on a tablet",
            device = MonarchDeviceFrameDevice.Tablet,
            flat = true,
        ) {
            LinkPage()
        }
    }

    @Composable
    private fun LinkPage(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.padding(MonarchTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            MonarchText(text = "Ada Lovelace", style = MonarchTextStyle.BodyStrong)
            LINKS.forEach { link ->
                MonarchButton(
                    text = link,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    variant = MonarchButtonVariant.Secondary,
                )
            }
        }
    }
}

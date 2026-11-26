package com.rookhq.monarch.sample.sections.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.launch

/** The catalogue page of InfoTooltip: a RichTooltip opened by tapping the info button next to a label. */
object InfoTooltipSection : CataloguePage {
    override val title = "InfoTooltip"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val state = rememberTooltipState(isPersistent = true)
        val scope = rememberCoroutineScope()
        CatalogueCaption("Tap the info button")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = "Visibility score", style = MonarchTextStyle.Label)
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = {
                    RichTooltip(title = { Text("Visibility score") }) {
                        Text("How often your listing appeared in search results this month, from 0 to 100.")
                    }
                },
                state = state,
                focusable = true,
            ) {
                IconButton(onClick = { scope.launch { state.show() } }) {
                    Icon(
                        SampleIcons.Info,
                        contentDescription = "About visibility score",
                    )
                }
            }
        }
    }
}

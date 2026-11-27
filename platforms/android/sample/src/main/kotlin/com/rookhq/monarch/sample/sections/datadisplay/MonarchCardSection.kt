package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Card. */
object MonarchCardSection : CataloguePage {
    override val title = "Card"

    @Composable
    override fun Content() {
        var opened by rememberSaveable { mutableIntStateOf(0) }
        MonarchCard(modifier = Modifier.fillMaxWidth()) {
            MonarchText(text = "Quarterly report", style = MonarchTextStyle.Subheading, heading = false)
            MonarchText(text = "A static card.", color = MonarchTheme.colors.textMuted)
        }
        MonarchCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
            MonarchText(text = "Elevated", style = MonarchTextStyle.Subheading, heading = false)
            MonarchText(text = "Raised surface and shadow.", color = MonarchTheme.colors.textMuted)
        }
        MonarchCard(onClick = { opened++ }, modifier = Modifier.fillMaxWidth()) {
            MonarchText(text = "Tappable", style = MonarchTextStyle.Subheading, heading = false)
            MonarchText(text = "Opened $opened times.", color = MonarchTheme.colors.textMuted)
        }
    }
}

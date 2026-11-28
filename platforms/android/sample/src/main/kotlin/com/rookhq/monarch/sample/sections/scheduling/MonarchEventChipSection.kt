package com.rookhq.monarch.sample.sections.scheduling

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChip
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/** The catalogue page of EventChip. */
object MonarchEventChipSection : CataloguePage {
    override val title = "EventChip"

    @Composable
    override fun Content() {
        var opened by rememberSaveable { mutableIntStateOf(0) }
        val open = { opened += 1 }
        MonarchText(text = "Opened $opened times", style = MonarchTextStyle.Caption)
        CatalogueCaption("Dot")
        MonarchEventChipStatus.entries.forEach { status ->
            MonarchEventChip(title = "Stand-up", time = "09:30", status = status, onClick = open)
        }
        MonarchEventChip(title = "Retro", time = "16:00", past = true, onClick = open)
        CatalogueCaption("Bar")
        MonarchEventChipStatus.entries.forEach { status ->
            MonarchEventChip(
                title = "Design review",
                variant = MonarchEventChipVariant.Bar,
                status = status,
                color = MonarchTheme.colors.successSolid,
                time = "14:00 – 15:00",
                timeLabel = "2 to 3 in the afternoon",
                detail = "Room 4",
                onClick = open,
            )
        }
        CatalogueCaption("Block")
        MonarchEventChip(
            title = "Workshop",
            modifier = Modifier.height(MonarchTokens.Space.space24.dp),
            variant = MonarchEventChipVariant.Block,
            time = "10:00 – 12:00",
            detail = "Lab",
            onClick = open,
        )
        MonarchEventChip(
            title = "Check-in",
            modifier = Modifier.height(MonarchTokens.Space.space12.dp),
            variant = MonarchEventChipVariant.Block,
            compact = true,
            time = "12:00",
            onClick = open,
        )
    }
}

package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceDot
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceDotSize
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of PresenceDot. */
object MonarchPresenceDotSection : CataloguePage {
    override val title = "PresenceDot"

    @Composable
    override fun Content() {
        MonarchPresenceDotSize.entries.forEach { size ->
            CatalogueCaption(size.name)
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
                MonarchPresenceStatus.entries.forEach { MonarchPresenceDot(status = it, size = size) }
            }
        }
        CatalogueCaption("Beside a name")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchPresenceDot(status = MonarchPresenceStatus.Away, customStatus = "In a meeting")
            MonarchText(text = "Ada Lovelace")
        }
        CatalogueCaption("On an avatar")
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            MonarchPresenceStatus.entries.forEach { status ->
                Box(
                    modifier = Modifier
                        .size(MonarchTheme.spacing.space10)
                        .background(MonarchTheme.colors.bgEmphasis, CircleShape),
                ) {
                    MonarchPresenceDot(
                        status = status,
                        modifier = Modifier.align(Alignment.BottomEnd),
                        size = MonarchPresenceDotSize.Lg,
                        overlay = true,
                    )
                }
            }
        }
    }
}

package com.rookhq.monarch.sample.sections.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.marketinghero.MonarchMarketingHero
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingAlign
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of MarketingHero. The primary action counts its taps in the note. */
object MonarchMarketingHeroSection : CataloguePage {
    override val title = "MarketingHero"

    @Composable
    override fun Content() {
        var started by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Centered")
        MonarchMarketingHero(
            title = "Write it once. Find it whenever.",
            eyebrow = "Rook Example",
            lead = "Structured documents for teams whose decisions need to stay connected.",
            note = if (started == 0) "Set up in under five minutes." else "Started $started times.",
        ) {
            MonarchButton(
                text = "Start writing",
                onClick = { started++ },
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Lg,
                shape = MonarchTheme.shapes.full,
            )
            MonarchButton(
                text = "See how it works",
                onClick = {},
                size = MonarchButtonSize.Lg,
                shape = MonarchTheme.shapes.full,
            )
        }
        CatalogueCaption("Start aligned, without the wash")
        MonarchMarketingHero(
            title = "Plan the week together.",
            lead = "Shared calendars for small teams.",
            align = MonarchMarketingAlign.Start,
            wash = false,
        )
    }
}

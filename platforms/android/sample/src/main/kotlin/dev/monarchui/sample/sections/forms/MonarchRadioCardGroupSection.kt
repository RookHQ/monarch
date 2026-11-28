package dev.monarchui.sample.sections.forms

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardGroup
import dev.monarchui.components.forms.radiocardgroup.MonarchRadioCardOption
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of RadioCardGroup. */
object MonarchRadioCardGroupSection : CataloguePage {
    override val title = "RadioCardGroup"

    private const val COLUMNS = 3

    @Composable
    override fun Content() {
        var plan by rememberSaveable { mutableStateOf<String?>(null) }
        var speed by rememberSaveable { mutableStateOf<String?>("standard") }
        CatalogueCaption("Plans, with an icon and a disabled card")
        MonarchRadioCardGroup(
            options = listOf(
                MonarchRadioCardOption("starter", "Starter", description = "For one person.", meta = "1 seat"),
                MonarchRadioCardOption(
                    "team",
                    "Team",
                    description = "Shared projects and roles.",
                    meta = "Up to 50 seats",
                    icon = { Icon(imageVector = SampleIcons.Users, contentDescription = null) },
                ),
                MonarchRadioCardOption("enterprise", "Enterprise", description = "Talk to sales.", enabled = false),
            ),
            selected = plan,
            onSelect = { plan = it },
            label = "Plan",
        )
        CatalogueCaption("Three columns on a wide window")
        MonarchRadioCardGroup(
            options = listOf(
                MonarchRadioCardOption("standard", "Standard", description = "3 to 5 days"),
                MonarchRadioCardOption("express", "Express", description = "1 to 2 days"),
                MonarchRadioCardOption("pickup", "Pick up", description = "From a locker"),
            ),
            selected = speed,
            onSelect = { speed = it },
            label = "Delivery",
            columns = COLUMNS,
        )
    }
}

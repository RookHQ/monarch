package dev.monarchui.sample.sections.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of BackLink: the top app bar's navigation icon and BackHandler. */
object BackLinkSection : CataloguePage {
    override val title = "BackLink"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var taps by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("navigationIcon of a TopAppBar")
        TopAppBar(
            title = { Text("Domain settings") },
            navigationIcon = {
                IconButton(onClick = { taps++ }) { MonarchIcon(SampleIcons.ArrowBack, contentDescription = "Back") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MonarchTheme.colors.bgSurface),
        )
        MonarchText(text = "Back tapped $taps times")

        var intercept by rememberSaveable { mutableStateOf(false) }
        var intercepted by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("BackHandler: intercept system back while there are unsaved changes")
        MonarchSwitchRow(checked = intercept, onCheckedChange = { intercept = it }, label = "Unsaved changes")
        BackHandler(enabled = intercept) { intercepted++ }
        MonarchText(text = "System back intercepted $intercepted times")
    }
}

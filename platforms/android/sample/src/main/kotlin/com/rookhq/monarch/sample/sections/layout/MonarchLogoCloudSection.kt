package com.rookhq.monarch.sample.sections.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.layout.logocloud.MonarchLogoCloud
import com.rookhq.monarch.components.layout.logocloud.MonarchLogoCloudLayout
import com.rookhq.monarch.components.layout.logocloud.MonarchLogoCloudLogo
import com.rookhq.monarch.components.layout.logocloud.MonarchLogoCloudSize
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of LogoCloud. Tapping a linked logo names it below the grid. */
object MonarchLogoCloudSection : CataloguePage {
    override val title = "LogoCloud"

    private const val GRID_LOGOS = 4

    private val names = listOf("Company A", "Company B", "Company C", "Company D", "Company E", "Company F")

    @Composable
    override fun Content() {
        var opened by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Wordmarks in a row")
        MonarchLogoCloud(logos = names.map { MonarchLogoCloudLogo(it) }, title = "Placeholder organisations")
        CatalogueCaption("Grid with links")
        MonarchLogoCloud(
            logos = names.take(GRID_LOGOS).map { MonarchLogoCloudLogo(it, link = it) },
            title = "Integrations",
            layout = MonarchLogoCloudLayout.Grid,
            size = MonarchLogoCloudSize.Sm,
            onLogoClick = { opened = it.name },
        )
        if (opened.isNotEmpty()) MonarchText(text = "Opened $opened")
    }
}

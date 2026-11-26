@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiartifactversions.MonarchAiArtifactVersion
import com.rookhq.monarch.components.intelligence.aiartifactversions.MonarchAiArtifactVersions
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of AiArtifactVersions. */
object MonarchAiArtifactVersionsSection : CataloguePage {
    override val title = "AiArtifactVersions"

    private const val HOUR = 3_600_000L
    private const val NEWEST = 3

    private fun versions(now: Long) = listOf(
        MonarchAiArtifactVersion(
            version = 1,
            createdAt = now - 3 * HOUR,
            content = "We ship on Friday.\nSupport is told on Thursday.",
        ),
        MonarchAiArtifactVersion(
            version = 2,
            createdAt = now - 2 * HOUR,
            label = "Shorter intro",
            content = "We ship on Monday.\nSupport is told on Thursday.",
        ),
        MonarchAiArtifactVersion(
            version = 3,
            createdAt = now - HOUR,
            label = "Adds the status page",
            content = "We ship on Monday.\nSupport is told on Friday.\nThe status page is updated.",
        ),
    )

    @Composable
    override fun Content() {
        var selection by rememberSaveable { mutableIntStateOf(NEWEST) }
        val now = rememberSaveable { System.currentTimeMillis() }
        val all = versions(now)
        CatalogueCaption("Step, jump with the menu, or compare with the previous version")
        MonarchAiArtifactVersions(
            selection = selection,
            versions = all,
            onSelectionChange = { selection = it },
            previousIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            // The back arrow turned around; it mirrors in right-to-left layouts like the other one.
            nextIcon = {
                MonarchIcon(
                    SampleIcons.ChevronRight,
                    contentDescription = null,
                )
            },
        )
        MonarchText(text = all.first { it.version == selection }.content.orEmpty())
        CatalogueCaption("One version, disabled")
        MonarchAiArtifactVersions(
            selection = 1,
            versions = all.take(1),
            onSelectionChange = {},
            previousIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            nextIcon = {
                MonarchIcon(
                    SampleIcons.ChevronRight,
                    contentDescription = null,
                )
            },
            enabled = false,
        )
    }
}

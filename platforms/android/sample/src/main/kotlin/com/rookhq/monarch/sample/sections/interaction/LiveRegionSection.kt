package com.rookhq.monarch.sample.sections.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of LiveRegion: the Compose live-region semantics property. */
object LiveRegionSection : CataloguePage {
    override val title = "LiveRegion"

    @Composable
    override fun Content() {
        var saves by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Polite: read after TalkBack finishes speaking")
        MonarchButton(text = "Save", onClick = { saves++ })
        MonarchText(
            text = if (saves == 0) "Not saved yet" else "Saved $saves times",
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        var attempts by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Assertive: for an error that blocks the person")
        MonarchButton(text = "Pay", onClick = { attempts++ })
        MonarchText(
            text = if (attempts == 0) "" else "The card was declined ($attempts). Use another card.",
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            color = MonarchTheme.colors.dangerStrong,
        )
    }
}

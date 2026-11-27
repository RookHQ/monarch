package com.rookhq.monarch.sample.sections.collaboration

import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.collaboration.audiolevelmeter.MonarchAudioLevelMeter
import com.rookhq.monarch.components.collaboration.audiolevelmeter.MonarchAudioLevelMeterSize
import com.rookhq.monarch.components.collaboration.audiolevelmeter.MonarchAudioLevelMeterVariant
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of AudioLevelMeter. The slider stands in for a level the app measures. */
object MonarchAudioLevelMeterSection : CataloguePage {
    override val title = "AudioLevelMeter"

    private const val START_LEVEL = 0.6f

    @Composable
    override fun Content() {
        var level by rememberSaveable { mutableFloatStateOf(START_LEVEL) }
        var muted by rememberSaveable { mutableStateOf(false) }
        Slider(
            value = level,
            onValueChange = { level = it },
            modifier = Modifier.semantics { contentDescription = "Simulated level" },
        )
        MonarchSwitchRow(checked = muted, onCheckedChange = { muted = it }, label = "Muted")
        CatalogueCaption("Segments, medium and small")
        MonarchAudioLevelMeter(level = level, muted = muted)
        MonarchAudioLevelMeter(level = level, size = MonarchAudioLevelMeterSize.Sm, muted = muted)
        CatalogueCaption("Bar")
        MonarchAudioLevelMeter(level = level, variant = MonarchAudioLevelMeterVariant.Bar, muted = muted)
    }
}

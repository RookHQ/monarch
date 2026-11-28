package com.rookhq.monarch.components.collaboration.callstage

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.collaboration.calltile.MonarchCallTile
import com.rookhq.monarch.components.collaboration.calltile.MonarchCallTileSize

/** What the tiles of a call stage need: the people, the pin, the surface and the slots. */
@Suppress("LongParameterList")
internal class CallStageTilesSpec(
    val participants: Map<String, MonarchCallStageParticipant>,
    val layout: MonarchCallStageLayout,
    val surface: MonarchCallStageSurface,
    val pinnedId: String?,
    val now: Long?,
    val icons: MonarchCallStageIcons,
    val labels: MonarchCallStageLabels,
    val onPinnedIdChange: ((String?) -> Unit)?,
    val onFullScreen: ((String) -> Unit)?,
    val video: (@Composable (MonarchCallStageParticipant) -> Unit)?,
)

/** The tiles: an even grid, or the main tile with a filmstrip under it (beside it on a wide full stage). */
@Composable
internal fun CallStageTiles(spec: CallStageTilesSpec) {
    val tokens = MonarchCallStageTokens
    val compact = tokens.compact(spec.surface)
    val gap = tokens.gap(compact)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        when (val layout = spec.layout) {
            is MonarchCallStageLayout.Grid -> {
                val ids = layout.ids.filter { it in spec.participants }
                val columns = tokens.columns(layout.columns, compact, narrow = maxWidth < tokens.NarrowWidth)
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    ids.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            row.forEach { id -> CallStageTile(spec, id, MonarchCallTileSize.Md, Modifier.weight(1f)) }
                            // Keeps the tiles of a short last row as wide as the others.
                            repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
            is MonarchCallStageLayout.Spotlight -> {
                val strip = layout.strip.filter { it in spec.participants }
                val main = layout.main.takeIf { it in spec.participants }
                if (!compact && maxWidth >= tokens.WideWidth && strip.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) {
                        if (main != null) CallStageTile(spec, main, MonarchCallTileSize.Lg, Modifier.weight(1f))
                        Column(
                            modifier = Modifier.width(tokens.StripColumn),
                            verticalArrangement = Arrangement.spacedBy(gap),
                        ) {
                            strip.forEach { CallStageTile(spec, it, MonarchCallTileSize.Sm, Modifier.fillMaxWidth()) }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        if (main != null) CallStageTile(spec, main, MonarchCallTileSize.Lg, Modifier.fillMaxWidth())
                        if (strip.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(gap),
                            ) {
                                strip.forEach {
                                    CallStageTile(spec, it, MonarchCallTileSize.Sm, Modifier.width(tokens.StripTile))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One participant's `MonarchCallTile`; compact surfaces have no Pin or full-screen buttons. */
@Composable
private fun CallStageTile(spec: CallStageTilesSpec, id: String, size: MonarchCallTileSize, modifier: Modifier) {
    val person = spec.participants[id] ?: return
    val buttons = !MonarchCallStageTokens.compact(spec.surface)
    val pin = spec.onPinnedIdChange.takeIf { buttons }
    val fullScreen = spec.onFullScreen.takeIf { buttons }
    val video = spec.video
    MonarchCallTile(
        name = person.name,
        modifier = modifier,
        statusTone = person.statusTone,
        size = size,
        self = person.self,
        micOff = person.micOff,
        speaking = person.speaking,
        handRaised = person.handRaised,
        screen = person.screen,
        pinned = spec.pinnedId == id,
        showVideo = person.showVideo,
        status = person.status,
        quality = person.quality,
        reaction = person.reaction,
        now = spec.now,
        image = person.image,
        onPinnedChange = pin?.let { change -> { next -> change(if (next) id else null) } },
        onFullScreen = fullScreen?.let { open -> { open(id) } },
        icons = spec.icons.tile,
        labels = spec.labels.tile,
        video = video?.let { slot -> { slot(person) } },
    )
}

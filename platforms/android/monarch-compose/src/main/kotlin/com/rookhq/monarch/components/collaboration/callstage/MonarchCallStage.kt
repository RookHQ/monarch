package com.rookhq.monarch.components.collaboration.callstage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControlsLayout
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBanner
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString

/**
 * The call itself: a header with the title, a running timer and the head count, everyone's call tiles in a grid
 * or a spotlight with a filmstrip, connection notices and the controls.
 *
 * A pattern built from Monarch primitives: `MonarchCallTile` for each person, `MonarchCalloutBanner` for the
 * notices, `MonarchBadge` and `MonarchButton` in the header, and the [controls] slot for `MonarchCallControls`.
 * It is UI only: the video surfaces come from the app's call SDK through [video], and every state is hoisted.
 * The layout is [MonarchCallStageLayout.automatic] unless [layout] is given: a pin, someone else's shared screen,
 * the speaker in speaker mode or the other person in a one-to-one call is large, the others in a filmstrip that
 * scrolls sideways (a column beside it on a wide full stage); otherwise an even grid, at most two columns on a
 * narrow screen or a compact surface. The stage grows with its tiles; put it in a scrolling container or give it
 * a height to fill a screen.
 *
 * For TalkBack the stage is a traversal group; without a [title] it is named "Call". The title is a heading, the
 * head count is a polite live region, the reconnecting notice and the muted hint are announced politely and the
 * [error] assertively, and each tile is `MonarchCallTile`, read as the person and what is true of them.
 *
 * @param participants everyone on the call, you included (`self`), in display order.
 * @param modifier the modifier of the stage.
 * @param title the heading, such as the meeting name.
 * @param startedAt when the call started, in epoch milliseconds: shows a running `mm:ss` timer.
 * @param now the current time in epoch milliseconds, for tests and previews. `null` uses the clock.
 * @param mode the layout chosen in `MonarchCallControls`: automatic, grid or speaker.
 * @param pinnedId the pinned participant, shown large.
 * @param onPinnedIdChange adds Pin buttons to the tiles on the full stage; called with the next pinned id, `null`
 * to unpin.
 * @param activeSpeakerId who is talking, for speaker mode.
 * @param layout replaces the automatic layout.
 * @param surface the full stage, docked or picture in picture. Compact surfaces have at most two columns and no
 * tile buttons.
 * @param lowData shows the "Low data" badge in the header.
 * @param reconnecting shows the warning "You're offline. Trying to reconnect…".
 * @param error a problem in the app's words, shown as a danger callout and announced.
 * @param speakingWhileMuted shows the "You're muted" hint under the tiles.
 * @param icons icons for the header buttons and the tiles. Without one the button writes its word.
 * @param labels replacements for every built-in string, including the tiles'.
 * @param onSurfaceChange adds Shrink (sends `Docked`) on the full stage and Expand (sends `Stage`) when docked.
 * @param onMinimize adds Minimize when docked.
 * @param onPictureInPicture adds the picture in picture button, except on the picture in picture surface. The
 * app enters picture in picture itself.
 * @param onFullScreen adds full screen to each tile on the full stage; receives the participant id.
 * @param video the participant's video from the app's call SDK; without it every tile shows the avatar.
 * @param controls the controls under the tiles, usually `MonarchCallControls`.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchCallStage(
    participants: List<MonarchCallStageParticipant>,
    modifier: Modifier = Modifier,
    title: String? = null,
    startedAt: Long? = null,
    now: Long? = null,
    mode: MonarchCallControlsLayout = MonarchCallControlsLayout.Automatic,
    pinnedId: String? = null,
    onPinnedIdChange: ((String?) -> Unit)? = null,
    activeSpeakerId: String? = null,
    layout: MonarchCallStageLayout? = null,
    surface: MonarchCallStageSurface = MonarchCallStageSurface.Stage,
    lowData: Boolean = false,
    reconnecting: Boolean = false,
    error: String? = null,
    speakingWhileMuted: Boolean = false,
    icons: MonarchCallStageIcons = MonarchCallStageIcons(),
    labels: MonarchCallStageLabels = MonarchCallStageLabels(),
    onSurfaceChange: ((MonarchCallStageSurface) -> Unit)? = null,
    onMinimize: (() -> Unit)? = null,
    onPictureInPicture: (() -> Unit)? = null,
    onFullScreen: ((String) -> Unit)? = null,
    video: (@Composable (MonarchCallStageParticipant) -> Unit)? = null,
    controls: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchCallStageTokens
    val compact = tokens.compact(surface)
    val chosen = layout ?: MonarchCallStageLayout.automatic(
        selfId = participants.firstOrNull { it.self }?.id,
        ids = participants.map { it.id },
        mode = mode,
        pinnedId = pinnedId,
        sharerId = participants.firstOrNull { it.screen && !it.self }?.id,
        activeSpeakerId = activeSpeakerId,
    )
    val region = monarchString(labels.region, R.string.monarch_callstage_region)
    val headCount = if (tokens.waiting(participants)) {
        monarchString(labels.waiting, R.string.monarch_callstage_waiting)
    } else {
        labels.inCall?.invoke(participants.size)
            ?: pluralStringResource(R.plurals.monarch_callstage_in_call, participants.size, participants.size)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                isTraversalGroup = true
                // With a title the heading names the stage; a second name would be read twice.
                if (title == null) contentDescription = region
            }
            .padding(tokens.gap(compact)),
        verticalArrangement = Arrangement.spacedBy(tokens.gap(compact)),
    ) {
        CallStageHeader(
            CallStageHeaderSpec(
                title = title,
                startedAt = startedAt,
                now = now,
                headCount = headCount,
                lowData = lowData,
                tools = tokens.tools(surface, onPictureInPicture != null, onSurfaceChange != null, onMinimize != null),
                icons = icons,
                labels = labels,
                onTool = { tool ->
                    when (tool) {
                        CallStageTool.PictureInPicture -> onPictureInPicture?.invoke()
                        CallStageTool.Shrink -> onSurfaceChange?.invoke(MonarchCallStageSurface.Docked)
                        CallStageTool.Expand -> onSurfaceChange?.invoke(MonarchCallStageSurface.Stage)
                        CallStageTool.Minimize -> onMinimize?.invoke()
                    }
                },
            ),
        )
        if (reconnecting) {
            MonarchCalloutBanner(
                message = monarchString(labels.reconnecting, R.string.monarch_callstage_reconnecting),
                tone = MonarchTone.Warning,
                announcement = MonarchBannerAnnouncement.Polite,
            )
        }
        if (error != null) {
            MonarchCalloutBanner(
                message = error,
                tone = MonarchTone.Danger,
                announcement = MonarchBannerAnnouncement.Assertive,
            )
        }
        CallStageTiles(
            CallStageTilesSpec(
                participants = participants.associateBy { it.id },
                layout = chosen,
                surface = surface,
                pinnedId = pinnedId,
                now = now,
                icons = icons,
                labels = labels,
                onPinnedIdChange = onPinnedIdChange,
                onFullScreen = onFullScreen,
                video = video,
            ),
        )
        if (speakingWhileMuted) {
            MonarchCalloutBanner(
                message = monarchString(labels.mutedHint, R.string.monarch_callstage_muted_hint),
                tone = MonarchTone.Warning,
                announcement = MonarchBannerAnnouncement.Polite,
            )
        }
        controls?.invoke()
    }
}

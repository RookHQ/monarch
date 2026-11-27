package com.rookhq.monarch.components.collaboration.callstage

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControlsLayout

/**
 * Where the tiles of a `MonarchCallStage` go: an even grid, or one main tile with a filmstrip. Pass one to
 * replace the automatic choice; [automatic] is pure, so an app can also use it to decide how much video to
 * request from its call SDK.
 */
@Immutable
public sealed class MonarchCallStageLayout {
    /**
     * Everyone in equal columns.
     *
     * @property ids the participant ids in display order.
     * @property columns the number of columns; the stage shows at most two on a narrow screen or a compact surface.
     */
    @Immutable
    public class Grid(public val ids: List<String>, public val columns: Int) : MonarchCallStageLayout() {
        override fun equals(other: Any?): Boolean = other is Grid && other.ids == ids && other.columns == columns

        override fun hashCode(): Int = 31 * ids.hashCode() + columns

        override fun toString(): String = "Grid(ids=$ids, columns=$columns)"
    }

    /**
     * One participant large, the others in a filmstrip.
     *
     * @property main the id shown large.
     * @property strip the other ids, in display order.
     */
    @Immutable
    public class Spotlight(public val main: String, public val strip: List<String>) : MonarchCallStageLayout() {
        override fun equals(other: Any?): Boolean = other is Spotlight && other.main == main && other.strip == strip

        override fun hashCode(): Int = 31 * main.hashCode() + strip.hashCode()

        override fun toString(): String = "Spotlight(main=$main, strip=$strip)"
    }

    /** The layout rules shared by every platform. */
    public companion object {
        private const val TWO_UP = 4
        private const val WIDE_COLUMNS = 3

        /** The grid columns for a number of tiles: 1 for one, 2 up to four, then 3. */
        public fun gridColumns(count: Int): Int = when {
            count <= 1 -> 1
            count <= TWO_UP -> 2
            else -> WIDE_COLUMNS
        }

        /**
         * Chooses the layout. A pin wins; then someone else's shared screen; then, in speaker mode, the active
         * speaker (never yourself, the first other person when the speaker is unknown); then, in automatic mode
         * with one other person, that person large. Otherwise a grid. Ids that are not in [ids] are ignored.
         *
         * @param selfId your participant id, if you are in [ids].
         * @param ids every participant id in display order, yourself included.
         * @param mode the layout chosen in `MonarchCallControls`.
         * @param pinnedId the pinned participant.
         * @param sharerId whoever is sharing a screen.
         * @param activeSpeakerId who is talking.
         */
        public fun automatic(
            selfId: String?,
            ids: List<String>,
            mode: MonarchCallControlsLayout,
            pinnedId: String? = null,
            sharerId: String? = null,
            activeSpeakerId: String? = null,
        ): MonarchCallStageLayout {
            val others = ids.filter { it != selfId }
            val main = pinnedId?.takeIf { it in ids }
                ?: sharerId?.takeIf { it != selfId && it in others }
                ?: speaker(mode, others, activeSpeakerId)
                ?: others.singleOrNull()?.takeIf { mode == MonarchCallControlsLayout.Automatic }
            return if (main != null) {
                Spotlight(main, ids.filter { it != main })
            } else {
                Grid(ids, gridColumns(ids.size))
            }
        }

        private fun speaker(mode: MonarchCallControlsLayout, others: List<String>, activeSpeakerId: String?): String? {
            if (mode != MonarchCallControlsLayout.Speaker || others.isEmpty()) return null
            return activeSpeakerId?.takeIf { it in others } ?: others.first()
        }
    }
}

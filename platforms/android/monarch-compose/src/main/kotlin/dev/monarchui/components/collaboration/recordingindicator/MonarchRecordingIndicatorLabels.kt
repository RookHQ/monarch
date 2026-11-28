package dev.monarchui.components.collaboration.recordingindicator

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchRecordingIndicator`.
 * Every `null` keeps the built-in string `monarch_recordingindicator_<key>`.
 *
 * @property rec the short visible text while recording, "REC". TalkBack reads [recording] instead.
 * @property recording what TalkBack reads while recording, "Recording".
 * @property paused the visible text while paused, "Paused", also read after [recording].
 * @property started announced when recording starts.
 * @property stopped announced when recording stops.
 * @property pausedAnnouncement announced when recording is paused.
 * @property resumed announced when recording resumes.
 * @property stop the name of the stop button.
 */
@Immutable
public class MonarchRecordingIndicatorLabels(
    public val rec: String? = null,
    public val recording: String? = null,
    public val paused: String? = null,
    public val started: String? = null,
    public val stopped: String? = null,
    public val pausedAnnouncement: String? = null,
    public val resumed: String? = null,
    public val stop: String? = null,
)

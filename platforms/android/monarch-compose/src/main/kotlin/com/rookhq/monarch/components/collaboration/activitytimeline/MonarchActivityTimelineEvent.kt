package com.rookhq.monarch.components.collaboration.activitytimeline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.rookhq.monarch.foundation.MonarchTone

/**
 * One event of a `MonarchActivityTimeline`: "Ana Ruiz moved Acme renewal".
 *
 * @property id the stable id of the event: the list key and the value kept in the expanded set. Unique.
 * @property verb what happened, such as "changed the status of" or "Ticket created from email". Already localised.
 * @property time when it happened, as epoch milliseconds (UTC).
 * @property actor who did it. `null` for events without an actor; the event then reads as its verb and target.
 * @property target what it was done to, written in bold after the verb, such as "Acme renewal" (the web's
 * `object`, which is a Kotlin keyword).
 * @property icon the marker, such as a `MonarchIcon`, tinted in the tone. It wins over the dot and the avatar.
 * Decoration: the sentence says what happened.
 * @property tone the colour of the dot or the icon.
 * @property tags short tags after the time, such as a stage or a source.
 * @property body text always shown under the sentence, such as a comment.
 * @property details text behind a Details disclosure, such as a field change or a payload.
 * @property detailRows field and value lines behind the Details disclosure, laid out in two aligned columns. Shown
 * under [details] when both are given.
 */
@Immutable
public class MonarchActivityTimelineEvent(
    public val id: String,
    public val verb: String,
    public val time: Long,
    public val actor: MonarchActivityTimelineActor? = null,
    public val target: String? = null,
    public val icon: (@Composable () -> Unit)? = null,
    public val tone: MonarchTone = MonarchTone.Neutral,
    public val tags: List<String> = emptyList(),
    public val body: String? = null,
    public val details: String? = null,
    public val detailRows: List<MonarchActivityTimelineDetail> = emptyList(),
)

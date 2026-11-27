package com.rookhq.monarch.components.technical.incidenttimeline

import androidx.compose.runtime.Immutable

/**
 * One update posted during an incident, shown by `MonarchIncidentTimeline`.
 *
 * @property id a stable, unique identifier.
 * @property stage the stage key: a `MonarchIncidentStage.key` or a product stage given to the timeline's `stages`.
 * @property body the update text. Line breaks are kept. Already localised.
 * @property time when it was posted, as epoch milliseconds (UTC).
 * @property automated whether the system posted it rather than a person. Tagged "Automated".
 * @property author who posted it, when the product shows authors.
 */
@Immutable
public class MonarchIncidentUpdate(
    public val id: String,
    public val stage: String,
    public val body: String,
    public val time: Long,
    public val automated: Boolean = false,
    public val author: String? = null,
) {
    /**
     * An update with a standard stage.
     *
     * @param id a stable, unique identifier.
     * @param stage the standard stage.
     * @param body the update text.
     * @param time when it was posted, as epoch milliseconds (UTC).
     * @param automated whether the system posted it.
     * @param author who posted it.
     */
    public constructor(
        id: String,
        stage: MonarchIncidentStage,
        body: String,
        time: Long,
        automated: Boolean = false,
        author: String? = null,
    ) : this(id, stage.key, body, time, automated, author)
}

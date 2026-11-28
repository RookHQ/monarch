package com.rookhq.monarch.components.workflow.roadmap

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.foundation.MonarchTone

/**
 * One project (or epic, or release) on a `MonarchRoadmap`.
 *
 * @property id a stable id, unique among the items. Dependencies and milestones refer to it.
 * @property label the plain-text name, shown in the row heading and on the bar and read by TalkBack.
 * @property start the start date as a day key (the epoch millisecond of its midnight in UTC), or `null`.
 * @property end the target date, inclusive, as a day key, or `null`. With neither date the project stays in its
 * group as a row that says "No dates".
 * @property groupId the id of the `MonarchRoadmapGroup` (initiative, theme or team) it belongs to.
 * @property progress the completion from 0 to 1, drawn inside the bar and read as a percentage, or `null`.
 * @property progressText a short progress text in the row heading, such as "12/20".
 * @property health the health last reported, written in the row heading, or `null` when the roadmap does not track
 * health for this project (nothing is written).
 * @property tone the bar colour. `null` takes it from [health]: at risk is warning, off track is danger, anything
 * else the accent colour.
 */
@Immutable
public class MonarchRoadmapItem(
    public val id: String,
    public val label: String,
    public val start: Long? = null,
    public val end: Long? = null,
    public val groupId: String? = null,
    public val progress: Float? = null,
    public val progressText: String? = null,
    public val health: MonarchRoadmapHealth? = null,
    public val tone: MonarchTone? = null,
)

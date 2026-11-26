package com.rookhq.monarch.components.workflow.roadmap

import com.rookhq.monarch.foundation.MonarchTone

/** The decisions of `MonarchRoadmap`. Sizes and colours of the chart itself come from `MonarchGanttTimeline`. */
internal object MonarchRoadmapTokens {
    /**
     * The bar tone of a project: [tone] when the app gives one, otherwise from [health]. At risk is warning, off
     * track is danger, and on track, not reported and no health keep the accent colour (`null`), as on the web.
     */
    fun barTone(tone: MonarchTone?, health: MonarchRoadmapHealth?): MonarchTone? = tone ?: when (health) {
        MonarchRoadmapHealth.AtRisk -> MonarchTone.Warning
        MonarchRoadmapHealth.OffTrack -> MonarchTone.Danger
        MonarchRoadmapHealth.OnTrack, MonarchRoadmapHealth.NotReported, null -> null
    }
}

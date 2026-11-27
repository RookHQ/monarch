package com.rookhq.monarch.components.intelligence.aisharedsession

/** Everything the participant list needs, gathered so the sheet takes one value. */
internal class MonarchAiSharedSessionListModel(
    val groups: MonarchAiSharedSessionGroups,
    val answeringId: String?,
    val answeringText: String?,
    val following: Set<String>,
    val onFollowingChange: ((Set<String>) -> Unit)?,
    val texts: MonarchAiSharedSessionTexts,
)

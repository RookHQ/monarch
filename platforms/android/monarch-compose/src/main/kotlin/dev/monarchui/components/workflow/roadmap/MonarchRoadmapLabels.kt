package dev.monarchui.components.workflow.roadmap

import androidx.compose.runtime.Immutable

/**
 * Replacements for the roadmap's own built-in strings. Every `null` keeps the built-in string
 * `monarch_roadmap_<key>`. The timeline's strings are replaced with `MonarchGanttTimelineLabels`.
 *
 * @property health the words of a health: "On track", "At risk", "Off track", "No health reported".
 * @property count the number of projects in a group heading: "1 project", "3 projects".
 * @property meta joins two parts of a row heading's second line: "8/20 · On track".
 */
@Immutable
public class MonarchRoadmapLabels(
    public val health: ((MonarchRoadmapHealth) -> String)? = null,
    public val count: ((count: Int) -> String)? = null,
    public val meta: ((first: String, second: String) -> String)? = null,
)

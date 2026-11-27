package com.rookhq.monarch.components.charts.heatmap

import androidx.compose.runtime.Immutable

/**
 * A row or a column of a `MonarchHeatmap`.
 *
 * @property label the text on the axis, in the table and in what TalkBack reads. Already localised.
 * @property key the stable id matched by `MonarchHeatmapCell.row` and `.column`. Defaults to the label.
 */
@Immutable
public class MonarchHeatmapAxisItem(
    public val label: String,
    public val key: String = label,
)

@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.workflow.goaltree

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewGoals = listOf(
    MonarchGoal(
        id = "o1",
        title = "Grow repeat customers",
        progress = 0.62f,
        status = MonarchGoalStatus.OnTrack,
        ownerName = "Sam Lee",
        meta = "weight 40%",
        alignedTo = "Customers come back",
    ),
    MonarchGoal(
        id = "k1",
        parentId = "o1",
        title = "Launch loyalty emails",
        progress = 1f,
        status = MonarchGoalStatus.Done,
    ),
    MonarchGoal(
        id = "k2",
        parentId = "o1",
        title = "Cut churn to 3%",
        progress = 0.3f,
        status = MonarchGoalStatus.AtRisk,
    ),
    MonarchGoal(id = "o2", title = "Ship the mobile app", progress = 0.1f, status = MonarchGoalStatus.OffTrack),
    MonarchGoal(
        id = "k3",
        parentId = "o2",
        title = "Beta with 50 testers",
        progress = 0f,
        status = MonarchGoalStatus.NotStarted,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchGoalTreePreview() {
    PreviewFrame {
        MonarchGoalTree(goals = PreviewGoals, expandIcon = { Text("›") }, onAddChild = {})
        MonarchGoalTree(goals = PreviewGoals, expandIcon = { Text("›") }, expanded = setOf("o1"), quietChildren = false)
        MonarchGoalTree(goals = emptyList(), expandIcon = { Text("›") })
    }
}

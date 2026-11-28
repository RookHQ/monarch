@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.workflow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.workflow.goaltree.MonarchGoal
import dev.monarchui.components.workflow.goaltree.MonarchGoalStatus
import dev.monarchui.components.workflow.goaltree.MonarchGoalTree
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** A chevron that points to the end; it mirrors in right-to-left layouts. */
private val GoalTreeChevron: ImageVector by lazy {
    ImageVector.Builder(
        name = "Chevron",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(8.6f, 7.4f)
            lineTo(10f, 6f)
            lineTo(16f, 12f)
            lineTo(10f, 18f)
            lineTo(8.6f, 16.6f)
            lineTo(13.2f, 12f)
            close()
        }
    }.build()
}

private val SampleGoals = listOf(
    MonarchGoal(
        id = "o1",
        title = "Grow repeat customers",
        progress = 0.62f,
        status = MonarchGoalStatus.OnTrack,
        ownerName = "Sam Lee",
        meta = "weight 40% · due 31 Mar",
        alignedTo = "Customers come back (Company)",
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
        title = "Cut monthly churn to 3%",
        progress = 0.3f,
        status = MonarchGoalStatus.AtRisk,
    ),
    MonarchGoal(
        id = "k3",
        parentId = "o1",
        title = "Interview 20 customers",
        progress = 0.55f,
        status = MonarchGoalStatus.OnTrack,
    ),
    MonarchGoal(
        id = "o2",
        title = "Ship the mobile app",
        progress = 0.1f,
        status = MonarchGoalStatus.OffTrack,
        ownerName = "Alex Kim",
    ),
    MonarchGoal(
        id = "k4",
        parentId = "o2",
        title = "Beta with 50 testers",
        progress = 0f,
        status = MonarchGoalStatus.NotStarted,
    ),
)

/** The catalogue page of GoalTree. */
object MonarchGoalTreeSection : CataloguePage {
    override val title = "GoalTree"

    @Composable
    override fun Content() {
        var added by rememberSaveable { mutableIntStateOf(0) }
        val goals = remember(added) {
            SampleGoals + (1..added).map {
                MonarchGoal(
                    id = "new$it",
                    parentId = "o1",
                    title = "New key result $it",
                    progress = 0f,
                    status = MonarchGoalStatus.NotStarted,
                )
            }
        }
        CatalogueCaption("Objectives with key results, an add button and a menu per goal")
        MonarchGoalTree(
            goals = goals,
            expandIcon = { MonarchIcon(GoalTreeChevron, contentDescription = null, size = MonarchIconSize.Sm) },
            onAddChild = { added++ },
            addIcon = { MonarchIcon(SampleIcons.Plus, contentDescription = null, size = MonarchIconSize.Sm) },
        ) { goal -> GoalMenu(goal) }
        CatalogueCaption("Every badge shown")
        MonarchGoalTree(
            goals = SampleGoals.filter { it.id != "o2" && it.parentId != "o2" },
            expandIcon = { MonarchIcon(GoalTreeChevron, contentDescription = null, size = MonarchIconSize.Sm) },
            quietChildren = false,
        )
        CatalogueCaption("Empty")
        MonarchGoalTree(goals = emptyList(), expandIcon = {})
    }

    @Composable
    private fun GoalMenu(goal: MonarchGoal) {
        var open by remember { mutableStateOf(false) }
        MonarchButton(
            text = "More",
            onClick = { open = true },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
            MonarchMenuItem(text = "Check in on ${goal.title}", onClick = { open = false })
            MonarchMenuItem(text = "History", onClick = { open = false })
        }
    }
}

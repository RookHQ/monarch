@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.ColorPainter
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerAction
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerActionKind
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerFrame
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerPoint
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerView
import com.rookhq.monarch.components.intelligence.aicomputerview.MonarchAiComputerViewStatus
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of AiComputerView: a simulated session the controls drive. */
object MonarchAiComputerViewSection : CataloguePage {
    override val title = "AiComputerView"

    private const val Width = 1280
    private const val Height = 800
    private const val StepMillis = 3_000L

    private val steps = listOf(
        Triple(MonarchAiComputerActionKind.Navigate, "Opened the billing portal", null),
        Triple(MonarchAiComputerActionKind.Click, "Clicked Sign in", MonarchAiComputerPoint(1080, 64)),
        Triple(MonarchAiComputerActionKind.Type, "Typed the email address", MonarchAiComputerPoint(640, 360)),
        Triple(MonarchAiComputerActionKind.Click, "Clicked Invoices", MonarchAiComputerPoint(220, 300)),
        Triple(MonarchAiComputerActionKind.Read, "Read the invoice list", null),
    )

    @Composable
    override fun Content() {
        var status by rememberSaveable { mutableStateOf(MonarchAiComputerViewStatus.Working) }
        var count by rememberSaveable { mutableIntStateOf(2) }
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        val start = remember { System.currentTimeMillis() }
        val actions = steps.take(count).mapIndexed { index, (kind, label, point) ->
            MonarchAiComputerAction(
                id = "$index",
                kind = kind,
                label = label,
                at = start + index * StepMillis,
                point = point,
                detail = if (kind == MonarchAiComputerActionKind.Type) "sam@example.com" else null,
            )
        }
        CatalogueCaption("A session: add actions, pause, take over and stop")
        MonarchButton(
            text = "Next action",
            onClick = { if (count < steps.size) count += 1 else status = MonarchAiComputerViewStatus.Done },
        )
        MonarchAiComputerView(
            status = status,
            frame = MonarchAiComputerFrame(
                image = ColorPainter(MonarchTheme.colors.bgSubtle),
                width = Width,
                height = Height,
                capturedAt = actions.lastOrNull()?.at ?: start,
                alt = "The billing portal",
            ),
            actions = actions,
            currentUrl = "https://billing.example.com/invoices",
            windowTitle = "Billing portal",
            pointer = actions.lastOrNull()?.point,
            waitingReason = "Sign in to continue",
            selectedActionId = selected,
            onSelectAction = { selected = it },
            onPauseChange = { paused ->
                status = if (paused) MonarchAiComputerViewStatus.Paused else MonarchAiComputerViewStatus.Working
            },
            onTakeOver = { status = MonarchAiComputerViewStatus.TakenOver },
            onHandBack = { status = MonarchAiComputerViewStatus.Working },
            onStop = { status = MonarchAiComputerViewStatus.Stopped },
        )
        CatalogueCaption("Waiting for the person, compact log")
        MonarchAiComputerView(
            status = MonarchAiComputerViewStatus.WaitingForYou,
            actions = actions,
            waitingReason = "Sign in with your company account to continue",
            onTakeOver = {},
            onStop = {},
            compact = true,
        )
    }
}

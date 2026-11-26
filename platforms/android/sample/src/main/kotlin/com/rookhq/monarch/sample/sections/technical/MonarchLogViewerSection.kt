package com.rookhq.monarch.sample.sections.technical

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.technical.logviewer.MonarchLogLevel
import com.rookhq.monarch.components.technical.logviewer.MonarchLogLine
import com.rookhq.monarch.components.technical.logviewer.MonarchLogViewer
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

private const val StreamEveryMillis = 700L
private const val MaxLines = 500
private const val HeightSteps = 5
private val Sources = listOf("api", "worker", "billing", "db")
private val Levels = listOf(
    MonarchLogLevel.Info,
    MonarchLogLevel.Debug,
    MonarchLogLevel.Info,
    MonarchLogLevel.Warn,
    MonarchLogLevel.Info,
    MonarchLogLevel.Error,
)
private val Messages = listOf(
    "GET /v1/items 200 in 42 ms",
    "Job 812 finished",
    "Slow query on invoices (900 ms)",
    "Payment provider timed out",
)

/** The catalogue page of LogViewer: a log that streams a line every 0.7 seconds while Streaming is on. */
object MonarchLogViewerSection : CataloguePage {
    override val title = "LogViewer"

    @Composable
    override fun Content() {
        var lines by remember { mutableStateOf(emptyList<MonarchLogLine>()) }
        var streaming by rememberSaveable { mutableStateOf(true) }
        LaunchedEffect(streaming) {
            while (streaming && lines.size < MaxLines) {
                delay(StreamEveryMillis)
                val n = lines.size
                lines = lines + MonarchLogLine(
                    id = "line-$n",
                    message = Messages[n % Messages.size],
                    level = Levels[n % Levels.size],
                    time = System.currentTimeMillis(),
                    source = Sources[n % Sources.size],
                )
            }
        }
        CatalogueCaption("Streaming")
        MonarchLogViewer(
            lines = lines,
            modifier = Modifier
                .fillMaxWidth()
                .height(MonarchTheme.spacing.space24 * HeightSteps),
            loading = streaming && lines.isEmpty(),
        ) {
            TextButton(onClick = { streaming = !streaming }) { Text(if (streaming) "Stop" else "Stream") }
            TextButton(onClick = { lines = emptyList() }) { Text("Clear") }
        }
    }
}

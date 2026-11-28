@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.technical

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.technical.installverify.MonarchInstallSnippet
import dev.monarchui.components.technical.installverify.MonarchInstallTarget
import dev.monarchui.components.technical.installverify.MonarchInstallVerify
import dev.monarchui.components.technical.installverify.MonarchInstallVerifyStatus
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private const val SampleNow = 1_772_636_400_000L
private const val FiveMinutes = 300_000L

private val SampleSnippets = listOf(
    MonarchInstallSnippet(
        id = "html",
        label = "HTML",
        code = "<script src=\"https://cdn.example.com/widget.js\" data-key=\"pk_test_123\"></script>",
        language = "html",
        description = "Paste it before the closing head tag on every page.",
    ),
    MonarchInstallSnippet(
        id = "npm",
        label = "npm",
        code = "npm install @example/widget",
        language = "bash",
        description = "Run it in your project, then import the widget once.",
    ),
)

/** The catalogue page of InstallVerify. */
object MonarchInstallVerifySection : CataloguePage {
    override val title = "InstallVerify"

    @Composable
    override fun Content() {
        var connected by rememberSaveable { mutableStateOf(false) }
        var note by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Install, then press Simulate a report to connect")
        MonarchInstallVerify(
            snippets = SampleSnippets,
            status = if (connected) MonarchInstallVerifyStatus.Connected else MonarchInstallVerifyStatus.Waiting,
            targets = listOf(
                MonarchInstallTarget("www.example.com", if (connected) SampleNow - FiveMinutes else null),
                MonarchInstallTarget("shop.example.com"),
            ),
            now = SampleNow,
            developerInstructions = "Add the widget snippet to every page of www.example.com.",
            onOpenTest = { note = "Would open https://www.example.com in the browser" },
        )
        MonarchButton(
            text = if (connected) "Reset" else "Simulate a report",
            onClick = { connected = !connected },
            size = MonarchButtonSize.Sm,
        )
        note?.let { MonarchText(text = it) }
    }
}

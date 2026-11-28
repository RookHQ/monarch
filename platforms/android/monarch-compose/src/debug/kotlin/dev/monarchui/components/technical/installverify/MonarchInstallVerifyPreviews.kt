@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.technical.installverify

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val PreviewNow = 1_772_636_400_000L

@MonarchPreviews
@Composable
internal fun MonarchInstallVerifyPreview() {
    val snippets = listOf(
        MonarchInstallSnippet("html", "HTML", "<script src=\"https://cdn.example.com/w.js\"></script>", "html"),
        MonarchInstallSnippet("npm", "npm", "npm install @example/widget", "bash", "Run it in your project."),
    )
    PreviewFrame {
        MonarchInstallVerify(
            snippets = snippets,
            status = MonarchInstallVerifyStatus.Waiting,
            targets = listOf(MonarchInstallTarget("www.example.com")),
            now = PreviewNow,
            developerInstructions = "Add this snippet to every page.",
        )
        MonarchInstallVerify(
            snippets = snippets,
            status = MonarchInstallVerifyStatus.Connected,
            part = MonarchInstallVerifyPart.Verify,
            targets = listOf(MonarchInstallTarget("www.example.com", PreviewNow - 300_000L)),
            now = PreviewNow,
            onOpenTest = {},
        )
    }
}

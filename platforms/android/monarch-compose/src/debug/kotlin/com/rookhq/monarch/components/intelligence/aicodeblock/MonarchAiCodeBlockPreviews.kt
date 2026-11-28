package com.rookhq.monarch.components.intelligence.aicodeblock

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val PreviewCode =
    "export const port = 8080;\nexport function listen() {\n  return server.listen(port);\n}"
private const val PreviewDiff = "@@ -1,2 +1,2 @@\n-const port = 3000;\n+const port = 8080;\n listen(port);"

@MonarchPreviews
@Composable
internal fun MonarchAiCodeBlockPreview() {
    PreviewFrame {
        MonarchAiCodeBlock(code = PreviewCode, language = "ts", title = "server.ts", onInsert = {}, onApply = {})
        MonarchAiCodeBlock(code = PreviewDiff, language = "diff", incomplete = true, onApply = {})
    }
}

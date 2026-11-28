package com.rookhq.monarch.components.typography.codeblock

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchCodeBlockPreview() {
    PreviewFrame {
        MonarchCodeBlock(
            code = "const client = createClient({ retries: 3 }); // keep it small\nexport default client;",
            language = "ts",
            title = "client.ts",
            showLineNumbers = true,
        )
        MonarchCodeBlock(
            code = "@@ -1,2 +1,2 @@\n-const port = 3000;\n+const port = 8080;\n app.listen(port);",
            language = "diff",
            wrapped = true,
        )
        MonarchCodeBlock(code = "SELECT id FROM users;", showWrapToggle = false, copyable = false)
    }
}

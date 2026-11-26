import MonarchUI
import SwiftUI

struct MonarchAiStreamingTextSection: CataloguePage {
    static let title = "AI streaming text"

    @State private var text = ""
    @State private var isStreaming = false

    private let answer = """
        ## Release plan
        Ship the **beta** on Monday, then read the [release notes](https://example.com/notes).

        1. Freeze the branch
        2. Run `make release`
        3. Tell *support* the date

        | Step | Owner |
        | --- | --- |
        | Freeze | Ada |
        | Release | Lin |

        ```sh
        make release
        ```

        Everything else waits for the next cycle.
        """

    var body: some View {
        CatalogueCaption("Streamed a few characters at a time")
        MonarchButton("Stream the answer", variant: .primary) { stream() }
            .disabled(isStreaming)
        MonarchAiStreamingText(text, isStreaming: isStreaming)

        CatalogueCaption("Half-written syntax closed for display")
        MonarchAiStreamingText("Ship the **beta on Mon", isStreaming: true)
        MonarchAiStreamingText("Read [the notes](https://exa", isStreaming: true)

        CatalogueCaption("Complete")
        MonarchAiStreamingText("Done. The beta shipped on **Monday**.")
    }

    private func stream() {
        text = ""
        isStreaming = true
        Task {
            var index = answer.startIndex
            while index < answer.endIndex {
                try? await Task.sleep(for: .milliseconds(40))
                let next = answer.index(index, offsetBy: 4, limitedBy: answer.endIndex) ?? answer.endIndex
                text += answer[index..<next]
                index = next
            }
            isStreaming = false
        }
    }
}

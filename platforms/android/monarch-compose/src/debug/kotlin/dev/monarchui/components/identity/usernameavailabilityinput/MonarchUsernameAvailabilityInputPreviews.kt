package dev.monarchui.components.identity.usernameavailabilityinput

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchUsernameAvailabilityInputPreview() {
    PreviewFrame {
        MonarchUsernameAvailabilityInput(
            value = "",
            label = "Handle",
            onValueChange = {},
            validate = { null },
            prefix = "rook.com/",
            helperText = "Letters, numbers and dashes.",
        )
        MonarchUsernameAvailabilityInput(
            value = "ada",
            label = "Handle",
            onValueChange = {},
            validate = { MonarchUsernameAvailabilityResult(valid = true) },
            debounceMillis = 0L,
        )
        MonarchUsernameAvailabilityInput(
            value = "admin",
            label = "Handle",
            onValueChange = {},
            validate = { MonarchUsernameAvailabilityResult(valid = false, message = "That handle is taken.") },
            debounceMillis = 0L,
        )
    }
}

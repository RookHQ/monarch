package dev.monarchui.sample.sections.actions

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ShareButton: the Android Sharesheet opened from a MonarchButton. */
object ShareButtonSection : CataloguePage {
    override val title = "ShareButton"

    @Composable
    override fun Content() {
        val context = LocalContext.current
        CatalogueCaption("Intent.ACTION_SEND in Intent.createChooser")
        MonarchButton(
            text = "Share invoice",
            onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://example.com/invoices/1042")
                    putExtra(Intent.EXTRA_TITLE, "Invoice 1042")
                }
                context.startActivity(Intent.createChooser(send, null))
            },
            variant = MonarchButtonVariant.Primary,
        )
        MonarchText(
            text = "The system Sharesheet already offers Copy, so no copy fallback is shown.",
            color = MonarchTheme.colors.textMuted,
        )
    }
}

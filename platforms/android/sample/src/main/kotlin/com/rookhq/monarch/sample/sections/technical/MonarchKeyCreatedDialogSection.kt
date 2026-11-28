package com.rookhq.monarch.sample.sections.technical

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.technical.keycreateddialog.MonarchKeyCreatedDialog
import com.rookhq.monarch.components.technical.keycreateddialog.MonarchKeyCreatedDialogKind
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of KeyCreatedDialog: a secret shown once, masked, and a public key. */
object MonarchKeyCreatedDialogSection : CataloguePage {
    override val title = "KeyCreatedDialog"

    @Composable
    override fun Content() {
        var shown by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Create a key")
        MonarchButton(text = "Create secret key", onClick = { shown = "secret" })
        MonarchButton(text = "Create masked key", onClick = { shown = "masked" })
        MonarchButton(text = "Show publishable key", onClick = { shown = "public" })
        when (shown) {
            "secret" -> MonarchKeyCreatedDialog(
                secret = "sk_live_51Hx9a2Lr8f3Qw7",
                onDone = { shown = null },
                name = "Production server",
                note = "The old key stops working in 24 hours.",
            )
            "masked" -> MonarchKeyCreatedDialog(
                secret = "whsec_8f2c1d4b",
                onDone = { shown = null },
                label = "Signing secret",
                masked = true,
            )
            "public" -> MonarchKeyCreatedDialog(
                secret = "pk_live_8Kd02",
                onDone = { shown = null },
                kind = MonarchKeyCreatedDialogKind.Public,
                label = "Publishable key",
            )
        }
    }
}

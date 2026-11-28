package dev.monarchui.sample.sections.technical

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.technical.secretfield.MonarchSecretField
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of SecretField: a key to reveal and copy, and a saved secret that is not shown. */
object MonarchSecretFieldSection : CataloguePage {
    override val title = "SecretField"

    @Composable
    override fun Content() {
        var key by rememberSaveable { mutableStateOf("sk_test_example_0000000000") }
        var secret by rememberSaveable { mutableStateOf("") }
        var token by rememberSaveable { mutableStateOf("abc") }
        CatalogueCaption("Reveal and copy")
        MonarchSecretField(value = key, label = "API key", onValueChange = { key = it }, copyable = true)
        CatalogueCaption("Saved, not shown")
        MonarchSecretField(value = secret, label = "Signing secret", onValueChange = { secret = it }, saved = true)
        CatalogueCaption("Error")
        MonarchSecretField(
            value = token,
            label = "Bot token",
            onValueChange = { token = it },
            errorText = if (token.startsWith("xoxb-")) null else "Tokens start with xoxb-.",
        )
        CatalogueCaption("Read-only")
        MonarchSecretField(
            value = "whsec_8f2c1d",
            label = "Webhook secret",
            onValueChange = {},
            readOnly = true,
            copyable = true,
        )
    }
}

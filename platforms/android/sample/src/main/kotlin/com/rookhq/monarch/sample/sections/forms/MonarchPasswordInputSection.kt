package com.rookhq.monarch.sample.sections.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.forms.passwordinput.MonarchPasswordInput
import com.rookhq.monarch.components.forms.passwordinput.MonarchPasswordInputPurpose
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of PasswordInput. */
object MonarchPasswordInputSection : CataloguePage {
    override val title = "PasswordInput"

    @Composable
    override fun Content() {
        var current by rememberSaveable { mutableStateOf("") }
        var created by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Sign in")
        MonarchPasswordInput(
            value = current,
            onValueChange = { current = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Password",
        )
        CatalogueCaption("Sign up, with an error")
        MonarchPasswordInput(
            value = created,
            onValueChange = { created = it },
            modifier = Modifier.fillMaxWidth(),
            label = "New password",
            purpose = MonarchPasswordInputPurpose.New,
            helperText = "At least 8 characters.",
            errorText = if (created.isNotEmpty() && created.length < 8) "Use at least 8 characters." else null,
        )
        CatalogueCaption("Disabled")
        MonarchPasswordInput(
            value = "secret",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = "Password",
            enabled = false,
        )
    }
}

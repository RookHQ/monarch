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
import com.rookhq.monarch.components.forms.passwordstrengthmeter.MonarchPasswordStrengthMeter
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of PasswordStrengthMeter. */
object MonarchPasswordStrengthMeterSection : CataloguePage {
    override val title = "PasswordStrengthMeter"

    @Composable
    override fun Content() {
        var password by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Under a new password field")
        MonarchPasswordInput(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = "New password",
            purpose = MonarchPasswordInputPurpose.New,
        )
        MonarchPasswordStrengthMeter(password = password, modifier = Modifier.fillMaxWidth())
        CatalogueCaption("Without the rules")
        MonarchPasswordStrengthMeter(password = "Abcdefg1", modifier = Modifier.fillMaxWidth(), showRules = false)
    }
}

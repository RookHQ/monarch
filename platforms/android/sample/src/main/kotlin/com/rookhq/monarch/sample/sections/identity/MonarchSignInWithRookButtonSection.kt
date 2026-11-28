package com.rookhq.monarch.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.identity.signinwithrookbutton.MonarchSignInWithRookButton
import com.rookhq.monarch.components.identity.signinwithrookbutton.MonarchSignInWithRookButtonSize
import com.rookhq.monarch.components.identity.signinwithrookbutton.MonarchSignInWithRookButtonVariant
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of SignInWithRookButton. */
object MonarchSignInWithRookButtonSection : CataloguePage {
    override val title = "SignInWithRookButton"

    @Composable
    override fun Content() {
        var starting by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Variants")
        MonarchSignInWithRookButtonVariant.entries.forEach { variant ->
            MonarchSignInWithRookButton(onClick = { starting = true }, variant = variant, label = variant.name)
        }
        CatalogueCaption("Sizes, with a mark")
        MonarchSignInWithRookButtonSize.entries.forEach { size ->
            MonarchSignInWithRookButton(onClick = {}, size = size, mark = rememberVectorPainter(SampleIcons.Lock))
        }
        CatalogueCaption("Starting sign-in")
        MonarchSwitchRow(checked = starting, onCheckedChange = { starting = it }, label = "Loading")
        MonarchSignInWithRookButton(onClick = { starting = true }, loading = starting, fullWidth = true)
        CatalogueCaption("Disabled")
        MonarchSignInWithRookButton(onClick = {}, enabled = false)
    }
}

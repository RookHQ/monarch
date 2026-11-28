package dev.monarchui.sample.sections.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.identity.signinwithrookbutton.MonarchSignInWithRookButton
import dev.monarchui.components.identity.signinwithrookbutton.MonarchSignInWithRookButtonSize
import dev.monarchui.components.identity.signinwithrookbutton.MonarchSignInWithRookButtonVariant
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

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

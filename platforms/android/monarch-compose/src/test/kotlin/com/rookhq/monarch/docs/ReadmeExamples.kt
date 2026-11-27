package com.rookhq.monarch.docs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.monarchTypography
import com.rookhq.monarch.theme.MonarchContrast
import com.rookhq.monarch.theme.MonarchScheme
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

// The Kotlin code blocks of README.md. Each region below starts with a
// "readme:" marker line and stops at an "end" marker line, and must appear in
// the README exactly; `ReadmeExamplesTest` checks it.

// readme: tokens
val canvasArgb: Long = MonarchTokens.light.colors.bgCanvas
val gutter = MonarchTokens.Space.space4.dp
val bodySize = MonarchTokens.FontSize.base.sp
val highContrastDark = MonarchTokens.hcDark.withAccent(MonarchTokens.Accents.atlas)
// end

// readme: getting-started
class InvoicesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonarchTheme {
                InvoicesScreen(onCreate = { })
            }
        }
    }
}

@Composable
fun InvoicesScreen(onCreate: () -> Unit) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(MonarchTheme.spacing.space4),
        ) {
            MonarchText(text = "Invoices", style = MonarchTextStyle.Title)
            MonarchText(text = "3 are due this week.", color = MonarchTheme.colors.textMuted)
            MonarchButton(text = "New invoice", onClick = onCreate, variant = MonarchButtonVariant.Primary)
        }
    }
}
// end

// readme: theme-setup
@Composable
fun AtlasTheme(content: @Composable () -> Unit) {
    MonarchTheme(
        scheme = MonarchScheme.System,
        contrast = MonarchContrast.System,
        accent = MonarchTokens.Accents.atlas,
        typography = monarchTypography(fontFamily = FontFamily.SansSerif),
        dynamicColor = false,
        content = content,
    )
}
// end

// readme: pinned-theme
@Composable
fun DarkAtlasPreview(content: @Composable () -> Unit) {
    MonarchTheme(tokens = MonarchTokens.dark.withAccent(MonarchTokens.Accents.atlas), content = content)
}
// end

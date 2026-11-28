package com.rookhq.monarch.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequence
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceKind
import com.rookhq.monarch.components.feedback.consequencelist.MonarchConsequenceList
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarShape
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.navigation.link.MonarchLink
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of OAuthConsentScreen: a recipe of existing parts. */
object OauthConsentScreenSection : CataloguePage {
    override val title = "OAuthConsentScreen"

    @Composable
    override fun Content() {
        CatalogueCaption("An unverified app asks for access")
        // Whether the identity provider has verified the app.
        val verified by remember { mutableStateOf(false) }
        var allowing by remember { mutableStateOf(false) }
        var denying by remember { mutableStateOf(false) }
        val scopes = listOf(
            MonarchConsequence(MonarchConsequenceKind.Kept, "See your name and email address"),
            MonarchConsequence(MonarchConsequenceKind.Kept, "Read your notes"),
            MonarchConsequence(
                MonarchConsequenceKind.Warning,
                "Create and delete notes",
                detail = "Notes can change anything in your notes.",
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchAvatar(
                    image = null,
                    name = "Notes",
                    shape = MonarchAvatarShape.Square,
                    size = MonarchAvatarSize.Lg,
                    decorative = true,
                )
                Column(modifier = Modifier.weight(1f)) {
                    MonarchText(
                        text = "Notes wants to access your account",
                        style = MonarchTextStyle.Heading,
                        heading = true,
                    )
                    MonarchText(text = "notes.example.com", style = MonarchTextStyle.Caption)
                }
            }
            if (verified) {
                MonarchBadge(text = "Verified", tone = MonarchTone.Success)
            } else {
                MonarchInlineNotice(
                    message = "Only continue if you know and trust Notes.",
                    title = "This app hasn’t been verified",
                    tone = MonarchTone.Warning,
                )
            }
            MonarchCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonarchAvatar(image = null, name = "Priya Raman", decorative = true)
                    Column {
                        MonarchText(text = "Priya Raman", style = MonarchTextStyle.BodyStrong)
                        MonarchText(text = "priya@example.com", style = MonarchTextStyle.Caption)
                    }
                }
            }
            MonarchConsequenceList(
                items = scopes,
                heading = "Notes will be able to:",
                kindLabel = { if (it == MonarchConsequenceKind.Warning) "Sensitive" else "Allowed" },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space1)) {
                MonarchText(text = "Before you allow, read Notes’", style = MonarchTextStyle.Caption)
                MonarchLink(text = "privacy policy", onClick = { /* Open the app's policy URL. */ }, external = true)
                MonarchText(text = "and", style = MonarchTextStyle.Caption)
                MonarchLink(text = "terms of service", onClick = { /* Open the app's terms URL. */ }, external = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
                MonarchButton(
                    text = "Cancel",
                    onClick = { denying = true },
                    modifier = Modifier.weight(1f),
                    enabled = !allowing,
                    loading = denying,
                )
                MonarchButton(
                    text = "Allow",
                    onClick = { allowing = true },
                    modifier = Modifier.weight(1f),
                    variant = MonarchButtonVariant.Primary,
                    enabled = !denying,
                    loading = allowing,
                )
            }
        }
    }
}

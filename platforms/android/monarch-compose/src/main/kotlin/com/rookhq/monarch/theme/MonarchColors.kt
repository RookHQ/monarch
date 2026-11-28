package com.rookhq.monarch.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.rookhq.monarch.tokens.MonarchColorTokens

/**
 * The semantic colour roles of the active theme as Compose [Color] values.
 *
 * A thin view over the generated [MonarchColorTokens]: every property reads
 * the token of the same name, so the two cannot hold different values.
 * `MonarchColorsTest` fails when the generated class gains or loses a role
 * that is not mirrored here.
 *
 * Read it through [MonarchTheme.colors].
 *
 * @property tokens the generated ARGB values this view reads.
 */
@Immutable
public class MonarchColors(public val tokens: MonarchColorTokens) {
    /** True in a scope that reads the accent roles as the AI accent (see `MonarchAiAccent`). */
    internal var aiAsAccent: Boolean = false
        private set

    /** The same roles with every accent role reading the AI accent: a choice mark in an AI component is magenta. */
    internal fun withAiAsAccent(): MonarchColors = MonarchColors(tokens).also { it.aiAsAccent = true }

    /** The `bgCanvas` role. */
    public val bgCanvas: Color get() = Color(tokens.bgCanvas)

    /** The `bgSurface` role. */
    public val bgSurface: Color get() = Color(tokens.bgSurface)

    /** The `bgRaised` role. */
    public val bgRaised: Color get() = Color(tokens.bgRaised)

    /** The `bgSubtle` role. */
    public val bgSubtle: Color get() = Color(tokens.bgSubtle)

    /** The `bgMuted` role. */
    public val bgMuted: Color get() = Color(tokens.bgMuted)

    /** The `bgEmphasis` role. */
    public val bgEmphasis: Color get() = Color(tokens.bgEmphasis)

    /** The `bgInverse` role. */
    public val bgInverse: Color get() = Color(tokens.bgInverse)

    /** The `textDefault` role. */
    public val textDefault: Color get() = Color(tokens.textDefault)

    /** The `textMuted` role. */
    public val textMuted: Color get() = Color(tokens.textMuted)

    /** The `textSubtle` role. */
    public val textSubtle: Color get() = Color(tokens.textSubtle)

    /** The `textDisabled` role. */
    public val textDisabled: Color get() = Color(tokens.textDisabled)

    /** The `textInverse` role. */
    public val textInverse: Color get() = Color(tokens.textInverse)

    /** The `textInverseMuted` role. */
    public val textInverseMuted: Color get() = Color(tokens.textInverseMuted)

    /** The `textLink` role. */
    public val textLink: Color get() = Color(tokens.textLink)

    /** The `borderSubtle` role. */
    public val borderSubtle: Color get() = Color(tokens.borderSubtle)

    /** The `borderDefault` role. */
    public val borderDefault: Color get() = Color(tokens.borderDefault)

    /** The `borderStrong` role. */
    public val borderStrong: Color get() = Color(tokens.borderStrong)

    /** The `borderInput` role. */
    public val borderInput: Color get() = Color(tokens.borderInput)

    /** The `borderInverse` role. */
    public val borderInverse: Color get() = Color(tokens.borderInverse)

    /** The `accentSolid` role. */
    public val accentSolid: Color get() = Color(if (aiAsAccent) tokens.aiSolid else tokens.accentSolid)

    /** The `accentSolidHover` role. */
    public val accentSolidHover: Color get() = Color(if (aiAsAccent) tokens.aiSolidHover else tokens.accentSolidHover)

    /** The `accentSolidActive` role. */
    public val accentSolidActive: Color
        get() = Color(if (aiAsAccent) tokens.aiSolidActive else tokens.accentSolidActive)

    /** The `accentOnSolid` role. */
    public val accentOnSolid: Color get() = Color(if (aiAsAccent) tokens.aiOnSolid else tokens.accentOnSolid)

    /** The `accentSubtle` role. */
    public val accentSubtle: Color get() = Color(if (aiAsAccent) tokens.aiSubtle else tokens.accentSubtle)

    /** The `accentSubtleHover` role. */
    public val accentSubtleHover: Color
        get() = Color(if (aiAsAccent) tokens.aiSubtleHover else tokens.accentSubtleHover)

    /** The `accentStrong` role. */
    public val accentStrong: Color get() = Color(if (aiAsAccent) tokens.aiStrong else tokens.accentStrong)

    /** The `accentOnStrong` role. */
    public val accentOnStrong: Color get() = Color(if (aiAsAccent) tokens.aiOnStrong else tokens.accentOnStrong)

    /** The `accentBorder` role. */
    public val accentBorder: Color get() = Color(if (aiAsAccent) tokens.aiBorder else tokens.accentBorder)

    /** The `accentStrongInverse` role. */
    public val accentStrongInverse: Color
        get() = Color(if (aiAsAccent) tokens.aiStrongInverse else tokens.accentStrongInverse)

    /** The `dangerSolid` role. */
    public val dangerSolid: Color get() = Color(tokens.dangerSolid)

    /** The `dangerSolidHover` role. */
    public val dangerSolidHover: Color get() = Color(tokens.dangerSolidHover)

    /** The `dangerSolidActive` role. */
    public val dangerSolidActive: Color get() = Color(tokens.dangerSolidActive)

    /** The `dangerOnSolid` role. */
    public val dangerOnSolid: Color get() = Color(tokens.dangerOnSolid)

    /** The `dangerSubtle` role. */
    public val dangerSubtle: Color get() = Color(tokens.dangerSubtle)

    /** The `dangerSubtleHover` role. */
    public val dangerSubtleHover: Color get() = Color(tokens.dangerSubtleHover)

    /** The `dangerStrong` role. */
    public val dangerStrong: Color get() = Color(tokens.dangerStrong)

    /** The `dangerOnStrong` role. */
    public val dangerOnStrong: Color get() = Color(tokens.dangerOnStrong)

    /** The `dangerBorder` role. */
    public val dangerBorder: Color get() = Color(tokens.dangerBorder)

    /** The `dangerStrongInverse` role. */
    public val dangerStrongInverse: Color get() = Color(tokens.dangerStrongInverse)

    /** The `successSolid` role. */
    public val successSolid: Color get() = Color(tokens.successSolid)

    /** The `successSolidHover` role. */
    public val successSolidHover: Color get() = Color(tokens.successSolidHover)

    /** The `successSolidActive` role. */
    public val successSolidActive: Color get() = Color(tokens.successSolidActive)

    /** The `successOnSolid` role. */
    public val successOnSolid: Color get() = Color(tokens.successOnSolid)

    /** The `successSubtle` role. */
    public val successSubtle: Color get() = Color(tokens.successSubtle)

    /** The `successSubtleHover` role. */
    public val successSubtleHover: Color get() = Color(tokens.successSubtleHover)

    /** The `successStrong` role. */
    public val successStrong: Color get() = Color(tokens.successStrong)

    /** The `successOnStrong` role. */
    public val successOnStrong: Color get() = Color(tokens.successOnStrong)

    /** The `successBorder` role. */
    public val successBorder: Color get() = Color(tokens.successBorder)

    /** The `successStrongInverse` role. */
    public val successStrongInverse: Color get() = Color(tokens.successStrongInverse)

    /** The `warningSolid` role. */
    public val warningSolid: Color get() = Color(tokens.warningSolid)

    /** The `warningSolidHover` role. */
    public val warningSolidHover: Color get() = Color(tokens.warningSolidHover)

    /** The `warningSolidActive` role. */
    public val warningSolidActive: Color get() = Color(tokens.warningSolidActive)

    /** The `warningOnSolid` role. */
    public val warningOnSolid: Color get() = Color(tokens.warningOnSolid)

    /** The `warningSubtle` role. */
    public val warningSubtle: Color get() = Color(tokens.warningSubtle)

    /** The `warningSubtleHover` role. */
    public val warningSubtleHover: Color get() = Color(tokens.warningSubtleHover)

    /** The `warningStrong` role. */
    public val warningStrong: Color get() = Color(tokens.warningStrong)

    /** The `warningOnStrong` role. */
    public val warningOnStrong: Color get() = Color(tokens.warningOnStrong)

    /** The `warningBorder` role. */
    public val warningBorder: Color get() = Color(tokens.warningBorder)

    /** The `warningStrongInverse` role. */
    public val warningStrongInverse: Color get() = Color(tokens.warningStrongInverse)

    /** The `infoSolid` role. */
    public val infoSolid: Color get() = Color(tokens.infoSolid)

    /** The `infoSolidHover` role. */
    public val infoSolidHover: Color get() = Color(tokens.infoSolidHover)

    /** The `infoSolidActive` role. */
    public val infoSolidActive: Color get() = Color(tokens.infoSolidActive)

    /** The `infoOnSolid` role. */
    public val infoOnSolid: Color get() = Color(tokens.infoOnSolid)

    /** The `infoSubtle` role. */
    public val infoSubtle: Color get() = Color(tokens.infoSubtle)

    /** The `infoSubtleHover` role. */
    public val infoSubtleHover: Color get() = Color(tokens.infoSubtleHover)

    /** The `infoStrong` role. */
    public val infoStrong: Color get() = Color(tokens.infoStrong)

    /** The `infoOnStrong` role. */
    public val infoOnStrong: Color get() = Color(tokens.infoOnStrong)

    /** The `infoBorder` role. */
    public val infoBorder: Color get() = Color(tokens.infoBorder)

    /** The `infoStrongInverse` role. */
    public val infoStrongInverse: Color get() = Color(tokens.infoStrongInverse)

    /** The `aiSolid` role. */
    public val aiSolid: Color get() = Color(tokens.aiSolid)

    /** The `aiSolidHover` role. */
    public val aiSolidHover: Color get() = Color(tokens.aiSolidHover)

    /** The `aiSolidActive` role. */
    public val aiSolidActive: Color get() = Color(tokens.aiSolidActive)

    /** The `aiOnSolid` role. */
    public val aiOnSolid: Color get() = Color(tokens.aiOnSolid)

    /** The `aiSubtle` role. */
    public val aiSubtle: Color get() = Color(tokens.aiSubtle)

    /** The `aiSubtleHover` role. */
    public val aiSubtleHover: Color get() = Color(tokens.aiSubtleHover)

    /** The `aiStrong` role. */
    public val aiStrong: Color get() = Color(tokens.aiStrong)

    /** The `aiOnStrong` role. */
    public val aiOnStrong: Color get() = Color(tokens.aiOnStrong)

    /** The `aiBorder` role. */
    public val aiBorder: Color get() = Color(tokens.aiBorder)

    /** The `aiStrongInverse` role. */
    public val aiStrongInverse: Color get() = Color(tokens.aiStrongInverse)

    /** The `aiThinking` role. */
    public val aiThinking: Color get() = Color(tokens.aiThinking)

    /** The `aiTool` role. */
    public val aiTool: Color get() = Color(tokens.aiTool)

    /** The `aiCitation` role. */
    public val aiCitation: Color get() = Color(tokens.aiCitation)

    /** The `aiDiffAdd` role. */
    public val aiDiffAdd: Color get() = Color(tokens.aiDiffAdd)

    /** The `aiDiffAddStrong` role. */
    public val aiDiffAddStrong: Color get() = Color(tokens.aiDiffAddStrong)

    /** The `aiDiffRemove` role. */
    public val aiDiffRemove: Color get() = Color(tokens.aiDiffRemove)

    /** The `aiDiffRemoveStrong` role. */
    public val aiDiffRemoveStrong: Color get() = Color(tokens.aiDiffRemoveStrong)

    /** The `confidenceLow` role. */
    public val confidenceLow: Color get() = Color(tokens.confidenceLow)

    /** The `confidenceMedium` role. */
    public val confidenceMedium: Color get() = Color(tokens.confidenceMedium)

    /** The `confidenceHigh` role. */
    public val confidenceHigh: Color get() = Color(tokens.confidenceHigh)

    /** The `focusRing` role. */
    public val focusRing: Color get() = Color(if (aiAsAccent) tokens.aiStrong else tokens.focusRing)

    /** The `overlayScrim` role. */
    public val overlayScrim: Color get() = Color(tokens.overlayScrim)

    /** The `data1` role. */
    public val data1: Color get() = Color(tokens.data1)

    /** The `data2` role. */
    public val data2: Color get() = Color(tokens.data2)

    /** The `data3` role. */
    public val data3: Color get() = Color(tokens.data3)

    /** The `data4` role. */
    public val data4: Color get() = Color(tokens.data4)

    /** The `data5` role. */
    public val data5: Color get() = Color(tokens.data5)

    /** Two instances are equal when they read the same token object. */
    override fun equals(other: Any?): Boolean = other is MonarchColors && other.tokens === tokens

    override fun hashCode(): Int = System.identityHashCode(tokens)
}

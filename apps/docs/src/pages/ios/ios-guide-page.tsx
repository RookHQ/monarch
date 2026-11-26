import nativeFacts from 'virtual:monarch-native-facts';
import ios from 'virtual:monarch-native-platform/ios';
import { PageFrame, PageSection } from '../../layout/page-frame.js';
import { NEW_TAB } from '../../lib/external.js';
import { href } from '../../lib/router.js';
import { CodeBlock } from '../../features/code/code-block.js';
import { REPO_FILE } from '../../features/native/labels.js';
import {
  AdaptiveTopics,
  CatalogueSummary,
  Elsewhere,
  Examples,
  GuideLink,
  PlatformMeta,
  PlatformTerms,
  PreviewGallery,
  Readme,
  Reference,
  Unreleased,
  hasGuide,
} from '../../features/native/landing.js';

const facts = nativeFacts.ios;
const { sections } = ios;
const code = (text: string) => <code className="docs-code-inline">{text}</code>;

/**
 * `/ios/guide/`: how Monarch for iOS is built and what it covers, the long
 * form behind the landing page (`/ios/`). A Swift package of SwiftUI styles,
 * wrappers and a few custom components. Prose about the approach is written here from
 * docs/architecture/NATIVE-PLATFORMS.md, docs/guides/native-adaptive-layout.md
 * and the README; installation, setup, tokens, accessibility and the known
 * limitations are the README's own sections; versions and counts are read at
 * build time (plugins/native.ts).
 */
export function IosGuidePage() {
  const r = facts.requirements;
  return (
    <PageFrame
      eyebrow="iOS"
      title="The Monarch for iOS guide"
      lead="The SwiftUI implementation of Monarch. It shares tokens, colour roles, type roles and component vocabulary with Monarch on the web and on Android. It looks like Monarch and behaves like iOS."
      meta={<PlatformMeta platform="ios" facts={facts} />}
    >
      <Unreleased facts={facts}>
        It is not tagged yet, so it cannot yet be added by package URL; use a local checkout, as
        described in <a href={href('/ios/get-started', 'installation')}>Get started</a>. The first
        release ships with the next {code('@rookcom/monarch-react')} patch, as a {code('0.14.N')}{' '}
        tag. No Rook product ships on it yet, so the API has not been proven in production and may
        change.
      </Unreleased>

      <PageSection id="foundation" title="Swift and SwiftUI">
        <p className="docs-text">
          Monarch for iOS is one Swift package, {code('Monarch')}, under {code('platforms/ios')},
          with two libraries: {code('MonarchTokens')}, generated constants that need only
          Foundation, and {code('MonarchUI')}, the SwiftUI theme, foundations and components.
          {r.swiftTools ? ` The manifest declares Swift tools version ${r.swiftTools}` : ''}
          {r.swiftTools && r.ios ? ` and iOS ${r.ios} as the deployment target` : ''}
          {r.swiftTools ? '.' : ''}
          {r.dependencies ? '' : ' The package has no dependencies.'}
        </p>
        <p className="docs-text">
          Most of the library is not new controls. Every item is classified before it is built:
        </p>
        <ul className="docs-list">
          <li>
            <strong>Native adoption.</strong> The SwiftUI control is used as it is inside the
            Monarch theme, and Monarch ships nothing: {code('Toggle')}, {code('Picker')},
            navigation, lists, sheets, menus and date pickers.
          </li>
          <li>
            <strong>A style or thin wrapper</strong> over a system control, applied the way SwiftUI
            intends: {code('.buttonStyle(.monarch(.primary))')},{' '}
            {code('.toggleStyle(.monarchCheckbox)')}, {code('.progressViewStyle(.monarchLinear)')}.
            The control stays the system's, so its VoiceOver roles, values and gestures do too.
          </li>
          <li>
            <strong>Custom</strong>, built by Monarch only where the platform has no equivalent,
            such as a badge or an inline banner.
          </li>
        </ul>
      </PageSection>

      <PageSection id="philosophy" title="Apple first">
        <p className="docs-text">
          Monarch has one design language and three implementations, and the iOS one is not a port
          of the React components. A Monarch screen on iOS looks like Monarch and behaves like iOS:
          fields, buttons, checkboxes, segmented controls, tabs, chips, badges, cards, banners and
          list rows have Monarch's own shapes, colours and type through SwiftUI styles and custom
          views, and the system keeps what it does best (the switch, which is tinted, sheets, menus,
          pickers, the keyboard and scrolling). It does not have to match the same screen on Android
          or the web: a sheet with detents and a navigation stack belong to iOS, and the same design
          can and should differ between platforms. For anything Monarch does not cover, start from
          Apple's developer documentation and the Human Interface Guidelines, which stay current
          when the platform changes.
        </p>
        <ul className="docs-list">
          <li>
            <strong>Navigation is SwiftUI's.</strong> Use {code('NavigationStack')},{' '}
            {code('NavigationSplitView')} and {code('TabView')}. Monarch does not provide a
            navigation component, a navigation bar or a tab bar.
          </li>
          <li>
            <strong>Sheets and alerts are the system's.</strong> Use {code('sheet')},{' '}
            {code('fullScreenCover')}, {code('popover')}, {code('alert')} and{' '}
            {code('confirmationDialog')}. Monarch does not provide a sheet, dialog or popover
            component.
          </li>
          <li>
            <strong>SF Symbols.</strong> {code('MonarchIcon')} shows SF Symbols at 16, 20 or 24
            points, decorative or labelled.
          </li>
          <li>
            <strong>Dynamic Type.</strong> All text and icons scale with Dynamic Type, including the
            accessibility sizes. Monarch does not cap or override the text-size setting.
          </li>
          <li>
            <strong>The system typeface.</strong> Text is set in San Francisco with Monarch's sizes,
            weights and tracking. The optional MonarchFontsPoppins product adds Poppins under the
            SIL Open Font License 1.1; an app that bundles another font passes the family name.
          </li>
          <li>
            <strong>No control the platform lacks.</strong> iOS has no radio button, so Monarch does
            not add one: a single choice is a {code('Picker')}.
          </li>
          <li>
            <strong>System settings are followed.</strong> {code('monarchTheme()')} follows Dark
            Mode and Increase Contrast and sets the tint, so system controls take the accent. Reduce
            Motion removes the press, checkbox and indeterminate-progress animations.
          </li>
        </ul>
        <p className="docs-text">
          Where a component differs from its Android and web counterparts, its page says how and why
          under <em>Platform behaviour</em>.
        </p>
      </PageSection>

      <PageSection id="components" title="Component catalogue">
        <CatalogueSummary platform="ios" facts={facts} />
      </PageSection>

      <PageSection id="previews" title="Native previews">
        <PreviewGallery platform="ios" items={ios.previews} />
        <p className="docs-text">
          {ios.previews.length
            ? 'These images are rendered from the SwiftUI components. '
            : 'This site does not show rendered images of the iOS components yet. '}
          In Xcode, components have a {code('#Preview')} in their source file, and the catalogue app
          in {code('platforms/ios/Sample')} has one page per component. Its Xcode project is
          generated with XcodeGen and is not committed; open the generated project and run the{' '}
          {code('MonarchCatalogue')} scheme on a simulator.
        </p>
        {facts.commands.sampleGenerate ? (
          <CodeBlock
            code={`cd platforms/ios\n${facts.commands.sampleGenerate}`}
            label="Code: generate the iOS catalogue project"
            lang="sh"
          />
        ) : null}
      </PageSection>

      <PageSection id="examples" title="Swift examples">
        <p className="docs-text">
          Examples come from the inventory. The generator writes each one into the test target, and
          the tests compile and render it in all four themes.
        </p>
        <Examples platform="ios" examples={ios.examples} />
      </PageSection>

      <PageSection id="accessibility" title="Accessibility">
        <Readme platform="ios" html={sections.accessibility} />
        {hasGuide('accessibility') ? (
          <p className="docs-text">
            <GuideLink id="accessibility">The accessibility and testing guide</GuideLink> covers the
            rules every native component follows.
          </p>
        ) : null}
      </PageSection>

      <PageSection id="ipad" title="iPad and adaptive layouts">
        <p className="docs-text">
          Monarch does not wrap SwiftUI's layout, window, navigation or presentation APIs. On iPad,
          use size classes, {code('NavigationSplitView')} and resizable windows as SwiftUI provides
          them; Monarch components keep their 44-point touch target at every window and text size.
        </p>
        <AdaptiveTopics platform="ios" facts={facts} />
      </PageSection>

      {sections['known-limitations'] ? (
        <PageSection id="limitations" title="Known limitations">
          <Readme platform="ios" html={sections['known-limitations']} />
        </PageSection>
      ) : null}

      {ios.foundations.length ? (
        <PageSection id="reference" title="Theme and foundations reference">
          <p className="docs-text">
            Inventory entries that have no web component, documented here in full.
          </p>
          <Reference platform="ios" foundations={ios.foundations} />
        </PageSection>
      ) : null}

      <PageSection id="releases" title="Migration and release notes">
        <p className="docs-text">
          Monarch for iOS has the version of {code('@rookcom/monarch-react')}, in the{' '}
          {code('0.14.x')} patch series, shared with Monarch for Android. Each release is a plain{' '}
          {code('0.14.N')} git tag, which Swift Package Manager reads through the root{' '}
          {code('Package.swift')}. A deprecated API is removed no sooner than 90 days and two patch
          releases after the release that deprecates it. The{' '}
          <a href={href('/ios/releases')}>release notes</a> are its changelog.
          {facts.unreleased && facts.versions <= 1
            ? ' There is no migration guide yet, because there is no earlier version to migrate from.'
            : ''}
        </p>
        {sections['experimental-api'] ? (
          <>
            <h3 className="docs-subtitle">Experimental API</h3>
            <Readme platform="ios" html={sections['experimental-api']} />
          </>
        ) : null}
        <p className="docs-text">
          The rules for deprecation and migration guides are in{' '}
          <a {...NEW_TAB} href={`${REPO_FILE}docs/governance/native-api-stability.md`}>
            Native API stability
          </a>
          .
        </p>
      </PageSection>

      <PageSection id="platform-terms" title="Platform terms">
        <PlatformTerms platform="ios" />
      </PageSection>

      <PageSection id="elsewhere" title="Elsewhere in Monarch">
        <Elsewhere platform="ios" />
      </PageSection>
    </PageFrame>
  );
}

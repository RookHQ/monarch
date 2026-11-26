import nativeFacts from 'virtual:monarch-native-facts';
import ios from 'virtual:monarch-native-platform/ios';
import { PageFrame, PageSection } from '../../layout/page-frame.js';
import { href } from '../../lib/router.js';
import {
  GuideLink,
  PlatformMeta,
  Readme,
  Unreleased,
  hasGuide,
} from '../../features/native/landing.js';

const facts = nativeFacts.ios;
const { sections } = ios;
const code = (text: string) => <code className="docs-code-inline">{text}</code>;

/**
 * `/ios/get-started/`: requirements, installation, the first screen, the
 * theme and the tokens. The text is the README's own sections (plugins/native.ts).
 */
export function IosGetStartedPage() {
  return (
    <PageFrame
      eyebrow="iOS"
      title="Get started with Monarch for iOS"
      lead="Add the Swift package, apply the Monarch theme at the root of the app and use the tokens. Everything here is the package README's own text."
      meta={<PlatformMeta platform="ios" facts={facts} />}
    >
      <Unreleased facts={facts}>
        It is not tagged yet, so it cannot yet be added by package URL; use a local checkout, as
        described under Installation below. The first release ships with the next{' '}
        {code('@rookcom/monarch-react')} patch, as a {code('0.14.N')} tag. No Rook product ships on
        it yet, so the API has not been proven in production and may change.
      </Unreleased>

      <PageSection id="requirements" title="Requirements and compatibility">
        <Readme platform="ios" html={sections.requirements} />
        {hasGuide('compatibility') ? (
          <p className="docs-text">
            <GuideLink id="compatibility">The compatibility matrix</GuideLink> records the tested
            toolchain and OS range. Monarch claims support only for what is in it.
          </p>
        ) : null}
      </PageSection>

      <PageSection id="installation" title="Installation">
        <Readme platform="ios" html={sections.installing} />
      </PageSection>

      <PageSection id="getting-started" title="Getting started">
        <Readme platform="ios" html={sections['getting-started']} />
      </PageSection>

      <PageSection id="theme-setup" title="Theme setup">
        <Readme platform="ios" html={sections['theme-setup']} />
      </PageSection>

      <PageSection id="tokens" title="Token usage">
        <Readme platform="ios" html={sections['token-usage']} />
        <p className="docs-text">
          The values are the ones on the <a href={href('/colours')}>Colours</a> and{' '}
          <a href={href('/tokens')}>Tokens</a> pages: one source generates the CSS variables and the
          Swift constants.
        </p>
      </PageSection>
    </PageFrame>
  );
}

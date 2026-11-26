import { useState } from 'react';
import { Button, InlineNotice } from '@rookcom/monarch-react';
import { InstallVerify, type InstallSnippet } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const box = { width: '100%', maxWidth: '40rem' } as const;
const now = '2026-01-10T12:00:00Z';

const snippets: InstallSnippet[] = [
  {
    id: 'html',
    label: 'HTML',
    code: '<script src="https://cdn.example.com/widget.js" data-key="pk_test_4f1a9c" async></script>',
    description: 'Paste it before the closing </body> tag on every page.',
  },
  {
    id: 'react',
    label: 'React',
    code: 'import { Widget } from \'example-widget\';\n\nexport function App() {\n  return <Widget publishableKey="pk_test_4f1a9c" />;\n}',
    description: 'Render it once, near the root of your app.',
  },
  {
    id: 'npm',
    label: 'npm',
    code: 'npm install example-widget',
    description: 'Install the package, then use the React snippet.',
  },
];

function Simulated() {
  const [connected, setConnected] = useState(false);
  return (
    <div style={{ ...box, display: 'grid', gap: 'var(--mn-space-4)' }}>
      <InstallVerify
        snippets={snippets}
        status={connected ? 'connected' : 'waiting'}
        targets={[
          { label: 'www.example.com', lastSeenAt: connected ? '2026-01-10T11:59:00Z' : null },
        ]}
        now={now}
        credential={
          <InlineNotice tone="success">
            Publishable key ready. It only works on your listed websites.
          </InlineNotice>
        }
        developerInstructions="Add this snippet to every page of www.example.com: <script src=…>"
        developerEmailHref="mailto:?subject=Install%20the%20widget"
        testHref="https://example.com/try"
        testHint="A page running the real widget, so you can try it yourself."
      />
      <Button size="sm" onClick={() => setConnected((c) => !c)}>
        {connected ? 'Reset demo' : 'Simulate the site reporting in'}
      </Button>
    </div>
  );
}

export default {
  id: 'install-verify',
  title: 'InstallVerify',
  summary: 'Install a snippet per platform, then watch for the site to report in.',
  sections: {
    purpose: {
      summary:
        'Guides a user through putting an embed (chat widget, tracking script, SDK) on their site and confirms it worked: code per platform with copy, a handoff for their developer, then "Waiting for your site…" until the product sees it and says "Connected on example.com". Needed by: Tickets (live chat setup wizard and Install tab).',
      useWhen: [
        'A product gives the user code to add to their own website or app.',
        'The product can tell when that code first reports in.',
      ],
      avoidWhen: [
        'Showing code with no install step: use CopyField or a code block.',
        'Connecting a domain: use DomainProgress.',
      ],
    },
    designPrinciples: {
      items: [
        'One clear next step: copy the code, or hand it to someone who can.',
        'Verification updates by itself; the user never has to press "check".',
        'Honest when it cannot check: the `unavailable` state says how to check by hand.',
        'Data in: the product polls and owns keys, URLs and handoff text.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref and other props to the root `<div>`.',
        'Use `part="install"` and `part="verify"` for separate wizard steps; headings show only with `part="all"`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Tickets `components/admin/channels/livechat/InstallVerify.tsx`): `InstallPanel` becomes `<InstallVerify part="install">` and `VerifyPanel` `<InstallVerify part="verify">`. `buildSnippet(kind)` for each `SNIPPET_KINDS` entry becomes `snippets` (`{ id: kind, label, code, description: SNIPPET_HELP[kind] }`); `KeyStatusLine` and the paste-a-key field go in `credential`; `developerInstructions(handoff)` and `developerMailto(handoff)` become `developerInstructions` and `developerEmailHref`.',
        '`useSetupStatus` stays in Tickets: map its result to `status` (`isMissingEndpoint` → `unavailable`, any seen origin → `connected`) and `status.data.origins` to `targets` (`{ label: hostOf(origin), lastSeenAt }`). `tryPageHref` / `tryOk` become `testHref` / `testDisabled`; `onConnected` is unchanged.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'part',
          values: ['all', 'install', 'verify'],
          description: 'Both halves with headings, or one half for a wizard step.',
        },
        {
          prop: 'status',
          values: ['waiting', 'connected', 'unavailable'],
          description: 'The verification state.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Waiting',
          attribute: 'data-status="waiting"',
          description: 'Pulsing dot and "Waiting for your site…".',
        },
        {
          name: 'Connected',
          attribute: 'data-status="connected"',
          description: 'Green check and "Connected on {hosts}". `onConnected` fires once.',
        },
        {
          name: 'Unavailable',
          attribute: 'data-status="unavailable"',
          description: 'Info icon and how to check by hand.',
        },
        {
          name: 'Seen target',
          attribute: 'data-seen',
          description: 'A target the code reported from, with a filled dot and "seen {time}".',
        },
        {
          name: 'Test disabled',
          attribute: 'disabled',
          description:
            '`testDisabled`: the test page is a disabled button with `testHint` beside it.',
        },
      ],
    },
    interaction: {
      items: [
        'Platform tabs switch the snippet; each snippet copies with its button and selects on focus.',
        '"Copy instructions for your developer" copies `developerInstructions`; "Email them" opens `developerEmailHref`.',
        '"Open a test page" opens `testHref` in a new tab.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Platform tabs, snippet, copy button, handoff actions, test link.' },
        {
          keys: 'Arrow Left / Right, Home, End',
          action: 'Move between platform tabs (selects on focus).',
        },
        { keys: 'Enter / Space', action: 'Copy, open the email link or the test page.' },
      ],
    },
    accessibility: {
      role: 'tablist, status, list',
      name: 'Tabs: `messages.platforms` ("Platform"). Each snippet: "{label} snippet". Targets list: `messages.targetsLabel`.',
      items: [
        'The verification box is a polite status, so "Connected on …" is announced when it changes.',
        'Waiting, connected and unavailable differ by text and icon, not colour alone.',
        'The test link says it opens a new tab.',
        'Copy results are announced by CopyButton.',
      ],
    },
    responsive: {
      items: [
        'Platform tabs scroll sideways inside themselves when they do not fit at 390px.',
        'Snippets wrap inside a scrolling text area; hosts wrap.',
        'Handoff and test actions are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Waiting uses accent, connected success, unavailable info tokens.',
        'Reduced motion: the waiting pulse and the connected pop are off.',
        'Forced colours: borders and indicators use `CanvasText`.',
      ],
    },
    examples: {
      items: [
        { title: 'Install and verify', stack: true, render: () => <Simulated /> },
        {
          title: 'Verify only, test page not ready',
          stack: true,
          render: () => (
            <div style={box}>
              <InstallVerify
                part="verify"
                snippets={snippets}
                status="waiting"
                targets={[{ label: 'www.example.com' }, { label: 'shop.example.com' }]}
                testHref="https://example.com/try"
                testDisabled
                testHint="Available once the key is ready."
              />
            </div>
          ),
        },
        {
          title: 'Cannot verify',
          stack: true,
          render: () => (
            <div style={box}>
              <InstallVerify part="verify" snippets={snippets} status="unavailable" />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Poll while the verify half is on screen and pass the latest `status` and `targets`.',
        'Put key or credential state in `credential`, with a retry when it failed.',
        'Use `unavailable` when the product cannot check, rather than waiting forever.',
      ],
      dont: [
        'Do not mark `connected` from a test page served by the product itself.',
        'Do not put secret keys in snippets; only publishable ones.',
      ],
    },
    related: {
      items: [
        { id: 'copy-field', reason: 'The snippet boxes.' },
        { id: 'copy-button', reason: 'The developer handoff button.' },
        { id: 'tabs', reason: 'The platform switcher.' },
        { id: 'domain-progress', reason: 'The same waiting-to-live pattern for custom domains.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'copy-button', 'copy-field', 'relative-time', 'tabs'],
    },
    testing: {
      unit: [
        'Both halves with headings, tabs and the first snippet selected; waiting status and targets.',
        'Arrow keys switch snippets and call `onSnippetChange`; `part="install"` hides headings.',
        'Copies the snippet and the developer instructions; email link.',
        '`onConnected` fires once across re-renders; connected hosts and "seen" times.',
        'Unavailable state; test link opens in a new tab; disabled test action.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

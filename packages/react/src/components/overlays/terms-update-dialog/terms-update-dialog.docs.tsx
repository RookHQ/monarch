import { useState } from 'react';
import { Button, TermsUpdateDialog } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const DOCUMENTS = [
  { label: 'Terms of Service', href: '#terms', version: '2026-12-01' },
  { label: 'Privacy Policy', href: '#privacy', version: '2026-12-01' },
];

function Summary() {
  return (
    <>
      <p>Here is what changed:</p>
      <ul>
        <li>We explain how long we keep support messages.</li>
        <li>We list the providers that host your data.</li>
      </ul>
    </>
  );
}

function BlockingUpdate() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Show blocking update</Button>
      <TermsUpdateDialog
        open={open}
        onOpenChange={setOpen}
        summary={<Summary />}
        effectiveDate="2026-12-01"
        documents={DOCUMENTS}
        onAccept={() => wait(800)}
        onDecline={() => setOpen(false)}
        declineLabel="Sign out"
      />
    </>
  );
}

function InformationalUpdate() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Show notice</Button>
      <TermsUpdateDialog
        mode="informational"
        open={open}
        onOpenChange={setOpen}
        title="We’ve updated our Privacy Policy"
        summary="We now explain how long we keep support messages. Nothing changes in how you use the product."
        effectiveDate="2026-12-01"
        documents={DOCUMENTS.slice(1)}
        onAccept={() => {}}
      />
    </>
  );
}

function FailingAccept() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Show update with an error</Button>
      <TermsUpdateDialog
        open={open}
        onOpenChange={setOpen}
        summary="We clarified how refunds work."
        effectiveDate="2026-12-01"
        documents={DOCUMENTS.slice(0, 1)}
        onAccept={async () => {
          await wait(800);
          throw new Error('We could not save your choice. Try again.');
        }}
        onDecline={() => setOpen(false)}
        declineLabel="Contact us"
      />
    </>
  );
}

export default {
  id: 'terms-update-dialog',
  title: 'TermsUpdateDialog',
  summary:
    'Tells signed-in people that the terms or a policy changed, with a summary, effective date and links; blocking or informational.',
  sections: {
    purpose: {
      summary:
        'Shown when the terms or a policy changed since the person last accepted. It says what changed and from when, links to the new documents, and records acceptance with pending and error states. It is a pattern, not legal advice: the product’s legal team decides when a change needs fresh acceptance (blocking) and when a notice is enough (informational), and the wording.',
      useWhen: [
        'The accepted version stored for a person is older than the current version of the terms or a policy.',
        'Telling people about a policy change they do not need to accept again (informational mode).',
      ],
      avoidWhen: [
        'Sign-up: use TermsAcceptance in the form.',
        'Cookie and tracking choices: a new consent policy version is handled by the consent components.',
        'Routine product news: use a banner or a toast.',
      ],
    },
    designPrinciples: {
      items: [
        'Summarise the change in plain words; the full documents are one click away.',
        'State the effective date, formatted for the person’s locale.',
        'Blocking means blocking: no close button, Escape or outside click; an honest way out (sign out, contact us) is offered through `onDecline` instead.',
        'Honest progress: acceptance can be async; the dialog stays open while it saves and shows a failure in place with a retry.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`TermsUpdateDialog` is controlled (`open` + `onOpenChange`) or uncontrolled (`defaultOpen`, `trigger`). It forwards its ref to the dialog element.',
        '`onAccept()` records the acceptance. Return a promise for the pending state; reject to show an error. On success the dialog closes.',
        '`onDecline()` runs the secondary action and leaves the dialog open; the product signs out or navigates away.',
        '`formatEffectiveDate(value, locale)` is exported: a `YYYY-MM-DD` string keeps its calendar day in every time zone.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`documents` is `{ label, href, version? }[]`.',
        'Every visible string is a prop for translation: `title`, `effectiveLabel`, `documentsLabel`, `versionLabel`, `acceptLabel`, `declineLabel`, `closeLabel`, `errorMessage`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'mode',
          values: ['blocking', 'informational'],
          description:
            '`blocking` (default): only accepting or the decline action leaves it; the accept button reads "Accept and continue". `informational`: dismissible with Escape, outside click or the close button; "Got it" calls `onAccept`. Exposed as `data-mode`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg', 'xl'],
          description: 'Dialog width; default `md`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pending',
          attribute: 'aria-busy (dialog and the running button), data-busy',
          description:
            'The running button shows a spinner and keeps focus; the other button is disabled; nothing dismisses the dialog.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description:
            'A danger Alert above the actions, linked to the accept button. The person can retry.',
        },
      ],
    },
    interaction: {
      items: [
        'Opening moves focus into the dialog (the first link or button); focus stays inside while it is open.',
        'Accept calls `onAccept`; a synchronous result closes it at once, a promise keeps it pending until it settles.',
        'Blocking: Escape and outside clicks are ignored and there is no close button.',
        'Informational: Escape, outside click and the close button close it without calling `onAccept`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between the links and buttons; focus stays inside.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused link or button.' },
        {
          keys: 'Escape',
          action: 'Closes it in informational mode; ignored in blocking mode and while pending.',
        },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal)',
      name: '`title`; the summary becomes the accessible description.',
      items: [
        'The effective date is a `<time>` element with a machine-readable `datetime`.',
        'The document list is a labelled region with a heading.',
        'An error is announced assertively (`role="alert"`) and describes the accept button.',
      ],
    },
    responsive: {
      items: [
        'Default width 32rem (`md`), never wider than the screen minus 2rem.',
        'Below 480px the footer buttons share the width; long summaries scroll inside the body while the actions stay visible.',
        'On touch screens document links are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Dialog surface, muted text, link and danger tokens; light, dark and more-contrast themes apply automatically.',
        'In forced colours the date and version notes use the system text colour.',
      ],
    },
    examples: {
      items: [
        { title: 'Blocking, with sign out', render: () => <BlockingUpdate /> },
        { title: 'Informational', render: () => <InformationalUpdate /> },
        { title: 'Error and retry', render: () => <FailingAccept /> },
      ],
    },
    doDont: {
      do: [
        'Open it when the stored accepted version is older than the current one, and store the new version in `onAccept`.',
        'Return the save promise from `onAccept` so progress and failure show in the dialog.',
        'Offer a real way out in blocking mode (sign out, contact us).',
        'Pass `locale` so the date matches the person’s language.',
      ],
      dont: [
        'Do not use blocking mode for changes that only need a notice.',
        'Do not hide what changed behind the links; summarise it.',
        'Do not describe the dialog as making the product compliant with a law; your legal team decides the wording.',
      ],
    },
    related: {
      items: [
        { id: 'terms-acceptance', reason: 'Acceptance at sign-up.' },
        { id: 'confirm-dialog', reason: 'The same pending and error pattern for actions.' },
        { id: 'dialog', reason: 'The base it is built on.' },
        { id: 'alert', reason: 'Shows the error inside the dialog.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'alert', 'link', 'dialog'],
    },
    testing: {
      unit: [
        'Opens as a labelled dialog with the summary as description, the effective date and document links; axe passes.',
        'Blocking: Escape and outside clicks do not dismiss it; focus stays inside.',
        'Blocking: accepting closes it; the decline action runs without closing it.',
        'Async accept: pending sets aria-busy, keeps focus and disables the decline button; success closes it once.',
        'Async accept: a rejection shows the error, describes the button, keeps it open and allows retry.',
        'Generic message for non-Error rejections; controlled `error`.',
        'Informational: Escape closes it without accepting; "Got it" accepts.',
        'Dates are formatted with Intl and the locale; calendar dates keep their day.',
        'Renders on the server (closed and open).',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

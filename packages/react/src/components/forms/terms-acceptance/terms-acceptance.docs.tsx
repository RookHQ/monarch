import { useState } from 'react';
import { Button, TermsAcceptance } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const DOCUMENTS = [
  { label: 'Terms of Service', href: '#terms', version: '2026-11-01' },
  { label: 'Privacy Policy', href: '#privacy', version: '2026-11-01' },
];

function SignUpForm() {
  const [sent, setSent] = useState<string | null>(null);
  return (
    <form
      style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 420 }}
      onSubmit={(event) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        setSent(`terms=${String(data.get('terms'))}`);
      }}
    >
      <TermsAcceptance documents={DOCUMENTS} name="terms" versionsName="terms_versions" />
      <div>
        <Button type="submit" variant="primary">
          Create account
        </Button>
      </div>
      {sent ? <p>Submitted: {sent}</p> : null}
    </form>
  );
}

function ControlledAcceptance() {
  const [accepted, setAccepted] = useState(false);
  const [at, setAt] = useState<string | null>(null);
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxWidth: 420 }}>
      <TermsAcceptance
        documents={DOCUMENTS}
        checked={accepted}
        onAcceptedChange={(next, details) => {
          setAccepted(next);
          setAt(next ? details.at : null);
        }}
      />
      <p>{at ? `Accepted at ${at}` : 'Not accepted yet'}</p>
    </div>
  );
}

export default {
  id: 'terms-acceptance',
  title: 'TermsAcceptance',
  summary:
    'The required "I agree to the Terms of Service and Privacy Policy" checkbox for sign-up, with linked documents and versions.',
  sections: {
    purpose: {
      summary:
        'Asks the person to agree to the product’s terms and policies before an account is created, links each document from the label, and reports which versions were accepted and when so the product can keep proof of acceptance. It is a pattern, not legal advice: the product’s legal team decides which documents are required, the wording and how acceptance is stored.',
      useWhen: [
        'Sign-up and checkout forms where agreeing to the terms is required to continue.',
        'Any form that must record which version of a document the person agreed to.',
      ],
      avoidWhen: [
        'Optional choices such as marketing messages: use MarketingConsentCheckbox.',
        'Telling signed-in people that the terms changed: use TermsUpdateDialog.',
        'Cookie and tracking choices: use the consent components (CookieConsentBanner, ConsentPreferences).',
      ],
    },
    designPrinciples: {
      items: [
        'An active choice: the box starts unticked; do not pre-tick it.',
        'The documents are readable before agreeing: each one is a link inside the label, opening in the same tab unless `target` says otherwise.',
        'Errors are explained, not just blocked: a submit attempt while unticked shows "Accept the terms to continue" under the box and moves focus to it.',
        'Proof built in: every change reports the document versions and an ISO timestamp.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`TermsAcceptance` forwards its ref to the wrapper `<div>`; other div props pass through.',
        '`onAcceptedChange(accepted, { versions, at })`: `versions` maps each document’s `id` (or `href`) to its `version`.',
        '`termsVersions(documents)` is exported, so a server can record the same versions the checkbox posts.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`documents` is `{ label, href, version?, id? }[]`.',
        'Every visible string is a prop for translation: `label` (a function that receives the joined links) and `requiredMessage`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'required',
          values: ['true', 'false'],
          description:
            'Default `true`: the native `required` attribute blocks a plain form submit while unticked, and the error appears.',
        },
        {
          prop: 'target',
          values: ['undefined', '_blank'],
          description:
            'Where the links open. `_blank` adds an external icon and an "opens in a new tab" note.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Unticked',
          attribute: 'data-state="unchecked"',
          description: 'The initial state. Nothing is posted.',
        },
        {
          name: 'Accepted',
          attribute: 'data-state="checked"',
          description: 'Posts `name=value` and, with `versionsName`, the versions as JSON.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid, aria-invalid (checkbox)',
          description:
            'After a submit attempt while unticked, or when `invalid` is set. The error is linked to the box and clears once it is ticked.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled (checkbox)',
          description: 'The box cannot be changed; the links still work.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the box or the label text ticks it; clicking a document link opens the document without ticking it.',
        'Submitting a form while the box is unticked shows the error, focuses the box and does not show the browser’s own bubble.',
        'Ticking the box clears the error. Resetting the form clears the box and the error.',
        'Works uncontrolled (`defaultChecked`) or controlled (`checked` + `onAcceptedChange`).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the box, then to each document link.' },
        { keys: 'Space', action: 'Ticks or clears the box.' },
        { keys: 'Enter (link)', action: 'Opens the document.' },
      ],
    },
    accessibility: {
      role: 'checkbox',
      name: 'The label, including the document names ("I agree to the Terms of Service and Privacy Policy").',
      items: [
        'The box has `aria-required="true"` when required and `aria-invalid` with the error as its description when invalid.',
        'The error is announced politely when it appears.',
        'Links inside the label are separate tab stops with their own names.',
      ],
    },
    responsive: {
      items: [
        'The label wraps; long document names break instead of overflowing at 390px.',
        'On touch screens the row is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Checkbox, link and field-error tokens; light, dark and more-contrast themes apply automatically.',
        'In forced colours the box keeps a visible border and the error state uses the system highlight colour.',
      ],
    },
    examples: {
      items: [
        { title: 'In a sign-up form', render: () => <SignUpForm />, stack: true },
        { title: 'Controlled, with the acceptance time', render: () => <ControlledAcceptance /> },
        {
          title: 'Custom wording, links in a new tab',
          render: () => (
            <TermsAcceptance
              documents={DOCUMENTS}
              target="_blank"
              label={(links) => <>I have read and accept the {links}</>}
            />
          ),
        },
        {
          title: 'Error state',
          render: () => <TermsAcceptance documents={DOCUMENTS} invalid />,
        },
      ],
    },
    doDont: {
      do: [
        'Give each document a `version` and store `{ versions, at }` with the account.',
        'Pass `locale` when rendering on the server so the joined list matches on the client.',
        'Keep the label short and let the documents carry the detail.',
      ],
      dont: [
        'Do not pre-tick the box or tick it for the person.',
        'Do not bundle marketing consent into this box; ask for it separately.',
        'Do not describe the box as making the product compliant with a law; your legal team decides the wording.',
      ],
    },
    related: {
      items: [
        { id: 'marketing-consent-checkbox', reason: 'The optional marketing opt-in next to it.' },
        { id: 'terms-update-dialog', reason: 'Asks signed-in people to accept changed terms.' },
        { id: 'checkbox', reason: 'The control it is built on.' },
        { id: 'form-field', reason: 'Provides the error wiring.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['checkbox', 'form-field', 'link'],
    },
    testing: {
      unit: [
        'Labels the box with linked documents; unticked and required by default; axe passes.',
        'Joins three documents with the locale list format; `target="_blank"` adds rel.',
        'Space toggles it and reports versions and time; clicking a link does not tick it.',
        'A native form submit while unticked is blocked, shows the error and focuses the box; once ticked, `new FormData(form)` holds the value and the versions.',
        'Posts nothing while unticked; `required={false}` is optional.',
        'Controlled `checked` and controlled `invalid` with a custom message.',
        'Custom label and disabled.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

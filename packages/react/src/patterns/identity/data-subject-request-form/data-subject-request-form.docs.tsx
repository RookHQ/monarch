import { Button } from '@rookcom/monarch-react';
import {
  DEFAULT_DATA_SUBJECT_REQUEST_TYPES,
  DataSubjectRequestForm,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const countries = [
  { code: 'AU', label: 'Australia' },
  { code: 'BR', label: 'Brazil' },
  { code: 'CA', label: 'Canada' },
  { code: 'DE', label: 'Germany' },
  { code: 'IN', label: 'India' },
  { code: 'GB', label: 'United Kingdom' },
  { code: 'US-CA', label: 'United States – California' },
  { code: 'US', label: 'United States – another state' },
];

function PublicForm() {
  return (
    <DataSubjectRequestForm
      countries={countries}
      verificationNote="To protect the account, we will email a link to confirm the request before acting on it."
      responseTimeNote="We will reply by email. The time it takes depends on where you live and the type of request."
      onSubmit={async () => {
        await wait(900);
        return { referenceId: 'PR-20260504-0193' };
      }}
    />
  );
}

function FailingForm() {
  return (
    <DataSubjectRequestForm
      countries={countries}
      defaultValues={{ type: 'access', email: 'alex@example.com', country: 'DE' }}
      onSubmit={async () => {
        await wait(800);
        throw new Error('The request could not be sent right now. Try again in a few minutes.');
      }}
    />
  );
}

function ShortList() {
  return (
    <DataSubjectRequestForm
      types={DEFAULT_DATA_SUBJECT_REQUEST_TYPES.filter((t) =>
        ['access', 'delete', 'correct'].includes(t.id),
      )}
      defaultValues={{ email: 'alex@example.com' }}
      successActions={<Button>Back to settings</Button>}
      onSubmit={() => wait(600).then(() => 'PR-0042')}
    />
  );
}

export default {
  id: 'data-subject-request-form',
  title: 'DataSubjectRequestForm',
  summary:
    'A public privacy-request form: request type cards, contact details, agent details, country, acknowledgement, reference on success.',
  sections: {
    purpose: {
      summary:
        'Gives people one clear place to ask for a copy of their data, deletion, correction, an objection or restriction, an opt-out of sale or sharing, or to withdraw consent, for themselves or as an authorized agent. It validates with an error summary and focus on the first problem, sends one typed request object, and shows the reference id the product returns. It collects a request; it does not decide which rights apply or how fast to answer. The product’s legal team decides the request types per country, the wording and any response times.',
      useWhen: [
        'A public privacy page (signed in or not) where anyone can send a privacy request.',
        'A support or help centre route for privacy requests that people then handle.',
      ],
      avoidWhen: [
        'A signed-in, self-service data download: use DataExportRequest.',
        'An operator erasing a person’s data: use GdprEraseFlow.',
        'Changing cookie or tracking choices: use ConsentPreferences.',
      ],
    },
    designPrinciples: {
      items: [
        'The request type is an explicit choice: no card is pre-selected unless the product passes `defaultValues.type` (for example from a link).',
        'Ask for little: email and country are required; the name is optional; details are required only for types that need them (correction by default).',
        'Agent requests show a separate block for the agent’s own name and email, and relabel the person’s fields ("Their email").',
        'Errors are summarised at the top for screen readers and next to each field; focus goes to the first problem.',
        'No legal promises: the response time is the product’s `responseTimeNote`; there is no default deadline.',
        'Country: Monarch has LocationFields, but it needs an async data adapter and adds region, city and postal code; a request form only needs one country or region, so it uses a NativeSelect from `countries`, or a text field without them.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onSubmit` receives a `DataSubjectRequest` with trimmed text; `agent` is present only for `authorized-agent`.',
        '`onSubmit` may resolve with a string or `{ referenceId }` to show the reference; anything else shows success without one.',
        '`DEFAULT_DATA_SUBJECT_REQUEST_TYPES` is exported so a product can filter or extend it.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'types',
          values: ['DEFAULT_DATA_SUBJECT_REQUEST_TYPES', 'DataSubjectRequestType[]'],
          description:
            'The request types offered; `detailsRequired` makes the details field required for that type.',
        },
        {
          prop: 'countries',
          values: ['undefined', 'DataSubjectRequestCountry[]'],
          description: 'A country or region select; without it, a text field.',
        },
        {
          prop: 'verificationNote',
          values: ['undefined', 'ReactNode'],
          description: 'An info note on how identity will be checked, before the acknowledgement.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Editing',
          attribute: 'data-state="editing"',
          description: 'The form; no errors are shown before the first submit.',
        },
        {
          name: 'Invalid',
          attribute: 'aria-invalid (fields), data-invalid (type group)',
          description:
            'After a submit with problems: summary at the top, message under each field; messages update as fields are fixed.',
        },
        {
          name: 'Sending',
          attribute: 'data-state="sending", aria-busy',
          description: 'While the promise from `onSubmit` is pending; repeat submits are ignored.',
        },
        {
          name: 'Failed',
          attribute: 'role="alert"',
          description: 'A rejection shows its message above the button and keeps every answer.',
        },
        {
          name: 'Sent',
          attribute: 'data-state="sent"',
          description: 'The success screen with the reference id and `responseTimeNote`.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing "Someone I am authorized to act for" shows the agent block; choosing "Myself" hides it again (its values are kept but not sent).',
        'Submitting validates everything; the first invalid field receives focus.',
        'Each summary entry is a link that moves focus to its field.',
        'After a successful submit, focus moves to the success heading.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between groups and fields; each radio group is one stop.',
        },
        { keys: 'Arrow keys', action: 'Choose a request type or relationship within its group.' },
        { keys: 'Space', action: 'Ticks the acknowledgement.' },
        { keys: 'Enter', action: 'Submits from a text field, or follows a summary link.' },
      ],
    },
    accessibility: {
      role: 'form content with fieldsets, radio groups and an alert summary',
      name: 'Each group is named by its legend; each field by its FieldLabel.',
      items: [
        'Request cards are native radios named by their label and described by their one-line description.',
        'The error summary is `role="alert"` and is re-announced on every invalid submit.',
        'Fields are wired with Field: `aria-invalid`, `aria-required` and `aria-describedby` for help and error text.',
        'The type group is described by its error message when nothing is chosen.',
        'The success heading receives focus so the outcome is read out.',
      ],
    },
    responsive: {
      items: [
        'Request cards fill as many columns of at least 16rem as fit, one column at 390px.',
        'Relationship choices sit side by side and wrap on narrow screens.',
        'Radio cards and the submit button are at least 44px tall on coarse pointers; below 30rem the button spans the width.',
      ],
    },
    theme: {
      items: [
        'Radio cards use the same tokens as ChoiceDialog: accent border and subtle fill when chosen, focus ring on the card.',
        'The summary uses the danger border and subtle fill; the success panel the success ones.',
        'Forced-colors mode uses native radios, Highlight for the chosen card and LinkText for summary links.',
      ],
    },
    examples: {
      items: [
        { title: 'Public privacy request form', render: () => <PublicForm /> },
        { title: 'A failing submission', render: () => <FailingForm /> },
        { title: 'Fewer types, prefilled email and a success action', render: () => <ShortList /> },
      ],
    },
    doDont: {
      do: [
        'Pass the types your legal team has approved for the places you serve.',
        'Return a reference id from `onSubmit` so people can follow up.',
        'Say how identity is checked in `verificationNote` and when to expect a reply in `responseTimeNote`.',
        'Pass `countries` with the codes your regime map uses.',
      ],
      dont: [
        'Do not state legal deadlines or name laws unless your legal team wrote that copy.',
        'Do not ask for more personal information than you need to handle the request.',
        'Do not pre-select a request type without a reason (such as a link from "Delete my data").',
      ],
    },
    related: {
      items: [
        {
          id: 'data-export-request',
          reason: 'Self-service copy of the data for signed-in people.',
        },
        { id: 'gdpr-erase-flow', reason: 'How an operator carries out an erasure request.' },
        { id: 'consent-preferences', reason: 'Where people change cookie and tracking choices.' },
        { id: 'privacy-choices-link', reason: 'The footer link that can lead to this form.' },
        {
          id: 'location-fields',
          reason: 'Full address entry, when a country alone is not enough.',
        },
        { id: 'form-field', reason: 'The label, help and error wiring used for every field.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'alert', 'checkbox', 'form-field', 'text-field'],
    },
    testing: {
      unit: [
        'Renders six unselected type cards with descriptions, relationship, fields and the verification note; axe passes.',
        'Invalid submit: summary with links in field order, focus on the first problem, aria-invalid and descriptions; links move focus; fixed fields leave the summary; axe passes.',
        'Details become required for the correction type.',
        'Agent block appears for an authorized agent, is validated and sent; axe passes.',
        'The agent block hides again for "Myself".',
        'Busy while sending, repeat submits ignored, rejection shown with answers kept.',
        'Success shows the reference and response note and focuses the heading; axe passes.',
        'Text field without countries; custom types and messages; void result shows success without a reference.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

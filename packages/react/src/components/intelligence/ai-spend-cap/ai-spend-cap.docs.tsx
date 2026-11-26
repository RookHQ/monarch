import { useState } from 'react';
import { AiSpendCap, type AiSpendCapSettings } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const RESETS_AT = '2026-12-01T00:00:00Z';

const BREAKDOWN = [
  { id: 'research', label: 'Research agent', spent: 2310 },
  { id: 'summaries', label: 'Meeting summaries', spent: 1245 },
  { id: 'drafting', label: 'Email drafting', spent: 645 },
];

function EditableDemo() {
  const [settings, setSettings] = useState<AiSpendCapSettings>({
    limit: 5000,
    warnAt: 0.8,
    behaviour: 'pause',
  });
  return (
    <AiSpendCap
      spent={4200}
      currency="GBP"
      locale="en-GB"
      period="month"
      resetsAt={RESETS_AT}
      timeZone="UTC"
      maxLimit={50000}
      breakdown={BREAKDOWN}
      {...settings}
      onSave={setSettings}
    />
  );
}

export default {
  id: 'ai-spend-cap',
  title: 'AiSpendCap',
  summary: 'Set a spending limit for AI usage and see where spending stands against it.',
  sections: {
    purpose: {
      summary:
        'Lets a person or a workspace admin cap what AI may spend in a day, week or month, shows the spending against that cap, and says plainly what happens when it is reached, so an agent can never run up an unexpected bill.',
      useWhen: [
        'AI usage costs money and someone must be able to set a ceiling on it (workspace settings, a billing page, an agent’s settings).',
        'People should see how close spending is to the limit and what happens at the limit.',
        'An agent has paused at the limit and the person needs a clear way to raise it or ask for more.',
      ],
      avoidWhen: [
        'You only report usage against quotas the person cannot change: use AiUsageMeter.',
        'The quota is not money (tokens, credits, storage, seats): use AiUsageMeter or UsageMeter.',
        'You need the cost of one run step by step: use AiAgentRunTimeline.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in through props, decisions out through callbacks. It never bills, blocks, fetches or stores anything.',
        'Money is always an integer (minor units or micros) with a currency code, formatted by `Intl.NumberFormat`; a float never holds an amount.',
        'Nothing changes until Save. Removing the limit is a separate action with its own inline confirmation.',
        'The state is said in words with an icon; colour only supports it.',
        'The choice at the limit is written as what will happen ("AI stops…", "AI carries on…"), not as a setting name.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<section>` attributes pass through and the ref is forwarded to the root.',
        '`aiSpendCapState({ spent, limit, warnAt })` returns `ok`, `warning`, `reached` or `no-limit`, for apps that branch on it (for example to stop sending).',
        '`formatAiSpendAmount(amount, { currency, unit, locale })` formats an integer amount the same way the component does. `aiSpendAmountToMajor` and `aiSpendAmountFromMajor` convert between the integer and a typed amount.',
        '`unit="micros"` means millionths of the major unit: `12_400_000` is 12.40. `spent`, `limit`, `maxLimit`, the breakdown and the saved `limit` all use the same unit.',
        '`onSave` receives `{ limit, warnAt, behaviour }`; `limit` is `null` after "Remove limit" is confirmed. Store it and pass the new values back as props.',
        'Pass `timeZone` with `resetsAt` for identical server and client rendering of the date.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'No visual variants. `canEdit={false}` gives the read-only view; `unit` picks minor units or micros.',
      ],
    },
    states: {
      items: [
        {
          name: 'Within limit',
          attribute: 'data-state="ok"',
          description: 'Neutral bar and "Within limit".',
        },
        {
          name: 'Warning',
          attribute: 'data-state="warning"',
          description: 'At or above `warnAt`: warning bar and "Close to the limit".',
        },
        {
          name: 'Reached',
          attribute: 'data-state="reached"',
          description:
            'At or over the limit: danger bar and "Limit reached". With `behaviour="pause"` a notice says AI is paused and offers "Raise limit".',
        },
        {
          name: 'No limit',
          attribute: 'data-state="no-limit"',
          description: 'The amount spent only, with no bar, and "Set a limit".',
        },
        {
          name: 'Editing',
          attribute: 'data-editing',
          description: 'The form is open. Controlled with `editing`, or uncontrolled.',
        },
        {
          name: 'Read-only',
          attribute: 'data-readonly',
          description:
            '`canEdit={false}`: no form, a line on who can change the limit and, with `onRequestIncrease`, a request button.',
        },
        {
          name: 'Saving',
          description: '`saving`: the Save button is busy and the form is locked.',
        },
      ],
      notes: ['`data-behaviour` on the root carries `pause` or `notify`.'],
    },
    interaction: {
      items: [
        '"Edit limit" (or "Set a limit", or "Raise limit" in the paused notice) opens the form and focuses the amount.',
        'Save validates the amount: it must be there, above zero and no more than `maxLimit`. The amount is read as typed in the locale ("1.250,50" in German).',
        'An amount below what is already spent is not saved at first: a message explains what it means and the button becomes "Save anyway".',
        'Save calls `onSave` once with the settings and closes the form; Cancel closes it and throws the draft away.',
        '"Remove limit" asks inline, with focus on "Keep limit"; confirming calls `onSave` with `limit: null`.',
        'When the form closes, focus returns to the button that opens it.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through the buttons and, in the form, the amount, warn-at, the radio group and the actions.',
        },
        { keys: 'Enter (in the amount)', action: 'Saves, the same as the Save button.' },
        { keys: 'Arrow Up / Down (in the amount)', action: 'Raises or lowers the amount by one.' },
        {
          keys: 'Arrow keys (in the radio group)',
          action: 'Move to and select the other behaviour.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
      ],
    },
    accessibility: {
      role: 'group (a named `<section>`)',
      name: 'The visible heading, `messages.title`',
      items: [
        'The bar is a `role="meter"` named "Spent against the limit" whose value text is the amount line, e.g. "£12.40 of £50.00 this month".',
        'The state is text with a decorative icon. A change of state is announced once through a polite status region; the first state and changes of amount are not announced.',
        'The form is a named `form`. The amount is a labelled spinbutton described by its hint and its error; the behaviour is a `radiogroup` whose radios are described by their plain-language text.',
        'Errors appear in a polite live region and focus returns to the amount.',
        'The removal confirmation is a named group described by its consequence; no browser dialog is used.',
        'The reset date is a `<time>` with a machine-readable `datetime`.',
      ],
    },
    responsive: {
      items: [
        'Heading, state, amount and details wrap; there is no horizontal scroll at 390px.',
        'On a narrow screen or a narrow AI surface the form actions share the width.',
        'Buttons grow to 44px touch targets on coarse pointers.',
        'RTL: logical properties throughout.',
      ],
    },
    theme: {
      items: [
        'Surface `--mn-color-bg-surface` with a `--mn-color-border-subtle` border.',
        'The state chip uses the success, warning and danger subtle and strong tokens; no limit is neutral.',
        'The bar uses Progress meter tones: neutral within the limit, warning, then danger.',
        'In forced colours the chip, the borders and the confirmation use CanvasText on Canvas.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Edit, save and remove',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '26rem' }}>
              <EditableDemo />
            </div>
          ),
        },
        {
          title: 'Within the limit',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '26rem' }}>
              <AiSpendCap
                spent={1240}
                limit={5000}
                currency="GBP"
                locale="en-GB"
                period="month"
                resetsAt={RESETS_AT}
                timeZone="UTC"
              />
            </div>
          ),
        },
        {
          title: 'Reached and paused',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '26rem' }}>
              <AiSpendCap
                spent={5000}
                limit={5000}
                currency="GBP"
                locale="en-GB"
                period="month"
                behaviour="pause"
                resetsAt={RESETS_AT}
                timeZone="UTC"
              />
            </div>
          ),
        },
        {
          title: 'Read-only, with a request',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '26rem' }}>
              <AiSpendCap
                spent={5000}
                limit={5000}
                currency="EUR"
                locale="de-DE"
                period="week"
                canEdit={false}
                onRequestIncrease={() => {}}
              />
            </div>
          ),
        },
        {
          title: 'No limit, amounts in micros',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '26rem' }}>
              <AiSpendCap
                spent={3_275_000}
                limit={null}
                unit="micros"
                currency="USD"
                locale="en-US"
                period="day"
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Enforce the limit on the server; this component only shows and collects it.',
        'Pass integers straight from your API in minor units or micros, with the currency code.',
        'Set `maxLimit` when a plan or a policy caps what this person may choose.',
        'Use `messages.readOnly` to name who can change the limit when the person cannot.',
      ],
      dont: [
        'Do not convert amounts to floating-point numbers before passing them in.',
        'Do not apply the settings before `onSave` fires.',
        'Do not show estimated spending as if it were measured.',
        'Do not use it for token or credit quotas: that is AiUsageMeter.',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-usage-meter',
          reason: 'Reports AI usage against quotas, without a limit to set.',
        },
        { id: 'usage-meter', reason: 'Plan usage on a billing page.' },
        { id: 'currency-input', reason: 'The amount field the form is built on.' },
        { id: 'progress-bar', reason: 'The meter the bar is built on.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'currency-input',
        'form-field',
        'inline-notice',
        'money',
        'number-input',
        'progress-bar',
        'radio-group',
      ],
    },
    testing: {
      unit: [
        'aiSpendCapState maps spending to ok, warning, reached and no-limit, with a custom warnAt and a zero limit.',
        'Money formatting from integer minor units and micros in en-GB, en-US and de-DE; conversion between typed amounts and integers.',
        'Named region, amount line, meter value text, state text, details and reset date; amount only without a limit.',
        'State changes are announced once; the first state and changes of amount are not.',
        'Opening the form, editing the amount, warn-at and behaviour, saving with the button and with Enter, cancelling, focus on open and close.',
        'Validation: empty, zero, unreadable, above maxLimit, and below what is spent until saved again.',
        'Removing the limit through the inline confirmation, with focus movement and no browser dialog.',
        'Paused notice and "Raise limit"; read-only view and the request button; controlled `editing`; `saving`.',
        'A saved warn-at share outside the choices (such as 100%) opens the form on the nearest choice, and that choice is what Save reports.',
        'Translated strings and custom warn-at options; ref and prop forwarding.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

import { Button, InlineNotice } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'inline-notice',
  title: 'InlineNotice',
  summary: 'A one-line tone message attached to a field, row, card or panel.',
  sections: {
    purpose: {
      summary:
        'A short message that belongs to one thing on the page: under a form, inside a card, in a table row or a side panel. Icon, an optional bold lead-in, one sentence and at most one action.',
      useWhen: [
        'A note about one control or record: "Unverified. Emails from this domain may land in spam."',
        'A small inline error with a retry, where an empty or error state would be too big.',
        'An edit conflict with one resolving action next to the editor.',
      ],
      avoidWhen: [
        'The message needs a title and a paragraph, or describes a whole section: use Alert.',
        'The message is about the account, the app or the whole view (trial, maintenance, scope): use CalloutBanner.',
        'Confirming an action that just happened: use Toast.',
        'A validation message for one field: use the Field error (FieldError), which is wired to the input.',
      ],
    },
    designPrinciples: {
      items: [
        'One line, one idea. If it needs a second paragraph it is an Alert.',
        'It sits next to what it is about and is the same width as that thing, not the page.',
        'Silent by default; `live` when it appears because of something that just happened. `live` (true) picks the politeness from the tone.',
        'Never colour alone: every tone has its own icon, and `plain` text uses the tone’s strong colour, which meets 4.5:1 on every surface.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root `<div>`; native attributes pass through.',
        'The message renders in a `<p>`, so `children` must be phrasing content (text, links, `<strong>`).',
        'Renders nothing while closed.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['neutral', 'info', 'success', 'warning', 'danger'],
          description: 'Colour and default icon. Default `info`. Exposed as `data-tone`.',
        },
        {
          prop: 'variant',
          values: ['subtle', 'plain'],
          description:
            '`subtle`: a tinted strip. `plain`: icon and coloured text with no fill or padding, for tight spots. Default `subtle`. Exposed as `data-variant`.',
        },
        {
          prop: 'live',
          values: ['true', 'polite', 'assertive'],
          description:
            '`true` = by tone (danger → alert, others → status); `polite` → status; `assertive` → alert. Unset: no live role.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Open', description: 'Rendered. The default.' },
        {
          name: 'Dismissed',
          description:
            'Uncontrolled: hidden after the close button is pressed. Controlled: hidden when `open` becomes false.',
        },
        {
          name: 'Live',
          attribute: 'role="status" | role="alert" (on the message)',
          description: 'Announced when it appears; only when `live` is set.',
        },
      ],
    },
    interaction: {
      items: [
        '`action` renders one control on the trailing edge, for example a `size="xs"` Button.',
        'The close button appears with `dismissible` or `onDismiss`; pressing it calls `onDismiss` and, when uncontrolled, hides the notice.',
        'After dismissal, focus moves to the next focusable element (or the previous one), never to the page body.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the action, then the close button.' },
        { keys: 'Enter / Space', action: 'Activates the action or dismisses the notice.' },
      ],
      notes: ['The notice itself is not focusable and never takes focus when it appears.'],
    },
    accessibility: {
      role: 'none, status or alert (on the message, with `live`)',
      name: 'Not named; the message is its content.',
      items: [
        'The icon is `aria-hidden`; the lead-in (`title`) and the sentence carry the meaning and are read together.',
        'The live role is on the message only, so the action and close button labels are not announced with it.',
        'The close button is an IconButton labelled `dismissLabel` (default "Dismiss").',
      ],
    },
    responsive: {
      items: [
        'The message wraps; the action and close button stay on the trailing edge, vertically centred.',
        'It never exceeds its container (`max-width: 100%`), down to 320px.',
      ],
    },
    theme: {
      items: [
        'Subtle uses the tone’s `subtle` background with default text and a `strong` icon; plain uses the tone’s `strong` colour for text.',
        'Light, dark and high-contrast themes apply through tokens. No animation.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Tones',
          stack: true,
          render: () => (
            <>
              <InlineNotice tone="info">Members sync from Rook ID every 15 minutes.</InlineNotice>
              <InlineNotice tone="success">Connected to the shared inbox.</InlineNotice>
              <InlineNotice tone="warning" title="Unverified.">
                Emails from this domain may land in spam.
              </InlineNotice>
              <InlineNotice
                tone="danger"
                live
                action={
                  <Button size="xs" onClick={() => {}}>
                    Retry
                  </Button>
                }
              >
                Could not load comments.
              </InlineNotice>
              <InlineNotice tone="neutral" dismissible>
                Draft restored from this device.
              </InlineNotice>
            </>
          ),
        },
        {
          title: 'Plain, under a control',
          stack: true,
          render: () => (
            <>
              <InlineNotice variant="plain" tone="success">
                Domain verified
              </InlineNotice>
              <InlineNotice variant="plain" tone="warning">
                Renews in 6 days
              </InlineNotice>
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Place it directly next to what it is about.',
        'Keep it to one sentence; use the `title` lead-in for the key word ("Conflict.").',
        'Give an error a way out with `action` (Retry, Use theirs).',
      ],
      dont: [
        'Do not use it for page-level messages; that is CalloutBanner.',
        'Do not repeat a field error in an InlineNotice; the Field already announces it.',
        'Do not make static notices live.',
      ],
    },
    related: {
      items: [
        { id: 'alert', reason: 'Section message with a title and a paragraph.' },
        { id: 'callout-banner', reason: 'Page- or app-level banner, optionally a landmark.' },
        { id: 'form-field', reason: 'Field errors and descriptions tied to an input.' },
        { id: 'toast', reason: 'Transient confirmation of an action.' },
        { id: 'button', reason: 'The action and the close IconButton.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button'],
      notes: ['Intent `subtle` and `strong` tokens.'],
    },
    testing: {
      unit: [
        'Renders the message with tone, variant and a decorative icon; axe clean.',
        'Forwards its ref and native attributes.',
        'live picks status or alert by tone, or as given.',
        'Runs the trailing action from the keyboard; axe clean.',
        'Dismisses with the keyboard and moves focus to the next control.',
        'Controlled `open` wins over dismissal.',
        'Plain variant, custom icon and no icon; dismissible without a callback with a custom label.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/inline-notice'],
    },
  },
} satisfies ComponentDocs;

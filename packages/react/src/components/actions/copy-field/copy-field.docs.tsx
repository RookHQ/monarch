import { CopyField, Field, FieldDescription, FieldLabel } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const embed = `<script src="https://example.com/widget.js"
  data-workspace="ws_01HZX4" async></script>`;

export default {
  id: 'copy-field',
  title: 'CopyField',
  summary: 'A read-only value with a copy button: links, records, keys and snippets.',
  sections: {
    purpose: {
      summary:
        'Shows a value the user needs to take elsewhere (a webhook URL, a DNS record, an embed snippet, a calendar feed) in a read-only text box with a CopyButton beside it.',
      useWhen: [
        'The user should see the value and copy it, for example while configuring another service.',
        'The value is long or structured: `multiline` shows it in a text area.',
      ],
      avoidWhen: [
        'The value is a secret: use SecretField, which masks it.',
        'Only the button is needed and the value is shown elsewhere: use CopyButton.',
        'The user should edit the value: use Input.',
      ],
    },
    designPrinciples: {
      items: [
        'The value is always selectable by hand: focus selects all of it, and a failed copy focuses and selects it.',
        'One label names both parts: the text box ("Webhook URL") and the button ("Copy Webhook URL").',
        'Works inside a Field, so a visible label, help text and errors are wired the usual way.',
        'Monospace by default, because most copied values are codes, hosts or URLs.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<div class="mn-copy-field">` (the ref) holding a read-only Input or Textarea and a CopyButton.',
        'Inside a Field the control takes the Field id, description and error wiring from `useFieldControl`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: ['`copyButtonProps` passes extra props (for example `variant`) to the CopyButton.'],
    },
    variants: {
      items: [
        {
          prop: 'multiline',
          values: ['true', 'false'],
          description:
            'Text area (`rows`, default 3) instead of a one-line input (`data-multiline`).',
        },
        {
          prop: 'mono',
          values: ['true', 'false'],
          description: 'Monospace value (`data-mono`). Default true.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg'],
          description: 'Input and button size. Default `md`.',
        },
        {
          prop: 'iconOnly',
          values: ['true', 'false'],
          description: 'Icon-only copy button for narrow layouts.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Read-only',
          attribute: 'readonly',
          description: 'The value can be selected but not changed.',
        },
        {
          name: 'Copied / error',
          attribute: 'data-state on .mn-copy-field__button',
          description: 'From CopyButton. On error the value is also focused and selected.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'Both the text box and the button are disabled.',
        },
      ],
    },
    interaction: {
      items: [
        'Focusing the text box selects the whole value (`selectOnFocus`, default true).',
        'The copy button copies `value` and confirms like CopyButton.',
        'If copying fails, the value is focused and selected, then `onCopyError` is called.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Focuses the text box (selecting the value), then the copy button.',
        },
        { keys: 'Ctrl/⌘ + C', action: 'Copies the selected value by hand.' },
        { keys: 'Enter / Space', action: 'On the button: copies the value.' },
      ],
    },
    accessibility: {
      role: 'textbox (read-only) and button',
      name: 'Outside a Field, `label` is the text box `aria-label`; inside one, the FieldLabel. The button is named "Copy {label}".',
      items: [
        'The copy result is announced by CopyButton’s polite live region.',
        'Read-only rather than disabled, so the value stays focusable and readable by screen readers.',
        'Spell-check and autocomplete are off for the value.',
      ],
    },
    responsive: {
      items: [
        'The text box shrinks to fit (`min-width: 0`); the button keeps its size, so it works at 320px.',
        'Long one-line values are truncated visually but remain complete for selection and copying.',
        'Multi-line values scroll inside the text area instead of wrapping.',
      ],
    },
    theme: {
      items: [
        'The read-only background is `--mn-color-bg-subtle`, so it reads as "not editable" in every theme.',
        'All colours are Monarch tokens; more-contrast themes apply automatically.',
      ],
    },
    examples: {
      items: [
        {
          title: 'One-line values',
          stack: true,
          render: () => (
            <>
              <CopyField label="Webhook URL" value="https://example.com/hooks/inbound/7f3a2c" />
              <CopyField label="TXT record" value="verify=c41e9b20d7" size="sm" iconOnly />
            </>
          ),
        },
        {
          title: 'In a Field, multi-line',
          stack: true,
          render: () => (
            <Field style={{ width: '100%', maxWidth: '36rem' }}>
              <FieldLabel>Embed code</FieldLabel>
              <CopyField label="Embed code" value={embed} multiline />
              <FieldDescription>Paste before the closing body tag.</FieldDescription>
            </Field>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give each field a specific label ("Calendar feed URL"), so its button is distinguishable.',
        'Use `mono={false}` for prose, such as a message template.',
        'Report `onCopyError` the way the product reports other errors.',
      ],
      dont: [
        'Do not use CopyField for secrets.',
        'Do not use it for values the user edits; it is read-only by design.',
      ],
    },
    related: {
      items: [
        { id: 'copy-button', reason: 'The button on its own.' },
        { id: 'secret-field', reason: 'For keys and tokens that must be masked.' },
        { id: 'text-field', reason: 'The Input and Textarea it renders.' },
        { id: 'form-field', reason: 'Adds a visible label, help text and errors.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['copy-button', 'form-field', 'text-field'],
    },
    testing: {
      unit: [
        'Shows the value read-only and names the text box and button from `label`.',
        'Copies the value with the button and announces it.',
        'Selects the whole value on focus; Tab order is text box, then button.',
        'On failure, focuses and selects the value and calls `onCopyError`.',
        'Inside a Field, the FieldLabel names the text area.',
        'Icon-only keeps a full accessible name; `disabled` disables both parts.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/copy-field'],
    },
  },
} satisfies ComponentDocs;

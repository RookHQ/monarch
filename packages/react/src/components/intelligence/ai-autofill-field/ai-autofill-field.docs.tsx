import { useState } from 'react';
import { Field, FieldLabel, Input } from '@rookcom/monarch-react';
import { AiAutofillField, type AiAutofillState } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function AutofilledField({
  label,
  suggestion,
  confidence,
  source,
}: {
  label: string;
  suggestion: string;
  confidence: number;
  source?: string;
}) {
  const [value, setValue] = useState(suggestion);
  const [state, setState] = useState<AiAutofillState>('suggested');
  return (
    <AiAutofillField
      value={suggestion}
      state={state}
      confidence={confidence}
      source={source}
      label={label.toLowerCase()}
      onAccept={() => setState('accepted')}
      onClear={() => {
        setValue('');
        setState('cleared');
      }}
      onEdit={() => setState('edited')}
    >
      <Field>
        <FieldLabel>{label}</FieldLabel>
        <Input value={value} onChange={(e) => setValue(e.target.value)} />
      </Field>
    </AiAutofillField>
  );
}

function FormDemo() {
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-4)', width: '100%', maxWidth: '26rem' }}>
      <AutofilledField
        label="Company"
        suggestion="Northwind Ltd"
        confidence={0.93}
        source="invoice-0412.pdf"
      />
      <AutofilledField
        label="Invoice total"
        suggestion="1,240.00"
        confidence={0.71}
        source="invoice-0412.pdf"
      />
      <AutofilledField label="Due date" suggestion="2026-05-30" confidence={0.38} />
    </div>
  );
}

const noop = () => {};

export default {
  id: 'ai-autofill-field',
  title: 'AiAutofillField',
  summary: 'Marks a form field that AI filled, with confidence, source and Accept / Clear.',
  sections: {
    purpose: {
      summary:
        'When AI fills a form (from a document, an email or a record), people must see which values it chose, how sure it was and where each came from, and confirm or remove them. This wrapper adds that to any field without changing the control.',
      useWhen: [
        'Extracting values from an uploaded document into a form.',
        'Pre-filling a record from context the person can check.',
      ],
      avoidWhen: [
        'Values the person typed themselves.',
        'Read-only AI output (use AiGeneratedBadge next to it).',
      ],
    },
    designPrinciples: {
      items: [
        'Provenance stays visible: "AI suggested" until checked, "AI filled" once accepted, "Edited by you" once changed.',
        'Low confidence is said in words ("Needs check"), not colour alone.',
        'The control is untouched: any Field, Input, Select or custom control can be wrapped.',
        'Clear really clears: your `onClear` empties the value.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: drive `state` from your form state; the wrapper reports Accept, Clear and the first edit.',
        'Pass children as a function to receive `aria-describedby` for the control; with plain children the id is added to the first input, textarea, select or combobox inside.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: ['No visual variants; the look follows `state`.'],
    },
    states: {
      items: [
        {
          name: 'Suggested',
          attribute: 'data-state="suggested"',
          description:
            'AI tint on the control, "AI suggested", confidence, source, Accept and Clear.',
        },
        {
          name: 'Accepted',
          attribute: 'data-state="accepted"',
          description: '"AI filled" badge and source; no actions.',
        },
        {
          name: 'Edited',
          attribute: 'data-state="edited"',
          description: '"Edited by you" and the original AI value, struck through.',
        },
        {
          name: 'Cleared',
          attribute: 'data-state="cleared"',
          description: 'No AI marking at all.',
        },
      ],
    },
    interaction: {
      items: [
        'Accept calls `onAccept` and moves focus to the control.',
        'Clear calls `onClear` (empty the value there) and moves focus to the control.',
        'The first `input` event from the control while suggested or accepted calls `onEdit`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves from the control to the confidence badge (when it has an explanation), Accept and Clear.',
        },
      ],
      notes: ['Accept and Clear are ordinary buttons; the control keeps its own keys.'],
    },
    accessibility: {
      role: 'none (a wrapper); the status line describes the control',
      name: 'The control keeps its own label.',
      items: [
        'The status line ("AI suggested", "Needs check, 42% confidence", "From …") is linked to the control with `aria-describedby`, so it is read with the field.',
        'With `label`, the actions are named "Accept suggested company" and "Clear suggested company", so many fields stay distinguishable.',
        'The provenance badge is not a Tab stop inside the field; its details are in the description.',
      ],
    },
    responsive: {
      items: [
        'The status line wraps under the control; actions move to the next line when there is no room.',
        'Accept and Clear are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'AI intent tokens for the suggested tint (`--mn-color-ai-subtle`, `--mn-color-ai-border`); confidence uses `--mn-color-confidence-*` through AiConfidenceMeter.',
        'Forced colours mark a suggested control with a Highlight border.',
      ],
    },
    examples: {
      items: [
        { title: 'Fields filled from a document', stack: true, render: () => <FormDemo /> },
        {
          title: 'States',
          stack: true,
          render: () => (
            <div
              style={{
                display: 'grid',
                gap: 'var(--mn-space-4)',
                width: '100%',
                maxWidth: '26rem',
              }}
            >
              {(['accepted', 'edited'] as const).map((state) => (
                <AiAutofillField
                  key={state}
                  value="Northwind Ltd"
                  state={state}
                  source="invoice-0412.pdf"
                  onAccept={noop}
                  onClear={noop}
                >
                  {(control) => (
                    <Field>
                      <FieldLabel>{`Company (${state})`}</FieldLabel>
                      <Input
                        defaultValue={state === 'edited' ? 'Northwind Group' : 'Northwind Ltd'}
                        {...control}
                      />
                    </Field>
                  )}
                </AiAutofillField>
              ))}
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `confidence` and `source` whenever the model provides them.',
        'Pass `label` so each field’s Accept and Clear are distinguishable.',
        'Announce once at form level how many fields AI filled.',
      ],
      dont: [
        'Do not submit suggested values without the person seeing them.',
        'Do not hide a low-confidence suggestion’s "Needs check".',
      ],
    },
    related: {
      items: [
        { id: 'ai-confidence-meter', reason: 'The confidence badge it shows.' },
        { id: 'ai-generated-badge', reason: 'The provenance badge it shows.' },
        { id: 'form-field', reason: 'The usual child.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'ai-confidence-meter', 'ai-generated-badge'],
    },
    testing: {
      unit: [
        'Suggested state shows the AI badge, "Needs check", the source, and describes the control.',
        'Accept removes the actions and refocuses the control.',
        'Clear empties the value and removes the marking and the description link.',
        'Typing calls onEdit once and shows "Edited by you" with the original value.',
        'Render-function children receive aria-describedby.',
        'Strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

import { useState } from 'react';
import {
  AiPromptInput,
  AiPromptTemplatePicker,
  AiPromptToolbar,
  AiPromptToolbarButton,
  type AiPromptTemplate,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { ListIcon } from '../../../internal/docs-demo.js';

const TEMPLATES: AiPromptTemplate[] = [
  {
    id: 'reply',
    title: 'Customer reply',
    description: 'Answer a customer question politely.',
    category: 'support',
    body: 'Write a reply to {{customer}} about {{topic}}. Keep it {{length}} and offer one next step.',
    variables: [
      { name: 'customer', label: 'Customer name', required: true, placeholder: 'Ana Lopez' },
      {
        name: 'topic',
        label: 'Topic',
        required: true,
        multiline: true,
        placeholder: 'The order that arrived late',
      },
      {
        name: 'length',
        label: 'Length',
        defaultValue: 'short',
        description: 'For example short or detailed.',
      },
    ],
  },
  {
    id: 'summary',
    title: 'Meeting summary',
    description: 'Turn notes into decisions and actions.',
    category: 'internal',
    body: 'Summarize {{meeting_notes}} as decisions, open questions and actions with owners.',
  },
  {
    id: 'brief',
    title: 'Project brief',
    description: 'A one-page brief from a goal.',
    category: 'internal',
    body: 'Draft a one-page brief for {{project}} with goal, scope, risks and milestones.',
  },
  {
    id: 'recap',
    title: 'Conversation recap',
    description: 'No blanks: used as is.',
    category: 'internal',
    body: 'Recap this conversation in three sentences.',
  },
];

const CATEGORIES = [
  { id: 'support', label: 'Support' },
  { id: 'internal', label: 'Internal' },
];

function GalleryDemo() {
  const [used, setUsed] = useState('');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', width: '100%', maxWidth: '48rem' }}>
      <AiPromptTemplatePicker templates={TEMPLATES} categories={CATEGORIES} onUse={setUsed} />
      {used ? <p style={{ margin: 0 }}>Prompt: {used}</p> : null}
    </div>
  );
}

function PromptDemo() {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState('');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', width: '100%', maxWidth: '48rem' }}>
      {open ? (
        <AiPromptTemplatePicker
          templates={TEMPLATES}
          onCancel={() => setOpen(false)}
          onUse={(text) => {
            setDraft(text);
            setOpen(false);
          }}
        />
      ) : null}
      <AiPromptInput
        label="Message the assistant"
        value={draft}
        onValueChange={setDraft}
        onSubmit={() => setDraft('')}
        toolbar={
          <AiPromptToolbar>
            <AiPromptToolbarButton icon={<ListIcon />} onClick={() => setOpen(true)}>
              Templates
            </AiPromptToolbarButton>
          </AiPromptToolbar>
        }
      />
    </div>
  );
}

export default {
  id: 'ai-prompt-template-picker',
  title: 'AiPromptTemplatePicker',
  summary: 'Pick a reusable prompt, fill its {{blanks}} in a small form and get the finished text.',
  sections: {
    purpose: {
      summary:
        'Turns good prompts into reusable templates: people pick one from a searchable gallery, fill in its blanks in a short form with a live preview, and get the finished prompt to review and send.',
      useWhen: [
        'A team repeats the same kinds of requests and wants them consistent.',
        'A prompt needs a few specific details (a name, a topic) every time.',
      ],
      avoidWhen: [
        'First-turn ideas with no blanks: use AiStarterPrompts.',
        'Commands the assistant runs: use ai-slash-commands.',
        'Document templates (not prompts): use TemplatePicker.',
      ],
    },
    designPrinciples: {
      items: [
        'Nothing is sent automatically: the filled text goes to `onUse`, normally into the prompt input for review.',
        'See before you use: the gallery previews the prompt with its blanks marked, and the form previews the finished text.',
        'Blanks are never silently dropped: required ones are validated, optional empty ones are removed, and unknown ones stay as written.',
        'Reuses Monarch form parts: TemplatePicker for the gallery, Field and Input for the blanks.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`fillPromptTemplate(body, values)` replaces `{{name}}` (spaces inside the braces allowed) with `values[name]`; names with no value stay as written, an empty string removes the blank, and values are inserted literally.',
        '`extractPromptVariables(body)` lists the blanks in order; `promptTemplateVariables(template)` returns `variables`, or the blanks read from the body (all required).',
        '`onUse(text, { template, values })` also receives the values, for analytics or saving.',
        '`AI_PROMPT_TEMPLATE_PICKER_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variables[].multiline',
          values: ['false', 'true'],
          description: 'A Textarea instead of an Input for longer blanks.',
        },
      ],
      notes: ['Categories appear as filters when `categories` is given.'],
    },
    states: {
      items: [
        {
          name: 'Pick',
          attribute: 'data-step="pick"',
          description:
            'The gallery, the preview of the selected template and Continue (disabled until one is chosen).',
        },
        {
          name: 'Fill',
          attribute: 'data-step="fill"',
          description: 'The form, one field per blank, with the live preview.',
        },
        {
          name: 'Invalid',
          description:
            'An empty required blank on submit: the field is invalid with "Enter …" and receives focus.',
        },
        {
          name: 'No blanks',
          description: 'The primary button reads "Use template" and uses it straight away.',
        },
        { name: 'Empty', description: '"No templates yet."' },
      ],
    },
    interaction: {
      items: [
        'Choosing a template shows its prompt in the preview; Continue opens the form and moves focus to its heading.',
        'Typing in a field updates the preview; fixing an invalid field clears its error.',
        'Use template validates and calls `onUse`; Back returns to the gallery with the selection and values kept, focus on Continue.',
        'Cancel (with `onCancel`) is available on both steps.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action:
            'Search, filters, the template group, then the buttons; in the form, each field then the buttons.',
        },
        {
          keys: 'Arrow keys',
          action: 'Move through the templates and select (TemplatePicker’s grid keys).',
        },
        { keys: 'Enter', action: 'Activates Continue; in a single-line field, submits the form.' },
      ],
    },
    accessibility: {
      role: 'radiogroup of templates, then a form',
      name: '"Prompt templates" (messages.label); the form step is introduced by its heading',
      items: [
        'The preview beside the gallery is a region named "Template preview"; the form’s preview is a region named by its heading.',
        'Fields use Field: labels, help, "Optional" text and errors are wired with `aria-describedby` and `aria-invalid`.',
        'Focus moves to the form heading on Continue, to the first invalid field on a failed submit, and back to Continue on Back.',
        'Blanks in previews are marked with a dashed border and their label, not colour alone.',
      ],
    },
    responsive: {
      items: [
        'The gallery cards wrap to one column at 390px and the preview moves under them.',
        'Fields flow in 14rem columns, one column on phones; below 30rem buttons share the full width.',
        'On coarse pointers buttons are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Blanks: `--mn-color-ai-subtle` fill, dashed `--mn-color-ai-border`, `--mn-color-ai-strong` text; filled values are underlined in `--mn-color-ai-border`.',
        'Preview panel: `--mn-color-bg-subtle`, `--mn-color-border-subtle`.',
        'In forced colours blanks and the panel use Canvas and CanvasText.',
      ],
    },
    examples: {
      items: [
        { title: 'Gallery, form and preview', stack: true, render: () => <GalleryDemo /> },
        {
          title: 'From a prompt toolbar into the input',
          stack: true,
          render: () => <PromptDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Put the filled text in the prompt input so people can edit it before sending.',
        'Give blanks clear labels and an example as the placeholder.',
        'Mark only the blanks the prompt cannot work without as required.',
      ],
      dont: [
        'Do not send the filled prompt automatically.',
        'Do not put secrets or personal data in shared template bodies.',
        'Do not use more than about five blanks; split the template instead.',
      ],
    },
    related: {
      items: [
        { id: 'template-picker', reason: 'The gallery it is built on.' },
        { id: 'ai-prompt-input', reason: 'Where the filled prompt goes.' },
        { id: 'ai-prompt-toolbar', reason: 'A Templates button to open it.' },
        { id: 'ai-starter-prompts', reason: 'Ready-made first prompts without blanks.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'form-field', 'template-picker', 'text-field'],
    },
    testing: {
      unit: [
        '`extractPromptVariables`, `fillPromptTemplate` (missing, empty, literal values) and `promptTemplateVariables`.',
        'Named gallery, preview with blanks, Continue disabled until a choice.',
        'Form: heading focus, defaults, optional text, required validation with focus and description, live preview, `onUse` arguments.',
        'Enter submits, Back keeps the selection and focuses Continue, templates without blanks, Cancel, search and empty state.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

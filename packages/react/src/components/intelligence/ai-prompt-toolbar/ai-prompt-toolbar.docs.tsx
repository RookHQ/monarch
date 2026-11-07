import { useState } from 'react';
import {
  AiPromptInput,
  AiPromptToolbar,
  AiPromptToolbarButton,
  AiPromptToolbarToggle,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { GridIcon, ListIcon, SearchIcon } from '../../../internal/docs-demo.js';

function InPromptDemo() {
  const [web, setWeb] = useState(false);
  const [sent, setSent] = useState('');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '40rem' }}>
      <AiPromptInput
        label="Message the assistant"
        onSubmit={(text) => setSent(`${text}${web ? ' (web search on)' : ''}`)}
        toolbar={
          <AiPromptToolbar>
            <AiPromptToolbarToggle icon={<SearchIcon />} pressed={web} onPressedChange={setWeb}>
              Search the web
            </AiPromptToolbarToggle>
            <AiPromptToolbarButton icon={<ListIcon />} onClick={() => setSent('Templates opened')}>
              Templates
            </AiPromptToolbarButton>
          </AiPromptToolbar>
        }
      />
      {sent ? <p style={{ margin: 0 }}>{sent}</p> : null}
    </div>
  );
}

export default {
  id: 'ai-prompt-toolbar',
  title: 'AiPromptToolbar',
  summary: 'The slot row under the prompt, with compact buttons and on/off toggles.',
  sections: {
    purpose: {
      summary:
        'Gives the controls under a prompt one consistent row: options such as web search, templates, mode, model and tone. Toggles report their state with `aria-pressed`, so "Search the web: on" is clear to everyone.',
      useWhen: [
        'Filling AiPromptInput’s `toolbar` slot.',
        'A prompt needs a few options that change how the next message is answered.',
      ],
      avoidWhen: [
        'Formatting text: use the rich-text editor toolbar.',
        'Actions on a finished response: use AiMessageActions.',
        'Choosing one of several modes: put AiModeSwitcher inside the toolbar rather than several toggles.',
      ],
    },
    designPrinciples: {
      items: [
        'Quiet by default: ghost buttons in muted text so the prompt stays the focus.',
        'State is never colour alone: a pressed toggle gets a border as well as the AI fill, and `aria-pressed`.',
        'A toggle’s label never changes with its state; the state is announced separately.',
        'Plain Tab order: pickers inside (segmented controls, selects) keep their own arrow keys.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'AiPromptToolbar is a `<div role="group">` named "Prompt options"; native attributes pass through and the ref is forwarded.',
        'AiPromptToolbarButton is a small ghost Button; its `children` must be a string, used as the label, or (with `hideLabel`) as the accessible name and tooltip.',
        'AiPromptToolbarToggle is controlled with `pressed` and `onPressedChange`, or uncontrolled with `defaultPressed`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'hideLabel',
          values: ['false', 'true'],
          description:
            'Icon-only button; the label becomes the tooltip and name. Exposed as `data-icon-only`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Toggle on / off',
          attribute: 'data-state="on|off" and aria-pressed on the toggle',
          description: 'On: AI-subtle fill, AI border and AI-strong text.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'Inherited from Button: not focusable, muted.',
        },
      ],
    },
    interaction: {
      items: [
        'Buttons call `onClick`; toggles flip and call `onPressedChange(next)`.',
        'The row wraps onto a second line when it does not fit.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between the controls in the row.' },
        { keys: 'Enter / Space', action: 'Activates a button or flips a toggle.' },
      ],
      notes: [
        'The group is not an ARIA toolbar with roving focus on purpose: it holds composite widgets whose arrow keys would conflict.',
      ],
    },
    accessibility: {
      role: 'group',
      name: '"Prompt options" (the `label` prop)',
      items: [
        'Toggles are buttons with `aria-pressed`; screen readers say "Search the web, toggle button, pressed".',
        'Icon-only buttons are named by their label, which is also the tooltip.',
        'Icons are decorative.',
      ],
    },
    responsive: {
      items: [
        'Controls wrap, so there is no horizontal scroll at 390px.',
        'On coarse pointers every button is at least 44px tall, icon-only ones 44px wide.',
      ],
    },
    theme: {
      items: [
        'Buttons: ghost Button with `--mn-color-text-muted` text, `--mn-color-text-default` on hover.',
        'Pressed toggles: `--mn-color-ai-subtle` (hover `--mn-color-ai-subtle-hover`), `--mn-color-ai-border`, `--mn-color-ai-strong`.',
        'In forced colours buttons get a ButtonText border and pressed toggles use Highlight and HighlightText.',
      ],
    },
    examples: {
      items: [
        { title: 'In AiPromptInput', stack: true, render: () => <InPromptDemo /> },
        {
          title: 'Icon-only and toggles',
          render: () => (
            <AiPromptToolbar>
              <AiPromptToolbarButton icon={<GridIcon />} hideLabel>
                Apps
              </AiPromptToolbarButton>
              <AiPromptToolbarToggle icon={<SearchIcon />} defaultPressed>
                Search the web
              </AiPromptToolbarToggle>
              <AiPromptToolbarToggle>Deep research</AiPromptToolbarToggle>
            </AiPromptToolbar>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep the row short: three or four controls, the rest in a menu.',
        'Use a toggle for options that stay on for the next messages, such as web search.',
        'Name toggles by the feature ("Search the web"), not the action ("Turn on…").',
      ],
      dont: [
        'Do not change a toggle’s label when it is pressed.',
        'Do not put the Send button here; AiPromptInput owns it.',
        'Do not invent AI icons; use the Bot glyph for AI identity.',
      ],
    },
    related: {
      items: [
        { id: 'ai-prompt-input', reason: 'Its `toolbar` slot holds this row.' },
        { id: 'ai-mode-switcher', reason: 'One of several modes, inside the row.' },
        { id: 'ai-tone-picker', reason: 'Compact tone choice for the row.' },
        { id: 'ai-prompt-template-picker', reason: 'Opened from a Templates button.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Named group with props and ref forwarded; custom label.',
        'Buttons: visible label, icon-only name and tooltip, ghost small style, click.',
        'Toggles: keyboard toggling of `aria-pressed` and `data-state`, controlled value.',
        'Inside AiPromptInput’s toolbar slot.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

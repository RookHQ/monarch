import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { AiOnboardingTour, type AiOnboardingTourStep } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const STEPS: AiOnboardingTourStep[] = [
  {
    title: 'What the assistant can see',
    body: 'The page you have open, the records you select and any files you attach. It does not see other workspaces.',
  },
  {
    title: 'What it can do',
    body: 'Summarise pages, draft text and answer questions about your records. It asks before it changes anything.',
    target: '#docs-ai-tour-ask',
  },
  {
    title: 'How to turn it off',
    body: 'Use “Turn off AI” below at any time, or switch it off per app in workspace settings.',
  },
];

function InlineDemo() {
  const [state, setState] = useState<'open' | 'finished' | 'disabled'>('open');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-4)', width: '100%', maxWidth: '28rem' }}>
      <Button id="docs-ai-tour-ask">Ask the assistant</Button>
      {state === 'open' ? (
        <AiOnboardingTour
          steps={STEPS}
          onFinish={() => setState('finished')}
          onDisable={() => setState('disabled')}
          onSkip={() => setState('finished')}
        />
      ) : (
        <div style={{ display: 'flex', gap: 'var(--mn-space-3)', alignItems: 'center' }}>
          <span style={{ color: 'var(--mn-color-text-muted)' }}>
            {state === 'disabled' ? 'AI turned off.' : 'Introduction finished.'}
          </span>
          <Button size="sm" onClick={() => setState('open')}>
            Show again
          </Button>
        </div>
      )}
    </div>
  );
}

function FloatingDemo() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setOpen(true)}>Start the floating introduction</Button>
      {open ? (
        <AiOnboardingTour
          placement="floating"
          steps={STEPS}
          onFinish={() => setOpen(false)}
          onDisable={() => setOpen(false)}
          onSkip={() => setOpen(false)}
        />
      ) : null}
    </>
  );
}

export default {
  id: 'ai-onboarding-tour',
  title: 'AiOnboardingTour',
  summary: 'A short step-by-step introduction to AI in a product, with an explicit “Turn off AI”.',
  sections: {
    purpose: {
      summary:
        'People should learn what an AI feature sees and does before relying on it, and should be able to opt out in one step. AiOnboardingTour walks through a few short steps, can point at the part of the page each step describes, and always offers “Turn off AI”.',
      useWhen: [
        'AI is switched on in a product for the first time, or a new AI capability arrives.',
        'People need to know what data the assistant reads before they use it.',
      ],
      avoidWhen: [
        'A general product tour unrelated to AI.',
        'A single fact that fits in an AiDisclaimer or a callout.',
      ],
    },
    designPrinciples: {
      items: [
        'Honest first: say what it sees, what it can do and how to turn it off, in that order.',
        'The opt-out is never hidden: “Turn off AI” is on every step, not only the last.',
        'Short: three to five steps of one or two sentences.',
        'Never blocks the page: inline by default, a non-modal dialog when floating.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section>`.',
        'The product decides when to show the tour (for example once per person) and what “Turn off AI” does.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'placement',
          values: ['inline', 'floating'],
          description:
            '`inline` renders where it is placed. `floating` docks to the bottom corner of the viewport as a non-modal dialog.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Step', attribute: 'data-step', description: 'Index of the step shown.' },
        { name: 'Last step', attribute: 'data-last', description: 'Next becomes “Got it”.' },
        {
          name: 'Pointing',
          attribute: 'data-mn-ai-tour-target (on the target)',
          description: 'The element the step describes is outlined and scrolled into view.',
        },
      ],
    },
    interaction: {
      items: [
        'Next and Back move between steps and move focus to the new step’s heading, so it is read out.',
        'The last step’s “Got it” calls `onFinish`.',
        '“Turn off AI” calls `onDisable` on any step.',
        'With `onSkip`, a close button and Escape skip the tour.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through close, Turn off AI, Back and Next.' },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
        { keys: 'Escape', action: 'Skips the tour (when `onSkip` is given).' },
      ],
    },
    accessibility: {
      role: 'region (inline) or non-modal dialog (floating)',
      name: 'The current step’s title, through aria-labelledby; the body is its description.',
      items: [
        'The step counter (“Step 2 of 3”) is visible text; the progress dots are decorative.',
        'Focus moves to the new heading after Back or Next, never on first render.',
        'The Bot glyph is decorative; the “AI” label is text.',
      ],
    },
    responsive: {
      items: [
        'Inline: up to 28rem wide, shrinks to its container.',
        'Floating: 24rem, never wider than the viewport minus 32px.',
        'The footer wraps at 390px; buttons are 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'AI intent tokens: `--mn-color-ai-border`, `--mn-color-ai-subtle`, `--mn-color-ai-strong`, `--mn-color-ai-solid`.',
        'Forced colours draw borders with CanvasText and progress and target outlines with Highlight.',
      ],
    },
    examples: {
      items: [
        { title: 'Inline, pointing at a button', stack: true, render: () => <InlineDemo /> },
        { title: 'Floating', render: () => <FloatingDemo /> },
      ],
    },
    doDont: {
      do: [
        'Show it once, when AI is first available to the person.',
        'Make “Turn off AI” actually turn it off, or open the setting that does.',
        'Name real data sources in “what it can see”.',
      ],
      dont: [
        'Do not hide the opt-out until the last step.',
        'Do not use the tour to market features; explain them.',
      ],
    },
    related: {
      items: [
        { id: 'ai-settings-section', reason: 'Where workspace admins turn AI on or off per app.' },
        { id: 'ai-disclaimer', reason: 'A one-line reminder after the introduction.' },
        { id: 'onboarding-checklist', reason: 'General product onboarding.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'First step is a region named by its title and described by its body, with a counter.',
        'Back and Next move steps and focus the new heading; the last step finishes.',
        '“Turn off AI” is on every step and calls onDisable.',
        'The step target is outlined and released.',
        'Floating is a non-modal dialog; the close button and Escape skip.',
        'Controlled step and strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

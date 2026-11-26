import { AiSafetyNotice } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'ai-safety-notice',
  title: 'AiSafetyNotice',
  summary:
    'Says that a request was blocked, or an answer was adjusted or filtered, and what to do next.',
  sections: {
    purpose: {
      summary:
        'Tells people, calmly and specifically, that a safety system stepped in: it blocked a request, changed an answer or removed parts of it. It gives the reason when the product knows it and a way forward, so people are not left with a silent gap or a vague error.',
      useWhen: [
        'In place of an answer the safety system blocked (`blocked`).',
        'Above an answer that was changed before it was shown (`modified`).',
        'Above an answer with parts removed (`filtered`).',
      ],
      avoidWhen: [
        'Technical failures such as timeouts or rate limits: use AiErrorMessage.',
        'General “AI can make mistakes” reminders: use AiDisclaimer.',
        'Warnings about the model’s certainty: use AiConfidenceMeter.',
      ],
    },
    designPrinciples: {
      items: [
        'Neutral, never accusing: it describes what happened, not what the person did wrong.',
        'Specific: a reason when one is known, and next steps people can act on.',
        'Each kind has its own icon (lock, pencil, filter), so meaning never depends on colour.',
        'Blocked uses a warning tone; adjusted and filtered use an informational tone because an answer is still shown.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; native `<div>` attributes pass through, including `role` to override `note`.',
        '`title` and `children` replace the wording for one instance; `messages` replaces the defaults for localisation.',
        '`learnMoreHref` passes through `isSafeUrl`; unsafe links are not rendered.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'kind',
          values: ['blocked', 'modified', 'filtered'],
          description:
            '`blocked`: warning tone and lock icon, no answer was given. `modified`: info tone and pencil icon. `filtered`: info tone and filter icon. Exposed as `data-kind`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Kind',
          attribute: 'data-kind',
          description: 'What the safety system did; see Variants.',
        },
      ],
    },
    interaction: {
      items: [
        '“Edit prompt” calls `onEditPrompt`; the product usually puts the prompt back in the composer and focuses it.',
        'The guidelines link opens in the same tab.',
      ],
      notes: ['Without `onEditPrompt` and `learnMoreHref` the notice is static.'],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to “Edit prompt” and the guidelines link, when present.' },
        { keys: 'Enter / Space', action: 'Activates the focused action.' },
      ],
    },
    accessibility: {
      role: 'note',
      name: 'The title (via `aria-labelledby`)',
      items: [
        'Static content, not a live region: the thread announces the end of the response; place the notice where the answer would have been.',
        'The icon is decorative; the title carries the meaning.',
        'The reason is introduced by a visible “Reason:” label so it reads in context.',
      ],
    },
    responsive: {
      items: [
        'Fills its container and wraps text; actions wrap onto a new line.',
        'On coarse pointers the actions are at least 44px tall.',
        'Logical properties throughout, so the accent edge flips in RTL.',
      ],
    },
    theme: {
      items: [
        'Blocked: `--mn-color-warning-subtle`, `--mn-color-warning-border`, icon `--mn-color-warning-strong`.',
        'Modified and filtered: `--mn-color-info-subtle`, `--mn-color-info-border`, icon `--mn-color-info-strong`.',
        'Forced colours: Canvas background with a CanvasText border and LinkText link.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Blocked request with next steps',
          stack: true,
          render: () => (
            <AiSafetyNotice
              kind="blocked"
              reason="The request asked for someone’s home address."
              nextSteps={
                <p>Ask about public information instead, such as a company’s office location.</p>
              }
              onEditPrompt={() => {}}
              learnMoreHref="https://example.com/guidelines"
            />
          ),
        },
        {
          title: 'Adjusted answer',
          stack: true,
          render: () => (
            <AiSafetyNotice kind="modified" reason="Medical dosage details were removed." />
          ),
        },
        {
          title: 'Filtered answer',
          stack: true,
          render: () => <AiSafetyNotice kind="filtered" />,
        },
      ],
    },
    doDont: {
      do: [
        'Give the reason when the product knows it, in plain words.',
        'Offer a way forward: edit the prompt, ask something related, read the guidelines.',
      ],
      dont: [
        'Do not imply the person did something wrong.',
        'Do not use it for outages or errors; those are not safety decisions.',
      ],
    },
    related: {
      items: [
        { id: 'ai-error-message', reason: 'Technical failures with retry.' },
        { id: 'ai-disclaimer', reason: 'Standing reminder that AI can be wrong.' },
        { id: 'ai-message', reason: 'Where the notice usually appears.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Each kind renders its own title as the note’s accessible name, with a decorative icon.',
        'Reason, next steps, Edit prompt callback and guidelines link render; unsafe links are dropped.',
        'Custom title, children and messages replace the wording; ref and props forwarding.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

import { AiDisclaimer } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'ai-disclaimer',
  title: 'AiDisclaimer',
  summary: '"AI can make mistakes. Check important info.", worded and styled per risk.',
  sections: {
    purpose: {
      summary:
        'Reminds people that AI output can be wrong, with wording and emphasis matched to how much a mistake would cost where it appears, so the warning is quiet where stakes are low and impossible to miss where they are high.',
      useWhen: [
        'Under an assistant’s prompt input or answer (`low`).',
        'Next to AI output people may reuse, such as a draft or a summary (`medium`).',
        'Next to AI output people may act on: money, health, legal, deleting or sending things (`high`).',
      ],
      avoidWhen: [
        'Labelling a single piece of content as AI-made: use AiGeneratedBadge.',
        'Warning about a specific claim the model is unsure of: use AiConfidenceMeter.',
        'Errors: use AiErrorMessage.',
      ],
    },
    designPrinciples: {
      items: [
        'One sentence, plain words, no legal boilerplate.',
        'Emphasis grows with risk: muted line, then an info line, then a warning note.',
        'Each level has its own icon shape, so it never depends on colour.',
        'Low risk uses the Bot glyph to mark it as about AI; never sparkles.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; native `<div>` attributes pass through, including `role` to override `note`.',
        'Children replace the wording for one instance; `messages` replaces the default per risk (for localisation).',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'risk',
          values: ['low', 'medium', 'high'],
          description:
            '`low` (default): centred, muted, extra small, Bot glyph. `medium`: default text colour, info icon, stronger wording. `high`: warning-toned box with a warning icon and review wording. Exposed as `data-risk`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Risk',
          attribute: 'data-risk',
          description: 'The only state; see Variants.',
        },
      ],
    },
    interaction: { items: [], notes: ['Not interactive.'] },
    keyboard: { items: [], notes: ['Not focusable and has no keyboard interaction.'] },
    accessibility: {
      role: 'note',
      name: 'None; its text is the content',
      items: [
        'The text is always visible and read in order; the icon is decorative.',
        'It is not a live region: it is static guidance, not an event.',
        'Text contrast uses the muted and default text tokens, which meet contrast on every theme.',
      ],
    },
    responsive: {
      items: ['Wraps to as many lines as needed; no fixed width. RTL uses logical properties.'],
    },
    theme: {
      items: [
        'Low: `--mn-color-text-muted` with the glyph in `--mn-color-ai-strong`.',
        'Medium: `--mn-color-text-default` with the icon in `--mn-color-info-strong`.',
        'High: `--mn-color-warning-subtle` background, `--mn-color-warning-border`, icon in `--mn-color-warning-strong`.',
        'Forced colours: CanvasText on Canvas with a visible border for `high`.',
      ],
    },
    examples: {
      items: [
        { title: 'Low risk (default)', stack: true, render: () => <AiDisclaimer /> },
        { title: 'Medium risk', stack: true, render: () => <AiDisclaimer risk="medium" /> },
        { title: 'High risk', stack: true, render: () => <AiDisclaimer risk="high" /> },
        {
          title: 'Custom wording',
          stack: true,
          render: () => (
            <AiDisclaimer risk="medium">
              Answers use only the documents in this workspace and can still be wrong.
            </AiDisclaimer>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Place it once per surface, close to the AI output it is about.',
        'Choose the risk by what someone could do with a wrong answer.',
      ],
      dont: [
        'Do not repeat it under every message.',
        'Do not use `high` everywhere; people stop reading warnings that are always loud.',
      ],
    },
    related: {
      items: [
        { id: 'ai-generated-badge', reason: 'Marks one piece of content as AI-made.' },
        { id: 'ai-confidence-meter', reason: 'How sure the model is about one answer.' },
        { id: 'ai-prompt-input', reason: 'The usual place for the low-risk line.' },
        { id: 'inline-notice', reason: 'Non-AI notes and warnings.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Low-risk default renders as a note with the default wording and a decorative icon; ref and props forwarding.',
        'Medium and high risk use their own wording and `data-risk`.',
        'Children and `messages` replace the wording; `role` can be overridden.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

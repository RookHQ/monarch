import { StatusDot } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'status-dot',
  title: 'StatusDot',
  summary: 'A small coloured dot with a text label, in six tones, never colour alone.',
  sections: {
    purpose: {
      summary:
        'Shows the state of something in the lightest possible way: a coloured dot followed by a word ("Running", "Paused"). Needed by: the shared product rail (`src/components/rail/RookRail.tsx` in Rook App, Rook Base, Rook Calendar, Rook Dots and the other products), whose local `StatusDot` on the account avatar is colour-only and hidden from assistive technology; `hideLabel` keeps the same look with a spoken label.',
      useWhen: [
        'Dense lists and tables where a filled Badge would be too heavy (service health, job state, sync state).',
        'A live or in-progress state that should draw the eye a little (`pulse`).',
      ],
      avoidWhen: [
        'The status is a person’s availability: use PresenceDot, which has the presence vocabulary and shapes.',
        'The status should be the most visible thing in the row: use Badge or StatusBadge.',
        'There is no word to go with the colour: write one first.',
      ],
    },
    designPrinciples: {
      items: [
        'Never colour alone: `label` is required by the type, and the dot is hidden from assistive technology.',
        'Tone names the meaning (`success`, `danger`), not the colour, so every theme and product accent works.',
        'Quiet by default: small text, no fill behind the label.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Forwards its ref to the root `<span>`; native `<span>` attributes pass through.'],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Compared with Badge `dot`: Badge draws a dot inside a tinted pill; StatusDot has no pill and its dot carries the tone colour while the label stays in the default text colour.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['neutral', 'accent', 'info', 'success', 'warning', 'danger'],
          description: 'Colour of the dot. Default `neutral`. Exposed as `data-tone`.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description:
            '`sm` (default) for table cells and small text, `md` next to body text. Exposed as `data-size`.',
        },
        {
          prop: 'pulse',
          values: ['true', 'false'],
          description:
            'A ring that fades out from the dot on a loop, for live states. Exposed as `data-pulse`.',
        },
        {
          prop: 'hideLabel',
          values: ['true', 'false'],
          description:
            'Keeps the label for assistive technology only (`data-label-hidden`). Use with a visible legend.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pulsing',
          attribute: 'data-pulse',
          description: 'The ring animates; it is removed when the user prefers reduced motion.',
        },
        {
          name: 'Label hidden',
          attribute: 'data-label-hidden',
          description: 'Only the dot is visible; the label is visually hidden text.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: [
        'Not interactive. Wrap it in a button or link when the status should open something.',
      ],
    },
    keyboard: { items: [], notes: ['Not focusable: it is static text.'] },
    accessibility: {
      role: 'None (inline text)',
      name: 'The `label` text.',
      items: [
        'The dot has `aria-hidden="true"`; the label is read as ordinary text.',
        'With `hideLabel`, the label moves into a visually hidden span, so screen readers still read it.',
        'Not a live region. When the status changes and that must be announced, put it inside a LiveRegion.',
        'The dot uses the `*-solid` status tokens, which meet 3:1 against the surface for non-text contrast.',
      ],
    },
    responsive: {
      items: [
        'Inline and shrinkable: a long label is cut with an ellipsis instead of wrapping, so table rows keep their height.',
        'Size does not change with viewport width.',
      ],
    },
    theme: {
      items: [
        'Dot colours are the `--mn-color-{tone}-solid` tokens; `neutral` uses `--mn-color-text-subtle` and `accent` follows the product accent.',
        'In forced colours the dot becomes `CanvasText` and the label carries the meaning.',
        'Reduced motion stops the pulse.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Tones',
          render: () => (
            <>
              <StatusDot label="Draft" />
              <StatusDot tone="info" label="Scheduled" />
              <StatusDot tone="success" label="Running" />
              <StatusDot tone="warning" label="Paused" />
              <StatusDot tone="danger" label="Failed" />
              <StatusDot tone="accent" label="Featured" />
            </>
          ),
        },
        {
          title: 'Live state with pulse',
          render: () => <StatusDot tone="danger" pulse size="md" label="Recording" />,
        },
        {
          title: 'In a table cell',
          stack: true,
          render: () => (
            <table>
              <tbody>
                <tr>
                  <th scope="row">Nightly export</th>
                  <td>
                    <StatusDot tone="success" label="Completed" />
                  </td>
                </tr>
                <tr>
                  <th scope="row">Search index</th>
                  <td>
                    <StatusDot tone="warning" label="Behind by 4 minutes" />
                  </td>
                </tr>
              </tbody>
            </table>
          ),
        },
        {
          title: 'Hidden label with a legend',
          render: () => (
            <>
              <StatusDot tone="success" hideLabel label="Online" />
              <span>Online</span>
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write the status as a word or short phrase ("Behind by 4 minutes").',
        'Use `pulse` only for states that are actually live.',
        'Use the same tone for the same meaning across a product.',
      ],
      dont: [
        'Don’t use `hideLabel` without a visible legend nearby.',
        'Don’t use `accent` to mean success or failure; it is the brand colour.',
        'Don’t pulse several dots on one screen.',
      ],
    },
    related: {
      items: [
        {
          id: 'badge',
          reason: 'A tinted pill label; heavier, for statuses that should stand out.',
        },
        {
          id: 'status-badge',
          reason: 'A registry of named statuses with icons and custom colours.',
        },
        { id: 'presence-dot', reason: 'A person’s availability (online, away, busy).' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Renders a decorative `aria-hidden` dot followed by the visible label.',
        'Defaults to the neutral tone and exposes size and pulse as data attributes.',
        'Keeps a hidden label in a visually hidden span.',
        'Forwards its ref and native attributes.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

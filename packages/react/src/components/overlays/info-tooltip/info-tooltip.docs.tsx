import { Field, FieldLabel, InfoTooltip, Input } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'info-tooltip',
  title: 'InfoTooltip',
  summary:
    'The "i" next to a label that explains it: a toggletip that works with touch and keyboard.',
  sections: {
    purpose: {
      summary:
        'Explains a term, metric or setting in a sentence without cluttering the layout. Unlike a hover tooltip it opens on click, tap, Enter or Space, so it works on phones and with a keyboard, and its text is announced to screen readers. Needed by: Ranker (audit and rivals metrics); replaces the hand-rolled "i" tooltips in other apps.',
      useWhen: [
        'A metric or column heading needs a one- or two-sentence definition.',
        'A form label needs context that most people will not need every time.',
      ],
      avoidWhen: [
        'The explanation is essential to complete the task: show it as FieldDescription text instead.',
        'The bubble would hold links, buttons or a form: use a Popover, which manages focus.',
        'Naming an icon-only button: use Tooltip on that button.',
      ],
    },
    designPrinciples: {
      items: [
        'A toggletip, not a tooltip: the button exists only to reveal the text, so it opens on activation rather than on hover.',
        'Focus never moves: the person stays where they were and can keep tabbing.',
        'Announced once through a live region that is always present, so screen readers read it reliably.',
        'Small in the layout, large to the finger: the glyph is 14px but the touch target is 44px.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`InfoTooltip` forwards its ref to the button. Other button props (`disabled`, `data-*`, `onClick`) pass through to it.',
        'The bubble is a Popover; `side`, `align` and `contentClassName` style it.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Controlled with `open` + `onOpenChange`, or uncontrolled with `defaultOpen`.',
        'Migrating from the app copy (Ranker `components/ui/info-tooltip`): `children` and `side` are unchanged; pass a specific `label` ("About visibility score") instead of the old fixed "More info"; drop the wrapping TooltipProvider; the bubble now opens on click or tap instead of hover.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'icon',
          values: ['info', 'help'],
          description:
            '`info` shows an "i", `help` a "?". `iconNode` replaces the glyph. Exposed as `data-icon` on the button.',
        },
        {
          prop: 'side',
          values: ['top', 'right', 'bottom', 'left'],
          description: 'Preferred side of the bubble; it flips when there is no room.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description:
            'On the wrapper; the button has `aria-expanded="true"` and the bubble shows.',
        },
        { name: 'Closed', attribute: 'data-state="closed"', description: 'Only the button shows.' },
        {
          name: 'Disabled',
          description: 'With `disabled`, the button cannot be focused or opened.',
        },
      ],
    },
    interaction: {
      items: [
        'Click or tap toggles the bubble. Hover does nothing on its own.',
        'An outside click, Escape or a second activation closes it.',
        'Focus stays on the button while it is open.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'Opens or closes the bubble.' },
        { keys: 'Escape', action: 'Closes the bubble; focus stays on the button.' },
        { keys: 'Tab', action: 'Moves on to the next control; the bubble does not trap focus.' },
      ],
    },
    accessibility: {
      role: 'button (with `aria-expanded`), bubble `note`, plus a polite `status` live region',
      name: '`label` (default "More information"); the bubble has the same name.',
      items: [
        'Works with touch, keyboard and pointer alike (WCAG 2.1.1, 2.5.x); content is not hover-only (1.4.13).',
        'The bubble’s text is copied into a visually hidden `role="status"` region that is always mounted, so it is announced on open.',
        'The glyph is `aria-hidden`; the button carries the name.',
        'The touch target grows to 44px on coarse pointers without shifting the layout.',
        'Bubble content should be text only: it is not in the tab order.',
      ],
    },
    responsive: {
      items: [
        'The bubble is at most 18rem wide and never wider than the viewport minus 2rem, so it fits a 390px screen.',
        'Long words wrap inside the bubble.',
      ],
    },
    theme: {
      items: [
        'The bubble uses the Popover surface tokens (raised background, subtle border, large shadow).',
        'The glyph uses the muted text colour and the default text colour when open or hovered.',
        'Forced colours: the button uses ButtonText and a Highlight focus ring; the bubble gets a CanvasText border.',
        'Reduced motion removes the colour transition and the bubble’s entry animation.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Next to a metric',
          render: () => (
            <span>
              Visibility score{' '}
              <InfoTooltip label="About visibility score">
                Share of tracked keywords where you rank on page one, updated daily.
              </InfoTooltip>
            </span>
          ),
        },
        {
          title: 'In a form label',
          render: () => (
            <Field>
              <FieldLabel>
                Retention period{' '}
                <InfoTooltip label="About retention period" icon="help" side="right">
                  Records older than this are archived automatically. You can restore them for 30
                  days.
                </InfoTooltip>
              </FieldLabel>
              <Input defaultValue="90 days" />
            </Field>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Name the button after what it explains ("About retention period").',
        'Keep the text to one or two plain sentences.',
        'Put information people always need in the visible description instead.',
      ],
      dont: [
        'Do not put links, buttons or inputs in the bubble; use a Popover.',
        'Do not use it as the only label of a control.',
        'Do not stack several next to each other; explain the group once.',
      ],
    },
    related: {
      items: [
        { id: 'tooltip', reason: 'Hover and focus hint that names an icon-only control.' },
        {
          id: 'popover',
          reason: 'Floating panel with links, buttons or a form, with focus management.',
        },
        { id: 'form-field', reason: 'FieldDescription for help text everyone should see.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['popover'],
      notes: [
        'Uses `--mn-control-xs` for the button size and the Popover surface tokens for the bubble.',
      ],
    },
    testing: {
      unit: [
        'Named button toggles a named note on click; focus stays on the button; no axe violations open or closed.',
        'The text is announced through a persistent `status` region and cleared on close.',
        'Enter and Space open it; Escape closes it and focus stays; Tab moves on.',
        'Closes on an outside click; opens on a touch tap; does not open on hover alone.',
        'Controlled open state; default label, help glyph and custom icon; ref forwarding.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

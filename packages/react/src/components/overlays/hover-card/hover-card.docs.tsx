import { HoverCard, HoverCardContent, HoverCardTrigger, Link } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const muted = { margin: 0, color: 'var(--mn-color-text-muted)', fontSize: 'var(--mn-text-sm)' };

export default {
  id: 'hover-card',
  title: 'HoverCard',
  summary: 'A preview card on hover and keyboard focus, after a short delay. Never essential.',
  sections: {
    purpose: {
      summary:
        'Shows a preview of what a link points to (a page, a record, a team) when the pointer rests on it or it has keyboard focus, so people can check before they navigate. Needed by: Rook ID and Rook Links (shadcn/ui HoverCard copies). For people, use UserHoverCard, which is the same behaviour with a profile layout.',
      useWhen: [
        'A link or mention whose target people often want to glance at first: a document, a ticket, a repository, a team.',
        'The same information is one click away on the target page.',
      ],
      avoidWhen: [
        'The content is essential or the only place it appears: touch screens never show the card.',
        'A short text label for an icon button: use Tooltip.',
        'A person’s profile: use UserHoverCard.',
        'Content people must interact with to finish a task: use Popover (opened by a click).',
      ],
    },
    designPrinciples: {
      items: [
        'Supplementary only: the trigger works on its own (it is a link), and the card adds a preview.',
        'Intent, not accident: a delay on hover and on focus stops cards flashing open while the pointer passes or people Tab through a list.',
        'Keyboard parity: focus opens it, Tab reaches its links, Escape closes it.',
        'Never steals focus when it opens.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Parts: HoverCard (state and timing), HoverCardTrigger (merges behaviour into one focusable child) and HoverCardContent (the card, forwards its ref to the panel).',
        'Built on Popover: HoverCardContent takes PopoverContent props such as `side`, `align`, `sideOffset` and `collisionPadding`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`openDelay` (default 600 ms) applies to hover and focus; `closeDelay` (default 300 ms) to leaving.',
        '`open`/`defaultOpen`/`onOpenChange` for control or to load the preview lazily.',
        'Migrating from shadcn/ui (`@/components/ui/hover-card`): `HoverCard`, `HoverCardTrigger` and `HoverCardContent` keep their names, `openDelay`, `closeDelay`, `open`, `defaultOpen`, `onOpenChange`, `side`, `align` and `sideOffset`. `HoverCardTrigger asChild` is accepted and implied (the child is always used), and the default `openDelay` is 600 rather than Radix’s 700.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No variants. Size the content with your own layout; the card is 20rem wide at most.',
      ],
    },
    states: {
      items: [
        {
          name: 'Closed',
          description: 'Nothing renders; the trigger has no description.',
        },
        {
          name: 'Open',
          attribute: 'data-state="open", data-side, data-align',
          description: 'The card shows beside the trigger and describes it.',
        },
      ],
    },
    interaction: {
      items: [
        'The pointer resting on the trigger for `openDelay` opens the card; leaving trigger and card for `closeDelay` closes it.',
        'Moving the pointer from the trigger onto the card keeps it open.',
        'Keyboard focus on the trigger opens the card after `openDelay`; focus stays on the trigger.',
        'Touch input never opens it: a tap follows the link as usual.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab (onto the trigger)', action: 'Opens the card after the delay.' },
        { keys: 'Tab (while open)', action: 'Moves into the card’s links and buttons, if any.' },
        {
          keys: 'Tab (past the last item)',
          action: 'Returns to the trigger and closes the card; the next Tab moves on.',
        },
        { keys: 'Shift+Tab (from the first item)', action: 'Returns to the trigger.' },
        { keys: 'Escape', action: 'Closes the card; focus stays on or returns to the trigger.' },
      ],
    },
    accessibility: {
      role: 'dialog (non-modal), described from the trigger',
      name: 'The card is labelled by its trigger’s text.',
      items: [
        'While open, the trigger’s `aria-describedby` points at the card, so screen readers read the preview after the link name, as with a tooltip. Keep the content short for that reason.',
        'The card never takes focus on open, so typing and screen reader reading position are not interrupted.',
        'The trigger must be focusable on its own (a link or button), which keeps it usable without the card.',
        'Meets WCAG 1.4.13: the card can be dismissed (Escape), hovered, and stays until pointer and focus leave.',
      ],
    },
    responsive: {
      items: [
        'At most 20rem wide and never wider than the viewport minus 2rem; flips and shifts to stay on screen.',
        'Not shown on touch screens, so it adds nothing to a phone layout.',
      ],
    },
    theme: {
      items: [
        'Uses the popover surface, border, radius and shadow tokens, so every theme and dark mode apply.',
        'Forced-colours mode draws a CanvasText border; the open animation stops under `prefers-reduced-motion: reduce`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Page preview',
          render: () => (
            <p style={{ margin: 0 }}>
              See the{' '}
              <HoverCard>
                <HoverCardTrigger>
                  <Link href="#onboarding">Onboarding checklist</Link>
                </HoverCardTrigger>
                <HoverCardContent>
                  <div style={{ display: 'grid', gap: '0.375rem' }}>
                    <strong>Onboarding checklist</strong>
                    <p style={muted}>12 steps for a new team member’s first week.</p>
                    <p style={muted}>Updated 3 days ago</p>
                  </div>
                </HoverCardContent>
              </HoverCard>{' '}
              before Monday.
            </p>
          ),
        },
        {
          title: 'With an action inside',
          render: () => (
            <HoverCard openDelay={300}>
              <HoverCardTrigger>
                <Link href="#design-team">Design team</Link>
              </HoverCardTrigger>
              <HoverCardContent side="top">
                <div style={{ display: 'grid', gap: '0.5rem' }}>
                  <strong>Design team</strong>
                  <p style={muted}>8 members · 3 open projects</p>
                  <Link href="#design-team/members">See members</Link>
                </div>
              </HoverCardContent>
            </HoverCard>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use it on links whose target page holds the same information.',
        'Keep the preview short: it is read as the link’s description.',
        'Load data lazily in `onOpenChange` for previews that need a request.',
      ],
      dont: [
        'Do not put essential information, form fields or the only copy of an action in it.',
        'Do not use it on non-focusable text: keyboard users could not open it.',
        'Do not set `openDelay` below about 300 ms; cards would flash open.',
      ],
    },
    related: {
      items: [
        { id: 'user-hover-card', reason: 'The same behaviour with a profile layout.' },
        { id: 'tooltip', reason: 'A short text label.' },
        { id: 'popover', reason: 'Interactive content opened by a click.' },
        { id: 'link', reason: 'The usual trigger.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['popover'],
    },
    testing: {
      unit: [
        'Opens after the delay on hover, named by its trigger and describing it; closes on leave.',
        'Stays open while the pointer is on the card; a quick pass does not open it.',
        'Opens on keyboard focus without taking focus; Tab enters the card; Escape closes and keeps focus on the trigger.',
        'Tabbing past the card closes it; touch never opens it.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

import { useState } from 'react';
import { Button, Inline, Sticky } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './sticky.fixture.json';

const ROWS = Array.from(
  { length: fixture.data.rows.count },
  (_, i) => `${fixture.data.rows.prefix} ${i + 1}`,
);

function HeaderDemo() {
  const [stuck, setStuck] = useState(false);
  return (
    <div
      // A scroll box with nothing focusable in it: a named, focusable region so keyboard
      // users can scroll it.
      // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
      tabIndex={0}
      role="region"
      aria-label="Entries example"
      style={{
        height: 260,
        width: '100%',
        maxWidth: 420,
        overflowY: 'auto',
        border: '1px solid var(--mn-color-border-default)',
        borderRadius: 'var(--mn-radius-lg)',
      }}
    >
      <p style={{ padding: 12, margin: 0 }}>{fixture.data.intro}</p>
      <Sticky onStuckChange={setStuck} style={{ padding: '8px 12px' }}>
        <Inline justify="between">
          <strong>{fixture.data.header}</strong>
          <span style={{ fontSize: 'var(--mn-text-sm)' }}>
            {stuck ? fixture.data.pinnedLabel : fixture.data.placeLabel}
          </span>
        </Inline>
      </Sticky>
      <ul style={{ margin: 0, padding: 12, listStyle: 'none', display: 'grid', gap: 8 }}>
        {ROWS.map((r) => (
          <li key={r}>{r}</li>
        ))}
      </ul>
    </div>
  );
}

function FooterDemo() {
  return (
    <div
      style={{
        height: 220,
        width: '100%',
        maxWidth: 420,
        overflowY: 'auto',
        border: '1px solid var(--mn-color-border-default)',
        borderRadius: 'var(--mn-radius-lg)',
      }}
    >
      <ul style={{ margin: 0, padding: 12, listStyle: 'none', display: 'grid', gap: 8 }}>
        {ROWS.slice(0, 12).map((r) => (
          <li key={r}>{r}</li>
        ))}
      </ul>
      <Sticky as="footer" edge="bottom" style={{ padding: 12 }}>
        <Inline justify="end">
          <Button variant="secondary">Cancel</Button>
          <Button>Save</Button>
        </Inline>
      </Sticky>
    </div>
  );
}

export default {
  id: 'sticky',
  title: 'Sticky',
  summary: 'Pins content to the top or bottom edge while scrolling, and knows when it is stuck.',
  sections: {
    purpose: {
      summary:
        'Sticky headers, filter bars and action footers are `sticky top-0` in most Rook apps (rookatlas, rookbase, rookcalendar, rookchat, rookdevelopers, rookdots, rookgtm, rookid, rookpeople, rookrecruit, rookstride and rooktickets frontends), and each one hand-rolls a scroll listener to add a shadow when pinned. Sticky does both with an IntersectionObserver. Needed by: those apps.',
      useWhen: [
        'A section header, filter bar or table toolbar that should stay visible while its content scrolls.',
        'A save bar at the bottom of a long form.',
        'Anything that should look different only while pinned (`data-stuck`, `onStuckChange`).',
      ],
      avoidWhen: [
        'The app header and rail: AppShell handles them.',
        'Content that must cover the page (a banner over everything): use a fixed overlay pattern.',
        'Tall content: a sticky block taller than the screen can never be fully seen.',
      ],
    },
    designPrinciples: {
      items: [
        'CSS does the sticking; JavaScript only observes, so there is no scroll listener and no jank.',
        'Elevation only while stuck: a surface background and a small shadow separate it from scrolling content.',
        'Works inside scrolling panels: the observer uses the nearest scrolling ancestor.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a zero-size `<span class="mn-sticky__sentinel" aria-hidden>` before (top) or after (bottom) the sticky element, which is `<div class="mn-sticky">` or `as`.',
        'Stuck state: `data-stuck` and `onStuckChange(stuck)`. Without IntersectionObserver it still sticks, it just never reports stuck.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Tailwind: `sticky top-0 z-10 bg-background` becomes `<Sticky>`; `top-16` becomes `offset="4rem"`; a scroll listener that toggles a shadow class becomes `data-stuck` styling or `onStuckChange`.',
      ],
    },
    variants: {
      items: [
        { prop: 'edge', values: ['top', 'bottom'], description: 'Which edge it pins to.' },
        {
          prop: 'offset',
          values: ['0', 'CSS length', 'number of px'],
          description: 'Distance from that edge, for example below a fixed header.',
        },
        {
          prop: 'elevateWhenStuck',
          values: ['true', 'false'],
          description: 'Surface background and `--mn-shadow-sm` while stuck.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Stuck',
          attribute: 'data-stuck',
          description: 'Pinned to its edge; elevated unless `elevateWhenStuck` is false.',
        },
        {
          name: 'Edge',
          attribute: 'data-edge="top|bottom"',
          description: 'Which edge it pins to.',
        },
      ],
    },
    interaction: {
      items: [
        'Stays within its parent, as CSS sticky does: it unpins when the parent scrolls away.',
        '`onStuckChange` fires only on a change, never repeatedly while scrolling.',
      ],
    },
    keyboard: { items: [], notes: ['Not interactive itself; its content keeps normal tab order.'] },
    accessibility: {
      role: 'none by default; use `as="header"` or `as="footer"` where it fits',
      name: 'Not needed unless it is a landmark that needs a label.',
      items: [
        'The marker element is hidden from assistive technology.',
        'Pinned content must not hide focused elements: give scrolling containers `scroll-padding` equal to the sticky height (WCAG 2.4.11).',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: [
        'Keep sticky content short on phones (one row); at 390px a tall sticky block leaves little room to read.',
      ],
    },
    theme: {
      items: [
        'Stuck: `--mn-color-bg-surface` and `--mn-shadow-sm`, at `--mn-z-sticky`.',
        'The shadow transition is removed under reduced motion; forced-colours mode draws a CanvasText border instead of the shadow.',
      ],
    },
    examples: {
      items: [
        { title: 'Header that knows it is pinned', stack: true, render: () => <HeaderDemo /> },
        { title: 'Save bar at the bottom', stack: true, render: () => <FooterDemo /> },
      ],
    },
    doDont: {
      do: [
        'Set `offset` to the height of anything fixed above it.',
        'Add `scroll-padding-top` to the scroll container.',
      ],
      dont: [
        'Do not make more than one block sticky to the same edge in one scroll area.',
        'Do not put long content in a sticky block.',
      ],
    },
    related: {
      items: [
        { id: 'table-toolbar', reason: 'A toolbar for tables that is often placed inside Sticky.' },
        { id: 'scroll-area', reason: 'A labelled scroll container to stick within.' },
        { id: 'app-shell', reason: 'The app header and rail, already fixed.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Top sticky with an aria-hidden marker before it, observed; axe.',
        'Stuck when the marker passes the top edge; onStuckChange only on change; not stuck when below.',
        'Bottom edge with offset and no elevation; CSS-length offset.',
        'No IntersectionObserver; disconnects on unmount; ref; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

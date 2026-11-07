import { useEffect, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { AiScrollAnchor, useAiScrollAnchor } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const LINES = [
  'The draft has three sections.',
  'The first summarises the request.',
  'The second lists the open questions.',
  'The third proposes next steps.',
  'Each step has an owner and a date.',
  'Nothing is sent until you approve it.',
];

function GrowingLog() {
  const [count, setCount] = useState(2);
  useEffect(() => {
    if (count >= 24) return;
    const t = setTimeout(() => setCount((c) => c + 1), 700);
    return () => clearTimeout(t);
  }, [count]);
  return (
    <AiScrollAnchor
      watch={count}
      tabIndex={0}
      aria-label="Growing answer"
      style={{ height: 180, overflow: 'auto', border: '1px solid var(--mn-color-border-subtle)' }}
      footer={({ atBottom, scrollToBottom }) =>
        atBottom ? null : (
          <div style={{ position: 'sticky', bottom: 8, textAlign: 'center' }}>
            <Button size="sm" onClick={() => scrollToBottom()}>
              Jump to latest
            </Button>
          </div>
        )
      }
    >
      {Array.from({ length: count }, (_, i) => (
        <p key={i} style={{ margin: 8 }}>
          {LINES[i % LINES.length]}
        </p>
      ))}
    </AiScrollAnchor>
  );
}

function HookDemo() {
  const [items, setItems] = useState(['First line.']);
  const { scrollRef, contentRef, atBottom } = useAiScrollAnchor({ watch: items.length });
  return (
    <div style={{ display: 'grid', gap: 8 }}>
      <div
        ref={scrollRef}
        // A scrollable region must be reachable by keyboard.
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        tabIndex={0}
        aria-label="Hook demo"
        style={{ height: 120, overflow: 'auto', border: '1px solid var(--mn-color-border-subtle)' }}
      >
        <div ref={contentRef}>
          {items.map((t, i) => (
            <p key={i} style={{ margin: 8 }}>
              {t}
            </p>
          ))}
        </div>
      </div>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <Button size="sm" onClick={() => setItems((l) => [...l, `Line ${l.length + 1}.`])}>
          Add a line
        </Button>
        <span>{atBottom ? 'At the bottom' : 'Scrolled up'}</span>
      </div>
    </div>
  );
}

export default {
  id: 'ai-scroll-anchor',
  title: 'AiScrollAnchor',
  summary: 'Headless bottom-pinned scrolling for streaming answers and chat threads.',
  sections: {
    purpose: {
      summary:
        'Keeps a scroll container at the bottom while content streams in, but only while the person is already at the bottom, so reading earlier text is never interrupted. `useAiScrollAnchor` is the hook; `AiScrollAnchor` is a thin unstyled wrapper. Needed by: AiThread.',
      useWhen: [
        'A region grows at the end while the person reads it: a streaming answer, a chat log, a live build output.',
        'You need a "Jump to latest" control that appears only when the person has scrolled away.',
      ],
      avoidWhen: [
        'You are rendering a full conversation: use AiThread, which already uses this hook.',
        'The content does not grow at the end (a document, a table): plain overflow is enough.',
      ],
    },
    designPrinciples: {
      items: [
        'The person is in charge: scrolling up releases the pin immediately; nothing pulls them back until they return to the bottom or ask to.',
        'Never moves focus. Scrolling is done by setting the scroll position only.',
        'Headless: no stylesheet, no layout opinions. You supply the height and `overflow: auto`.',
        'Smooth scrolling becomes instant when the person prefers reduced motion.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useAiScrollAnchor({ watch, threshold, onAtBottomChange })` returns `{ scrollRef, contentRef, atBottom, scrollToBottom(behavior?), isPinned() }`.',
        '`AiScrollAnchor` forwards its ref to the scrolling `<div>`; `children` and `footer` may be functions of `{ atBottom, scrollToBottom }`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: { items: [], notes: ['No visual variants: the component ships no styles.'] },
    states: {
      items: [
        {
          name: 'At the bottom',
          attribute: 'data-at-bottom',
          description:
            'The scroll position is within `threshold` (48px) of the end. New content keeps it there.',
        },
        {
          name: 'Scrolled away',
          description:
            'The person scrolled up past the threshold. `data-at-bottom` is absent and growth no longer moves the view.',
        },
      ],
    },
    interaction: {
      items: [
        'A ResizeObserver on the content and the scroller re-pins after any size change (streamed text, images loading, a mobile keyboard).',
        'A change to `watch` re-pins in the same frame, before paint.',
        'Scrolling up past the threshold releases the pin; scrolling back within it restores the pin.',
        '`scrollToBottom()` scrolls smoothly (instantly under reduced motion) and pins again.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Arrow keys, Page Up / Page Down, Home / End',
          action:
            'Native scrolling when the scroller is focusable (give it `tabIndex={0}` and a label). Scrolling up releases the pin like the mouse wheel does.',
        },
      ],
    },
    accessibility: {
      role: 'None of its own; give the scroller the role your content needs (AiThread uses `log`).',
      name: 'Pass `aria-label` or `aria-labelledby` when the scroller is focusable.',
      items: [
        'Focus is never moved, so a screen-reader or keyboard user stays where they are while text streams in.',
        'A scrollable region must be reachable by keyboard: add `tabIndex={0}` and an accessible name.',
        'Announcing new content is not this hook’s job: pair it with a live region (AiThread owns one).',
      ],
    },
    responsive: {
      items: [
        'Works at any size; the observer also re-pins when the scroller itself resizes, for example when a mobile keyboard opens.',
      ],
    },
    theme: {
      items: ['No colours or styles of its own, so it follows whatever the content uses.'],
    },
    examples: {
      items: [
        { title: 'Growing content with a jump button', stack: true, render: () => <GrowingLog /> },
        { title: 'The hook on your own elements', stack: true, render: () => <HookDemo /> },
      ],
    },
    doDont: {
      do: [
        'Give the scroller a fixed or flex-constrained height and `overflow: auto`.',
        'Pass a `watch` value that changes with the content, such as the message count plus the streaming text length.',
        'Offer a visible way back to the bottom when `atBottom` is false.',
      ],
      dont: [
        'Do not call `scrollToBottom` on every token: the pin already follows growth.',
        'Do not move focus to new content to keep it in view.',
      ],
    },
    related: {
      items: [
        { id: 'ai-thread', reason: 'A full conversation list built on this hook.' },
        { id: 'ai-streaming-text', reason: 'The streaming text that usually grows inside it.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Pins to the bottom when content grows (watch change and ResizeObserver) with mocked scroll metrics.',
        'Scrolling up releases the pin and keeps the position; a scroll within the threshold keeps it pinned.',
        '`scrollToBottom` re-pins, uses `behavior: auto` under reduced motion and does not move focus.',
        'Wrapper renders the content element, the render-function children and `data-at-bottom`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

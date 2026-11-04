import { InlineErrorRetry, Link } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = () => new Promise<void>((resolve) => setTimeout(resolve, 1200));

export default {
  id: 'inline-error-retry',
  title: 'InlineErrorRetry',
  summary: 'A compact "this part didn’t load" row with Try again.',
  sections: {
    purpose: {
      summary:
        'Reports that one section of a working page failed to load and lets the person retry it in place, without replacing the whole region.',
      useWhen: [
        'A card, widget or list section failed while the rest of the page loaded.',
        'A refresh failed but older data is still shown (`variant="banner"`, `tone="warning"`).',
        'A form field’s options failed to load (a country list, say).',
      ],
      avoidWhen: [
        'The whole region failed and has nothing else to show: use ErrorState.',
        'The whole route failed: use ErrorPage.',
        'The message is not an error: use Alert.',
      ],
    },
    designPrinciples: {
      items: [
        'One sentence from the product saying what did not load, then Try again. No raw error text.',
        'Reassure when you can ("The rest of the dashboard is up to date").',
        'Quiet by default: `inline` is a plain row; `banner` adds a tint only when it must stand above stale data.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        'If `onRetry` returns a promise (a refetch), the button stays busy until it settles. Pass `retrying` to control the busy state yourself; the returned value is then ignored.',
        'Extra actions go in `actions`, after Try again.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['inline', 'banner'],
          description:
            '`inline` (default) is an unboxed row; `banner` adds a border, radius and tinted background. Exposed as `data-variant`.',
        },
        {
          prop: 'tone',
          values: ['danger', 'warning'],
          description:
            '`danger` (default) when nothing could load; `warning` when stale data is still on screen. Exposed as `data-tone`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Failed', description: 'The message and Try again.' },
        {
          name: 'Retrying',
          attribute: 'data-retrying; aria-busy="true" on the button',
          description:
            'The button shows a spinner and ignores clicks but keeps focus; "Retrying" is announced politely.',
        },
        {
          name: 'Failed again',
          description:
            'When a retry settles and the row is still shown, the message is re-inserted into the alert so it is announced again.',
        },
      ],
    },
    interaction: {
      items: [
        'Try again calls `onRetry` once per attempt; repeat clicks while busy are ignored.',
        'Focus stays on Try again during and after a failed retry.',
        'When a successful retry removes the row while it holds focus, focus moves to `returnFocusTo` (a ref or a function), usually the loaded section’s heading.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves to Try again, then any extra actions.' },
        { keys: 'Enter / Space', action: 'Retries.' },
      ],
    },
    accessibility: {
      role: 'alert (the message) and status (the retrying announcement)',
      items: [
        'The message sits in role="alert", so it is announced when the row appears and again after each failed retry.',
        'The "Retrying" announcement is a separate polite role="status", outside the alert, so it never interrupts.',
        'The busy button stays focusable (aria-disabled + aria-busy) so keyboard users do not lose their place.',
        'Without `returnFocusTo`, focus falls back to the document when the row disappears; give the loaded content a focusable heading (`tabIndex={-1}`) and pass it.',
        'The icon is `aria-hidden`; the spinner slows under `prefers-reduced-motion: reduce`.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/inline-error-retry.',
      ],
    },
    responsive: {
      items: [
        'A wrapping flex row: the message takes the remaining width (12rem minimum) and the actions drop below it on narrow screens, down to 320px.',
      ],
    },
    theme: {
      items: [
        'The icon uses `--mn-color-danger-strong` (or warning); `banner` uses the matching subtle background and border tokens.',
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Inline, with a pending retry',
          stack: true,
          render: () => (
            <InlineErrorRetry onRetry={wait}>Your recent payouts didn’t load.</InlineErrorRetry>
          ),
        },
        {
          title: 'Banner above stale data',
          stack: true,
          render: () => (
            <>
              <InlineErrorRetry variant="banner" onRetry={wait}>
                We couldn’t load this chart. The rest of the dashboard is up to date.
              </InlineErrorRetry>
              <InlineErrorRetry
                variant="banner"
                tone="warning"
                onRetry={wait}
                actions={<Link href="#status">View status</Link>}
              >
                Showing data from 10 minutes ago.
              </InlineErrorRetry>
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Name what did not load ("Your earnings didn’t load"), not that an error happened.',
        'Return the refetch promise from `onRetry`, or pass `retrying`.',
        'Pass `returnFocusTo` so keyboard users land on the loaded content.',
      ],
      dont: [
        'Do not show the error message from the server.',
        'Do not stack several InlineErrorRetry rows for one failure; one per independent section.',
      ],
    },
    related: {
      items: [
        { id: 'error-state', reason: 'The whole region failed and has nothing else to show.' },
        { id: 'alert', reason: 'A message that is not a load failure.' },
        { id: 'error-page', reason: 'The whole route failed.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Announces the failure as an alert and retries from the keyboard.',
        'Uncontrolled: stays busy until the returned promise settles, keeps focus, ignores repeat clicks and announces "Retrying".',
        'Re-inserts the message after a failed retry so it is announced again.',
        'Controlled: follows `retrying` and re-announces on busy → idle.',
        'Moves focus to `returnFocusTo` when a successful retry removes it, and never steals focus it did not hold.',
        'Banner variant, warning tone, extra actions, custom labels and ref.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery',
        'Cross-browser axe: /components/inline-error-retry',
      ],
    },
  },
} satisfies ComponentDocs;

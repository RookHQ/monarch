import { useState } from 'react';
import { FreshnessIndicator } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const HOUR = 3_600_000;

function RefreshDemo() {
  const [updatedAt, setUpdatedAt] = useState(() => Date.now() - 5 * HOUR);
  return (
    <FreshnessIndicator
      variant="banner"
      source="Directory"
      updatedAt={updatedAt}
      staleAfter={HOUR}
      expiredAfter={24 * HOUR}
      details={['Live updates on']}
      onRefresh={() =>
        new Promise<void>((resolve) =>
          setTimeout(() => {
            setUpdatedAt(Date.now());
            resolve();
          }, 800),
        )
      }
      actions={<a href="#manage">Manage source</a>}
    />
  );
}

export default {
  id: 'freshness-indicator',
  title: 'FreshnessIndicator',
  summary: 'How fresh a data set or sync is, with an optional Refresh button.',
  sections: {
    purpose: {
      summary:
        'Tells readers when a copy of data was last updated and warns when it is out of date, so they know whether to trust what they see or refresh it. Needed by: Atlas (registry and directory sync status).',
      useWhen: [
        'A page or panel shows a synced or cached copy of data from another system.',
        'Readers may need to trigger a sync or refresh themselves.',
      ],
      avoidWhen: [
        'The source of one value: use Provenance.',
        'A long-running job with progress: use ProgressBar or UploadQueue.',
        'A service health or uptime status: use StatusBadge or UptimeBar.',
      ],
    },
    designPrinciples: {
      items: [
        'Thresholds are data: the product says when data becomes stale (`staleAfter`) and expired (`expiredAfter`).',
        'Status is never colour only: stale and expired add the words “Out of date” and “Very out of date”.',
        'Fetching and syncing stay in the product; `onRefresh` only reports when it is done.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref) with a `<p>` of status text and an optional actions group. Other div attributes pass through.',
        '`freshnessStatus(updatedAt, { staleAfter, expiredAfter, now })` is exported to reuse the same rule elsewhere (for example a filter).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Atlas `RegistryFreshness`): the component fetched its own status; Atlas now passes `updatedAt={status.syncedAt}`, `status={isLoading ? "checking" : error ? "error" : undefined}`, `error={status.lastError}`, the events / truncated / disabled / failures facts as `details`, `onRefresh={() => sync.mutateAsync()}` for writers (keep the toasts in Atlas), and the manage link as `actions`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['inline', 'banner'],
          description:
            '`inline` (default) is one line of muted text; `banner` adds a border and background for the top of a page. Exposed as `data-variant`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Fresh',
          attribute: 'data-status="fresh"',
          description: 'Green dot, “Updated 3 minutes ago”.',
        },
        {
          name: 'Stale',
          attribute: 'data-status="stale"',
          description: 'Warning dot and “Out of date”.',
        },
        {
          name: 'Expired',
          attribute: 'data-status="expired"',
          description: 'Danger dot and “Very out of date”.',
        },
        {
          name: 'Never',
          attribute: 'data-status="never"',
          description: 'Neutral dot, “Never updated”.',
        },
        {
          name: 'Checking',
          attribute: 'data-status="checking"',
          description: 'Pulsing info dot, “Checking…”.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: 'Danger dot and the `error` text.',
        },
        {
          name: 'Refreshing',
          attribute: 'aria-busy on the button',
          description:
            'The Refresh button shows a spinner and stays focusable until the promise settles.',
        },
      ],
    },
    interaction: {
      items: [
        'Refresh calls `onRefresh`; a returned promise keeps the button busy and repeat presses are ignored.',
        'The relative time updates itself as it ages (`live`, default on).',
        'Hovering or focusing the time shows the absolute date and time.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the time (tooltip), the Refresh button and any actions.' },
        { keys: 'Enter / Space', action: 'Refreshes.' },
      ],
    },
    accessibility: {
      role: 'group of text with a polite status region',
      name: 'Not named; it is read as text in reading order.',
      items: [
        'A visually hidden `role="status"` region announces “Refreshed.”, “Refresh failed.” and changes to stale, expired or error, but nothing on first render.',
        'The dot is decorative; each status has words.',
        'Middle dots are hidden and replaced by commas for screen readers.',
      ],
    },
    responsive: {
      items: [
        'The text and actions wrap onto separate lines on narrow screens; long text breaks instead of overflowing.',
        'On touch screens the Refresh button is at least 44 px tall.',
      ],
    },
    theme: {
      items: [
        'Dot colours are `--mn-color-success-solid`, `--mn-color-warning-solid`, `--mn-color-danger-solid` and `--mn-color-info-solid`; flags use the matching `-strong` text tokens.',
        'Forced colours: CanvasText dot, text and border.',
        'The checking pulse stops when reduced motion is requested.',
      ],
    },
    examples: {
      items: [
        { title: 'Banner with refresh', stack: true, render: () => <RefreshDemo /> },
        {
          title: 'States',
          stack: true,
          render: () => (
            <>
              <FreshnessIndicator updatedAt={Date.now() - 2 * 60_000} staleAfter={HOUR} />
              <FreshnessIndicator
                updatedAt={Date.now() - 3 * HOUR}
                staleAfter={HOUR}
                expiredAfter={24 * HOUR}
              />
              <FreshnessIndicator
                updatedAt={Date.now() - 50 * HOUR}
                staleAfter={HOUR}
                expiredAfter={24 * HOUR}
              />
              <FreshnessIndicator updatedAt={null} />
              <FreshnessIndicator updatedAt={null} status="checking" />
              <FreshnessIndicator
                updatedAt={Date.now() - HOUR}
                status="error"
                error="The source did not respond"
              />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pick thresholds that match how often the source changes.',
        'Show the refresh error itself in a toast or Alert; the indicator only announces that it failed.',
      ],
      dont: [
        'Do not show Refresh to people who cannot trigger a sync.',
        'Do not use it for single values; use Provenance.',
      ],
    },
    related: {
      items: [
        { id: 'provenance', reason: 'Source and age of one value.' },
        { id: 'relative-time', reason: 'The time inside it.' },
        { id: 'status-badge', reason: 'A named status without time.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'relative-time'] },
    testing: {
      unit: [
        'Shows the source and age with a fresh status.',
        'Turns stale and expired with written status words and announces the change.',
        'Never, checking and error states, and details.',
        'Refresh by keyboard: busy state, focus kept, “Refreshed.” announced; failures announced.',
        '`freshnessStatus` rules.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

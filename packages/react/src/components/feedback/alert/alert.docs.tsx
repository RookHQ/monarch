import { Alert, Button } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'alert',
  title: 'Alert',
  summary: 'Inline message about a page or section.',
  sections: {
    purpose: {
      summary:
        'An inline message about the state of a page or section, with an optional title, body and actions.',
      useWhen: [
        'Explaining a condition that affects the current view (sync paused, domain not verified).',
      ],
      avoidWhen: [
        'Transient confirmation of an action: use Toast.',
        'Nothing to show yet: use EmptyState.',
        'A one-line note next to a field, row or card: use InlineNotice.',
        'A banner across the page or app (trial, maintenance, billing) or one the user can dismiss: use CalloutBanner.',
      ],
    },
    designPrinciples: {
      items: [
        'Silent by default: only alerts that appear in response to a user action set `live`.',
        '`assertive` only for errors that block progress.',
        'Each tone has a default icon; replace it with `icon` or pass `null` for none.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'All native `<div>` attributes pass through; an explicit `role` wins over the one derived from `live`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['neutral', 'info', 'success', 'warning', 'danger'],
          description: 'Colour and default icon. Default `info`. Exposed as `data-tone`.',
        },
        {
          prop: 'live',
          values: ['polite', 'assertive'],
          description:
            '`polite` sets role="status", `assertive` sets role="alert"; unset means no live role.',
        },
      ],
    },
    states: {
      items: [{ name: 'Static', description: 'No interactive states; actions keep their own.' }],
    },
    interaction: {
      items: ['None of its own. Buttons passed in `actions` render on the trailing edge.'],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive itself; focus moves to controls inside `actions`.'],
    },
    accessibility: {
      role: 'none, status (live="polite") or alert (live="assertive")',
      items: [
        'The tone icon is `aria-hidden`; the title and body carry the message.',
        'A live role makes screen readers announce the alert when it appears.',
      ],
    },
    responsive: { items: ['No breakpoints: the alert fills its container width.'] },
    theme: { items: ['Each tone uses its subtle, strong and border tokens, so all themes apply.'] },
    examples: {
      items: [
        {
          title: 'Tones',
          stack: true,
          render: () => (
            <>
              <Alert tone="info" title="Sync scheduled">
                Rook ID members refresh every 15 minutes.
              </Alert>
              <Alert tone="success" title="Saved" />
              <Alert
                tone="warning"
                title="Domain not verified"
                actions={<Button size="sm">Verify</Button>}
              />
              <Alert tone="danger" title="Payment failed" live="assertive">
                Update the card to keep the workspace active.
              </Alert>
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Set `live` when the alert appears because of something the user did.',
        'Offer the fixing action in `actions`.',
      ],
      dont: [
        'Do not make static page alerts live; they would be announced on every render.',
        'Do not use `assertive` for information that does not block the user.',
      ],
    },
    related: {
      items: [
        { id: 'toast', reason: 'Transient feedback after an action.' },
        {
          id: 'callout-banner',
          reason:
            'Page- or app-level banner (trial, maintenance, billing, scope), dismissible and optionally a landmark.',
        },
        {
          id: 'inline-notice',
          reason:
            'One line attached to a field, row or card; no body block. Use Alert when the message needs a title and a paragraph.',
        },
      ],
    },
    dependencies: { packages: [], components: [] },
    testing: {
      unit: ['Announces only when `live` is set (status / alert).'],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;

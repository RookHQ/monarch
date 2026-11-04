import { Button, CalloutBanner } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'callout-banner',
  title: 'CalloutBanner',
  summary:
    'Page- or section-level banner: trials, maintenance, billing, failed syncs and narrowed scopes.',
  sections: {
    purpose: {
      summary:
        'A message about the account, the product or the whole view, shown above the content it affects. It can carry actions, be dismissed (the product decides whether that sticks) and be a landmark.',
      useWhen: [
        'Account or workspace state that affects everything: a trial ending, a failed payment, a plan limit.',
        'Planned or ongoing maintenance, or a degraded service.',
        'A section-level problem with a fix, such as a failed sync with Retry.',
        'The view is narrowed to a Space, team or filter and the user should be able to clear it (`variant="scope"`).',
      ],
      avoidWhen: [
        'Confirming an action that just happened: use Toast.',
        'A short note attached to one field, row or card: use InlineNotice.',
        'A message about one section with a title and a paragraph that never needs dismissing: Alert is enough.',
        'The user must decide before continuing: use Dialog.',
      ],
    },
    designPrinciples: {
      items: [
        'One banner per placement: stack at most one `page` banner above the app and one `section` banner above a view.',
        'Lead with the consequence ("Your trial ends in 3 days"), then what to do, then the action.',
        'Silent by default. Set `live` only when the banner appears because of something that just happened.',
        'Dismissal persistence is the product’s: `onDismiss` fires, Monarch stores nothing, and `defaultOpen={false}` hides a banner the user already closed.',
        'Tone carries meaning, never colour alone: every tone has its own icon, and the text says what happened.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root element (`<div>`, `<section>` or `<aside>` depending on `landmark`).',
        'All native attributes pass through to the root, including `aria-label` for a landmark with no title.',
        'Renders nothing while closed.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['neutral', 'info', 'accent', 'success', 'warning', 'danger'],
          description:
            'Colours and default icon. `accent` follows the product accent, for trials and announcements. Default `info`. Exposed as `data-tone`.',
        },
        {
          prop: 'placement',
          values: ['section', 'page'],
          description:
            '`section`: a rounded callout inside the content. `page`: a full-width strip with only a bottom border, for the top of the app. Default `section`. Exposed as `data-placement`.',
        },
        {
          prop: 'variant',
          values: ['callout', 'scope'],
          description:
            '`scope`: a compact one-line "you are looking at a subset" notice with no icon by default. Default `callout`. Exposed as `data-variant`.',
        },
        {
          prop: 'actionPlacement',
          values: ['end', 'inline'],
          description:
            '`end` puts actions on the trailing edge (below the text on narrow screens); `inline` right after the text. Default `end`. Exposed as `data-action-placement`.',
        },
        {
          prop: 'landmark',
          values: ['none', 'region', 'complementary'],
          description:
            'Root element and landmark role: `<div>`, `<section>` (region) or `<aside>` (complementary). Default `none`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Open', description: 'Rendered. The default.' },
        {
          name: 'Dismissed',
          description:
            'Uncontrolled: hidden after the close button is pressed. Controlled: hidden when `open` becomes false.',
        },
        {
          name: 'Live',
          attribute: 'role="status" | role="alert" (on the text)',
          description: 'Announced when it appears; only when `live` is set.',
        },
      ],
    },
    interaction: {
      items: [
        'The close button appears when `dismissible` or `onDismiss` is set; pressing it calls `onDismiss` and, when uncontrolled, hides the banner.',
        'With `open`, the banner stays until the parent sets `open={false}`, for example after saving the dismissal.',
        'If focus was on the close button, it moves to the next focusable element after the banner (or the previous one), never to the page body.',
        'Actions are ordinary buttons or links passed in `actions`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the actions, then the close button.' },
        { keys: 'Enter / Space', action: 'Activates the focused action or dismisses the banner.' },
      ],
      notes: ['The banner itself never takes focus when it appears.'],
    },
    accessibility: {
      role: 'none by default; region (`<section>`) or complementary (`<aside>`) with `landmark`',
      name: 'A landmark is named by `title` (aria-labelledby) or by `aria-label`.',
      items: [
        'The live role (`status` or `alert`) is on the text, not the root, so a landmark keeps its own role and the announcement is only the message.',
        'The tone icon is `aria-hidden`; the title and body carry the meaning.',
        'The close button is an IconButton labelled `dismissLabel` (default "Dismiss").',
        'Text and icon colours come from tokens with contrast guaranteed in every theme: title and body on the tinted background, icons in the tone’s strong colour.',
      ],
    },
    responsive: {
      items: [
        'Below 40rem, trailing actions wrap under the text, aligned with it.',
        'The title and body wrap; nothing is truncated. Works down to 320px.',
        '`placement="page"` uses the layout gutter (`--mn-layout-gutter`) so it lines up with page content.',
      ],
    },
    theme: {
      items: [
        'Each tone uses its intent subtle, strong and border tokens; `accent` uses the product accent, so each Rook product gets its own colour.',
        'Light, dark and high-contrast themes apply automatically.',
        'No animation, so reduced motion needs nothing.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Page banner with an action',
          stack: true,
          render: () => (
            <CalloutBanner
              placement="page"
              tone="accent"
              title="Your trial ends in 3 days"
              actions={
                <Button size="sm" variant="primary">
                  Choose a plan
                </Button>
              }
              onDismiss={() => {}}
            >
              Add a payment method to keep your workspace and its data.
            </CalloutBanner>
          ),
          code: `<CalloutBanner
  placement="page"
  tone="accent"
  title="Your trial ends in 3 days"
  actions={<Button size="sm" variant="primary">Choose a plan</Button>}
  onDismiss={() => saveDismissal('trial-banner')}
>
  Add a payment method to keep your workspace and its data.
</CalloutBanner>`,
        },
        {
          title: 'Tones',
          stack: true,
          render: () => (
            <>
              <CalloutBanner tone="info" landmark="region" title="Scheduled maintenance">
                Sign-in is unavailable on Sunday from 02:00 to 03:00 UTC.
              </CalloutBanner>
              <CalloutBanner tone="warning" title="Payment method expires this month" />
              <CalloutBanner
                tone="danger"
                title="Sync failed"
                live="assertive"
                actions={<Button size="sm">Retry</Button>}
              >
                The registry did not respond. Your changes are saved locally.
              </CalloutBanner>
              <CalloutBanner tone="success" title="Domain verified" dismissible />
            </>
          ),
          code: `<CalloutBanner tone="info" landmark="region" title="Scheduled maintenance">
  Sign-in is unavailable on Sunday from 02:00 to 03:00 UTC.
</CalloutBanner>
<CalloutBanner tone="warning" title="Payment method expires this month" />
<CalloutBanner tone="danger" title="Sync failed" live="assertive" actions={<Button size="sm">Retry</Button>}>
  The registry did not respond. Your changes are saved locally.
</CalloutBanner>
<CalloutBanner tone="success" title="Domain verified" dismissible />`,
        },
        {
          title: 'Scope notice',
          stack: true,
          render: () => (
            <CalloutBanner
              variant="scope"
              tone="neutral"
              live="polite"
              actionPlacement="inline"
              actions={
                <Button size="xs" variant="ghost" onClick={() => {}}>
                  Show all Spaces
                </Button>
              }
            >
              Showing tickets in <strong>Support</strong>.
            </CalloutBanner>
          ),
          code: `<CalloutBanner
  variant="scope"
  tone="neutral"
  live="polite"
  actionPlacement="inline"
  actions={<Button size="xs" variant="ghost" onClick={clearSpace}>Show all Spaces</Button>}
>
  Showing tickets in <strong>Support</strong>.
</CalloutBanner>`,
        },
      ],
    },
    doDont: {
      do: [
        'Store the dismissal in `onDismiss` and pass `defaultOpen={false}` (or `open`) next time.',
        'Give a page banner one clear action; link to details rather than packing in paragraphs.',
        'Use `landmark="region"` for banners people may want to return to, such as maintenance notices.',
      ],
      dont: [
        'Do not stack several page banners; show the most urgent one.',
        'Do not use `live="assertive"` for promotions or reminders.',
        'Do not make a billing or security problem dismissible if it still blocks the user.',
      ],
    },
    related: {
      items: [
        { id: 'alert', reason: 'Section message with title and body that is never dismissed.' },
        { id: 'inline-notice', reason: 'One line attached to a field, row or card.' },
        { id: 'toast', reason: 'Transient confirmation of an action.' },
        { id: 'button', reason: 'Actions and the close IconButton.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button'],
      notes: [
        'Intent `subtle`, `strong` and `border` tokens; `--mn-layout-gutter` for page placement.',
      ],
    },
    testing: {
      unit: [
        'Renders title, body, decorative icon and actions with tone, placement and variant attributes; axe clean.',
        'Forwards its ref and native attributes.',
        'landmark="region" and "complementary" render named `<section>` / `<aside>` landmarks; axe clean.',
        'A landmark without a title is named by aria-label.',
        'The live role sits on the text inside a landmark (alert / status).',
        'Dismisses uncontrolled, calls onDismiss and moves focus to the next control, or the previous one.',
        'Controlled `open` keeps it until the parent closes it; `defaultOpen={false}` renders nothing.',
        'Custom dismiss label; scope variant with inline action and no default icon; icon replace and removal.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/callout-banner'],
    },
  },
} satisfies ComponentDocs;

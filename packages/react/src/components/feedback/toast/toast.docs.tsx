import { useEffect, useRef } from 'react';
import { Button, ToastProvider, getErrorMessage, useToast } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './toast.fixture.json';

function ArchiveButton({ archived = false }: { archived?: boolean }) {
  const { toast } = useToast();
  const archive = () =>
    toast({
      title: fixture.data.toast.title,
      description: fixture.data.toast.description,
      tone: fixture.data.toast.tone as 'success',
      action: { label: fixture.data.toast.action, onClick: () => {} },
    });
  // The headline preview starts just after the ticket was archived.
  const shown = useRef(false);
  useEffect(() => {
    if (!archived || shown.current) return;
    shown.current = true;
    archive();
  });
  return <Button onClick={archive}>{fixture.data.trigger}</Button>;
}

function ErrorButton() {
  const { toast } = useToast();
  return (
    <Button
      variant="danger-subtle"
      onClick={() =>
        toast.error(
          getErrorMessage(
            { response: { data: { message: 'That handle is already in use' } } },
            { fallback: 'Could not save the workspace' },
          ),
        )
      }
    >
      Save with a conflict
    </Button>
  );
}

export default {
  id: 'toast',
  title: 'Toast',
  summary: 'Transient notifications from toast() or useToast(), rendered by ToastProvider.',
  sections: {
    purpose: {
      summary:
        'Brief, transient feedback about an action that just happened, with one optional action such as Undo.',
      useWhen: [
        'Confirming an action (saved, archived, assigned).',
        'Reporting a failure that does not block the page (`toast.error`).',
        'Offering Undo for a reversible action instead of asking for confirmation first.',
      ],
      avoidWhen: [
        'The message must stay visible: use Alert, InlineNotice or CalloutBanner.',
        'The user must decide something: use Dialog.',
        'The error belongs to a field: use the Field error.',
      ],
    },
    designPrinciples: {
      items: [
        'Mount ToastProvider once near the root. In components call `toast()` from `useToast()`; anywhere else (API clients, stores, plain modules) import `toast` from the package.',
        'A toast never takes focus; it is announced through a live region and reachable with F8.',
        'Danger is announced assertively, everything else politely.',
        'Toasts with an action, and danger toasts, stay 10s by default; others 5s. Users can be given more time with `durationScale`.',
        'At most three are open (`limit`); older ones close first.',
        'Never cover a docked panel: move the region with `offset` or the public `--mn-toast-offset-*` variables, never by overriding `.mn-toast-viewport`.',
        'Never put essential information only in a toast. Errors that block the user belong on the page as well.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`toast(options)` returns `{ id, dismiss }`. `toast.success`, `toast.error` (danger), `toast.warning` and `toast.info` take `(title, options?)`.',
        '`dismiss(id)` closes one toast; `dismiss()` closes all.',
        '`useToast()` throws outside ToastProvider.',
        'The package also exports a global `toast` with the same calls plus `toast.dismiss(id?)`. It works outside React and goes to the most recently mounted ToastProvider; calls made before one mounts are shown when it does (up to 10). On the server it does nothing.',
        '`toast(title, options?)` takes a plain title as well as `toast(options)`.',
        '`getErrorMessage(error, { fallback, network })` turns any thrown value into one readable sentence: field errors, `message`, `errors[]`, `detail`, from the response body first. Transport messages ("Request failed with status code 500", "Failed to fetch") and HTML error pages are replaced by the fallback.',
        'Offsets: `offset={{ blockStart, blockEnd, inlineEnd }}` on ToastProvider (numbers are pixels, strings any CSS length) moves the region in from the window edges. It sets three public CSS variables on the viewport, which a host stylesheet may also set on any ancestor (`:root`, `html[data-…]`) when the offset depends on app state: `--mn-toast-offset-block-end` (bottom positions), `--mn-toast-offset-block-start` (`top-right`) and `--mn-toast-offset-inline-end` (right positions). Each defaults to `0`, is added to the region’s own `--mn-space-4` padding and to the safe-area inset, and is allowed by `overrides/monarch-internals`. A value passed to `offset` wins over the same variable from a stylesheet.',
        'Docked panel recipe (CSS, no Monarch internals): `html[data-dock] { --mn-toast-offset-block-end: 4rem; }` lifts toasts above a launcher; `@media (min-width: 641px) { html[data-dock="open"] { --mn-toast-offset-inline-end: 26rem; } }` moves them beside a 26rem side panel; `html[data-dock="large"] { --mn-toast-offset-inline-end: calc(100% - 24rem - 0.5rem); }` sends them to the opposite edge of a wide panel (24rem is the region width on screens wider than 26rem).',
        "Migrating from sonner: replace `import { toast } from 'sonner'` with `import { toast } from '@rookcom/monarch-react'` and `<Toaster />` with `<ToastProvider>`. `toast(msg)`, `toast.success(msg, { description, action })`, `toast.error`, `toast.warning`, `toast.info` and `toast.dismiss(id)` have the same shape; `action` is `{ label, onClick }`; use `toast(msg)` for sonner's `toast.message(msg)`.",
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'ToastOptions: title, description, tone, action ({ label, onClick, altText }), duration, politeness, onDismiss.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['neutral', 'info', 'success', 'warning', 'danger'],
          description:
            'Accent edge and icon (none for `neutral`). Default `neutral`. Exposed as `data-tone`.',
        },
        {
          prop: 'politeness',
          values: ['polite', 'assertive'],
          description:
            'How it is announced. Default `assertive` for danger, `polite` otherwise. Exposed as `data-politeness`.',
        },
        {
          prop: 'ToastProvider position',
          values: ['bottom-right', 'bottom-center', 'top-right'],
          description:
            'Viewport placement. Default `bottom-right`. Right positions sit at the inline end, so they mirror to the left edge in right-to-left pages. Swipe direction is towards that edge for right positions, down otherwise.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'Visible; auto-dismisses after its duration unless paused.',
        },
        {
          name: 'Paused',
          description:
            'While the pointer is over the region, focus is inside it, or the window is in the background.',
        },
        {
          name: 'Closed',
          attribute: 'data-state="closed"',
          description: 'Exit animation runs, then the record is removed; `onDismiss` fires once.',
        },
        {
          name: 'Swiping',
          attribute: 'data-swipe',
          description: 'Follows the pointer and dismisses at the end of a swipe.',
        },
      ],
    },
    interaction: {
      items: [
        'Auto-dismisses after `duration` (5s; 10s for danger or with an action) times the provider’s `durationScale`. `Infinity` on either keeps it until dismissed.',
        'Pauses on hover and focus, and resumes when both leave.',
        'Activating the action runs `onClick` and dismisses the toast.',
        'The Dismiss button, Escape and a swipe close the toast.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'F8', action: 'Moves focus to the toast region (configurable with `hotkey`).' },
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between toasts, their action and Dismiss buttons.',
        },
        { keys: 'Escape', action: 'Dismisses the focused toast.' },
      ],
      notes: ['Focus is never moved to a toast automatically.'],
    },
    accessibility: {
      role: 'region (named, with the hotkey) containing a list; each toast is a list item',
      name: 'The region is named by `label` plus the hotkey ("Notifications (F8)"); each toast by its title.',
      items: [
        'Each toast is announced once through a separate live region: `aria-live="assertive"` for danger (or `politeness="assertive"`), `polite` otherwise. The list items carry no live role, so nothing is announced twice and the list stays valid.',
        'Toasts never steal focus (WCAG 2.4.3); F8 reaches them, so the action is keyboard accessible (2.1.1).',
        'Timing is adjustable (WCAG 2.2.1): hover or focus pauses, `durationScale` gives more time or turns auto-dismiss off.',
        'The close button is labelled `dismissLabel` (default "Dismiss") and described by the toast title, so several toasts stay distinguishable.',
        'The action announces `altText` (default its label) so screen-reader users know how else to do it after the toast is gone.',
        'Icons are `aria-hidden`. Reduced motion shortens the enter and exit animations to nothing.',
      ],
    },
    responsive: {
      items: [
        'The viewport is min(24rem, 100vw - 2rem) wide, so toasts fit narrow screens down to 320px.',
        'The region adds the safe-area insets (`env(safe-area-inset-*)`) to its offsets, so on pages with `viewport-fit=cover` toasts stay clear of a notch or home indicator.',
        'Offsets can change per breakpoint: set `--mn-toast-offset-*` inside a media query, for example a taller bottom offset on phones where a docked panel becomes a bottom bar.',
      ],
    },
    theme: {
      items: [
        'Raised surface and tone accent tokens, so light, dark and high-contrast themes apply.',
        'Reduced motion shortens the enter and exit animations.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Success with undo',
          stage: true,
          render: () => (
            <ToastProvider>
              <ArchiveButton archived />
            </ToastProvider>
          ),
        },
        {
          title: 'Errors from an API',
          render: () => (
            // Docs page only: each example has its own provider, so each gets a unique
            // region name and its own corner. Apps mount exactly one.
            <ToastProvider label="Error example notifications" position="bottom-center">
              <ErrorButton />
            </ToastProvider>
          ),
        },
        {
          title: 'Clear of a docked panel',
          render: () => (
            // The offset keeps toasts above a 3rem launcher docked at the bottom right.
            // A stylesheet can set the same --mn-toast-offset-* variables instead.
            <ToastProvider
              label="Docked panel example notifications"
              offset={{ blockEnd: '4rem', inlineEnd: '1rem' }}
            >
              <ArchiveButton />
            </ToastProvider>
          ),
        },
        {
          title: 'More time for people who need it',
          render: () => (
            <ToastProvider
              durationScale={2}
              label="Timing example notifications"
              position="top-right"
            >
              <ArchiveButton />
            </ToastProvider>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep the description to one line.',
        'Offer Undo for reversible actions instead of a confirmation dialog.',
        'Pass errors through `getErrorMessage` so people never see raw server or transport text.',
        'Wire `durationScale` to an accessibility preference if your app has one.',
        'Move toasts clear of docked panels with `offset` or the `--mn-toast-offset-*` variables.',
      ],
      dont: [
        'Do not mount more than one ToastProvider.',
        'Do not put essential information only in a toast; it disappears.',
        'Do not set `politeness="assertive"` for confirmations.',
        'Do not override `.mn-toast-viewport` positions (`bottom`, `right`); set the public offset variables instead.',
      ],
    },
    related: {
      items: [
        { id: 'alert', reason: 'Persistent message about a section.' },
        {
          id: 'inline-notice',
          reason: 'Persistent one-line message next to a field, row or card.',
        },
        { id: 'callout-banner', reason: 'Persistent page-level banner.' },
        { id: 'notification-center', reason: 'Planned persistent notification list.' },
      ],
    },
    dependencies: { packages: ['@radix-ui/react-toast', 'react'], components: [] },
    testing: {
      unit: [
        'Shows, runs the action (which dismisses) and dismisses with the close button.',
        'No axe violations with toasts of every tone open: list items carry no role (regression).',
        'Danger is announced assertively and other tones politely; politeness can be overridden.',
        'Never moves focus when a toast appears.',
        'F8 (or a custom hotkey) moves focus to the named region; Tab reaches the action; Escape dismisses.',
        'Each Dismiss button is labelled and described by its toast title.',
        'Keeps at most `limit` open; `dismiss()` closes all; `onDismiss` fires once each.',
        'Shorthands set the tone; a handle dismisses only its toast.',
        'Timing: 5s / 10s defaults, `durationScale`, `Infinity`, pause on focus and on hover.',
        'The action uses `altText` for screen readers.',
        'getErrorMessage: body before transport text, field errors, lists, fallbacks, network message.',
        'useToast throws outside a provider; renders on the server.',
        '`offset` writes the public `--mn-toast-offset-*` variables (numbers as pixels) and leaves the style empty without it.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Cross-browser axe: /components/toast',
        'Cross-browser placement: the offset variables, set by `offset` or by a stylesheet on an ancestor, move the region in from the bottom and end edges.',
      ],
    },
  },
} satisfies ComponentDocs;

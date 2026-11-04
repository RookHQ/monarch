import { Button, ToastProvider, getErrorMessage, useToast } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function ArchiveButton() {
  const { toast } = useToast();
  return (
    <Button
      onClick={() =>
        toast({
          title: 'Ticket archived',
          description: 'You can undo this.',
          tone: 'success',
          action: { label: 'Undo', onClick: () => {} },
        })
      }
    >
      Archive ticket
    </Button>
  );
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
  summary: 'Transient notifications from useToast(), rendered by ToastProvider.',
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
        'Mount ToastProvider once near the root; call `toast()` from `useToast()` anywhere below it.',
        'A toast never takes focus; it is announced through a live region and reachable with F8.',
        'Danger is announced assertively, everything else politely.',
        'Toasts with an action, and danger toasts, stay 10s by default; others 5s. Users can be given more time with `durationScale`.',
        'At most three are open (`limit`); older ones close first.',
        'Never put essential information only in a toast. Errors that block the user belong on the page as well.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`toast(options)` returns `{ id, dismiss }`. `toast.success`, `toast.error` (danger), `toast.warning` and `toast.info` take `(title, options?)`.',
        '`dismiss(id)` closes one toast; `dismiss()` closes all.',
        '`useToast()` throws outside ToastProvider.',
        '`getErrorMessage(error, { fallback, network })` turns any thrown value into one readable sentence: field errors, `message`, `errors[]`, `detail`, from the response body first. Transport messages ("Request failed with status code 500", "Failed to fetch") and HTML error pages are replaced by the fallback.',
        'Migrating from sonner: `toast.success(msg, { action })` and `toast.error(msg)` have the same shape; `action` is `{ label, onClick }`.',
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
            'Viewport placement. Default `bottom-right`. Swipe direction is right for right positions, down otherwise.',
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
          render: () => (
            <ToastProvider>
              <ArchiveButton />
            </ToastProvider>
          ),
          code: `// Once, near the root:
<ToastProvider>{app}</ToastProvider>

// Anywhere below:
const { toast } = useToast();
toast({
  title: 'Ticket archived',
  description: 'You can undo this.',
  tone: 'success',
  action: { label: 'Undo', onClick: undo },
});`,
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
          code: `const { toast } = useToast();
try {
  await save();
} catch (error) {
  toast.error(getErrorMessage(error, { fallback: 'Could not save the workspace' }));
}`,
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
          code: `// From a user preference; Infinity turns auto-dismiss off.
<ToastProvider durationScale={prefs.slowNotifications ? 3 : 1}>{app}</ToastProvider>`,
        },
      ],
    },
    doDont: {
      do: [
        'Keep the description to one line.',
        'Offer Undo for reversible actions instead of a confirmation dialog.',
        'Pass errors through `getErrorMessage` so people never see raw server or transport text.',
        'Wire `durationScale` to an accessibility preference if your app has one.',
      ],
      dont: [
        'Do not mount more than one ToastProvider.',
        'Do not put essential information only in a toast; it disappears.',
        'Do not set `politeness="assertive"` for confirmations.',
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
      ],
      axe: true,
      ssr: true,
      browser: ['Cross-browser axe: /components/toast'],
    },
  },
} satisfies ComponentDocs;

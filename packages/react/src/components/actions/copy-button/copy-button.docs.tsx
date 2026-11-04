import { useState } from 'react';
import { CopyButton, useCopyToClipboard, Button } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function WithErrorReport() {
  const [problem, setProblem] = useState<string | null>(null);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', justifyItems: 'start' }}>
      <CopyButton
        value="https://example.com/invite/7f3a"
        label="Copy invite link"
        copiedLabel="Link copied"
        onCopy={() => setProblem(null)}
        onCopyError={(error) => setProblem(error.message)}
      />
      {problem ? (
        <p style={{ margin: 0, fontSize: 'var(--mn-text-xs)' }}>
          Copy failed ({problem}). Select the link and copy it manually.
        </p>
      ) : null}
    </div>
  );
}

function HookDemo() {
  const { copy, status } = useCopyToClipboard();
  return (
    <Button size="sm" onClick={() => void copy('ORDER-2024-0193')}>
      {status === 'copied' ? 'Reference copied' : 'Copy reference'}
    </Button>
  );
}

export default {
  id: 'copy-button',
  title: 'CopyButton',
  summary:
    'Copies a value to the clipboard, confirms it visibly and to screen readers, and reports failures.',
  sections: {
    purpose: {
      summary:
        'One button that copies a value (a link, a key, a DNS record), shows a short "Copied" state, announces the result in a polite live region and hands errors to the caller. `useCopyToClipboard` is the same logic for custom controls.',
      useWhen: [
        'The user needs a value elsewhere: an invite link, a webhook URL, an ID, a code snippet.',
        'You are building your own copy control: use `useCopyToClipboard` so the success, error and reset behaviour match.',
      ],
      avoidWhen: [
        'The value should also be visible and selectable: use CopyField.',
        'The value is a secret: use SecretField with `copyable`, which copies without revealing it.',
      ],
    },
    designPrinciples: {
      items: [
        'Feedback is visible and spoken: the icon becomes a check and the label "Copied", and a polite live region says "Copied to clipboard".',
        'Never fails silently. The Clipboard API is tried first, then the legacy `execCommand` path; if both fail the button shows "Couldn\'t copy", announces it, and calls `onCopyError`.',
        'The button keeps its width: all three labels share one grid cell, so nothing jumps when the state changes.',
        'Built on Button, so variants, sizes, focus and disabled behaviour are the same as every other action.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'CopyButton forwards its ref to the `<button>`. It renders a visually hidden `role="status"` sibling for announcements.',
        '`useCopyToClipboard({ resetAfter, onCopy, onError })` returns `{ copy, status, error, reset }`. `copy(value)` resolves `true` or `false` and never rejects.',
        '`writeToClipboard(text)` is the low-level write (Clipboard API, then the legacy fallback); it rejects with an Error when both fail.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'All Button props except `children`, `asChild` and `leadingIcon` pass through, including `variant` and `size`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['primary', 'secondary', 'ghost', 'danger', 'danger-subtle'],
          description: 'From Button. Default `secondary`, or `ghost` when `iconOnly`.',
        },
        {
          prop: 'size',
          values: ['xs', 'sm', 'md', 'lg'],
          description: 'From Button. Default `md`.',
        },
        {
          prop: 'iconOnly',
          values: ['true', 'false'],
          description:
            'Square, icon-only button (`data-icon-only`). `label` stays the accessible name and tooltip.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Idle', attribute: 'data-state="idle"', description: 'Copy icon and `label`.' },
        {
          name: 'Copied',
          attribute: 'data-state="copied"',
          description:
            'Check icon in the success colour and `copiedLabel`, for `resetAfter` milliseconds (default 2000).',
        },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description:
            'Warning icon in the danger colour and `errorLabel`; `onCopyError` has been called.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'From Button: nothing is copied.',
        },
      ],
    },
    interaction: {
      items: [
        'Click, Enter or Space copies `value`. A function `value` is called at that moment and may be async.',
        'Your `onClick` runs first; call `event.preventDefault()` in it to skip the copy.',
        'Each attempt clears and refills the live region, so copying twice is announced twice.',
        'The state returns to idle after `resetAfter`; `0` keeps it until the next attempt.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves focus to the button.' },
        { keys: 'Enter / Space', action: 'Copies the value.' },
      ],
    },
    accessibility: {
      role: 'button',
      name: 'The visible label, plus the visually hidden `target` ("Copy" + "invite link"). Icon-only: `label` (+ `target`) as `aria-label`.',
      items: [
        'The result is announced through a `role="status"` live region outside the button, so it is not part of the button name.',
        'Icon-only buttons keep a stable `aria-label`; the change is announced instead of renamed.',
        'Use `target` when several copy buttons share a screen, so each has a distinct name (WCAG 2.4.6). The visible text stays at the start of the name (WCAG 2.5.3).',
        'Icons are decorative (`aria-hidden`).',
      ],
    },
    responsive: {
      items: [
        'Same height as Button at every width (`--mn-control-*`).',
        'The label never wraps; use `iconOnly` in tight rows such as table cells.',
      ],
    },
    theme: {
      items: [
        'Colours come from Button and the success and danger tokens, so light, dark and more-contrast themes apply automatically.',
        'On `primary` and `danger` buttons the state icon inherits the on-solid text colour.',
        'The icon colour transition is removed under `prefers-reduced-motion: reduce`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Labels and icon-only',
          render: () => (
            <>
              <CopyButton value="https://example.com/hooks/inbound" target="webhook URL" />
              <CopyButton
                value="https://example.com/s/q3-plan"
                label="Copy link"
                copiedLabel="Link copied"
                variant="primary"
              />
              <CopyButton value="ws_01HZX4" label="Copy workspace ID" iconOnly size="sm" />
            </>
          ),
          code: `<CopyButton value={webhookUrl} target="webhook URL" />
<CopyButton value={shareUrl} label="Copy link" copiedLabel="Link copied" variant="primary" />
<CopyButton value={workspaceId} label="Copy workspace ID" iconOnly size="sm" />`,
        },
        {
          title: 'Reporting a failure',
          render: () => <WithErrorReport />,
          code: `const [problem, setProblem] = useState<string | null>(null);

<CopyButton
  value={inviteUrl}
  label="Copy invite link"
  copiedLabel="Link copied"
  onCopy={() => setProblem(null)}
  onCopyError={(error) => setProblem(error.message)} // or toast({ tone: 'danger', … })
/>
{problem ? <p>Copy failed. Select the link and copy it manually.</p> : null}`,
        },
        {
          title: 'useCopyToClipboard',
          render: () => <HookDemo />,
          code: `const { copy, status } = useCopyToClipboard();

<Button size="sm" onClick={() => void copy(reference)}>
  {status === 'copied' ? 'Reference copied' : 'Copy reference'}
</Button>`,
        },
      ],
    },
    doDont: {
      do: [
        'Name what is copied: `target="API key"` or `label="Copy invite link"`.',
        'Handle `onCopyError`: show a toast or point the user at a selectable copy of the value.',
        'Use CopyField when the user should also see the value.',
      ],
      dont: [
        'Do not show a toast as the only confirmation; the button already confirms visibly and to screen readers.',
        'Do not put a secret in a plain CopyButton next to its plain text; use SecretField.',
        'Do not change `label` while the button is showing "Copied"; use `copiedLabel`.',
      ],
    },
    related: {
      items: [
        { id: 'copy-field', reason: 'Shows the value, read-only, with this button beside it.' },
        { id: 'secret-field', reason: 'Copies a masked secret without revealing it.' },
        { id: 'button', reason: 'The control CopyButton is built on.' },
        { id: 'toast', reason: 'A product-level way to report `onCopyError`.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button'],
    },
    testing: {
      unit: [
        'Copies the value, shows the copied state, and announces "Copied to clipboard".',
        'Copies with Enter and Space.',
        'A refused write calls `onCopyError`, shows the error state and announces it.',
        'Falls back to `execCommand` when the Clipboard API rejects; rejects when nothing is available.',
        'Returns to idle after `resetAfter`.',
        'Accepts sync and async function values.',
        'Icon-only keeps a stable accessible name.',
        'Does not copy when disabled or when `onClick` prevents default.',
        'Inactive labels are hidden from the accessible name.',
        '`useCopyToClipboard` exposes status, error and reset.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/copy-button'],
    },
  },
} satisfies ComponentDocs;

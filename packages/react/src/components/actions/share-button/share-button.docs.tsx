import { useState } from 'react';
import { ShareButton, emailShareTarget } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './share-button.fixture.json';

function ShareReport() {
  const [open, setOpen] = useState(true);
  return (
    <ShareButton
      url={fixture.data.url}
      title={fixture.data.title}
      messages={{ label: fixture.data.label }}
      targets={[emailShareTarget()]}
      open={open}
      onOpenChange={setOpen}
    />
  );
}

export default {
  id: 'share-button',
  title: 'ShareButton',
  summary: 'Shares a link with the system share sheet, or a copy-link panel where there is none.',
  sections: {
    purpose: {
      summary:
        'One button that shares a link the best way the device allows: the system share sheet on phones and supported browsers, otherwise a small panel with the link, Copy link and optional targets such as email. Needed by: none recorded; no Rook app uses the Web Share API yet.',
      useWhen: [
        'A page, document or item has a public or shareable link.',
        'People are likely to share from a phone, where the system share sheet reaches their apps.',
      ],
      avoidWhen: [
        'Only copying is needed: use CopyButton or CopyField.',
        'Sharing means inviting people with permissions: use an invite or permissions dialog.',
      ],
    },
    designPrinciples: {
      items: [
        'Never shares on its own: nothing happens until the person presses the button.',
        'Cancelling the system share sheet is a normal outcome, not an error.',
        'The fallback panel always offers the link itself, so sharing works even without a clipboard.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `<button>`; Button props pass through except `title`, which is the shared title (use `tooltip` for the button tooltip).',
        '`emailShareTarget(label?, icon?)` builds a `mailto:` target with the title as subject and the text and link as body.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`url`, `title` and `text` are passed to `navigator.share` unchanged. `navigator.canShare` is checked first when the browser has it.',
        'Target links must pass `isSafeUrl` (http, https, mailto); others are not rendered.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'mode',
          values: ['auto', 'panel'],
          description:
            '`auto` (default): the share sheet when the browser can share the data, else the panel. `panel`: always the panel, for desktop-first layouts that want Copy link.',
        },
        {
          prop: 'iconOnly',
          values: ['true', 'false'],
          description: 'Square icon button named by `messages.label`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Sharing',
          attribute: 'aria-busy, data-loading',
          description: 'While the system share sheet is open the button shows a spinner.',
        },
        {
          name: 'Panel open',
          attribute: 'aria-expanded="true", data-state="open"',
          description: 'The fallback panel is shown next to the button.',
        },
        {
          name: 'Copied',
          attribute: 'data-state="copied" on the copy button',
          description: 'Copy link confirms and announces the copy.',
        },
      ],
    },
    interaction: {
      items: [
        'Click: if the browser can share the data, the system share sheet opens and `onShare({ method: "native" })` runs after a share.',
        'Closing the share sheet calls `onShareCancel`; any other failure calls `onShareError` and opens the panel.',
        'Without a share sheet (or with `mode="panel"`) the click opens the panel; clicking again closes it.',
        'In the panel, focusing the link selects it; Copy link calls `onShare({ method: "copy" })`; a target opens in a new tab and calls `onShare({ method: "target", target })`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'Shares, or opens the panel.' },
        { keys: 'Tab', action: 'Moves through the link field, Copy link and the targets.' },
        { keys: 'Escape', action: 'Closes the panel and returns focus to the button.' },
      ],
    },
    accessibility: {
      role: 'button; the panel is a dialog',
      name: '`messages.label` ("Share"); the panel is named by `messages.panelLabel`.',
      items: [
        '`aria-haspopup="dialog"` and `aria-expanded` are set only when a click will open the panel, which is known after hydration.',
        'The link field has a visible label; the copy result is announced politely by CopyButton.',
        'Targets are a labelled list of links that open in a new tab with `rel="noopener noreferrer"`.',
      ],
    },
    responsive: {
      items: [
        'The panel is at most the viewport width minus 2 rem, so it fits at 390 px.',
        'On coarse pointers the button, Copy link and targets are at least 44 px tall.',
        'On phones the system share sheet is used whenever the browser offers it.',
      ],
    },
    theme: {
      items: [
        'Panel, field and targets use surface, border and text tokens and the Popover styling, in every theme.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Share sheet with panel fallback',
          stage: true,
          render: () => <ShareReport />,
        },
        {
          title: 'Always the panel, with email',
          render: () => (
            <ShareButton
              mode="panel"
              url="https://example.com/booking/team-call"
              title="Book a call"
              text="Pick a time that suits you."
              targets={[emailShareTarget()]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Share a canonical, public URL, not the current page’s internal address.',
        'Use `onShare` for analytics; it reports how the person shared.',
      ],
      dont: [
        'Do not call the share sheet on page load or after a timer; browsers block it and people dislike it.',
        'Do not show an error when the person closes the share sheet.',
      ],
    },
    related: {
      items: [
        { id: 'copy-button', reason: 'Copying a value without the share sheet.' },
        { id: 'copy-field', reason: 'A visible, read-only value with a copy button.' },
        { id: 'popover', reason: 'The fallback panel.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'copy-button', 'popover'],
    },
    testing: {
      unit: [
        'Does not share on render; a click calls `navigator.share` with url, title and text and reports `native`.',
        'A cancelled share sheet calls `onShareCancel`, not `onShareError`.',
        'Falls back to the panel when sharing fails or `canShare` rejects the data.',
        'The panel has a labelled link field, Copy link, a mailto target and no unsafe target; Escape returns focus to the button.',
        '`mode="panel"` never calls the share sheet; icon-only uses the label as its name.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

import { CalloutBlock, CalloutBlockExtension, RichTextEditor } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const EXTENSIONS = [CalloutBlockExtension];

export default {
  id: 'callout-block',
  title: 'CalloutBlock',
  summary:
    'A tinted note inside a document, as a React component and as a Tiptap node with a tone picker.',
  sections: {
    purpose: {
      summary:
        'Sets a note, tip or warning apart from the text around it in a document, help article or editor. Needed by: Rook Dossier (CalloutBlock node and CalloutNodeView in src/lib/tiptap/extensions.ts and src/components/document/nodeViews/CalloutNodeView.tsx).',
      useWhen: [
        'A paragraph or a few blocks of a document deserve emphasis: a tip, a caveat, a warning.',
        'Editors should be able to insert and restyle the note themselves (`CalloutBlockExtension`).',
      ],
      avoidWhen: [
        'The message is about the state of the app (saved, failed, offline): use Alert or InlineNotice, which have live-region semantics.',
        'A page-wide announcement: use CalloutBanner.',
      ],
    },
    designPrinciples: {
      items: [
        'Content, not a notification: no alert or status role, so screen readers read it in document order without interrupting.',
        'Tone keeps the Monarch status meaning (info, success, warning, danger) plus a neutral note; the icon repeats the tone so it is never colour alone.',
        'One schema for editing and reading: the node renders plain `<div data-type="callout" data-tone>` HTML that the read-only page styles with the same class.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`CalloutBlock` renders `.mn-callout-block[data-tone]` > `__toolbar` (optional) and `__row` > `__icon` + `__content`.',
        '`CalloutBlockExtension` is a Tiptap node named `callout` (group `block`, content `block+`). Commands: `setCallout({ tone })`, `unsetCallout()`, `setCalloutTone(tone)`.',
        'Import from `@rookcom/monarch-react/editor`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`CalloutBlockExtension.configure({ messages })` translates the tone picker (`toneLabel`, `tones`).',
        'Stored `data-tone` values outside `CALLOUT_TONES` are read as `neutral`.',
        'Migrating from Dossier: `variant` default/info/warn/good becomes `tone` neutral/info/warning/success; the class `callout warn` becomes `data-tone="warning"`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['neutral', 'info', 'success', 'warning', 'danger'],
          description: 'Tint, border and icon.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Tone', attribute: 'data-tone', description: 'One of the five tones.' },
        {
          name: 'Editable',
          description: 'In an editable editor the node view shows the tone picker above the text.',
        },
        {
          name: 'Read-only',
          description: 'No picker; the callout reads like the static component.',
        },
      ],
    },
    interaction: {
      items: [
        'In an editor, `setCallout` wraps the selected blocks; with nothing to wrap it inserts an empty callout.',
        'The tone picker changes the tone at once and is one undo step.',
        'Backspace at the very start of a callout lifts its content out again.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves into the tone picker (one Tab stop).' },
        { keys: 'Arrow keys', action: 'Choose the previous or next tone.' },
        {
          keys: 'Backspace (start of the callout)',
          action: 'Removes the callout, keeps the text.',
        },
      ],
    },
    accessibility: {
      role: 'none (content); the tone picker is a radiogroup',
      name: 'The picker is named by `messages.toneLabel` ("Callout style"); each tone by `messages.tones`.',
      items: [
        'The icon is decorative (`aria-hidden`); the tone is also conveyed by the text the author writes.',
        'The picker composes SegmentedControl, an APG radio group with a roving tabindex.',
      ],
    },
    responsive: {
      items: [
        'Full width of its column; long words wrap.',
        'Tone buttons grow to 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        '`--mn-color-{info,success,warning,danger}-subtle`, `-border` and `-strong`; `--mn-color-bg-subtle` for neutral.',
        'Forced colours draw a system border and icon.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Tones',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 8, width: '100%', maxWidth: 560 }}>
              <CalloutBlock>Drafts are visible only to you.</CalloutBlock>
              <CalloutBlock tone="info">The import keeps existing labels.</CalloutBlock>
              <CalloutBlock tone="success">The checklist is complete.</CalloutBlock>
              <CalloutBlock tone="warning">Archive the old project before you start.</CalloutBlock>
              <CalloutBlock tone="danger">Deleted pages cannot be restored.</CalloutBlock>
            </div>
          ),
        },
        {
          title: 'In an editor',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <RichTextEditor
                aria-label="Guide"
                extensions={EXTENSIONS}
                defaultValue='<p>Before you begin:</p><div data-type="callout" data-tone="info"><p>Sign in with an administrator account.</p></div>'
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep callouts short, one idea each.',
        'Pick the tone by meaning: danger for irreversible outcomes, warning for caveats.',
      ],
      dont: [
        'Do not use a callout to report something that just happened in the app; use a toast or Alert.',
        'Do not nest callouts.',
      ],
    },
    related: {
      items: [
        { id: 'alert', reason: 'Status messages about the app, with live-region roles.' },
        { id: 'block-editor', reason: 'Offers the callout in its slash menu.' },
        { id: 'toggle-block', reason: 'Collapsible section for longer asides.' },
      ],
    },
    dependencies: {
      packages: ['react', '@tiptap/core', '@tiptap/react'],
      components: ['segmented-control'],
    },
    testing: {
      unit: [
        'Tone attribute, decorative icon, no role, unknown tone falls back to neutral.',
        '`setCallout`, `setCalloutTone`, `unsetCallout` and HTML round-trip; unsafe stored tones rejected.',
        'Named radio-group tone picker changes the tone with arrow keys; hidden when read-only.',
        'Backspace at the start lifts the content out.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

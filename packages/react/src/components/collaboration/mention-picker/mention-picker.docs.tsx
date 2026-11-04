import { useState } from 'react';
import { Field, FieldDescription, FieldLabel } from '@rookcom/monarch-react';
import {
  MentionPicker,
  parseMentionTokens,
  useMentionTextarea,
  type MentionItem,
  type MentionSearch,
} from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const DIRECTORY: MentionItem[] = [
  { id: 'u_ana', label: 'Ana Lopez', description: 'Design' },
  { id: 'u_ben', label: 'Ben Okafor', description: 'Engineering' },
  { id: 'u_chloe', label: 'Chloé Martin', description: 'Support' },
  { id: 'u_dev', label: 'Dev Patel', description: 'Billing' },
  { id: 'u_eli', label: 'Eli Novak', description: 'No access to this project', disabled: true },
  { id: 'c_launch', label: 'launch', kind: 'channel', description: '12 members' },
  { id: 'c_design', label: 'design-review', kind: 'channel', description: '8 members' },
];

/** A fake directory search with a short delay, honouring the abort signal. */
const demoSearch: MentionSearch = (text, { trigger, signal }) =>
  new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      const q = text.toLowerCase();
      resolve(
        DIRECTORY.filter(
          (d) =>
            (trigger === '#' ? d.kind === 'channel' : true) && d.label.toLowerCase().includes(q),
        ).slice(0, 6),
      );
    }, 250);
    signal.addEventListener('abort', () => {
      clearTimeout(timer);
      reject(new DOMException('Aborted', 'AbortError'));
    });
  });

const textareaStyle = {
  width: '100%',
  minHeight: 96,
  padding: '8px 10px',
  border: '1px solid var(--mn-color-border-input)',
  borderRadius: 'var(--mn-radius-md)',
  background: 'var(--mn-color-bg-surface)',
  color: 'var(--mn-color-text-default)',
  font: 'inherit',
  resize: 'vertical' as const,
};

function CommentDemo() {
  const [value, setValue] = useState('Thanks @[Ana Lopez](u_ana), looping in ');
  const mention = useMentionTextarea({
    value,
    onChange: setValue,
    search: demoSearch,
    triggers: ['@', '#'],
  });
  const mentioned = parseMentionTokens(value).filter((s) => s.type === 'mention');
  return (
    <div style={{ width: '100%', maxWidth: 480 }}>
      <Field>
        <FieldLabel htmlFor="mention-demo">Comment</FieldLabel>
        <textarea id="mention-demo" style={textareaStyle} {...mention.textareaProps} />
        <FieldDescription>Type @ for people or # for channels.</FieldDescription>
      </Field>
      <MentionPicker {...mention.pickerProps} />
      <p style={{ margin: '8px 0 0', fontSize: 12, color: 'var(--mn-color-text-muted)' }}>
        Will notify:{' '}
        {mentioned.map((s) => (s.type === 'mention' ? s.label : '')).join(', ') || 'nobody'}
      </p>
    </div>
  );
}

export default {
  id: 'mention-picker',
  title: 'MentionPicker',
  summary:
    '@mention suggestions anchored to the caret, with async search, people and channel groups and listbox keys.',
  sections: {
    purpose: {
      summary:
        'Suggests people and channels while someone types `@` or `#` in a text field the product owns, and writes a stable mention token (`@[Ana Lopez](u_ana)`) when one is chosen, so the server can notify the right ids.',
      useWhen: [
        'Comments, replies and descriptions in a plain textarea (`useMentionTextarea`).',
        'A rich-text editor that reports its caret (`useMentionPicker`, which the wave 9 editor reuses).',
      ],
      avoidWhen: [
        'Picking an assignee or owner from a field: use AssigneePicker or EntityPicker.',
        'Choosing from a fixed list in a form: use Combobox.',
      ],
    },
    designPrinciples: {
      items: [
        'Focus never leaves the text field; the field points at the highlighted option, as in the APG combobox pattern.',
        'Searching is the product’s: Monarch debounces, aborts stale requests and shows loading, empty and error states.',
        'The token format is a contract: `formatMentionToken` and `parseMentionTokens` round-trip, and `serialize` lets a product keep its own format.',
        'Typing prose is never hijacked: the list only opens at a word start and closes on a two-word query with no matches.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useMentionTextarea({ value, onChange, search, triggers?, serialize?, onMention?, debounceMs?, onKeyDown? })` returns `textareaProps` (spread on the `<textarea>`, including `value` and `onChange`) and `pickerProps` (spread on `<MentionPicker>`).',
        '`useMentionPicker({ search, onSelect, debounceMs? })` is the editor-agnostic core: call `update(query, caretRect)` as the caret moves, and `handleKeyDown(event)` first in your key handler; spread `fieldProps` on the editable element.',
        '`search(text, { trigger, signal })` returns items or a promise of items; abort on `signal`.',
        'Helpers: `findMentionQuery(value, caret, triggers)`, `getCaretRect(field, index)`, `formatMentionToken(item, trigger)`, `parseMentionTokens(text, triggers)`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'triggers',
          values: ["['@']", "['@', '#']"],
          description:
            'Characters that open the list. `search` receives the trigger, so `#` can search channels only.',
        },
        {
          prop: 'MentionItem.kind',
          values: ['person', 'channel'],
          description:
            'People show an avatar, channels a `#` tile. Mixed results are split into labelled groups.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Searching',
          attribute: 'data-status="loading"',
          description: 'Spinner and “Searching…”.',
        },
        {
          name: 'Results',
          attribute: 'data-status="ready"',
          description: 'Listbox with the first enabled option highlighted.',
        },
        {
          name: 'No matches',
          description:
            '“No matches” for a one-word query; a two-word query with none closes the list.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: '“Couldn’t load suggestions.” with Retry.',
        },
        {
          name: 'Disabled option',
          attribute: 'data-disabled',
          description: 'Shown, skipped by arrow keys, cannot be chosen.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing the trigger at the start of the text, after a space or after `(` opens the list; `ana@example.com` does not.',
        'Each change searches after `debounceMs` (150 ms) and aborts the previous request.',
        'Choosing replaces the query with the token and a space, then puts the caret after it.',
        'The list opens below the caret and flips above it near the bottom of the viewport; it is kept inside the viewport horizontally.',
        'Blurring the field or pressing Escape closes the list.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Arrow Down / Arrow Up',
          action: 'Highlight the next or previous enabled suggestion (wraps).',
        },
        { keys: 'Enter / Tab', action: 'Insert the highlighted suggestion.' },
        {
          keys: 'Escape',
          action:
            'Close the list without leaving the field (and without closing a surrounding dialog).',
        },
        {
          keys: 'Home / End, Arrow Left / Right',
          action: 'Move the caret, as normal; moving it out of the query closes the list.',
        },
      ],
      notes: ['Keys reach your own `onKeyDown` whenever the list did not use them.'],
    },
    accessibility: {
      role: 'listbox with option children; groups for people and channels',
      name: '“Suggestions” (`messages.label`); groups are labelled “People” and “Channels”.',
      items: [
        'The field gets `aria-autocomplete="list"`, `aria-controls` while the listbox is shown and `aria-activedescendant` for the highlighted option, so screen readers follow the highlight while focus stays in the field.',
        'A polite status announces “Searching…”, “5 suggestions”, “No matches” or the error.',
        'The textarea keeps its native textbox role: ARIA does not allow `combobox` on `<textarea>`.',
        'Options use `aria-selected` for the highlight and `aria-disabled` for unavailable people.',
      ],
    },
    responsive: {
      items: [
        'The list is at most 18rem wide and never wider than the viewport minus 1rem; it scrolls past 22rem or half the viewport height.',
        'Long names and descriptions are truncated with an ellipsis.',
      ],
    },
    theme: {
      items: [
        'A raised surface (`bg-raised`, `border-subtle`, `shadow-lg`) like Monarch menus; the highlight is `bg-muted`.',
        'Forced colours outline the highlighted option.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Comment field with people and channels',
          render: () => <CommentDemo />,
          code: `const [value, setValue] = useState('');
const mention = useMentionTextarea({
  value,
  onChange: setValue,
  triggers: ['@', '#'],
  search: (text, { trigger, signal }) =>
    api.get('/mentions', { params: { q: text, kind: trigger === '#' ? 'channel' : 'all' }, signal })
      .then((r) => r.data),   // [{ id, label, kind, description, avatarSrc }]
});

<textarea aria-label="Comment" {...mention.textareaProps} />
<MentionPicker {...mention.pickerProps} />

// On send: who to notify
const ids = parseMentionTokens(value).flatMap((s) => (s.type === 'mention' ? [s.id] : []));`,
        },
        {
          title: 'With a rich-text editor',
          render: () => (
            <p style={{ margin: 0, fontSize: 13 }}>
              Editors call the core hook with their own caret position.
            </p>
          ),
          code: `const picker = useMentionPicker({
  search,
  onSelect: (item, query) => editor.replaceRange(query.start, query.end, mentionNode(item)),
});

editor.onCaretChange(({ text, caret, rect }) => picker.update(findMentionQuery(text, caret), rect));
editor.onKeyDown((e) => picker.handleKeyDown(e));

<Editable {...picker.fieldProps} />
<MentionPicker {...picker.pickerProps} />`,
        },
      ],
    },
    doDont: {
      do: [
        'Abort your request when `signal` fires.',
        'Return people the viewer may mention; mark the ones without access `disabled` with a reason in `description`.',
        'Resolve tokens to ids on the server too; never trust the label.',
      ],
      dont: [
        'Do not move focus into the list.',
        'Do not change the token format without updating every reader of stored text.',
        'Do not return hundreds of results; cap the search at about 8–10.',
      ],
    },
    related: {
      items: [
        { id: 'message-composer', reason: 'The chat composer that will host it (planned).' },
        { id: 'rich-text-editor', reason: 'Reuses `useMentionPicker` (planned, wave 9).' },
        { id: 'assignee-picker', reason: 'Choosing people for a field, not inline text.' },
        { id: 'combobox', reason: 'Choosing from a list in a form.' },
      ],
    },
    dependencies: { packages: ['react', 'react-dom'], components: ['avatar', 'spinner'] },
    testing: {
      unit: [
        'Opens a listbox for @ and wires `aria-autocomplete`, `aria-controls` and `aria-activedescendant`; announces the count.',
        'Arrow keys wrap and skip disabled options; Enter and Tab insert a token and leave the caret after it; clicks keep focus.',
        'Escape closes without reaching outer handlers; other keys pass to the product.',
        'People and channel groups; the # trigger.',
        'Aborts the previous search; loading, error with Retry, no matches, two-word close.',
        'Does not open inside an email address; closes on blur.',
        '`findMentionQuery`, `formatMentionToken` and `parseMentionTokens`.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/mention-picker',
        'Cross-browser axe: /components/mention-picker',
      ],
    },
  },
} satisfies ComponentDocs;

import { useRef, useState } from 'react';
import { Button, Textarea } from '@rookcom/monarch-react';
import {
  AiGhostText,
  AiInlineDiff,
  useAiCompletion,
  useAiRewrite,
  type UseAiCompletionResult,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './ai-ghost-text.fixture.json';

/** A stand-in for a model: continues a few known openings after a short wait. */
const CONTINUATIONS: [RegExp, string][] = [
  [/thanks$/i, ' for getting back to me so quickly.'],
  [/thank you$/i, ' for your patience while we looked into this.'],
  [/let me know$/i, ' if anything is unclear and I will follow up.'],
  [/best$/i, ' regards,'],
];

function demoComplete(text: string, cursor: number, signal: AbortSignal): Promise<string> {
  return new Promise((resolve) => {
    const timer = setTimeout(() => {
      const before = text.slice(0, cursor).trimEnd();
      resolve(CONTINUATIONS.find(([pattern]) => pattern.test(before))?.[1] ?? '');
    }, 250);
    signal.addEventListener('abort', () => clearTimeout(timer), { once: true });
  });
}

/**
 * The page opens on a suggestion that is already showing, as after a pause in
 * typing “Thanks”. The first edit, Tab, Escape or blur hands over to the live
 * completion.
 */
function ReplyDemo() {
  const live = useAiCompletion({ complete: demoComplete });
  const [seeded, setSeeded] = useState(true);
  const settle = () => setSeeded(false);
  const completion: UseAiCompletionResult = seeded
    ? {
        ...live,
        status: 'ready',
        suggestion: fixture.data.suggestion,
        text: fixture.data.text,
        cursor: fixture.data.text.length,
        dismiss: settle,
        update: (text, cursor) => {
          settle();
          live.update(text, cursor);
        },
        accept: () => {
          settle();
          const text = fixture.data.text + fixture.data.suggestion;
          return { inserted: fixture.data.suggestion, text, cursor: text.length };
        },
        acceptWord: () => {
          settle();
          return { inserted: ' for', text: 'Thanks for', cursor: 10 };
        },
      }
    : live;
  return (
    <AiGhostText completion={completion}>
      <Textarea
        aria-label={fixture.data.label}
        rows={4}
        defaultValue={fixture.data.text}
        placeholder={fixture.data.placeholder}
      />
    </AiGhostText>
  );
}

function ControlledDemo() {
  const [value, setValue] = useState('');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)' }}>
      <AiGhostText complete={demoComplete} debounceMs={150}>
        <Textarea
          aria-label="Message"
          rows={3}
          value={value}
          onChange={(event) => setValue(event.target.value)}
          placeholder="Type “Thank you” and pause"
        />
      </AiGhostText>
      <p style={{ margin: 0 }}>{value.length} characters</p>
    </div>
  );
}

function RewriteDemo() {
  const field = useRef<HTMLTextAreaElement>(null);
  const [value, setValue] = useState(
    'We are writing to let you know that the delivery will arrive a little bit later than planned.',
  );
  const rewrite = useAiRewrite({
    // A stand-in for a model call.
    rewrite: async ({ selection }) =>
      selection
        .replace('We are writing to let you know that the', 'The')
        .replace('a little bit later', 'later'),
  });
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)' }}>
      <Textarea
        ref={field}
        aria-label="Draft"
        rows={3}
        value={value}
        onChange={(event) => setValue(event.target.value)}
      />
      <div>
        <Button
          size="sm"
          loading={rewrite.status === 'pending'}
          onClick={() => {
            if (field.current) void rewrite.rewriteSelection(field.current, 'Make it shorter');
          }}
        >
          Shorten selection
        </Button>
      </div>
      {rewrite.proposal ? (
        <AiInlineDiff
          segments={rewrite.proposal.segments}
          onAccept={() => {
            const next = rewrite.accept();
            if (next !== null) setValue(next);
          }}
          onReject={() => rewrite.reject()}
        />
      ) : null}
    </div>
  );
}

export default {
  id: 'ai-ghost-text',
  title: 'AiGhostText',
  summary:
    'Inline completion for a textarea or input: the suggested continuation as dimmed text after the cursor, accepted with Tab.',
  sections: {
    purpose: {
      summary:
        'Shows what the model would write next, right where the person is typing, without taking anything over: the suggestion is dimmed text after the cursor that goes in only when they accept it. `useAiCompletion` holds the request logic (debounce, aborting stale requests, IME composition) and `useAiRewrite` turns “rewrite this selection” into a proposal the diff components can show.',
      useWhen: [
        'Replies, notes, descriptions and other free text where a likely continuation saves typing.',
        'You have a fast completion endpoint: suggestions that arrive after the person has moved on are thrown away.',
        'A selection should be rewritten on instruction and reviewed before it is applied (`useAiRewrite` with AiInlineDiff or AiEditProposal).',
      ],
      avoidWhen: [
        'Filling a whole form field from a document: use AiAutofillField.',
        'Asking the model about a selection in a floating prompt: use AiInlinePrompt.',
        'A rich-text or code editor with its own rendering: call `useAiCompletion` and draw the suggestion with the editor’s decorations.',
        'Passwords, one-time codes and other secrets: never send them to a model.',
      ],
    },
    designPrinciples: {
      items: [
        'The person’s text is never changed until they accept. The suggestion is drawn over the field, not written into it.',
        'Nothing moves: the overlay is positioned over the field, so a suggestion appearing or going causes no layout shift.',
        'Quiet by default: it waits for a pause, drops stale answers, stays away after Escape until the text changes, and never interrupts an input method.',
        'Tab only accepts when there is something to accept; otherwise it moves focus as usual.',
        'What is sent is the app’s choice: the component passes the field’s text to your function and nothing else.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`AiGhostText` wraps the field: put one `<textarea>` or text `<input>` (or a component that renders one, such as Textarea, Input or a Field around either) inside it. The field keeps its own props; it can be controlled or uncontrolled. The ref goes to the wrapping `<div>`.',
        '`complete(text, cursor, signal)` resolves with the text to insert at the cursor, or `""`, `null` or `undefined` for no suggestion. Pass `signal` to `fetch`: it aborts as soon as the person types again, moves the caret, leaves the field or the component unmounts.',
        'Suggestions are requested only with the caret at the end of the text (white space may follow) and no selection, so the dimmed text never covers what comes after it.',
        'Accepting writes through the field itself (an `input` event), so a controlled field receives it in `onChange` and form libraries see an ordinary edit. Where the browser supports it the insertion is on the undo stack.',
        '`useAiCompletion({ complete, debounceMs, minChars, enabled, onError })` returns `{ status, suggestion, text, cursor, update, dismiss, accept, acceptWord, setComposing }` and holds no DOM. Pass its result as `completion` to drive AiGhostText yourself, or use it alone in an editor. `aiGhostTextNextWord(suggestion, locale?)` is the pure word splitter.',
        '`useAiRewrite({ rewrite, target, onError })` returns `{ status, proposal, error, rewriteSelection, accept, reject }`. `rewriteSelection(field, instruction)` sends `{ text, selection, start, end, instruction }` (the whole text when nothing is selected); the proposal carries `segments` for AiInlineDiff, and `part` and `hunk` for AiEditProposal. `accept()` returns the whole text with the change applied; `applyAiRewrite(text, proposal)` applies it to newer text only if the range is unchanged.',
        'Privacy: every pause sends the text of the field to your completion function. Say so where people type, do not wrap fields that hold secrets, and set `enabled={false}` when the person has turned suggestions off.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'showAcceptHint',
          values: ['true', 'false'],
          description:
            'Shows the “Tab” key after the suggestion. It is also the button that accepts by touch, so keep it wherever people may not have a keyboard.',
        },
        {
          prop: 'enabled',
          values: ['true', 'false'],
          description: 'False turns suggestions off and clears the one showing.',
        },
      ],
      notes: ['The field decides the look: the overlay copies its font, padding and direction.'],
    },
    states: {
      items: [
        {
          name: 'Status',
          attribute: 'data-status',
          description:
            '`idle`, `waiting` (typing has not paused yet), `loading` (request in flight), `ready` (a suggestion is showing) or `error`.',
        },
        {
          name: 'Suggesting',
          attribute: 'data-suggesting',
          description:
            'A suggestion is drawn after the cursor and the field is described by the instruction.',
        },
        {
          name: 'Composing',
          description: 'An input method is composing: nothing is requested or shown until it ends.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing schedules a request after `debounceMs` (300 by default) once there are `minChars` (3) non-space characters before the cursor; each keystroke cancels the timer and aborts the request in flight.',
        'Typing the same characters as the suggestion keeps the rest of it showing without a new request.',
        'Accepting inserts the suggestion at the cursor and leaves focus in the field; accepting a word keeps the rest showing. `onAccept` reports what went in.',
        'The suggestion goes away when the caret moves, text is selected, the field loses focus, its value is replaced from outside, or it becomes read-only or disabled.',
        'After Escape the same text is not asked about again; the next edit starts over.',
        'The accept hint can be tapped or clicked; it does not take focus from the field.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Accepts the whole suggestion. With no suggestion it moves focus as usual.',
        },
        {
          keys: 'Right arrow (Left arrow in right-to-left text)',
          action: 'With the caret at the suggestion: accepts the whole suggestion.',
        },
        {
          keys: 'Control or Alt + the same arrow',
          action: 'Accepts the next word and keeps the rest showing.',
        },
        {
          keys: 'Escape',
          action:
            'Dismisses the suggestion. The key is consumed, so a surrounding dialog stays open; with no suggestion it passes through.',
        },
      ],
      notes: ['While an input method is composing, every key belongs to it.'],
    },
    accessibility: {
      role: 'none (a plain wrapper); the field keeps its own role and name',
      name: 'The field’s own label. The accept hint is a button named “Tab to accept the suggestion”.',
      items: [
        'The dimmed text is `aria-hidden`; a new suggestion is announced once through a polite `role="status"` region together with how to accept it. Typing through it or taking one word is not announced again.',
        'While a suggestion shows, the field’s `aria-describedby` gains an instruction with the keys; the field’s own descriptions are kept and the instruction is removed when the suggestion goes.',
        'Accepting and dismissing are announced (“Suggestion accepted.”, “Suggestion dismissed.”).',
        'The accept hint is not a tab stop (Tab itself accepts), and its accessible name starts with its visible text (WCAG 2.5.3). Its tap area is at least 24 by 24 CSS pixels.',
        'Suggested text uses the muted text colour and is never the only sign of a suggestion: the hint key follows it.',
        'Under reduced motion the suggestion does not fade in.',
      ],
    },
    responsive: {
      items: [
        'The wrapper takes the width of its container and the overlay follows the field’s measured box, including when the field resizes or scrolls.',
        'In a multi-line field the suggestion wraps like the text; in a single-line input it is clipped at the field’s edge.',
        'Touch: the hint key is the accept button; there is no hover-only behaviour.',
        'RTL: the overlay copies the field’s direction, and the forward arrow is the Left arrow.',
      ],
    },
    theme: {
      items: [
        'Suggestion: `--mn-color-text-muted`. Hint key: `--mn-color-bg-subtle` with `--mn-color-border-default`.',
        'Everything else (font, size, padding) comes from the field.',
        'Forced colours: the suggestion uses GrayText and the hint key ButtonText on ButtonFace.',
      ],
    },
    examples: {
      items: [
        { title: 'Reply with suggestions', stack: true, render: () => <ReplyDemo /> },
        { title: 'Controlled field', stack: true, render: () => <ControlledDemo /> },
        {
          title: 'Rewrite a selection and review it',
          stack: true,
          render: () => <RewriteDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Pass `signal` on to your request so stale completions stop costing anything.',
        'Keep completions short: a phrase or a sentence reads at a glance.',
        'Give people a setting to turn suggestions off and wire it to `enabled`.',
        'Label the field as usual; AiGhostText adds to its description and never replaces its name.',
      ],
      dont: [
        'Do not wrap password, payment or one-time-code fields.',
        'Do not insert the suggestion yourself on a timer or on blur: only the person accepts.',
        'Do not hide the accept hint on touch layouts: there is no Tab key to press.',
        'Do not use it to show validation or hints: it is for text the person may accept.',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-inline-diff',
          reason: 'Shows a `useAiRewrite` proposal as inline insertions and deletions.',
        },
        {
          id: 'ai-edit-proposal',
          reason: 'Shows a proposal as a card of hunks to accept or reject.',
        },
        {
          id: 'ai-inline-prompt',
          reason: 'Collects the instruction for a rewrite next to the selection.',
        },
        {
          id: 'ai-autofill-field',
          reason: 'A whole field value suggested by AI, with provenance.',
        },
        { id: 'ai-prompt-input', reason: 'The composer for a conversation with the model.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['ai-edit-proposal'] },
    testing: {
      unit: [
        '`aiGhostTextNextWord` takes leading space, one word and its punctuation.',
        '`useAiCompletion`: debounce with the latest text, cursor and an AbortSignal; the stale request is aborted and its late answer ignored; nothing below `minChars`, while disabled or while composing; typing through keeps the rest without a new request; whole and word accept; dismissal sticks until the text changes; failures reported once; timer and request cancelled on unmount.',
        'The suggestion renders after the typed text, `aria-hidden`, with a polite announcement and an `aria-describedby` instruction; the field’s value is untouched until accepted.',
        'Tab accepts into an uncontrolled field, keeps focus and passes through when nothing shows; a controlled field receives the accepted text through `onChange`; an outside value change drops the suggestion.',
        'The forward arrow accepts at the end (Left arrow in RTL); Control or Alt with it accepts one word without a second announcement.',
        'Escape dismisses, does not reach an outer key handler, and the same text is not asked about again.',
        'IME composition: the suggestion is cleared, nothing is requested, keys are left alone, and a request follows `compositionend`.',
        'Suggestions only at the end of the text; caret moves and blur drop them; typing aborts the request in flight; disabled, read-only and no `complete` do nothing.',
        'The hint button accepts by click without taking focus, is not a tab stop, and can be hidden; the field’s own description is kept; strings can be translated; a `completion` from the hook drives the component.',
        '`useAiRewrite`: sends the selection (or the whole text) with the instruction, returns `segments`, `hunk` and `part`, aborts for a newer call, on reject and on unmount, reports failures; `applyAiRewrite` leaves changed text alone.',
        'Ref and native props forwarding.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

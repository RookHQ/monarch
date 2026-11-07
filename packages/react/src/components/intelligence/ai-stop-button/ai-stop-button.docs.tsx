import { useEffect, useRef, useState } from 'react';
import { AiStopButton } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** A fake response that "streams" until stopped, then can continue. */
function StopDemo() {
  const [chars, setChars] = useState(0);
  const [stopped, setStopped] = useState(false);
  const timer = useRef<ReturnType<typeof setInterval> | undefined>(undefined);
  const text =
    'The quarterly summary covers revenue, hiring and the three open risks raised in review.';
  const done = chars >= text.length;

  useEffect(() => {
    if (stopped || done) return;
    timer.current = setInterval(() => setChars((c) => c + 1), 60);
    return () => clearInterval(timer.current);
  }, [stopped, done]);

  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', maxWidth: '28rem' }}>
      <p style={{ margin: 0, minHeight: '3lh' }}>{text.slice(0, chars)}</p>
      {done ? null : (
        <AiStopButton
          stopped={stopped}
          onStop={() => setStopped(true)}
          onContinue={() => setStopped(false)}
        />
      )}
    </div>
  );
}

export default {
  id: 'ai-stop-button',
  title: 'AiStopButton',
  summary: 'The visible Stop control for a streaming AI response, with a “Stopped.” state.',
  sections: {
    purpose: {
      summary:
        'Lets people interrupt an AI response that is streaming, and confirms that it stopped. A response that cannot be stopped wastes time and tokens and feels out of control; this control is the one visible way to stop, wherever the response appears.',
      useWhen: [
        'An AI response streams outside AiPromptInput (inline “Ask AI”, a generated summary, a side panel) and needs its own Stop.',
        'You want the “Stopped.” confirmation with a Continue action after the person stops.',
      ],
      avoidWhen: [
        'Inside AiPromptInput: its Send button already becomes Stop while busy.',
        'Cancelling a non-AI task (an upload, an export): use a Button labelled for that task.',
      ],
    },
    designPrinciples: {
      items: [
        'One shape for Stop everywhere: a filled square, never a close cross.',
        'Always reachable: a visible button here, plus Escape where the prompt or thread has focus.',
        'Stopping keeps what arrived; the control confirms it with “Stopped.” instead of silently vanishing.',
        'Focus never falls to the page: once stopped, it moves to Continue or the status text.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`ref` and other props go to the wrapper `<div>`; `buttonProps` go to the inner Stop `<button>`.',
        '`AiStopGlyph` is the square icon, for custom Stop controls that should match.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showLabel',
          values: ['true', 'false'],
          description:
            'Visible “Stop” text, or a square-only button with the text visually hidden.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: 'Button size. Both keep a 44px touch target.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'running',
          description: 'The Stop button is shown.',
          attribute: 'data-state="running"',
        },
        {
          name: 'stopped',
          description: '“Stopped.” is shown and announced, with Continue when `onContinue` is set.',
          attribute: 'data-state="stopped"',
        },
        {
          name: 'disabled',
          description: 'Stop cannot be pressed, e.g. while the abort is in flight.',
        },
      ],
    },
    interaction: {
      items: [
        'Pressing Stop calls `onStop`; set `stopped` once the request has been aborted.',
        'Continue calls `onContinue`; set `stopped` back to false when the response resumes.',
        'Escape is not handled here: call the same `onStop` from your thread’s or prompt’s Escape handler (AiPromptInput does this already).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to Stop, or to Continue once stopped.' },
        { keys: 'Enter / Space', action: 'Stops, or continues once stopped.' },
        {
          keys: 'Escape',
          action:
            'Not handled by this component; wire it to `onStop` where the conversation has focus.',
        },
      ],
    },
    accessibility: {
      role: 'button; the stopped message is a status region',
      name: '“Stop” (from `messages.stop`), visually hidden when `showLabel` is false.',
      items: [
        'The square glyph is decorative (`aria-hidden`); the text names the button.',
        '“Stopped.” sits in a `role="status"` region that exists before it is filled, so it is announced politely.',
        'If focus was on Stop when it stopped, focus moves to Continue, or to the status text when there is no Continue.',
        'Pass `buttonProps={{ "aria-keyshortcuts": "Escape" }}` when Escape also stops in your surface.',
      ],
    },
    responsive: {
      items: [
        'The hit area is at least 44 × 44px at every size; on coarse pointers the button itself grows to 44px.',
        'The stopped message and Continue wrap onto a second line in narrow spaces.',
      ],
    },
    theme: {
      items: [
        'A secondary button with neutral tokens, so it reads the same in light, dark and high-contrast themes.',
        'Forced colours: the glyph uses ButtonText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Stop a streaming response, then continue',
          stack: true,
          render: () => <StopDemo />,
        },
        {
          title: 'Square-only, as in a compact toolbar',
          render: () => <AiStopButton showLabel={false} onStop={() => {}} />,
        },
        {
          title: 'Stopped, with Continue',
          render: () => <AiStopButton stopped onContinue={() => {}} />,
        },
      ],
    },
    doDont: {
      do: [
        'Abort the request in `onStop` (for example `useAiConversation().stop`) and keep the partial text.',
        'Wire Escape to the same handler where the prompt or thread has focus.',
        'Remove the control once the response completes normally.',
      ],
      dont: [
        'Use a close (×) icon for Stop: people read it as dismiss.',
        'Hide Stop behind a menu or hover state while a response streams.',
        'Clear the partial response when someone stops it.',
      ],
    },
    related: {
      items: [
        { id: 'ai-prompt-input', reason: 'Its Send button turns into Stop while a response runs.' },
        {
          id: 'ai-conversation',
          reason: 'Provides `stop()` and the `streaming` / `stopped` status.',
        },
        {
          id: 'ai-thinking-indicator',
          reason: 'Shown alongside Stop before the first token arrives.',
        },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Stop calls `onStop`; the root has its class and `data-state`.',
        'The accessible name stays when the label is visually hidden.',
        'Keyboard: Enter stops and focus moves to Continue; Space continues.',
        'Without Continue, focus moves to the status text.',
        'Stopping from elsewhere does not steal focus.',
        'Custom messages and `buttonProps` reach the right elements.',
        'axe passes running and stopped; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

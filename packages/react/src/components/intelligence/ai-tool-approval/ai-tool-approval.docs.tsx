import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  AiMessageView,
  AiToolApproval,
  AiToolCall,
  mockTransport,
  textDeltas,
  useAiConversation,
  type AiToolCallPart,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const EMAIL: AiToolCallPart = {
  type: 'tool-call',
  id: 'send-1',
  name: 'send_email',
  input: {
    to: ['alex@example.test', 'kim@example.test', 'sam@example.test'],
    subject: 'Weekly update',
    body: 'Hi all, the weekly update is attached.',
  },
  state: 'pending-approval',
};

const DELETE: AiToolCallPart = {
  type: 'tool-call',
  id: 'delete-1',
  name: 'delete_records',
  input: { table: 'contacts', ids: [101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112] },
  state: 'pending-approval',
};

const describeEmail = (part: AiToolCallPart) => {
  const to = (part.input as { to?: string[] } | null)?.to ?? [];
  return `Send email to ${to.length} ${to.length === 1 ? 'person' : 'people'}`;
};

const transport = mockTransport({
  delayMs: 40,
  reply: [
    ...textDeltas('I drafted the weekly update. It needs your approval before I send it. '),
    { type: 'part', part: EMAIL },
  ],
  toolReply: (response) =>
    response.approved
      ? [
          {
            type: 'tool-update',
            id: response.toolCallId,
            patch: {
              state: 'succeeded',
              output: { delivered: 3 },
              endedAt: new Date(Date.now() + 900).toISOString(),
            },
          },
          ...textDeltas('Sent. All three people received the weekly update.'),
          { type: 'done' },
        ]
      : [
          { type: 'tool-update', id: response.toolCallId, patch: { state: 'denied' } },
          ...textDeltas(
            response.reason
              ? `Understood, I did not send it (${response.reason}).`
              : 'Understood, I did not send it.',
          ),
          { type: 'done' },
        ],
});

function ConversationDemo() {
  const { messages, status, busy, send, reset, approveTool, denyTool } = useAiConversation({
    transport,
  });
  const [allowed, setAllowed] = useState<string[]>([]);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)' }}>
      <div style={{ display: 'flex', gap: 'var(--mn-space-2)', flexWrap: 'wrap' }}>
        <Button
          size="sm"
          variant="primary"
          loading={busy}
          disabled={status === 'awaiting-approval'}
          onClick={() => send('Send the weekly update to the team.')}
        >
          Ask the assistant
        </Button>
        {messages.length ? (
          <Button size="sm" onClick={() => reset()}>
            Start over
          </Button>
        ) : null}
      </div>
      {messages.map((message) => (
        <AiMessageView
          key={message.id}
          message={message}
          showActions={false}
          renderPart={(part) =>
            part.type !== 'tool-call' ? undefined : part.state === 'pending-approval' &&
              !allowed.includes(part.name) ? (
              <AiToolApproval
                part={part}
                describe={describeEmail}
                onApprove={(input) => approveTool(part.id, input)}
                onDeny={(reason) => denyTool(part.id, reason)}
                onAlwaysAllow={(name) => setAllowed((list) => [...list, name])}
              />
            ) : (
              <AiToolCall part={part} describeTool={describeEmail} />
            )
          }
        />
      ))}
      <p style={{ margin: 0, color: 'var(--mn-color-text-muted)' }}>Status: {status}</p>
    </div>
  );
}

export default {
  id: 'ai-tool-approval',
  title: 'AiToolApproval',
  summary: 'The approve/deny gate for a tool call the assistant wants to run.',
  sections: {
    purpose: {
      summary:
        'Keeps a person in the loop before the assistant acts on their behalf: it says what will happen (“Send email to 3 people”, “Delete 12 records”), shows the arguments in readable form, states the risk and lets the person approve, edit the arguments, or deny with a reason.',
      useWhen: [
        'A `tool-call` part arrives with `state: "pending-approval"` and `useAiConversation` reports `awaiting-approval`.',
        'Any action that sends, spends, shares or deletes on the person’s behalf.',
      ],
      avoidWhen: [
        'Read-only tools (search, look-up): let them run and show AiToolCall.',
        'Actions the person started themselves outside the assistant: use ConfirmDialog.',
      ],
    },
    designPrinciples: {
      items: [
        'Say the consequence in plain words; the JSON is there to check, not to read first.',
        'Never make approving the easy accident: there is no default button, Enter in a field never approves, and for high and destructive risk focus starts on Deny.',
        'Destructive actions look different (danger tone, a consequence line) and cannot be always-allowed.',
        'Editing arguments is explicit: the approved input is only sent when the person changed it, and invalid JSON blocks approval with an inline error.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        'Wire it to the conversation: `onApprove={(input) => approveTool(part.id, input)}` and `onDeny={(reason) => denyTool(part.id, reason)}`. With a transport that has `respondToTool`, the reply continues on its own.',
        '`onApprove()` is called with no argument unless the person edited the arguments; `onDeny()` with no argument unless they typed a reason.',
        '`onAlwaysAllow(toolName)` runs just before `onApprove` when the checkbox is ticked. Storing the allow-list is the app’s job.',
        'After a decision the card shows “Approved”, “Denied” or “Cancelled” and ignores further clicks. A new `part.id` resets it.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'risk',
          values: ['low', 'medium', 'high', 'destructive'],
          description:
            'Sets the badge text and tone, the border colour and the focus target. `destructive` adds the consequence line, makes Approve a danger button and hides “Always allow”. Exposed as `data-risk`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pending',
          attribute: 'data-state="pending"',
          description: 'Waiting for a decision; all actions shown.',
        },
        {
          name: 'Editing arguments',
          attribute: 'aria-expanded on Edit arguments',
          description:
            'A labelled JSON editor with the current arguments appears above the actions.',
        },
        {
          name: 'Invalid JSON',
          attribute: 'aria-invalid on the editor',
          description: 'Approve is refused, the error is shown and focus returns to the editor.',
        },
        {
          name: 'Approved',
          attribute: 'data-state="approved"',
          description: 'Buttons removed, “Approved” shown, focus kept on the card.',
        },
        {
          name: 'Denied',
          attribute: 'data-state="denied"',
          description: 'Buttons removed, “Denied” shown.',
        },
        {
          name: 'Cancelled',
          attribute: 'data-state="cancelled"',
          description: 'The reply was stopped before anyone decided.',
        },
      ],
    },
    interaction: {
      items: [
        'Approve calls `onApprove` once; Deny calls `onDeny` once. The card then shows the outcome until the part changes.',
        'Edit arguments opens a JSON editor with the current arguments; Discard edits restores them.',
        'Add a reason opens an optional text field whose content is sent with Deny.',
        'Always allow this tool is offered only with `onAlwaysAllow` and never for destructive risk.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through Edit arguments, Add a reason, the checkbox, Deny and Approve.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button or toggles the checkbox.' },
        { keys: 'Enter (in a field)', action: 'Adds a new line; it never approves.' },
      ],
      notes: [
        'Focus is only moved when `focusOnMount` is set: to Deny for high and destructive risk, to Approve otherwise.',
        'Opening the JSON editor or the reason field moves focus into it.',
      ],
    },
    accessibility: {
      role: 'group',
      name: 'The title (`describe(part)` or the tool label), via aria-labelledby',
      items: [
        'Announced assertively once when it appears (“Approval needed: Send email to 3 people”) through a hidden `role="alert"` filled after mount.',
        'The consequence line is the group’s accessible description.',
        'Arguments are a `<dl>` of humanized keys and readable values (arrays joined with commas).',
        'The JSON editor is a labelled textarea in a Field; its error is linked with aria-describedby.',
        'Risk is a text badge, never colour alone.',
      ],
    },
    responsive: {
      items: [
        'Header, badge and actions wrap; argument rows stack label over value below 480px. No horizontal scroll at 390px.',
        'Buttons and the checkbox meet the 44px touch target on coarse pointers (from Button and Checkbox).',
      ],
    },
    theme: {
      items: [
        'Card on `--mn-color-bg-surface` with a thicker inline-start border: warning border by default, `--mn-color-ai-border` for low risk, danger border for destructive.',
        'Consequence line: `--mn-color-danger-subtle` / `--mn-color-danger-strong` (warning subtle for high).',
        'Forced colours: CanvasText borders and icons.',
      ],
    },
    examples: {
      items: [
        {
          title: 'In a conversation (approve continues the reply)',
          stack: true,
          render: () => <ConversationDemo />,
        },
        {
          title: 'Destructive',
          stack: true,
          render: () => (
            <AiToolApproval
              part={DELETE}
              risk="destructive"
              describe={() => 'Delete 12 records'}
              consequence="12 contacts will be deleted for everyone. This can’t be undone."
              argumentLabels={{ ids: 'Record ids' }}
              onApprove={() => {}}
              onDeny={() => {}}
            />
          ),
        },
        {
          title: 'Low risk, read-only arguments',
          stack: true,
          render: () => (
            <AiToolApproval
              part={{
                type: 'tool-call',
                id: 'cal-1',
                name: 'create_calendar_hold',
                input: { title: 'Design review', start: '2026-03-04 10:00', minutes: 30 },
                state: 'pending-approval',
              }}
              risk="low"
              editable={false}
              onAlwaysAllow={() => {}}
              onApprove={() => {}}
              onDeny={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write `describe` so the title states the effect with numbers (“Delete 12 records”).',
        'Use `destructive` for anything that cannot be undone, and pass a specific `consequence`.',
        'Persist always-allow choices per tool and per person, and let people revoke them.',
      ],
      dont: [
        'Do not auto-approve destructive tools, even with an allow-list.',
        'Do not put Approve first in focus order or focus it for risky actions.',
        'Do not hide the arguments: people must be able to check what will be sent.',
      ],
    },
    related: {
      items: [
        { id: 'ai-tool-call', reason: 'Shows the call once it is approved, running or done.' },
        { id: 'ai-conversation', reason: 'Provides `approveTool` and `denyTool`.' },
        { id: 'confirm-dialog', reason: 'Confirmation for actions the person started themselves.' },
        { id: 'ai-tool-result-card', reason: 'Preview of what the approved action produced.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'checkbox', 'form-field', 'text-field', 'ai-tool-call'],
    },
    testing: {
      unit: [
        'Readable arguments, risk badge, group name and assertive announcement.',
        'Approve without and with edited arguments; invalid JSON blocks approval and refocuses the editor.',
        'Deny with and without a reason; state resets for a new call id.',
        'Always allow runs before approve and is hidden for destructive risk.',
        'Destructive: danger button, consequence as description, focus on Deny; focusOnMount to Approve for low risk; no focus taken by default.',
        'Focus stays on the card after deciding; outcome for non-pending parts.',
        'End to end with useAiConversation and mockTransport: approve continues the reply to idle.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

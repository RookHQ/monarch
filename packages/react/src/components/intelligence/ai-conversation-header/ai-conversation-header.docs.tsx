import { useEffect, useState } from 'react';
import { AiConversationHeader } from '@rookcom/monarch-react/ai';
import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const AGENT = { name: 'Assistant', role: 'Workspace assistant' };

function RenameDemo() {
  const [title, setTitle] = useState('Quarterly plan review');
  return (
    <div style={{ width: '100%', maxWidth: '40rem' }}>
      <AiConversationHeader
        title={title}
        onRename={setTitle}
        agent={AGENT}
        model="Balanced model"
        actions={
          <>
            <Button size="sm">Share</Button>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button size="sm" variant="ghost">
                  More
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent>
                <DropdownMenuItem>Export</DropdownMenuItem>
                <DropdownMenuItem tone="danger">Delete conversation</DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </>
        }
      />
    </div>
  );
}

function NamingDemo() {
  const [title, setTitle] = useState('');
  const [naming, setNaming] = useState(true);
  useEffect(() => {
    if (!naming) return;
    const timer = window.setTimeout(() => {
      setTitle('Launch checklist for week 9');
      setNaming(false);
    }, 1500);
    return () => window.clearTimeout(timer);
  }, [naming]);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '40rem' }}>
      <AiConversationHeader title={title} naming={naming} onRename={setTitle} agent={AGENT} />
      <Button
        size="sm"
        style={{ justifySelf: 'start' }}
        onClick={() => {
          setTitle('');
          setNaming(true);
        }}
      >
        Name again
      </Button>
    </div>
  );
}

export default {
  id: 'ai-conversation-header',
  title: 'AiConversationHeader',
  summary: 'Conversation top bar: auto-named title with inline rename, agent, model and actions.',
  sections: {
    purpose: {
      summary:
        'Heads a conversation with its title, which is often generated from the first prompt and can be renamed in place, plus the assistant and model in use and actions such as Share.',
      useWhen: [
        'A full chat page or a conversation opened from a history list.',
        'People need to rename, share or manage the current conversation.',
      ],
      avoidWhen: [
        'A small docked assistant: AiAssistantPanel has its own header.',
        'Page titles outside a conversation: use PageHeader.',
      ],
    },
    designPrinciples: {
      items: [
        'The title is the person’s: they can always rename what the assistant named.',
        'Clear AI identity: the agent is shown with the Bot mark, never a sparkle.',
        'Calm while naming: a placeholder that shimmers, static under reduced motion.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`onRename(title)` gets the trimmed title and is skipped when it is empty or unchanged. A returned promise shows a pending state; a rejection keeps the editor open with an error.',
        '`model` and `actions` are slots: pass text, an AiModelPicker, buttons or a DropdownMenu.',
        '`AI_CONVERSATION_HEADER_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: { items: [], notes: ['There are no visual variants; omit slots to simplify it.'] },
    states: {
      items: [
        { name: 'Untitled', description: '"New conversation" in muted text.' },
        {
          name: 'Naming',
          attribute: 'data-naming',
          description:
            '"Naming conversation…" placeholder; the heading is aria-busy and Rename is hidden.',
        },
        { name: 'Editing', attribute: 'data-editing', description: 'The title is a text field.' },
        {
          name: 'Pending',
          description: 'The field is read-only and aria-busy while a rename saves.',
        },
        { name: 'Rename failed', description: 'The field is invalid with an assertive error.' },
      ],
    },
    interaction: {
      items: [
        'Rename turns the title into a field with the text selected.',
        'Enter saves and Escape cancels; both return focus to Rename.',
        'Moving focus away saves the change without moving focus back.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space on Rename', action: 'Starts editing the title.' },
        { keys: 'Enter', action: 'Saves the new title.' },
        { keys: 'Escape', action: 'Cancels the rename (does not bubble).' },
        { keys: 'Tab', action: 'Leaves the field and saves.' },
      ],
    },
    accessibility: {
      role: 'none on the root; the title is a heading',
      name: 'Heading text is the title; the field is named "Conversation title"',
      items: [
        'Rename is an icon button named "Rename conversation".',
        'The agent name is prefixed with a visually hidden "AI assistant:" and the model with "Model:".',
        'Actions sit in a group named "Conversation actions".',
        'A failed rename is announced with role="alert" and marks the field invalid.',
      ],
    },
    responsive: {
      items: [
        'The title truncates with an ellipsis and the meta line wraps, so 390px has no horizontal scroll.',
        'On coarse pointers Rename and the field are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Bar: `--mn-color-bg-surface` with a `--mn-color-border-subtle` bottom border.',
        'Agent: `--mn-color-ai-strong` on an `--mn-color-ai-subtle` mark.',
        'The naming shimmer uses `--mn-duration-shimmer`; it is removed under reduced motion and forced colours.',
      ],
    },
    examples: {
      items: [
        { title: 'Rename, agent, model and actions', stack: true, render: () => <RenameDemo /> },
        { title: 'Naming a new conversation', stack: true, render: () => <NamingDemo /> },
      ],
    },
    doDont: {
      do: [
        'Generate a short title after the first exchange and let people rename it.',
        'Put destructive actions such as Delete in the more menu, with confirmation.',
      ],
      dont: [
        'Do not overwrite a title the person renamed with a generated one.',
        'Do not crowd the bar with more than two visible actions.',
      ],
    },
    related: {
      items: [
        { id: 'ai-conversation-list', reason: 'The history the conversation was opened from.' },
        { id: 'ai-model-picker', reason: 'Can fill the model slot.' },
        { id: 'dropdown-menu', reason: 'For the more menu in the actions slot.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Title heading, agent and model with hidden prefixes, actions group; props and ref forwarded.',
        'Untitled and naming placeholder (aria-busy).',
        'Rename with Enter returns focus; Escape cancels; empty or unchanged skipped; blur saves.',
        'Failed rename keeps the editor open with an alert.',
        'Heading levels and messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

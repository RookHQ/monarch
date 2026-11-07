'use client';

import { forwardRef, useId, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { RetryGlyph } from '../../../internal/icons.js';
import { Button } from '../../../components/actions/button/index.js';
import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from '../../../components/forms/form-field/index.js';
import { Input, Textarea } from '../../../components/forms/text-field/index.js';
import { Checkbox } from '../../../components/forms/checkbox/index.js';
import type {
  AiAgentRef,
  UseAiConversationResult,
} from '../../../components/intelligence/ai-conversation/index.js';
import { AiAgentAvatar } from '../../../components/intelligence/ai-agent-avatar/index.js';
import {
  AiKnowledgeScope,
  type AiKnowledgeSource,
} from '../../../components/intelligence/ai-knowledge-scope/index.js';
import { AiThread } from '../../../components/intelligence/ai-thread/index.js';
import { AiPromptInput } from '../../../components/intelligence/ai-prompt-input/index.js';

/** Every string AiAgentBuilder shows or announces. English defaults. */
export interface AiAgentBuilderMessages {
  /** Accessible name of the whole builder. */
  label: string;
  persona: string;
  name: string;
  nameRequired: string;
  role: string;
  roleDescription: string;
  instructions: string;
  instructionsDescription: string;
  /** Character counter under the instructions. */
  characters: (count: number, max: number) => string;
  tools: string;
  toolsDescription: string;
  noTools: string;
  knowledge: string;
  knowledgeDescription: string;
  test: string;
  testDescription: string;
  /** Restarts the test conversation. */
  restart: string;
  testInput: string;
  testEmpty: string;
  /** Fallback name in the preview while the name is empty. */
  untitled: string;
  save: string;
  saved: string;
  dirty: string;
  saveError: string;
}

const DEFAULT_MESSAGES: AiAgentBuilderMessages = {
  label: 'Agent builder',
  persona: 'Persona',
  name: 'Name',
  nameRequired: 'Give the agent a name.',
  role: 'Role',
  roleDescription: 'One line shown under the name, e.g. “Support assistant”.',
  instructions: 'Instructions',
  instructionsDescription: 'How the agent should behave, what to focus on and what to avoid.',
  characters: (count, max) => `${count.toLocaleString()} / ${max.toLocaleString()} characters`,
  tools: 'Tools',
  toolsDescription:
    'Actions the agent may take. It asks before running anything that changes data.',
  noTools: 'No tools available.',
  knowledge: 'Knowledge',
  knowledgeDescription: 'Sources the agent can read to ground its answers.',
  test: 'Test',
  testDescription: 'Try the agent with the current settings. Nothing here is saved.',
  restart: 'Restart test',
  testInput: 'Message the agent',
  testEmpty: 'Send a message to try the agent.',
  untitled: 'New agent',
  save: 'Save agent',
  saved: 'Agent saved.',
  dirty: 'You have unsaved changes.',
  saveError: 'Couldn’t save.',
};

/** A tool the agent can be allowed to use. */
export interface AiAgentBuilderTool {
  id: string;
  name: string;
  description?: ReactNode | undefined;
}

/** Everything the builder edits. */
export interface AiAgentDefinition {
  name: string;
  role: string;
  /** Image URL for the avatar. */
  avatar?: string | undefined;
  instructions: string;
  /** Allowed tool ids. */
  tools: readonly string[];
  /** Selected knowledge source ids (see AiKnowledgeScope). */
  knowledge: readonly string[];
}

export const EMPTY_AI_AGENT_DEFINITION: AiAgentDefinition = {
  name: '',
  role: '',
  instructions: '',
  tools: [],
  knowledge: [],
};

export interface AiAgentBuilderProps extends Omit<
  HTMLAttributes<HTMLElement>,
  'onChange' | 'defaultValue'
> {
  /** The definition (controlled). */
  value?: AiAgentDefinition | undefined;
  /** The starting definition when uncontrolled. Default empty. */
  defaultValue?: AiAgentDefinition | undefined;
  /** Called on every edit. */
  onChange?: ((definition: AiAgentDefinition) => void) | undefined;
  /** Tools to allow, as a checkbox list. */
  tools?: readonly AiAgentBuilderTool[] | undefined;
  /** Knowledge sources, as an AiKnowledgeScope tree. Leave out to hide the section. */
  knowledgeSources?: readonly AiKnowledgeSource[] | undefined;
  /**
   * The test chat: a `useAiConversation` result. Each test message is sent
   * with the current definition as `body.agent`, so the transport can run
   * the unsaved agent. Leave out to hide the test pane.
   */
  conversation?: UseAiConversationResult | undefined;
  /** Request field the definition is sent under. Default `agent`. */
  definitionField?: string | undefined;
  /**
   * Saves the definition. Shown as a Save button when given; return a
   * promise to show progress, reject it to show the error.
   */
  onSave?: ((definition: AiAgentDefinition) => void | Promise<void>) | undefined;
  /** Whether the definition differs from what is saved. Shows `messages.dirty`. */
  dirty?: boolean | undefined;
  /** Next to the avatar preview, e.g. an upload button that sets `avatar`. */
  avatarField?: ReactNode | undefined;
  /** Longest allowed instructions. Default 8000. */
  maxInstructionsLength?: number | undefined;
  /** Heading level of the section titles. Default `h2`. */
  headingLevel?: 'h2' | 'h3' | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiAgentBuilderMessages> | undefined;
}

function errorText(error: unknown): string | null {
  return error instanceof Error && error.message ? error.message : null;
}

/**
 * A template for building an AI agent: persona (name, avatar, role),
 * instructions, allowed tools and knowledge sources, with a side-by-side
 * test chat that runs the current, unsaved definition.
 */
export const AiAgentBuilder = forwardRef<HTMLElement, AiAgentBuilderProps>(function AiAgentBuilder(
  {
    value: valueProp,
    defaultValue = EMPTY_AI_AGENT_DEFINITION,
    onChange,
    tools = [],
    knowledgeSources,
    conversation,
    definitionField = 'agent',
    onSave,
    dirty = false,
    avatarField,
    maxInstructionsLength = 8000,
    headingLevel: H = 'h2',
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const auto = useId().replace(/:/g, '');
  const base = `mn-ai-agent-builder-${auto}`;
  const Sub = H === 'h2' ? 'h3' : 'h4';

  const [inner, setInner] = useState<AiAgentDefinition>(defaultValue);
  const value = valueProp ?? inner;
  const update = (patch: Partial<AiAgentDefinition>) => {
    const next = { ...value, ...patch };
    if (valueProp === undefined) setInner(next);
    setStatus('idle');
    onChange?.(next);
  };

  const [status, setStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');
  const [error, setError] = useState<string | null>(null);
  const [triedSave, setTriedSave] = useState(false);
  const nameMissing = !value.name.trim();

  const save = async () => {
    if (!onSave) return;
    setTriedSave(true);
    if (nameMissing) {
      document.getElementById(`${base}-name`)?.focus();
      return;
    }
    setStatus('saving');
    setError(null);
    try {
      await onSave(value);
      setStatus('saved');
    } catch (err) {
      setError(errorText(err));
      setStatus('error');
    }
  };

  const agent: AiAgentRef = {
    name: value.name.trim() || m.untitled,
    ...(value.role ? { role: value.role } : {}),
    ...(value.avatar ? { avatar: value.avatar } : {}),
  };

  const toggleTool = (id: string, on: boolean) =>
    update({
      tools: on
        ? [...value.tools.filter((t) => t !== id), id]
        : value.tools.filter((t) => t !== id),
    });

  const sendTest = (text: string) =>
    conversation?.send(text, { body: { [definitionField]: value } });

  const statusText =
    status === 'error'
      ? [m.saveError, error].filter(Boolean).join(' ')
      : status === 'saved'
        ? m.saved
        : dirty
          ? m.dirty
          : '';

  return (
    <section
      ref={ref}
      aria-label={m.label}
      {...rest}
      className={cx('mn-ai-agent-builder', className)}
      data-testing={conversation ? '' : undefined}
      data-status={status === 'idle' ? undefined : status}
    >
      <div className="mn-ai-agent-builder__form">
        <section className="mn-ai-agent-builder__section" aria-labelledby={`${base}-persona`}>
          <H id={`${base}-persona`} className="mn-ai-agent-builder__title">
            {m.persona}
          </H>
          <div className="mn-ai-agent-builder__persona">
            <div className="mn-ai-agent-builder__avatar">
              <AiAgentAvatar agent={agent} size="xl" decorative />
              {avatarField}
            </div>
            <div className="mn-ai-agent-builder__fields">
              <Field id={`${base}-name`} required invalid={triedSave && nameMissing}>
                <FieldLabel>{m.name}</FieldLabel>
                <Input
                  value={value.name}
                  autoComplete="off"
                  onChange={(e) => update({ name: e.target.value })}
                />
                {triedSave && nameMissing ? <FieldError>{m.nameRequired}</FieldError> : null}
              </Field>
              <Field>
                <FieldLabel>{m.role}</FieldLabel>
                <FieldDescription>{m.roleDescription}</FieldDescription>
                <Input
                  value={value.role}
                  autoComplete="off"
                  onChange={(e) => update({ role: e.target.value })}
                />
              </Field>
            </div>
          </div>
        </section>

        <section className="mn-ai-agent-builder__section" aria-labelledby={`${base}-instr`}>
          <Field>
            <H id={`${base}-instr`} className="mn-ai-agent-builder__title">
              <FieldLabel>{m.instructions}</FieldLabel>
            </H>
            <FieldDescription>{m.instructionsDescription}</FieldDescription>
            <Textarea
              rows={8}
              maxLength={maxInstructionsLength}
              value={value.instructions}
              onChange={(e) => update({ instructions: e.target.value })}
              className="mn-ai-agent-builder__instructions"
            />
            <p className="mn-ai-agent-builder__count" aria-hidden>
              {m.characters(value.instructions.length, maxInstructionsLength)}
            </p>
          </Field>
        </section>

        <section className="mn-ai-agent-builder__section" aria-labelledby={`${base}-tools`}>
          <fieldset
            className="mn-ai-agent-builder__fieldset"
            aria-describedby={`${base}-tools-desc`}
          >
            <legend className="mn-ai-agent-builder__legend">
              <H id={`${base}-tools`} className="mn-ai-agent-builder__title">
                {m.tools}
              </H>
            </legend>
            <p id={`${base}-tools-desc`} className="mn-ai-agent-builder__description">
              {m.toolsDescription}
            </p>
            {tools.length ? (
              <ul className="mn-ai-agent-builder__tools">
                {tools.map((t) => (
                  <li key={t.id}>
                    <Checkbox
                      label={t.name}
                      description={t.description}
                      checked={value.tools.includes(t.id)}
                      onCheckedChange={(on) => toggleTool(t.id, on === true)}
                    />
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mn-ai-agent-builder__description">{m.noTools}</p>
            )}
          </fieldset>
        </section>

        {knowledgeSources ? (
          <section className="mn-ai-agent-builder__section" aria-labelledby={`${base}-know`}>
            <H id={`${base}-know`} className="mn-ai-agent-builder__title">
              {m.knowledge}
            </H>
            <p className="mn-ai-agent-builder__description">{m.knowledgeDescription}</p>
            <AiKnowledgeScope
              sources={knowledgeSources}
              value={value.knowledge}
              onChange={(knowledge) => update({ knowledge })}
              messages={{ label: m.knowledge }}
            />
          </section>
        ) : null}

        {onSave ? (
          <div className="mn-ai-agent-builder__footer">
            <p className="mn-ai-agent-builder__status" role="status" aria-live="polite">
              {statusText}
            </p>
            <Button variant="primary" loading={status === 'saving'} onClick={() => void save()}>
              {m.save}
            </Button>
          </div>
        ) : null}
      </div>

      {conversation ? (
        <section className="mn-ai-agent-builder__test" aria-labelledby={`${base}-test`}>
          <div className="mn-ai-agent-builder__test-head">
            <AiAgentAvatar
              agent={agent}
              size="sm"
              decorative
              state={conversation.busy ? 'working' : 'idle'}
            />
            <div className="mn-ai-agent-builder__test-heading">
              <Sub id={`${base}-test`} className="mn-ai-agent-builder__title">
                {m.test}
              </Sub>
              <p className="mn-ai-agent-builder__description">{m.testDescription}</p>
            </div>
            <Button
              size="sm"
              variant="ghost"
              leadingIcon={<RetryGlyph />}
              disabled={!conversation.messages.length}
              onClick={() => {
                conversation.stop();
                conversation.reset();
              }}
            >
              {m.restart}
            </Button>
          </div>
          <AiThread
            className="mn-ai-agent-builder__thread"
            messages={conversation.messages}
            status={conversation.status}
            error={conversation.error}
            onStop={conversation.stop}
            daySeparators={false}
            empty={<p className="mn-ai-agent-builder__description">{m.testEmpty}</p>}
            messageProps={{
              agent,
              onRegenerate: (msg) => conversation.regenerate(msg.id),
              onBranch: (i, msg) => conversation.branchTo(msg.id, i),
              onRetry: (msg) => conversation.regenerate(msg.id),
            }}
          />
          <div className="mn-ai-agent-builder__composer">
            <AiPromptInput
              label={m.testInput}
              status={conversation.status}
              onStop={conversation.stop}
              onSubmit={(text) => sendTest(text)}
              showAttachButton={false}
            />
          </div>
        </section>
      ) : null}
    </section>
  );
});

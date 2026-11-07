'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { MenuGlyph } from '../../../internal/icons.js';
import { IconButton } from '../../../components/actions/button/index.js';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from '../../../components/overlays/sheet/index.js';
import type {
  AiMessage,
  UseAiConversationResult,
} from '../../../components/intelligence/ai-conversation/index.js';
import {
  AiConversationList,
  type AiConversationListProps,
  type AiConversationSummary,
} from '../../../components/intelligence/ai-conversation-list/index.js';
import {
  AiConversationHeader,
  type AiConversationHeaderProps,
} from '../../../components/intelligence/ai-conversation-header/index.js';
import { AiThread, type AiThreadProps } from '../../../components/intelligence/ai-thread/index.js';
import type { AiMessageViewProps } from '../../../components/intelligence/ai-message/index.js';
import {
  AiPromptInput,
  type AiPromptInputProps,
  type AiPromptSubmitDetails,
} from '../../../components/intelligence/ai-prompt-input/index.js';
import {
  AiArtifactCanvas,
  type AiArtifactCanvasProps,
  type AiCanvasArtifact,
} from '../../../components/intelligence/ai-artifact-canvas/index.js';

/** Every string AiChatPage shows or announces. English defaults. */
export interface AiChatPageMessages {
  /** Accessible name of the history navigation and title of its sheet on phones. */
  history: string;
  /** The button that opens the history sheet on phones. */
  openHistory: string;
  /** Accessible name of the prompt input. */
  inputLabel: string;
  /** Accessible name of the main chat area. */
  chat: string;
}

const DEFAULT_MESSAGES: AiChatPageMessages = {
  history: 'Conversation history',
  openHistory: 'Show conversation history',
  inputLabel: 'Message the assistant',
  chat: 'Chat',
};

export interface AiChatPageProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** The `useAiConversation` result for the open conversation. */
  conversation: UseAiConversationResult;
  /** The history in the rail. */
  conversations: readonly AiConversationSummary[];
  /** The open conversation's id, marked current in the rail. */
  activeId?: string | undefined;
  /** Opens a conversation from the rail (load it and `conversation.reset(messages)`). */
  onSelectConversation: (id: string) => void;
  /** Shows "New conversation" at the top of the rail. */
  onNewConversation?: (() => void) | undefined;
  /** Extra AiConversationList props: `onRename`, `onDelete`, `onPin`, `onSearch`, `messages`… */
  historyProps?:
    | Partial<Omit<AiConversationListProps, 'conversations' | 'activeId' | 'onSelect' | 'onNew'>>
    | undefined;
  /** The open conversation's title. When given, an AiConversationHeader is shown. */
  title?: string | undefined;
  /** Extra AiConversationHeader props: `onRename`, `model`, `actions`, `naming`… */
  headerProps?: Partial<Omit<AiConversationHeaderProps, 'title'>> | undefined;
  /** Shown while the conversation is empty, e.g. an AiWelcome with starters. */
  empty?: ReactNode | undefined;
  /** Controls in the prompt bar, e.g. a model or mode picker. */
  toolbar?: ReactNode | undefined;
  /** Under the prompt input, e.g. an AiDisclaimer. */
  footer?: ReactNode | undefined;
  /** Replaces sending: called with the prompt instead of `conversation.send`. */
  onSend?: ((text: string, details: AiPromptSubmitDetails) => unknown) | undefined;
  /** Props for each AiMessageView, merged over the page's wiring (regenerate, feedback, branches, retry). */
  messageProps?:
    | Partial<Omit<AiMessageViewProps, 'message'>>
    | ((message: AiMessage) => Partial<Omit<AiMessageViewProps, 'message'>>)
    | undefined;
  /** Extra AiThread props. */
  threadProps?:
    Partial<Omit<AiThreadProps, 'messages' | 'status' | 'error' | 'onStop'>> | undefined;
  /** Extra AiPromptInput props, e.g. `onAttach`, `triggers`, `renderTriggerMenu`. */
  promptProps?:
    | Partial<
        Omit<AiPromptInputProps, 'onSubmit' | 'status' | 'onStop' | 'label' | 'toolbar' | 'footer'>
      >
    | undefined;
  /** The artifact shown in the canvas pane. Leave out for no canvas. */
  artifact?: AiCanvasArtifact | null | undefined;
  /** Whether the canvas pane is open. Default: open whenever `artifact` is set. */
  artifactOpen?: boolean | undefined;
  /** Called by the canvas close button and Escape. */
  onArtifactOpenChange?: ((open: boolean) => void) | undefined;
  /** Extra AiArtifactCanvas props: versions, `onSave`, `onCopy`, `renderPreview`… */
  artifactProps?:
    Partial<Omit<AiArtifactCanvasProps, 'artifact' | 'open' | 'onOpenChange'>> | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiChatPageMessages> | undefined;
}

const COMPACT_QUERY = '(max-width: 767.98px)';

/** True below 768px, read after mount (false on the server). */
function useCompact(): boolean {
  const [compact, setCompact] = useState(false);
  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return;
    const mq = window.matchMedia(COMPACT_QUERY);
    const update = () => setCompact(mq.matches);
    update();
    mq.addEventListener?.('change', update);
    return () => mq.removeEventListener?.('change', update);
  }, []);
  return compact;
}

/**
 * A full-page chat template: a history rail (AiConversationList), the
 * conversation (header, AiThread, AiPromptInput) and an optional artifact
 * canvas pane, all wired to one `useAiConversation` result. Below 768px it is
 * a single column with the history in a sheet; the canvas becomes a
 * full-screen sheet below 640px.
 */
export const AiChatPage = forwardRef<HTMLDivElement, AiChatPageProps>(function AiChatPage(
  {
    conversation,
    conversations,
    activeId,
    onSelectConversation,
    onNewConversation,
    historyProps,
    title,
    headerProps,
    empty,
    toolbar,
    footer,
    onSend,
    messageProps,
    threadProps,
    promptProps,
    artifact,
    artifactOpen,
    onArtifactOpenChange,
    artifactProps,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const auto = useId().replace(/:/g, '');
  const chatId = `mn-ai-chat-page-${auto}-chat`;
  const compact = useCompact();
  const [historyOpen, setHistoryOpen] = useState(false);
  const inputRef = useRef<HTMLTextAreaElement | null>(null);

  // Leaving the phone layout closes the sheet.
  useEffect(() => {
    if (!compact) setHistoryOpen(false);
  }, [compact]);

  const [canvasClosed, setCanvasClosed] = useState(false);
  const artifactKey = artifact ? `${artifact.id}:${artifact.version}` : null;
  useEffect(() => setCanvasClosed(false), [artifactKey]);
  const canvasOpen = Boolean(artifact) && (artifactOpen ?? !canvasClosed);
  const setCanvasOpen = (next: boolean) => {
    if (artifactOpen === undefined) setCanvasClosed(!next);
    onArtifactOpenChange?.(next);
  };

  const select = (id: string) => {
    setHistoryOpen(false);
    onSelectConversation(id);
  };
  const startNew = onNewConversation
    ? () => {
        setHistoryOpen(false);
        onNewConversation();
        inputRef.current?.focus();
      }
    : undefined;

  const list = (
    <AiConversationList
      {...historyProps}
      conversations={conversations}
      activeId={activeId}
      onSelect={select}
      onNew={startNew}
      className={cx('mn-ai-chat-page__history', historyProps?.className)}
      messages={{ label: m.history, ...historyProps?.messages }}
    />
  );

  const send = (text: string, details: AiPromptSubmitDetails) =>
    onSend ? onSend(text, details) : conversation.send(text);

  const wiredMessageProps = (message: AiMessage): Partial<Omit<AiMessageViewProps, 'message'>> => {
    const extra = typeof messageProps === 'function' ? messageProps(message) : messageProps;
    return {
      agent: conversation.agent,
      onRegenerate: (msg) => conversation.regenerate(msg.id),
      onFeedback: (f, msg) => conversation.setFeedback(msg.id, f),
      onBranch: (i, msg) => conversation.branchTo(msg.id, i),
      onRetry: (msg) => conversation.regenerate(msg.id),
      ...extra,
    };
  };

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-ai-chat-page', className)}
      data-compact={compact || undefined}
      data-canvas={canvasOpen || undefined}
      data-status={conversation.status}
    >
      {compact ? (
        <Sheet open={historyOpen} onOpenChange={setHistoryOpen}>
          <SheetContent side="left" size="sm" className="mn-ai-chat-page__sheet">
            <SheetHeader>
              <SheetTitle>{m.history}</SheetTitle>
            </SheetHeader>
            {list}
          </SheetContent>
        </Sheet>
      ) : (
        <div className="mn-ai-chat-page__rail">{list}</div>
      )}

      <section id={chatId} className="mn-ai-chat-page__main" aria-label={m.chat}>
        {compact || title !== undefined ? (
          <div className="mn-ai-chat-page__bar">
            {compact ? (
              <IconButton
                size="sm"
                label={m.openHistory}
                icon={<MenuGlyph />}
                aria-expanded={historyOpen}
                aria-haspopup="dialog"
                onClick={() => setHistoryOpen(true)}
                className="mn-ai-chat-page__menu"
              />
            ) : null}
            {title !== undefined ? (
              <AiConversationHeader
                agent={conversation.agent}
                {...headerProps}
                title={title}
                className={cx('mn-ai-chat-page__header', headerProps?.className)}
              />
            ) : null}
          </div>
        ) : null}
        <AiThread
          {...threadProps}
          className={cx('mn-ai-chat-page__thread', threadProps?.className)}
          messages={conversation.messages}
          status={conversation.status}
          error={conversation.error}
          onStop={conversation.stop}
          empty={empty ?? threadProps?.empty}
          messageProps={wiredMessageProps}
        />
        <div className="mn-ai-chat-page__composer">
          <AiPromptInput
            {...promptProps}
            label={m.inputLabel}
            status={conversation.status}
            onStop={conversation.stop}
            onSubmit={send}
            textareaRef={inputRef}
            {...(toolbar !== undefined ? { toolbar } : {})}
            {...(footer !== undefined ? { footer } : {})}
          />
        </div>
      </section>

      {artifact ? (
        <AiArtifactCanvas
          {...artifactProps}
          artifact={artifact}
          open={canvasOpen}
          onOpenChange={setCanvasOpen}
          className={cx('mn-ai-chat-page__canvas', artifactProps?.className)}
        />
      ) : null}
    </div>
  );
});

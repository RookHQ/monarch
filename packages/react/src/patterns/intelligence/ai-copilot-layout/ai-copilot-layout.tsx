'use client';

import {
  createContext,
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useId,
  useMemo,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import type { UseAiConversationResult } from '../../../components/intelligence/ai-conversation/index.js';
import {
  AiAssistantPanel,
  type AiAssistantContextItem,
  type AiAssistantPanelProps,
} from '../../../components/intelligence/ai-assistant-panel/index.js';
import {
  AiLauncher,
  type AiLauncherProps,
} from '../../../components/intelligence/ai-launcher/index.js';
import type { AiPromptSubmitDetails } from '../../../components/intelligence/ai-prompt-input/index.js';

/** One thing the person has selected in the app, shown as a chip and sent with the next prompt. */
export interface AiSelectionItem extends AiAssistantContextItem {
  /** Plain text the assistant receives, e.g. the selected paragraph. Default: `items` when it is a string. */
  text?: string | undefined;
  /** Structured data sent alongside, e.g. `{ recordIds: [...] }`. */
  data?: unknown;
}

/** What `useAiSelectionContext()` returns. */
export interface AiSelectionContextValue {
  /** The current selection, empty when nothing is selected. */
  selection: readonly AiSelectionItem[];
  /** Replaces the selection. Pass `[]` or `null` to clear it. */
  setSelection: (selection: readonly AiSelectionItem[] | null) => void;
  /** True inside an `AiCopilotLayout`. */
  available: boolean;
}

const NO_SELECTION: readonly AiSelectionItem[] = [];

const SelectionContext = createContext<AiSelectionContextValue>({
  selection: NO_SELECTION,
  setSelection: () => {},
  available: false,
});

/**
 * The selection shared between the app and the docked assistant. The page
 * writes what the person selected (`setSelection`); the assistant panel shows
 * it as context chips and sends it with the next prompt. Outside an
 * `AiCopilotLayout` it is empty and `setSelection` does nothing.
 */
export function useAiSelectionContext(): AiSelectionContextValue {
  return useContext(SelectionContext);
}

export interface AiCopilotLayoutProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** The app content. Call `useAiSelectionContext()` anywhere inside it. */
  children: ReactNode;
  /** The `useAiConversation` result the assistant shows and sends to. */
  conversation: UseAiConversationResult;
  /** Heading of the assistant panel, e.g. "Assistant". */
  title: ReactNode;
  /** Whether the assistant is open (controlled). */
  open?: boolean | undefined;
  /** Initial `open` when uncontrolled. Default false. */
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** Chips that always show, e.g. the current page. The selection follows them. */
  context?: readonly AiAssistantContextItem[] | undefined;
  /** Which side the assistant docks to. Default `right`. */
  side?: 'right' | 'left' | undefined;
  /** Render the floating launcher (with ⌘J / Ctrl+J). Default true. */
  launcher?: boolean | undefined;
  /** Request field the selection is sent under (`AiTransportRequest.body`). Default `selection`. */
  selectionField?: string | undefined;
  /** Clear the selection after it has been sent. Default false. */
  clearSelectionOnSend?: boolean | undefined;
  /** Extra props for the AiAssistantPanel (starters, toolbar, footer, messages…). */
  panelProps?:
    | Partial<
        Omit<
          AiAssistantPanelProps,
          'conversation' | 'title' | 'open' | 'onOpenChange' | 'context' | 'side' | 'placement'
        >
      >
    | undefined;
  /** Extra props for the AiLauncher. */
  launcherProps?: Partial<Omit<AiLauncherProps, 'open' | 'onOpenChange'>> | undefined;
}

const COMPACT_QUERY = '(max-width: 639.98px)';

/** True below 640px, read after mount (false on the server). */
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

function selectionPayload(items: readonly AiSelectionItem[]) {
  return items.map((s) => ({
    label: s.label,
    text: s.text ?? (typeof s.items === 'string' ? s.items : undefined),
    ...(s.data !== undefined ? { data: s.data } : {}),
  }));
}

/**
 * A copilot layout: the app's content with an AiAssistantPanel docked beside
 * it. The two share a selection context: the page writes what the person
 * selected through `useAiSelectionContext()`, and the panel shows it as
 * context chips and sends it with the next prompt. The content reflows to make
 * room on wide screens; below 640px the assistant opens as a full-screen sheet.
 */
export const AiCopilotLayout = forwardRef<HTMLDivElement, AiCopilotLayoutProps>(
  function AiCopilotLayout(
    {
      children,
      conversation,
      title,
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      context,
      side = 'right',
      launcher = true,
      selectionField = 'selection',
      clearSelectionOnSend = false,
      panelProps,
      launcherProps,
      className,
      ...rest
    },
    ref,
  ) {
    const auto = useId().replace(/:/g, '');
    const panelId = panelProps?.id ?? `mn-ai-copilot-${auto}-panel`;
    const [openInner, setOpenInner] = useState(defaultOpen);
    const open = openProp ?? openInner;
    const setOpen = useCallback(
      (next: boolean) => {
        if (openProp === undefined) setOpenInner(next);
        onOpenChange?.(next);
      },
      [openProp, onOpenChange],
    );

    const [selection, setSelectionState] = useState<readonly AiSelectionItem[]>(NO_SELECTION);
    const setSelection = useCallback((next: readonly AiSelectionItem[] | null) => {
      setSelectionState(next && next.length ? next : NO_SELECTION);
    }, []);
    const value = useMemo<AiSelectionContextValue>(
      () => ({ selection, setSelection, available: true }),
      [selection, setSelection],
    );

    const compact = useCompact();
    const chips = useMemo(() => [...(context ?? []), ...selection], [context, selection]);

    const send = (text: string, details: AiPromptSubmitDetails) => {
      if (panelProps?.onSend) return panelProps.onSend(text, details);
      const body = selection.length ? { [selectionField]: selectionPayload(selection) } : undefined;
      conversation.send(text, body ? { body } : undefined);
      if (clearSelectionOnSend) setSelection(null);
      return undefined;
    };

    return (
      <SelectionContext.Provider value={value}>
        <div
          ref={ref}
          {...rest}
          className={cx('mn-ai-copilot-layout', className)}
          data-open={open || undefined}
          data-side={side}
          data-compact={compact || undefined}
        >
          <div className="mn-ai-copilot-layout__content">{children}</div>
          <div className="mn-ai-copilot-layout__rail" hidden={!open}>
            <AiAssistantPanel
              {...panelProps}
              id={panelId}
              conversation={conversation}
              title={title}
              open={open}
              onOpenChange={setOpen}
              context={chips}
              side={side}
              placement={compact ? 'fixed' : 'inline'}
              onSend={send}
              className={cx('mn-ai-copilot-layout__panel', panelProps?.className)}
            />
          </div>
          {launcher ? (
            <AiLauncher
              position={side === 'left' ? 'bottom-start' : 'bottom-end'}
              {...launcherProps}
              open={open}
              onOpenChange={setOpen}
              aria-controls={panelId}
              hidden={open && !compact}
              className={cx('mn-ai-copilot-layout__launcher', launcherProps?.className)}
            />
          ) : null}
        </div>
      </SelectionContext.Provider>
    );
  },
);

'use client';

import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { Node, mergeAttributes } from '@tiptap/core';
import {
  NodeViewContent,
  NodeViewWrapper,
  ReactNodeViewRenderer,
  type NodeViewProps,
} from '@tiptap/react';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import { cx } from '../../../internal/cx.js';
import {
  DotGlyph,
  ErrorGlyph,
  InfoGlyph,
  SuccessGlyph,
  WarningGlyph,
} from '../../../internal/icons.js';

/** The colour and icon of a callout. Statuses keep their Monarch meaning. */
export type CalloutTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

/** Every tone, in picker order. */
export const CALLOUT_TONES: readonly CalloutTone[] = [
  'neutral',
  'info',
  'success',
  'warning',
  'danger',
];

const TONE_ICONS: Record<CalloutTone, (p: { className?: string }) => ReactNode> = {
  neutral: DotGlyph,
  info: InfoGlyph,
  success: SuccessGlyph,
  warning: WarningGlyph,
  danger: ErrorGlyph,
};

const isTone = (v: unknown): v is CalloutTone =>
  typeof v === 'string' && (CALLOUT_TONES as readonly string[]).includes(v);

/** Every string the callout's editing controls show or announce. */
export interface CalloutBlockMessages {
  /** Name of the tone picker. Default "Callout style". */
  toneLabel: string;
  /** Name of each tone. */
  tones: Record<CalloutTone, string>;
}

export const DEFAULT_CALLOUT_BLOCK_MESSAGES: CalloutBlockMessages = {
  toneLabel: 'Callout style',
  tones: {
    neutral: 'Note',
    info: 'Information',
    success: 'Success',
    warning: 'Warning',
    danger: 'Danger',
  },
};

export interface CalloutBlockProps extends HTMLAttributes<HTMLDivElement> {
  /** Colour and default icon. Default `neutral`. */
  tone?: CalloutTone | undefined;
  /** Replaces the tone's icon; `null` shows none. */
  icon?: ReactNode;
  /** Shown above the content, in a row of its own (the editing controls use it). */
  toolbar?: ReactNode;
}

/**
 * A highlighted note inside a document: a tinted box with an icon that sets
 * a paragraph or a few blocks apart from the text around it. It is content,
 * not a live message, so it has no alert or status role. In a Tiptap editor
 * use `CalloutBlockExtension`; on a static page render this directly.
 */
export const CalloutBlock = forwardRef<HTMLDivElement, CalloutBlockProps>(function CalloutBlock(
  { tone = 'neutral', icon, toolbar, className, children, ...rest },
  ref,
) {
  const Icon = TONE_ICONS[isTone(tone) ? tone : 'neutral'];
  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-callout-block', className)}
      data-tone={isTone(tone) ? tone : 'neutral'}
    >
      {toolbar ? <div className="mn-callout-block__toolbar">{toolbar}</div> : null}
      <div className="mn-callout-block__row">
        {icon === null ? null : (
          <span className="mn-callout-block__icon" aria-hidden contentEditable={false}>
            {icon ?? <Icon />}
          </span>
        )}
        <div className="mn-callout-block__content">{children}</div>
      </div>
    </div>
  );
});

/* ------------------------------------------------------------- extension */

/** Options for `CalloutBlockExtension.configure()`. */
export interface CalloutBlockExtensionOptions {
  /** Strings for the editing controls; `tones` may name only some tones. */
  messages:
    | (Partial<Omit<CalloutBlockMessages, 'tones'>> & {
        tones?: Partial<CalloutBlockMessages['tones']> | undefined;
      })
    | undefined;
  /** Extra HTML attributes on the rendered `<div data-type="callout">`. */
  HTMLAttributes: Record<string, unknown>;
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    callout: {
      /** Wraps the selected blocks in a callout (or inserts an empty one). */
      setCallout: (attrs?: { tone?: CalloutTone }) => ReturnType;
      /** Lifts the content out of the callout around the caret. */
      unsetCallout: () => ReturnType;
      /** Changes the tone of the callout around the caret. */
      setCalloutTone: (tone: CalloutTone) => ReturnType;
    };
  }
}

function CalloutNodeView({ node, updateAttributes, editor, extension }: NodeViewProps) {
  const options = extension.options as CalloutBlockExtensionOptions;
  const m = {
    ...DEFAULT_CALLOUT_BLOCK_MESSAGES,
    ...options.messages,
    tones: { ...DEFAULT_CALLOUT_BLOCK_MESSAGES.tones, ...options.messages?.tones },
  };
  const tone = isTone(node.attrs.tone) ? node.attrs.tone : 'neutral';
  return (
    <NodeViewWrapper data-type="callout">
      <CalloutBlock
        tone={tone}
        toolbar={
          editor.isEditable ? (
            <div contentEditable={false} className="mn-callout-block__picker">
              <SegmentedControl
                size="sm"
                aria-label={m.toneLabel}
                value={tone}
                onValueChange={(next) => updateAttributes({ tone: next })}
                options={CALLOUT_TONES.map((t) => {
                  const Icon = TONE_ICONS[t];
                  return { value: t, label: m.tones[t], icon: <Icon />, hideLabel: true };
                })}
              />
            </div>
          ) : null
        }
      >
        <NodeViewContent className="mn-callout-block__editable" />
      </CalloutBlock>
    </NodeViewWrapper>
  );
}

/**
 * The Tiptap node for CalloutBlock: `callout`, a block that holds other
 * blocks, with a `tone` attribute. Renders `<div data-type="callout"
 * data-tone="info">…</div>` in HTML and a tone picker while editable. Adds
 * `setCallout`, `unsetCallout` and `setCalloutTone`; Backspace at the very
 * start of a callout lifts its content out.
 */
export const CalloutBlockExtension = Node.create<CalloutBlockExtensionOptions>({
  name: 'callout',
  group: 'block',
  content: 'block+',
  defining: true,
  addOptions() {
    return { messages: undefined, HTMLAttributes: {} };
  },
  addAttributes() {
    return {
      tone: {
        default: 'neutral' as CalloutTone,
        parseHTML: (el: HTMLElement) => {
          const t = el.getAttribute('data-tone');
          return isTone(t) ? t : 'neutral';
        },
        renderHTML: (attrs: { tone?: unknown }) => ({
          'data-tone': isTone(attrs.tone) ? attrs.tone : 'neutral',
        }),
      },
    };
  },
  parseHTML() {
    return [{ tag: 'div[data-type="callout"]' }];
  },
  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes, { 'data-type': 'callout' }),
      0,
    ];
  },
  addCommands() {
    return {
      setCallout:
        (attrs) =>
        ({ commands, state, chain }) => {
          const tone = attrs?.tone && isTone(attrs.tone) ? attrs.tone : 'neutral';
          if (commands.wrapIn(this.name, { tone })) return true;
          // Nothing to wrap (an empty selection in a node that cannot be wrapped): insert one.
          return chain()
            .insertContentAt(state.selection.to, {
              type: this.name,
              attrs: { tone },
              content: [{ type: 'paragraph' }],
            })
            .run();
        },
      unsetCallout:
        () =>
        ({ commands }) =>
          commands.lift(this.name),
      setCalloutTone:
        (tone) =>
        ({ commands }) =>
          isTone(tone) ? commands.updateAttributes(this.name, { tone }) : false,
    };
  },
  addKeyboardShortcuts() {
    return {
      // Backspace at the very start of a callout's first block lifts it out.
      Backspace: ({ editor }) => {
        const { $from, empty } = editor.state.selection;
        if (!empty || $from.parentOffset !== 0) return false;
        for (let d = $from.depth; d > 0; d--) {
          if ($from.node(d).type.name === this.name) {
            for (let k = d; k < $from.depth; k++) if ($from.index(k) !== 0) return false;
            return editor.commands.lift(this.name);
          }
        }
        return false;
      },
    };
  },
  addNodeView() {
    return ReactNodeViewRenderer(CalloutNodeView);
  },
});

import type { AnyExtension, Extensions } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import CharacterCount from '@tiptap/extension-character-count';
import TaskList from '@tiptap/extension-task-list';
import TaskItem from '@tiptap/extension-task-item';
import Image from '@tiptap/extension-image';
import Mention from '@tiptap/extension-mention';
import type { SuggestionOptions } from '@tiptap/suggestion';
import { LINK_PROTOCOLS, isSafeImageSrc, isSafeUrl } from '../../../internal/sanitize.js';

/** Heading levels the editor can produce. */
export type RichTextHeadingLevel = 1 | 2 | 3 | 4;

export interface EditorKitOptions {
  /** Read on every render of the empty document, so it can change without a rebuild. */
  placeholder: () => string;
  headingLevels: readonly RichTextHeadingLevel[];
  maxLength: number | undefined;
  taskList: boolean;
  images: boolean;
  mention:
    | {
        trigger: string;
        suggestion: Omit<SuggestionOptions, 'editor'>;
      }
    | undefined;
}

/**
 * Builds the Tiptap extension list for one editor. Every node and mark comes
 * from here, so the toolbar, the Markdown shortcuts and the paste parser all
 * agree on the schema. Task lists, images and mentions are only added to the
 * schema when the editor is configured to use them.
 */
export function buildEditorKit(o: EditorKitOptions): Extensions {
  const kit: AnyExtension[] = [
    StarterKit.configure({
      heading: o.headingLevels.length ? { levels: [...o.headingLevels] } : false,
      // Typed "---" would otherwise become a rule the toolbar cannot remove.
      horizontalRule: false,
      codeBlock: { HTMLAttributes: { class: 'mn-rich-text-editor__code-block' } },
    }),
    Link.extend({ inclusive: false }).configure({
      openOnClick: false,
      autolink: true,
      linkOnPaste: true,
      protocols: [...LINK_PROTOCOLS],
      validate: (href) => isSafeUrl(href),
      HTMLAttributes: { rel: 'noopener noreferrer nofollow', target: '_blank' },
    }),
    Placeholder.configure({ placeholder: () => o.placeholder() }),
    CharacterCount.configure(o.maxLength ? { limit: o.maxLength } : {}),
  ];
  if (o.taskList) kit.push(TaskList, TaskItem.configure({ nested: true }));
  if (o.images)
    kit.push(
      Image.extend({
        parseHTML() {
          return [
            {
              tag: 'img[src]',
              getAttrs: (el) =>
                isSafeImageSrc((el as HTMLElement).getAttribute('src') ?? '') ? null : false,
            },
          ];
        },
      }).configure({ allowBase64: false, inline: false }),
    );
  if (o.mention)
    kit.push(
      Mention.configure({
        HTMLAttributes: { class: 'mn-rich-text-editor__mention' },
        // Rendered as <span data-type="mention" data-id data-label>@Label</span>.
        suggestion: { ...o.mention.suggestion, char: o.mention.trigger },
      }),
    );
  return kit;
}

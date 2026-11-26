import type { AnyExtension, Extensions } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';
import Link from '@tiptap/extension-link';
import { CharacterCount, Placeholder } from '@tiptap/extensions';
import { TaskItem, TaskList } from '@tiptap/extension-list';
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
      // Tiptap 3's StarterKit also bundles these. Link is added below with its own
      // safety options; the others stay off so the schema, the HTML it emits and
      // the keyboard behave as before (no <u> the toolbar cannot remove, no
      // trailing empty paragraph, the same Backspace and Delete in lists).
      link: false,
      underline: false,
      trailingNode: false,
      listKeymap: false,
    }),
    Link.extend({ inclusive: false }).configure({
      openOnClick: false,
      autolink: true,
      linkOnPaste: true,
      protocols: [...LINK_PROTOCOLS],
      // Tiptap 3 checks every href it parses, pastes, auto-links or sets against
      // `isAllowedUri`; `shouldAutoLink` gates auto-linking. (`validate` is the
      // deprecated Tiptap 2 name: it never guarded parsed or set links.)
      // Only http, https and mailto pass, with the same checks as everywhere else.
      isAllowedUri: (href) => isSafeUrl(href),
      shouldAutoLink: (href) => isSafeUrl(href),
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

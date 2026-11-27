import { useEffect, useState } from 'react';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { Button } from '@rookcom/monarch-react';
import { BubbleMenu } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './bubble-menu.fixture.json';

const TEXT = `<p>${fixture.data.text}</p>`;

function useDemoEditor() {
  return useEditor({
    immediatelyRender: false,
    extensions: [StarterKit.configure({ link: { openOnClick: false } })],
    content: TEXT,
    editorProps: { attributes: { 'aria-label': fixture.data.label, role: 'textbox' } },
  });
}

const frame = {
  boxSizing: 'border-box',
  width: '100%',
  maxWidth: 560,
  padding: 12,
  border: '1px solid var(--mn-color-border-default)',
  borderRadius: 8,
} as const;

function FormattingDemo() {
  const editor = useDemoEditor();
  // Start with a few words selected, so the menu is showing.
  useEffect(() => {
    if (!editor) return;
    // Document positions count the paragraph's opening, one more than the text offsets.
    const { start, end } = fixture.data.selection;
    editor.commands.setTextSelection({ from: start + 1, to: end + 1 });
  }, [editor]);
  return (
    <div style={frame}>
      <EditorContent editor={editor} />
      <BubbleMenu editor={editor} />
    </div>
  );
}

function CommentDemo() {
  const editor = useDemoEditor();
  const [quoted, setQuoted] = useState('');
  return (
    <div style={{ ...frame, display: 'grid', gap: 8 }}>
      <EditorContent editor={editor} />
      <BubbleMenu editor={editor} messages={{ label: 'Comment on selection' }}>
        <Button
          size="sm"
          variant="ghost"
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => {
            if (!editor) return;
            const { from, to } = editor.state.selection;
            setQuoted(editor.state.doc.textBetween(from, to, ' '));
          }}
        >
          Comment
        </Button>
      </BubbleMenu>
      <p style={{ margin: 0, fontSize: 12 }}>
        {quoted ? `Commenting on “${quoted}”` : 'No comment yet.'}
      </p>
    </div>
  );
}

export default {
  id: 'bubble-menu',
  title: 'BubbleMenu',
  summary:
    'A small toolbar that floats over selected text in a Tiptap editor, for inline formatting or actions such as Comment.',
  sections: {
    purpose: {
      summary:
        'Puts the actions that apply to a selection right next to it, without a permanent toolbar. Needed by: Rook Dossier (`src/components/document/DocumentEditor.tsx`, Tiptap’s tippy-based BubbleMenu with a Comment button).',
      useWhen: [
        'A document editor with little chrome, where inline formatting appears only when text is selected.',
        'Actions about the selected text: comment, link, ask an assistant about it.',
      ],
      avoidWhen: [
        'The editor already shows a full toolbar with the same buttons: use EditorToolbar alone.',
        'Plain text fields: the menu needs a Tiptap editor.',
        'Actions about the whole document: put them in the page header.',
      ],
    },
    designPrinciples: {
      items: [
        'Never steal focus: the menu appears while the person keeps typing or selecting; focus moves in only on request (Alt+F10).',
        'Close to the selection, never off-screen: above by default, flipped below near the top, clamped to the viewport.',
        'Formatting by default, product actions by composition: children replace the default toolbar.',
        'No extra positioning library: positions come from the editor itself (`coordsAtPos`), so neither Tiptap’s own menu nor its positioning library is loaded.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-bubble-menu` (fixed position, `data-side="top" | "bottom"`) only while it is visible; the default content is an EditorToolbar with `.mn-bubble-menu__toolbar`.',
        '`defaultBubbleMenuShouldShow(editor)` is exported to build on: `shouldShow={(ed) => defaultBubbleMenuShouldShow(ed) && !ed.isActive("link")}`.',
        'Import from `@rookcom/monarch-react/editor`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Tiptap’s `BubbleMenu` (`@tiptap/react`): `editor` and `shouldShow` keep their meaning (`shouldShow` receives the editor only); `tippyOptions` (Tiptap 2) and `options` (Tiptap 3) have no equivalent, use `placement` and `offset`.',
        'Custom children that are buttons should `preventDefault()` on mouse down, so clicking them does not collapse the selection.',
        '`messages.toolbar` translates the default toolbar.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'placement',
          values: ['top', 'bottom'],
          description: 'Preferred side. `top` flips below when there is no room above.',
        },
        {
          prop: 'children',
          values: ['default inline toolbar', 'custom content'],
          description: 'Formatting buttons, or the product’s own selection actions.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Hidden',
          description:
            'Not rendered: no selection, selection in code, read-only editor, focus outside the editor, while dragging out a selection, or after Escape.',
        },
        {
          name: 'Measuring',
          description: 'Rendered but invisible for one frame while it is positioned.',
        },
        {
          name: 'Shown',
          attribute: 'data-positioned, data-side',
          description: 'Placed above (`top`) or below (`bottom`) the selection.',
        },
      ],
    },
    interaction: {
      items: [
        'Appears when a selection with text is made, after the pointer is released.',
        'Follows the selection as it changes, the window scrolls or resizes.',
        'Stays open while focus is in the menu or its link popover; closes when focus leaves the editor.',
        'Escape in the text hides it until the selection changes.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Alt+F10 (in the text)', action: 'Moves focus to the menu’s first control.' },
        { keys: 'ArrowLeft / ArrowRight, Home / End', action: 'Move between toolbar buttons.' },
        { keys: 'Escape (in the menu)', action: 'Returns focus to the text, selection kept.' },
        { keys: 'Escape (in the text)', action: 'Hides the menu for this selection.' },
      ],
    },
    accessibility: {
      role: 'toolbar (default content) or group (custom children)',
      name: 'The toolbar’s name ("Formatting"), or `messages.label` (default "Selection actions") for custom children.',
      items: [
        'Focus is never moved when the menu appears; Alt+F10 is the conventional key to reach a contextual toolbar.',
        'Escape always gives focus back to the text with the selection unchanged.',
        'The default content is an APG toolbar with `aria-pressed` toggles.',
      ],
    },
    responsive: {
      items: [
        'Kept 8px inside the viewport horizontally; never wider than the viewport.',
        'The default toolbar scrolls horizontally inside the menu instead of wrapping.',
        'Toolbar buttons grow to 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Tokens: `--mn-color-bg-raised`, `--mn-color-border-default`, `--mn-shadow-md`, `--mn-radius-md`, `--mn-z-popover`.',
        'Forced colours: a CanvasText border replaces the shadow.',
        'The fade-in is removed under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Inline formatting',
          stack: true,
          stage: true,
          render: () => <FormattingDemo />,
        },
        { title: 'Custom action', stack: true, render: () => <CommentDemo /> },
      ],
    },
    doDont: {
      do: [
        'Keep it short: three to six controls.',
        'Offer the same actions elsewhere (toolbar, shortcuts) for people who never select text with a pointer.',
      ],
      dont: [
        'Do not put destructive actions in the bubble.',
        'Do not move focus into it from code when it appears.',
      ],
    },
    related: {
      items: [
        {
          id: 'editor-toolbar',
          reason: 'The toolbar it shows by default; also usable as a fixed toolbar.',
        },
        { id: 'block-editor', reason: 'Uses this menu over selected text.' },
        {
          id: 'popover',
          reason: 'For floating content anchored to a button instead of a selection.',
        },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['editor-toolbar'],
      notes: ['`@tiptap/core` is type-only; the editor comes from the product.'],
    },
    testing: {
      unit: [
        'Hidden until text is selected; shows the inline toolbar without moving focus.',
        'Hides when the selection collapses or focus leaves the editor.',
        'Buttons format the selection.',
        'Alt+F10 moves focus in; Escape returns to the text with the selection kept.',
        'Escape in the text dismisses it until the selection changes.',
        'Not shown in a code block or a read-only editor.',
        'Custom children in a named group; custom `shouldShow`.',
        'Link popover keeps the menu open and applies the link.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

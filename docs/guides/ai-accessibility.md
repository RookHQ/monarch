# AI accessibility

Rules every `@rookcom/monarch-react/ai` component follows, and every product
building an AI surface should keep. Streaming text changes many times a
second; a screen reader that reads every change is unusable, and a page that
jumps or steals focus is unusable for everyone.

## Announcing

- Stream into a visible element. Tell screen readers about new text **at
  sentence or paragraph boundaries**, through **one** throttled polite live
  region per conversation (the thread owns it), never token by token.
- When the reply ends, announce "Response complete". Announce errors
  assertively, once.
- Mark the message `aria-busy="true"` while it streams.
- Identify AI-generated content to assistive technology: messages carry a
  visually hidden "Assistant said:" prefix (translatable), and AI-generated
  fields elsewhere carry an "AI-generated" label.

## Control

- Stop is always reachable: a visible button (the send button becomes Stop)
  and Escape while focus is in the prompt input or the thread.
- Focus never moves while a reply streams. New messages do not take focus.

## Scrolling

- The thread stays pinned to the bottom only while the person is at the
  bottom. When they scroll up to read, it stops following and shows "Jump to
  latest".
- Under `prefers-reduced-motion`, scrolling is instant and there is no
  shimmer, no caret blink and no animated typing dots.

## Structure

- Reasoning is a disclosure with `<details>`/`<summary>` semantics, collapsed
  once the answer starts.
- Answer headings start below the page's own (an answer's `#` is an `h3` by
  default), so the page outline stays intact.
- Code blocks and wide tables scroll inside a focusable, labelled region,
  never the whole page.

## Colour and contrast

- AI tokens (`--mn-color-ai-*`) meet the same contrast targets as the status
  colours in all four themes. `--mn-gradient-ai` is decorative and is a solid
  colour in high contrast.
- State is never shown by colour alone: tool states, diffs and confidence use
  icons and text too.

## Safety

- Model output is untrusted: it is rendered as React elements, links are
  checked and open with `rel="noopener noreferrer nofollow"`, and images are
  off unless the product allows them.

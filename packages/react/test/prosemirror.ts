/**
 * jsdom gaps ProseMirror needs, for the ./editor tests only. Not in the global
 * setup: axe reads Range rectangles too, and these stubs would change what it
 * considers visible in every other component's test.
 */
// ProseMirror asks for caret rectangles when it scrolls the selection into view.
const emptyRect = () => ({
  x: 0,
  y: 0,
  top: 0,
  left: 0,
  bottom: 0,
  right: 0,
  width: 0,
  height: 0,
  toJSON: () => ({}),
});
const rangeProto = window.Range.prototype as unknown as Record<string, unknown>;
rangeProto.getClientRects ??= () => ({
  length: 0,
  item: () => null,
  [Symbol.iterator]: [][Symbol.iterator],
});
rangeProto.getBoundingClientRect ??= emptyRect;
const docProto = window.Document.prototype as unknown as Record<string, unknown>;
docProto.elementFromPoint ??= () => null;

// ProseMirror's view.pasteHTML builds a ClipboardEvent, which jsdom lacks.
class ClipboardEventStub extends Event {
  clipboardData: DataTransfer | null;
  constructor(type: string, init: ClipboardEventInit = {}) {
    super(type, init);
    this.clipboardData = init.clipboardData ?? null;
  }
}
globalThis.ClipboardEvent ??= ClipboardEventStub as unknown as typeof ClipboardEvent;

export {};

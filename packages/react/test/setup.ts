import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

afterEach(() => cleanup());

// jsdom gaps that Radix relies on.
// ResizeObserver: jsdom has none. The stub never fires on its own (as before),
// but `triggerResize` fires it, so measure and overflow paths can be tested.
interface ObserverRecord {
  callback: ResizeObserverCallback;
  observer: ResizeObserver;
  targets: Set<Element>;
}
// Held on globalThis so every import of this module shares one registry.
const registry: Set<ObserverRecord> =
  ((globalThis as Record<string, unknown>)['__mnResizeObservers'] as
    Set<ObserverRecord> | undefined) ?? new Set<ObserverRecord>();
(globalThis as Record<string, unknown>)['__mnResizeObservers'] = registry;

class ResizeObserverStub {
  readonly #record: ObserverRecord;
  constructor(callback: ResizeObserverCallback) {
    this.#record = {
      callback,
      observer: this as unknown as ResizeObserver,
      targets: new Set(),
    };
    registry.add(this.#record);
  }
  observe(target: Element) {
    this.#record.targets.add(target);
  }
  unobserve(target: Element) {
    this.#record.targets.delete(target);
  }
  disconnect() {
    this.#record.targets.clear();
  }
}
globalThis.ResizeObserver ??= ResizeObserverStub as unknown as typeof ResizeObserver;

function resizeEntry(target: Element, width: number, height: number): ResizeObserverEntry {
  const box = [
    { inlineSize: width, blockSize: height },
  ] as unknown as ReadonlyArray<ResizeObserverSize>;
  return {
    target,
    contentRect:
      DOMRectReadOnly.fromRect?.({ width, height }) ?? ({ width, height } as DOMRectReadOnly),
    borderBoxSize: box,
    contentBoxSize: box,
    devicePixelContentBoxSize: box,
  };
}

/**
 * Fires every stubbed ResizeObserver that watches `target` (or, without a
 * target, every observed element) with an entry of the given size. Mock the
 * measurements the component reads (clientWidth, getBoundingClientRect)
 * before calling it, and wrap it in `act`.
 */
export function triggerResize(
  target?: Element,
  size: { width?: number; height?: number } = {},
): void {
  const { width = 0, height = 0 } = size;
  for (const record of [...registry]) {
    const targets = target ? (record.targets.has(target) ? [target] : []) : [...record.targets];
    if (targets.length === 0) continue;
    record.callback(
      targets.map((t) => resizeEntry(t, width, height)),
      record.observer,
    );
  }
}

afterEach(() => {
  for (const record of registry) if (record.targets.size === 0) registry.delete(record);
});
const proto = window.HTMLElement.prototype as unknown as Record<string, unknown>;
proto.hasPointerCapture ??= () => false;
proto.setPointerCapture ??= () => {};
proto.releasePointerCapture ??= () => {};
proto.scrollIntoView ??= () => {};
window.matchMedia ??= ((query: string) => ({
  matches: false,
  media: query,
  onchange: null,
  addListener() {},
  removeListener() {},
  addEventListener() {},
  removeEventListener() {},
  dispatchEvent: () => false,
})) as unknown as typeof window.matchMedia;

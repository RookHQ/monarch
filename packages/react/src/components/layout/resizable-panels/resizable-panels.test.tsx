import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  ResizableHandle,
  ResizablePanel,
  ResizablePanelGroup,
  ResizablePanels,
  type ResizablePanelsProps,
} from './index.js';

// jsdom has no PointerEvent: a MouseEvent with a pointerId is enough here.
if (typeof window.PointerEvent === 'undefined') {
  class PointerEventPolyfill extends MouseEvent {
    pointerId: number;
    constructor(type: string, init: PointerEventInit = {}) {
      super(type, init);
      this.pointerId = init.pointerId ?? 0;
    }
  }
  (window as unknown as { PointerEvent: unknown }).PointerEvent = PointerEventPolyfill;
}

function Split(props: Partial<ResizablePanelsProps> & { collapsible?: boolean }) {
  const { collapsible, ...rest } = props;
  return (
    <ResizablePanels aria-label="Editor layout" {...rest}>
      <ResizablePanel defaultSize={30} minSize={20} maxSize={60} collapsible={collapsible}>
        Sidebar
      </ResizablePanel>
      <ResizableHandle withHandle />
      <ResizablePanel>Content</ResizablePanel>
    </ResizablePanels>
  );
}

const sep = () => screen.getByRole('separator', { name: 'Resize panels' });
const panels = () =>
  Array.from(document.querySelectorAll<HTMLElement>('.mn-resizable-panels__panel'));
const sizes = () => panels().map((p) => Number(p.getAttribute('data-size')));

let rectSpy: ReturnType<typeof vi.spyOn>;
beforeEach(() => {
  // Each panel reports its share of a 1000px-wide group.
  rectSpy = vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (
    this: HTMLElement,
  ) {
    const size = this.classList.contains('mn-resizable-panels__panel')
      ? Number(this.getAttribute('data-size')) * 10
      : 1000;
    return {
      width: size,
      height: size,
      top: 0,
      left: 0,
      right: size,
      bottom: size,
      x: 0,
      y: 0,
      toJSON() {},
    } as DOMRect;
  });
});
afterEach(() => rectSpy.mockRestore());

describe('ResizablePanels', () => {
  it('sizes panels from defaults and exposes a labelled separator', async () => {
    render(<Split />);
    expect(sizes()).toEqual([30, 70]);
    const s = sep();
    expect(s).toHaveAttribute('aria-orientation', 'vertical');
    expect(s).toHaveAttribute('aria-valuenow', '30');
    expect(s).toHaveAttribute('aria-valuemin', '20');
    expect(s).toHaveAttribute('aria-valuemax', '60');
    expect(s).toHaveAttribute('aria-controls', panels()[0]!.id);
    expect(s).toHaveAttribute('tabindex', '0');
    expect(panels()[0]!.style.getPropertyValue('--mn-resizable-panels-size')).toBe('30');
    expect(document.querySelector('.mn-resizable-panels__grip')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('resizes with the arrow keys within limits, saving each step', async () => {
    const user = userEvent.setup();
    const onLayout = vi.fn();
    render(<Split onLayout={onLayout} />);
    await user.tab();
    expect(sep()).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(sizes()).toEqual([35, 65]);
    expect(sep()).toHaveAttribute('aria-valuenow', '35');
    expect(onLayout).toHaveBeenLastCalledWith([35, 65]);
    await user.keyboard('{ArrowLeft}{ArrowLeft}{ArrowLeft}');
    expect(sizes()).toEqual([20, 80]);
    await user.keyboard('{End}');
    expect(sizes()).toEqual([60, 40]);
    await user.keyboard('{Home}');
    expect(sizes()).toEqual([20, 80]);
    await user.keyboard('{ArrowUp}');
    expect(sizes()).toEqual([20, 80]);
  });

  it('swaps Left and Right in rtl, and uses Up/Down when vertical', async () => {
    const user = userEvent.setup();
    const { unmount } = render(<Split dir="rtl" keyboardStep={10} />);
    await user.tab();
    await user.keyboard('{ArrowLeft}');
    expect(sizes()).toEqual([40, 60]);
    unmount();
    render(<Split direction="vertical" />);
    expect(sep()).toHaveAttribute('aria-orientation', 'horizontal');
    await user.tab();
    await user.keyboard('{ArrowDown}');
    expect(sizes()).toEqual([35, 65]);
    await user.keyboard('{ArrowRight}');
    expect(sizes()).toEqual([35, 65]);
  });

  it('collapses and restores a collapsible panel with Enter', async () => {
    const user = userEvent.setup();
    render(<Split collapsible />);
    await user.tab();
    await user.keyboard('{ArrowRight}{Enter}');
    expect(sizes()).toEqual([0, 100]);
    expect(panels()[0]).toHaveAttribute('data-collapsed');
    expect(sep()).toHaveAttribute('aria-valuemin', '0');
    await user.keyboard('{Enter}');
    expect(sizes()).toEqual([35, 65]);
    expect(panels()[0]).not.toHaveAttribute('data-collapsed');
  });

  it('keeps a panel collapsed to a rail on screen, and hides one collapsed to nothing', async () => {
    const user = userEvent.setup();
    const { unmount } = render(
      <ResizablePanels aria-label="Layout">
        <ResizablePanel defaultSize={30} minSize={20} collapsible collapsedSize={6}>
          Rail
        </ResizablePanel>
        <ResizableHandle />
        <ResizablePanel>Content</ResizablePanel>
      </ResizablePanels>,
    );
    await user.tab();
    await user.keyboard('{Enter}');
    expect(sizes()).toEqual([6, 94]);
    expect(panels()[0]).toHaveAttribute('data-collapsed');
    expect(panels()[0]).not.toHaveAttribute('hidden');
    unmount();
    render(<Split collapsible />);
    await user.tab();
    await user.keyboard('{Enter}');
    expect(panels()[0]).toHaveAttribute('hidden');
  });

  it('resizes by dragging and reports the layout once on release', () => {
    const onLayout = vi.fn();
    render(<Split onLayout={onLayout} />);
    const s = sep();
    fireEvent.pointerDown(s, { pointerId: 1, button: 0, clientX: 300 });
    fireEvent.pointerMove(s, { pointerId: 1, clientX: 400 });
    expect(sizes()).toEqual([40, 60]);
    expect(s).toHaveAttribute('data-dragging');
    expect(onLayout).not.toHaveBeenCalled();
    fireEvent.pointerMove(s, { pointerId: 1, clientX: 900 });
    expect(sizes()).toEqual([60, 40]);
    fireEvent.pointerUp(s, { pointerId: 1, clientX: 900 });
    expect(onLayout).toHaveBeenCalledTimes(1);
    expect(onLayout).toHaveBeenCalledWith([60, 40]);
    expect(s).not.toHaveAttribute('data-dragging');
  });

  it('collapses when dragged below half the minimum', () => {
    render(<Split collapsible />);
    const s = sep();
    fireEvent.pointerDown(s, { pointerId: 1, button: 0, clientX: 300 });
    fireEvent.pointerMove(s, { pointerId: 1, clientX: 50 });
    expect(sizes()).toEqual([0, 100]);
    fireEvent.pointerUp(s, { pointerId: 1 });
  });

  it('restores saved sizes and supports the shadcn names', () => {
    render(
      <ResizablePanelGroup direction="horizontal" defaultSizes={[1, 3]}>
        <ResizablePanel>A</ResizablePanel>
        <ResizableHandle label="Resize list" />
        <ResizablePanel>B</ResizablePanel>
      </ResizablePanelGroup>,
    );
    expect(sizes()).toEqual([25, 75]);
    expect(screen.getByRole('separator', { name: 'Resize list' })).toBeInTheDocument();
  });

  it('shares space among panels without a default, across three panels', async () => {
    const user = userEvent.setup();
    render(
      <ResizablePanels messages={{ handleLabel: 'Ajustar' }}>
        <ResizablePanel defaultSize={20}>A</ResizablePanel>
        <ResizableHandle />
        <ResizablePanel>B</ResizablePanel>
        <ResizableHandle disabled />
        <ResizablePanel>C</ResizablePanel>
      </ResizablePanels>,
    );
    expect(sizes()).toEqual([20, 40, 40]);
    const [first, second] = screen.getAllByRole('separator', { name: 'Ajustar' });
    expect(second).toHaveAttribute('tabindex', '-1');
    expect(second).toHaveAttribute('aria-disabled', 'true');
    second!.focus();
    await user.keyboard('{ArrowRight}');
    expect(sizes()).toEqual([20, 40, 40]);
    first!.focus();
    await user.keyboard('{ArrowRight}');
    expect(sizes()).toEqual([25, 35, 40]);
  });

  it('marks the narrow-screen stacking option and forwards its ref', () => {
    const ref = { current: null as HTMLDivElement | null };
    render(
      <ResizablePanels ref={ref} stackOnNarrow>
        <ResizablePanel>A</ResizablePanel>
        <ResizableHandle />
        <ResizablePanel>B</ResizablePanel>
      </ResizablePanels>,
    );
    expect(ref.current).toHaveClass('mn-resizable-panels');
    expect(ref.current).toHaveAttribute('data-stack-narrow');
    expect(ref.current).toHaveAttribute('data-direction', 'horizontal');
  });

  it('renders on the server', () => {
    const html = renderToString(<Split />);
    expect(html).toContain('mn-resizable-panels');
    expect(html).toContain('role="separator"');
    expect(html).toContain('aria-valuenow="30"');
  });
});

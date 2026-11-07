import { beforeAll, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { MiniCall } from './index.js';

// jsdom has no PointerEvent; a MouseEvent subclass carries clientX/clientY.
beforeAll(() => {
  if (typeof window.PointerEvent === 'undefined') {
    class PointerEventShim extends MouseEvent {
      pointerId: number;
      constructor(type: string, init: PointerEventInit = {}) {
        super(type, init);
        this.pointerId = init.pointerId ?? 0;
      }
    }
    (window as unknown as { PointerEvent: unknown }).PointerEvent = PointerEventShim;
  }
});

const speaker = { id: 'a', name: 'Robin Park', speaking: true };

describe('MiniCall', () => {
  it('shows the speaker, timer and controls in a named region', async () => {
    const onMicChange = vi.fn();
    const onExpand = vi.fn();
    const onLeave = vi.fn();
    render(
      <MiniCall
        participant={speaker}
        startedAt={Date.now()}
        count={3}
        micOn
        onMicChange={onMicChange}
        onExpand={onExpand}
        onLeave={onLeave}
      />,
    );
    const region = screen.getByRole('region', { name: 'Call (minimised)' });
    expect(region).toHaveAttribute('data-placement', 'floating');
    expect(region).toHaveStyle({ right: '16px', bottom: '16px' });
    expect(screen.getByRole('group', { name: 'Robin Park' })).toBeInTheDocument();
    expect(screen.getByRole('timer')).toBeInTheDocument();
    expect(screen.getByText('3 in call')).toBeInTheDocument();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Mute' }));
    expect(onMicChange).toHaveBeenCalledWith(false);
    await user.click(screen.getByRole('button', { name: 'Expand call' }));
    await user.click(screen.getByRole('button', { name: 'Leave call' }));
    expect(onExpand).toHaveBeenCalledOnce();
    expect(onLeave).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('says it is waiting without a participant', () => {
    render(<MiniCall placement="inline" />);
    expect(screen.getByText('Waiting for others…')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Move/ })).not.toBeInTheDocument();
  });

  it('moves with the arrow keys on its handle and reports the position', async () => {
    const onPositionChange = vi.fn();
    render(<MiniCall participant={speaker} onPositionChange={onPositionChange} />);
    const user = userEvent.setup();
    await user.tab();
    const handle = screen.getByRole('button', { name: 'Move call window. Use the arrow keys.' });
    expect(handle).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(onPositionChange).toHaveBeenLastCalledWith({ right: 40, bottom: 16 });
    await user.keyboard('{ArrowUp}');
    expect(onPositionChange).toHaveBeenLastCalledWith({ right: 40, bottom: 40 });
    expect(screen.getByRole('region')).toHaveStyle({ right: '40px', bottom: '40px' });
    await user.keyboard('{ArrowDown}{ArrowDown}');
    expect(onPositionChange).toHaveBeenLastCalledWith({ right: 40, bottom: 12 });
  });

  it('drags with a pointer and commits on release', () => {
    const onPositionChange = vi.fn();
    render(<MiniCall participant={speaker} onPositionChange={onPositionChange} />);
    const region = screen.getByRole('region');
    fireEvent.pointerDown(region, { button: 0, clientX: 500, clientY: 500, pointerId: 1 });
    fireEvent.pointerMove(region, { clientX: 400, clientY: 450, pointerId: 1 });
    expect(region).toHaveAttribute('data-dragging');
    fireEvent.pointerUp(region, { pointerId: 1 });
    expect(onPositionChange).toHaveBeenCalledWith({ right: 116, bottom: 66 });
    expect(region).not.toHaveAttribute('data-dragging');
  });

  it('expands on double-click but not from its buttons', async () => {
    const onExpand = vi.fn();
    render(<MiniCall participant={speaker} onExpand={onExpand} micOn onMicChange={() => {}} />);
    const user = userEvent.setup();
    await user.dblClick(screen.getByRole('button', { name: 'Mute' }));
    expect(onExpand).not.toHaveBeenCalled();
    await user.dblClick(screen.getByRole('group', { name: 'Robin Park' }));
    expect(onExpand).toHaveBeenCalledOnce();
  });

  it('renders on the server', () => {
    const html = renderToString(<MiniCall participant={speaker} startedAt={0} />);
    expect(html).toContain('mn-mini-call');
  });
});

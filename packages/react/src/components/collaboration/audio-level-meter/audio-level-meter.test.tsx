import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AudioLevelMeter } from './index.js';

const lit = (el: Element) => el.querySelectorAll('[data-on]').length;

function mockReducedMotion(matches: boolean) {
  vi.stubGlobal(
    'matchMedia',
    vi.fn(() => ({ matches, addEventListener: vi.fn(), removeEventListener: vi.fn() })),
  );
  window.matchMedia = globalThis.matchMedia;
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.useRealTimers();
});

describe('AudioLevelMeter', () => {
  it('is a named meter whose segments follow the level', async () => {
    const ref = createRef<HTMLDivElement>();
    const { rerender } = render(<AudioLevelMeter ref={ref} level={0.6} />);
    const meter = screen.getByRole('meter', { name: 'Microphone level' });
    expect(meter).toBe(ref.current);
    expect(meter).toHaveClass('mn-audio-level-meter');
    expect(meter).toHaveAttribute('aria-valuenow', '60');
    expect(meter).toHaveAttribute('aria-valuetext', '60%');
    expect(meter).toHaveAttribute('data-speaking');
    expect(lit(meter)).toBe(3);
    rerender(<AudioLevelMeter level={1.4} segments={8} />);
    expect(meter.children).toHaveLength(8);
    expect(lit(meter)).toBe(8);
    expect(meter).toHaveAttribute('aria-valuenow', '100');
    await expectNoA11yViolations();
  });

  it('shows muted as empty with text, and can be decorative', () => {
    const { rerender, container } = render(<AudioLevelMeter level={0.9} muted />);
    const meter = screen.getByRole('meter');
    expect(meter).toHaveAttribute('aria-valuetext', 'Muted');
    expect(meter).toHaveAttribute('data-muted');
    expect(lit(meter)).toBe(0);
    rerender(<AudioLevelMeter level={0.5} decorative />);
    expect(screen.queryByRole('meter')).toBeNull();
    expect(container.firstElementChild).toHaveAttribute('aria-hidden', 'true');
  });

  it('draws a single bar variant scaled by the level', () => {
    const { container } = render(<AudioLevelMeter variant="bar" level={0.25} />);
    const fill = container.querySelector<HTMLElement>('.mn-audio-level-meter__fill')!;
    expect(fill.style.transform).toBe('scaleX(0.25)');
  });

  it('only shows sound on or off under reduced motion', () => {
    mockReducedMotion(true);
    const { rerender } = render(<AudioLevelMeter level={0.3} />);
    const meter = screen.getByRole('meter');
    expect(meter).toHaveAttribute('data-reduced-motion');
    expect(lit(meter)).toBe(5);
    expect(meter).toHaveAttribute('aria-valuenow', '30');
    rerender(<AudioLevelMeter level={0.02} />);
    expect(lit(meter)).toBe(0);
  });

  it('measures a stream with the Web Audio API and stops when muted', () => {
    vi.useFakeTimers();
    const close = vi.fn(() => Promise.resolve());
    const disconnect = vi.fn();
    class FakeContext {
      createAnalyser() {
        return {
          fftSize: 0,
          getFloatTimeDomainData: (d: Float32Array) => d.fill(0.1),
        };
      }
      createMediaStreamSource() {
        return { connect: vi.fn(), disconnect };
      }
      close = close;
    }
    vi.stubGlobal('AudioContext', FakeContext);
    window.AudioContext = FakeContext as unknown as typeof AudioContext;
    const stream = { getAudioTracks: () => [{}] } as unknown as MediaStream;
    const { rerender } = render(<AudioLevelMeter stream={stream} />);
    act(() => {
      vi.advanceTimersByTime(130);
    });
    expect(screen.getByRole('meter')).toHaveAttribute('aria-valuenow', '60');
    rerender(<AudioLevelMeter stream={stream} muted />);
    expect(disconnect).toHaveBeenCalled();
    expect(close).toHaveBeenCalled();
    expect(screen.getByRole('meter')).toHaveAttribute('aria-valuenow', '0');
  });

  it('translates', () => {
    render(<AudioLevelMeter level={0.5} messages={{ label: 'Niveau du micro' }} />);
    expect(screen.getByRole('meter', { name: 'Niveau du micro' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<AudioLevelMeter level={0.4} />);
    expect(html).toContain('mn-audio-level-meter');
    expect(html).toContain('role="meter"');
  });
});

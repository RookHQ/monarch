import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CallControls } from '../call-controls/index.js';
import {
  CallStage,
  CallTimer,
  callGridColumns,
  chooseCallLayout,
  formatCallDuration,
  type CallParticipant,
} from './index.js';

const people: CallParticipant[] = [
  { id: 'me', name: 'Sam Lee', self: true },
  { id: 'a', name: 'Robin Park', micOff: true },
  { id: 'b', name: 'Alex Chen' },
];

describe('chooseCallLayout', () => {
  const ids = ['me', 'a', 'b'];
  it('uses a grid by default and caps columns', () => {
    expect(chooseCallLayout({ selfId: 'me', ids, mode: 'auto' })).toEqual({
      kind: 'grid',
      ids,
      cols: 2,
    });
    expect([1, 2, 4, 5, 9].map(callGridColumns)).toEqual([1, 2, 2, 3, 3]);
  });
  it('spotlights a pin, then a remote screen, then the speaker', () => {
    expect(chooseCallLayout({ selfId: 'me', ids, mode: 'grid', pinnedId: 'b' })).toMatchObject({
      kind: 'spotlight',
      mainId: 'b',
      strip: ['me', 'a'],
    });
    expect(chooseCallLayout({ selfId: 'me', ids, mode: 'grid', sharerId: 'a' })).toMatchObject({
      mainId: 'a',
    });
    expect(chooseCallLayout({ selfId: 'me', ids, mode: 'grid', sharerId: 'me' }).kind).toBe('grid');
    expect(
      chooseCallLayout({ selfId: 'me', ids, mode: 'speaker', activeSpeakerId: 'b' }),
    ).toMatchObject({ mainId: 'b' });
    expect(
      chooseCallLayout({ selfId: 'me', ids, mode: 'speaker', activeSpeakerId: 'me' }),
    ).toMatchObject({ mainId: 'a' });
  });
  it('shows the other person large in a one-to-one call', () => {
    expect(chooseCallLayout({ selfId: 'me', ids: ['me', 'a'], mode: 'auto' })).toMatchObject({
      kind: 'spotlight',
      mainId: 'a',
      strip: ['me'],
    });
  });
});

describe('formatCallDuration', () => {
  it('formats minutes and hours', () => {
    expect(formatCallDuration(0)).toBe('00:00');
    expect(formatCallDuration(65_000)).toBe('01:05');
    expect(formatCallDuration(3_725_000)).toBe('1:02:05');
    expect(formatCallDuration(-5)).toBe('00:00');
  });
});

describe('CallTimer', () => {
  it('ticks every second as a named timer', () => {
    vi.useFakeTimers();
    try {
      const start = Date.now() - 61_000;
      render(<CallTimer since={start} />);
      const timer = screen.getByRole('timer', { name: 'Call duration' });
      expect(timer).toHaveTextContent('01:01');
      act(() => {
        vi.advanceTimersByTime(2000);
      });
      expect(timer).toHaveTextContent('01:03');
    } finally {
      vi.useRealTimers();
    }
  });
});

describe('CallStage', () => {
  it('renders a titled region with every participant in a grid', async () => {
    render(
      <CallStage
        title="Design review"
        participants={people}
        startedAt={Date.now()}
        controls={<CallControls micOn onMicChange={() => {}} onLeave={() => {}} />}
      />,
    );
    const stage = screen.getByRole('region', { name: 'Design review' });
    expect(stage).toHaveAttribute('data-layout', 'grid');
    expect(within(stage).getByRole('group', { name: 'Sam Lee (you)' })).toBeInTheDocument();
    expect(within(stage).getByRole('group', { name: 'Robin Park' })).toBeInTheDocument();
    expect(screen.getByText('3 in call')).toBeInTheDocument();
    expect(screen.getByRole('timer')).toBeInTheDocument();
    expect(screen.getByRole('toolbar', { name: 'Call controls' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('says it is waiting when you are alone and falls back to a generic name', () => {
    render(<CallStage participants={[people[0]!]} />);
    expect(screen.getByRole('region', { name: 'Call' })).toBeInTheDocument();
    expect(screen.getByText('Waiting for others…')).toBeInTheDocument();
  });

  it('pins a participant from their tile', async () => {
    const onPinnedIdChange = vi.fn();
    const { rerender } = render(
      <CallStage participants={people} onPinnedIdChange={onPinnedIdChange} />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Pin Alex Chen' }));
    expect(onPinnedIdChange).toHaveBeenCalledWith('b');
    rerender(<CallStage participants={people} pinnedId="b" onPinnedIdChange={onPinnedIdChange} />);
    expect(screen.getByRole('region')).toHaveAttribute('data-layout', 'spotlight');
    await user.click(screen.getByRole('button', { name: 'Unpin Alex Chen' }));
    expect(onPinnedIdChange).toHaveBeenLastCalledWith(null);
  });

  it('switches surfaces and hides tile actions when compact', async () => {
    const onSurfaceChange = vi.fn();
    const { rerender } = render(
      <CallStage
        participants={people}
        onSurfaceChange={onSurfaceChange}
        onPinnedIdChange={() => {}}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Shrink call to the corner' }));
    expect(onSurfaceChange).toHaveBeenCalledWith('docked');
    rerender(
      <CallStage
        participants={people}
        surface="docked"
        onSurfaceChange={onSurfaceChange}
        onPinnedIdChange={() => {}}
      />,
    );
    expect(screen.queryByRole('button', { name: /^Pin / })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Minimise call' }));
    expect(onSurfaceChange).toHaveBeenLastCalledWith('mini');
    await user.click(screen.getByRole('button', { name: 'Expand call' }));
    expect(onSurfaceChange).toHaveBeenLastCalledWith('stage');
  });

  it('shows reconnecting, error and muted-speech notices', () => {
    render(
      <CallStage
        participants={people}
        reconnecting
        speakingWhileMuted
        lowData
        error="A connection could not be established."
      />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('A connection could not be established.');
    const statuses = screen.getAllByRole('status').map((s) => s.textContent);
    expect(statuses).toContain('You’re offline. Trying to reconnect…');
    expect(statuses).toContain('You’re muted. Press M or hold Space to talk.');
    expect(screen.getByText('Low data')).toBeInTheDocument();
  });

  it('hands each tile’s video element to onVideoElement', () => {
    const onVideoElement = vi.fn();
    const stream = {
      getVideoTracks: () => [{ readyState: 'live' }],
      getAudioTracks: () => [],
    } as unknown as MediaStream;
    render(
      <CallStage
        participants={[{ id: 'a', name: 'Robin', stream }]}
        onVideoElement={onVideoElement}
      />,
    );
    expect(onVideoElement).toHaveBeenCalledWith('a', expect.any(HTMLVideoElement));
  });

  it('renders on the server', () => {
    const html = renderToString(<CallStage title="Stand-up" participants={people} startedAt={0} />);
    expect(html).toContain('mn-call-stage');
    expect(html).toContain('00:00');
  });
});

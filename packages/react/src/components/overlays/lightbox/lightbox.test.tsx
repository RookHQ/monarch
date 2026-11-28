import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Lightbox, type LightboxItem, type LightboxProps } from './index.js';

const ITEMS: LightboxItem[] = [
  { src: '/media/one.jpg', alt: 'Sketch of the checkout flow', caption: 'First draft' },
  { src: '/media/two.jpg', alt: 'Photo of the team board', downloadUrl: '/media/two-original.jpg' },
  {
    src: '/media/three.mp4',
    alt: 'Screen recording of sign-in',
    kind: 'video',
    tracks: [
      { src: '/media/three.en.vtt', srcLang: 'en', label: 'English' },
      { src: 'javascript:alert(1)', srcLang: 'xx', label: 'Bad' },
    ],
  },
];

function Viewer(props: Partial<LightboxProps>) {
  return (
    <Lightbox items={ITEMS} trigger={<button type="button">Open gallery</button>} {...props} />
  );
}

describe('Lightbox', () => {
  it('opens as a named modal with the image, caption and position; Escape returns focus', async () => {
    render(<Viewer />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Open gallery' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Media viewer' });
    expect(dialog).toHaveClass('mn-lightbox');
    expect(screen.getByRole('img', { name: 'Sketch of the checkout flow' })).toHaveAttribute(
      'src',
      '/media/one.jpg',
    );
    expect(screen.getByText('First draft')).toBeInTheDocument();
    expect(dialog.querySelector('.mn-lightbox__position')).toHaveTextContent(
      '1 of 3: Sketch of the checkout flow',
    );
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(trigger).toHaveFocus();
  });

  it('traps focus inside', async () => {
    render(<Viewer defaultOpen />);
    const user = userEvent.setup();
    const dialog = await screen.findByRole('dialog');
    for (let i = 0; i < 8; i++) {
      await user.tab();
      expect(dialog.contains(document.activeElement)).toBe(true);
    }
  });

  it('moves between items with the buttons and arrow keys, Home and End', async () => {
    const onIndexChange = vi.fn();
    render(<Viewer defaultOpen onIndexChange={onIndexChange} />);
    const user = userEvent.setup();
    await screen.findByRole('dialog');
    const previous = screen.getByRole('button', { name: 'Previous' });
    expect(previous).toHaveAttribute('aria-disabled', 'true');
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByRole('img', { name: 'Photo of the team board' })).toBeInTheDocument();
    expect(onIndexChange).toHaveBeenLastCalledWith(1);
    expect(screen.getByRole('link', { name: 'Download' })).toHaveAttribute(
      'href',
      '/media/two-original.jpg',
    );
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('img', { name: 'Sketch of the checkout flow' })).toBeInTheDocument();
    await user.keyboard('{ArrowLeft}');
    expect(onIndexChange).toHaveBeenCalledTimes(2);
    await user.keyboard('{End}');
    const video = document.querySelector('video')!;
    expect(video).toHaveAttribute('aria-label', 'Screen recording of sign-in');
    const tracks = video.querySelectorAll('track');
    expect(tracks).toHaveLength(1);
    expect(tracks[0]).toHaveAttribute('kind', 'captions');
    expect(screen.getByRole('button', { name: 'Next' })).toHaveAttribute('aria-disabled', 'true');
    expect(screen.queryByRole('button', { name: 'Zoom in' })).toBeNull();
    await user.keyboard('{Home}');
    expect(screen.getByRole('img', { name: 'Sketch of the checkout flow' })).toBeInTheDocument();
  });

  it('wraps around with loop and mirrors arrows in right-to-left', async () => {
    render(
      <div dir="rtl">
        <Viewer defaultOpen loop />
      </div>,
    );
    const user = userEvent.setup();
    await screen.findByRole('dialog');
    await user.click(screen.getByRole('button', { name: 'Previous' }));
    expect(document.querySelector('video')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByRole('img', { name: 'Sketch of the checkout flow' })).toBeInTheDocument();
  });

  it('zooms with buttons and keys, clamps, resets, and resets on a new item', async () => {
    render(<Viewer defaultOpen maxZoom={2} />);
    const user = userEvent.setup();
    const dialog = await screen.findByRole('dialog');
    const zoomIn = screen.getByRole('button', { name: 'Zoom in' });
    const reset = screen.getByRole('button', { name: /^Reset zoom, \d+%$/ });
    expect(reset).toHaveTextContent('100%');
    expect(screen.getByRole('button', { name: 'Zoom out' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    await user.click(zoomIn);
    expect(reset).toHaveTextContent('150%');
    expect(dialog).toHaveAttribute('data-zoomed');
    await user.keyboard('+');
    await user.keyboard('+');
    expect(reset).toHaveTextContent('200%');
    expect(zoomIn).toHaveAttribute('aria-disabled', 'true');
    // While zoomed, arrows scroll instead of changing the item.
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('img', { name: 'Sketch of the checkout flow' })).toBeInTheDocument();
    await user.keyboard('-');
    expect(reset).toHaveTextContent('150%');
    await user.keyboard('0');
    expect(reset).toHaveTextContent('100%');
    expect(dialog).not.toHaveAttribute('data-zoomed');
    await user.click(zoomIn);
    expect(reset).toHaveTextContent('150%');
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByRole('button', { name: /^Reset zoom, \d+%$/ })).toHaveTextContent('100%');
  });

  it('does not load unsafe addresses and translates its labels', async () => {
    render(
      <Lightbox
        defaultOpen
        items={[{ src: 'javascript:alert(1)', alt: 'Bad', downloadUrl: 'javascript:alert(2)' }]}
        messages={{ title: 'Visionneuse', close: 'Fermer', blocked: 'Fichier bloqué' }}
      />,
    );
    const dialog = await screen.findByRole('dialog', { name: 'Visionneuse' });
    expect(dialog.querySelector('img')).toBeNull();
    expect(screen.getByText('Fichier bloqué')).toBeInTheDocument();
    expect(screen.queryByRole('link')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Next' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Fermer' })).toBeInTheDocument();
  });

  it('can be controlled', async () => {
    const onOpenChange = vi.fn();
    const { rerender } = render(<Viewer open={false} index={1} onOpenChange={onOpenChange} />);
    expect(screen.queryByRole('dialog')).toBeNull();
    rerender(<Viewer open index={1} onOpenChange={onOpenChange} />);
    expect(await screen.findByRole('img', { name: 'Photo of the team board' })).toBeInTheDocument();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Close' }));
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('End with no items does not report index -1', async () => {
    const user = userEvent.setup();
    const onIndexChange = vi.fn();
    render(<Lightbox items={[]} open onIndexChange={onIndexChange} />);
    (await screen.findByRole('dialog')).focus();
    await user.keyboard('{End}{Home}');
    expect(onIndexChange).not.toHaveBeenCalled();
  });

  it('renders the trigger on the server', () => {
    const html = renderToString(<Viewer />);
    expect(html).toContain('Open gallery');
    expect(html).toContain('aria-haspopup="dialog"');
  });
});

import { describe, expect, it, vi } from 'vitest';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiImageGeneration, type AiGeneratedImage, type AiImageGenerationProps } from './index.js';

const done: AiGeneratedImage[] = [
  { id: 'a', src: 'https://images.example.com/a.png', alt: 'A lighthouse at dusk' },
  { id: 'b', src: '/generated/b.png', alt: '' },
];

function Controlled(props: Partial<AiImageGenerationProps>) {
  const [images, setImages] = useState(props.images ?? done);
  return (
    <AiImageGeneration
      status="done"
      {...props}
      images={images}
      onAltChange={(id, alt) => {
        props.onAltChange?.(id, alt);
        setImages((list) => list.map((i) => (i.id === id ? { ...i, alt } : i)));
      }}
    />
  );
}

describe('AiImageGeneration', () => {
  it('renders a named group, forwards props and the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiImageGeneration
        ref={ref}
        status="done"
        images={done}
        onAltChange={() => {}}
        data-testid="root"
        className="extra"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-image-generation', 'extra');
    expect(root).toHaveAttribute('data-status', 'done');
    expect(root).not.toHaveAttribute('aria-busy');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'AI generated images' })).toBe(root);
    expect(screen.getByText('2 images generated')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'A lighthouse at dusk' })).toHaveAttribute(
      'src',
      'https://images.example.com/a.png',
    );
    // Empty alt text still gives the image a name.
    expect(screen.getByRole('img', { name: 'Generated image 2' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('marks empty alt text as invalid with an error message', () => {
    render(<AiImageGeneration status="done" images={done} onAltChange={() => {}} />);
    const first = screen.getByRole('textbox', { name: 'Alt text for image 1' });
    const second = screen.getByRole('textbox', { name: 'Alt text for image 2' });
    expect(first).not.toHaveAttribute('aria-invalid', 'true');
    expect(first).toBeRequired();
    expect(second).toHaveAttribute('aria-invalid', 'true');
    expect(second).toHaveAccessibleDescription('Describe the image for people who cannot see it.');
  });

  it('reports alt text changes and clears the error once filled', async () => {
    const user = userEvent.setup();
    const onAltChange = vi.fn();
    render(<Controlled onAltChange={onAltChange} />);
    const second = screen.getByRole('textbox', { name: 'Alt text for image 2' });
    await user.type(second, 'Hills');
    expect(onAltChange).toHaveBeenLastCalledWith('b', 'Hills');
    expect(second).not.toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('img', { name: 'Hills' })).toBeInTheDocument();
  });

  it('select and variations are reachable by keyboard with positional names', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    const onVariations = vi.fn();
    render(
      <AiImageGeneration
        status="done"
        images={done}
        selectedId="a"
        onSelect={onSelect}
        onVariations={onVariations}
        onAltChange={() => {}}
      />,
    );
    expect(screen.getByRole('button', { name: 'Use image 1' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.getByText('Selected')).toBeInTheDocument();
    await user.tab(); // alt 1
    await user.tab(); // use 1
    await user.tab(); // more 1
    expect(screen.getByRole('button', { name: 'More like this, image 1' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onVariations).toHaveBeenCalledWith('a');
    await user.tab(); // alt 2
    await user.tab(); // use 2
    expect(screen.getByRole('button', { name: 'Use image 2' })).toHaveFocus();
    await user.keyboard(' ');
    expect(onSelect).toHaveBeenCalledWith('b');
    await expectNoA11yViolations();
  });

  it('shows placeholders, progress and aria-busy while pending', async () => {
    render(
      <AiImageGeneration
        status="pending"
        progress={0.4}
        images={[
          { id: 'a', alt: '' },
          { id: 'b', alt: '' },
        ]}
        onSelect={() => {}}
        onAltChange={() => {}}
        data-testid="root"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByText('Generating images… 40%')).toBeInTheDocument();
    expect(root.querySelectorAll('.mn-ai-image-generation__placeholder')).toHaveLength(2);
    expect(screen.queryByRole('img')).toBeNull();
    expect(screen.queryByRole('button')).toBeNull();
    // No validation errors before anything exists.
    expect(screen.getByRole('textbox', { name: 'Alt text for image 1' })).toBeDisabled();
    expect(screen.queryByText('Describe the image for people who cannot see it.')).toBeNull();
    await expectNoA11yViolations();
  });

  it('shows refining previews while progressive, without actions', () => {
    const { container } = render(
      <AiImageGeneration
        status="progressive"
        images={done}
        onSelect={() => {}}
        onAltChange={() => {}}
      />,
    );
    expect(screen.getByText('Refining images…')).toBeInTheDocument();
    expect(container.querySelector('[data-state="refining"] img')).not.toBeNull();
    expect(screen.queryByRole('button', { name: /Use image/ })).toBeNull();
  });

  it('refuses unsafe image sources and shows a placeholder instead', () => {
    const { container } = render(
      <AiImageGeneration
        status="done"
        images={[
          { id: 'x', src: 'javascript:alert(1)', alt: 'Script' },
          { id: 'y', src: 'data:image/svg+xml,<svg onload=alert(1)>', alt: 'Data' },
          { id: 'z', src: 'blob:https://app.example.com/1', alt: 'Blob preview' },
        ]}
        onSelect={() => {}}
        onAltChange={() => {}}
      />,
    );
    const imgs = container.querySelectorAll('img');
    expect(imgs).toHaveLength(1);
    expect(imgs[0]).toHaveAttribute('src', 'blob:https://app.example.com/1');
    expect(screen.getAllByText('Preview unavailable')).toHaveLength(2);
    expect(container.innerHTML).not.toContain('javascript:');
    expect(container.innerHTML).not.toContain('data:image');
    // Unsafe images cannot be chosen.
    expect(screen.getAllByRole('button', { name: /Use image/ })).toHaveLength(1);
  });

  it('shows the error as an alert with retry', async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();
    render(
      <AiImageGeneration status="error" images={[]} onRetry={onRetry} onAltChange={() => {}} />,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('The images could not be generated.');
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('announces completion politely once', () => {
    const { rerender, container } = render(
      <AiImageGeneration status="pending" images={[{ id: 'a', alt: '' }]} onAltChange={() => {}} />,
    );
    const live = container.querySelector('[aria-live="polite"]');
    expect(live).toHaveTextContent('');
    act(() => {
      rerender(
        <AiImageGeneration
          status="done"
          images={[{ id: 'a', src: '/a.png', alt: 'Bridge' }]}
          onAltChange={() => {}}
        />,
      );
    });
    expect(live).toHaveTextContent('1 image generated.');
  });

  it('does not announce when first rendered as done, and localises strings', () => {
    const { container } = render(
      <AiImageGeneration
        status="done"
        images={done}
        onAltChange={() => {}}
        messages={{ done: (n) => `${n} fertig`, altLabel: (i) => `Alternativtext ${i}` }}
      />,
    );
    expect(container.querySelector('[aria-live="polite"]')).toHaveTextContent('');
    expect(screen.getByText('2 fertig')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Alternativtext 1' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiImageGeneration status="pending" images={[{ id: 'a', alt: '' }]} onAltChange={() => {}} />,
    );
    expect(html).toContain('mn-ai-image-generation');
    expect(html).toContain('aria-busy="true"');
    expect(
      renderToString(<AiImageGeneration status="done" images={done} onAltChange={() => {}} />),
    ).toContain('A lighthouse at dusk');
  });
});

import { useEffect, useRef, useState } from 'react';
import {
  AiImageGeneration,
  type AiGeneratedImage,
  type AiImageGenerationStatus,
} from '@rookcom/monarch-react/ai';
import { Button } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** Neutral placeholder art: monochrome shapes as local `blob:` URLs, so nothing is hotlinked. */
function useDemoArt(count: number): string[] {
  const [urls, setUrls] = useState<string[]>([]);
  useEffect(() => {
    if (typeof URL === 'undefined' || !URL.createObjectURL) return;
    const made = Array.from({ length: count }, (_, i) => {
      const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120"><rect width="120" height="120" fill-opacity="0.08"/><circle cx="${36 + i * 16}" cy="44" r="18" fill-opacity="0.35"/><path d="M0 120 L${40 + i * 10} ${62 + i * 4} L120 120 Z" fill-opacity="0.5"/></svg>`;
      return URL.createObjectURL(new Blob([svg], { type: 'image/svg+xml' }));
    });
    setUrls(made);
    return () => made.forEach((u) => URL.revokeObjectURL(u));
  }, [count]);
  return urls;
}

function GenerationDemo() {
  const art = useDemoArt(4);
  const [status, setStatus] = useState<AiImageGenerationStatus>('done');
  const [progress, setProgress] = useState<number | undefined>(undefined);
  const [selectedId, setSelectedId] = useState<string | undefined>(undefined);
  const [alts, setAlts] = useState<Record<string, string>>({
    a: 'Abstract hills under a round sun',
    b: '',
    c: 'Two overlapping shapes on a pale ground',
    d: '',
  });
  const timers = useRef<number[]>([]);
  useEffect(() => () => timers.current.forEach((t) => window.clearTimeout(t)), []);

  const images: AiGeneratedImage[] = ['a', 'b', 'c', 'd'].map((id, i) => ({
    id,
    src: status === 'pending' ? undefined : art[i],
    alt: alts[id] ?? '',
  }));

  const generate = () => {
    setStatus('pending');
    setProgress(0);
    setSelectedId(undefined);
    const steps: [number, AiImageGenerationStatus, number | undefined][] = [
      [600, 'pending', 0.3],
      [1200, 'progressive', 0.6],
      [1800, 'progressive', 0.9],
      [2400, 'done', undefined],
    ];
    timers.current = steps.map(([ms, s, p]) =>
      window.setTimeout(() => {
        setStatus(s);
        setProgress(p);
      }, ms),
    );
  };

  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', maxWidth: '40rem' }}>
      <div>
        <Button size="sm" onClick={generate} disabled={status === 'pending'}>
          Generate again
        </Button>
      </div>
      <AiImageGeneration
        status={status}
        progress={progress}
        images={images}
        selectedId={selectedId}
        onSelect={setSelectedId}
        onVariations={generate}
        onAltChange={(id, alt) => setAlts((prev) => ({ ...prev, [id]: alt }))}
      />
    </div>
  );
}

export default {
  id: 'ai-image-generation',
  title: 'AiImageGeneration',
  summary:
    'AI-generated images as a variations grid: placeholders and progress while generating, then a required alt text field and actions per image.',
  sections: {
    purpose: {
      summary:
        'Shows the result of an image generation request from start to finish: waiting, refining previews, the final variations and failure. Every image needs alt text before it is used, so generated images stay accessible wherever they end up.',
      useWhen: [
        'A person asks an assistant to generate images and picks one (or asks for more like one).',
        'Generated previews arrive progressively and should replace placeholders in place.',
        'The chosen image will be inserted into a document or record and needs alt text.',
      ],
      avoidWhen: [
        'The images were uploaded by a person: use FileUpload or UploadQueue.',
        'The image is a finished artifact in a conversation: use AiArtifactCard and AiArtifactCanvas.',
        'You need a general image gallery: this component is for generation results only.',
      ],
    },
    designPrinciples: {
      items: [
        'Alt text is part of the result, not an afterthought: each image has a labelled, required field, invalid while empty.',
        'Placeholders hold the final layout, so nothing shifts when images arrive.',
        'Model output is untrusted: only http(s), same-origin and `blob:` sources are loaded; anything else shows "Preview unavailable".',
        'Progress is text first; the bar repeats it visually.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root, which is `role="group"` named by `messages.label`.',
        'Images are controlled: `onAltChange(id, alt)` reports typing and you update `alt` in `images`.',
        '`onRetry` and `error` customise the error state; `selectedId` marks the chosen image.',
        'Image sources are checked with the same `isSafeImageSrc` rule the rich text editor uses.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'No visual variants. The grid fills its container, with as many 10rem columns as fit.',
      ],
    },
    states: {
      items: [
        {
          name: 'Pending',
          attribute: 'data-status="pending"',
          description:
            'Placeholder tiles (a shimmer, static under reduced motion), "Generating images…" with the percentage when `progress` is set, alt fields disabled, `aria-busy="true"`.',
        },
        {
          name: 'Progressive',
          attribute: 'data-status="progressive"',
          description:
            'Blurred previews refine in place; "Refining images…"; alt text can already be written; still `aria-busy`.',
        },
        {
          name: 'Done',
          attribute: 'data-status="done"',
          description:
            'Final images with "Use image" and "More like this"; "N images generated" is announced politely once.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: 'An alert with the error text and an optional "Try again" button.',
        },
        {
          name: 'Selected',
          attribute: 'data-selected (item)',
          description:
            'The image whose id is `selectedId`: an AI-coloured ring and a "Selected" tag.',
        },
        {
          name: 'Missing alt text',
          attribute: 'aria-invalid="true" (field)',
          description: 'Once an image exists, an empty alt field is invalid and shows why.',
        },
        {
          name: 'Unavailable',
          attribute: 'data-state="placeholder"',
          description:
            'A finished slot with no source, or a source that is not safe to load, shows "Preview unavailable" and has no actions.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing in an alt field calls `onAltChange`; the image’s accessible name follows the alt text.',
        '"Use image" calls `onSelect(id)` and is pressed on the selected image.',
        '"More like this" calls `onVariations(id)`; the app starts a new generation.',
        'Actions appear only when the generation is done and the image loaded from a safe source.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through each image’s alt field, then its actions, in order.',
        },
        {
          keys: 'Enter / Space',
          action: 'Activates "Use image", "More like this" or "Try again".',
        },
      ],
    },
    accessibility: {
      role: 'group',
      name: '"AI generated images" (`messages.label`), which also marks the region as AI-generated',
      items: [
        'The root is `aria-busy` while pending or refining; completion is announced in a polite live region, never on first render.',
        'Each image is named by its alt text, or "Generated image N" until it has one.',
        'Alt fields are labelled per image, required, and describe their error.',
        'Action buttons keep their visible text and add the image position ("Use image 2", "More like this, image 2").',
        'Errors use `role="alert"`. Placeholders and the progress bar are hidden from assistive technology; the status text carries the progress.',
      ],
    },
    responsive: {
      items: [
        'The grid uses `auto-fill` columns of at least 10rem (or the full width), so one column at 390px with no horizontal scroll.',
        'Action buttons wrap and are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Tiles use `--mn-color-bg-muted` and `--mn-color-border-subtle`; the shimmer and progress fill use the AI tokens.',
        'The selected ring and tag use `--mn-color-ai-solid` / `--mn-color-ai-on-solid`; the error uses the danger tokens.',
        'Reduced motion removes the shimmer and transitions; forced colours outline tiles, use Highlight for the selection and CanvasText for progress.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Generate, refine and choose',
          stack: true,
          render: () => <GenerationDemo />,
        },
        {
          title: 'Pending with progress',
          stack: true,
          render: () => (
            <AiImageGeneration
              status="pending"
              progress={0.35}
              images={[
                { id: '1', alt: '' },
                { id: '2', alt: '' },
              ]}
              onAltChange={() => {}}
              style={{ maxWidth: '24rem' }}
            />
          ),
        },
        {
          title: 'Error with retry',
          stack: true,
          render: () => (
            <AiImageGeneration
              status="error"
              images={[]}
              error="The image service did not respond."
              onRetry={() => {}}
              onAltChange={() => {}}
            />
          ),
        },
        {
          title: 'Unsafe source refused',
          stack: true,
          render: () => (
            <AiImageGeneration
              status="done"
              images={[{ id: 'x', src: 'javascript:void(0)', alt: 'Diagram of the plan' }]}
              onSelect={() => {}}
              onAltChange={() => {}}
              style={{ maxWidth: '12rem' }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Require alt text before inserting a generated image anywhere.',
        'Pass `progress` when the service reports it.',
        'Keep the same image ids from pending to done so tiles update in place.',
      ],
      dont: [
        'Do not pass `data:` or other unchecked URLs from model output; they are refused.',
        'Do not insert an image automatically: let the person choose with "Use image".',
        'Do not hide the error; offer `onRetry` when trying again can help.',
      ],
    },
    related: {
      items: [
        { id: 'ai-artifact-card', reason: 'A generated artifact in a conversation.' },
        { id: 'ai-artifact-canvas', reason: 'Opens and versions a generated artifact.' },
        { id: 'ai-generated-badge', reason: 'Marks content as AI-made after it is inserted.' },
        { id: 'file-upload', reason: 'Images a person uploads rather than generates.' },
        { id: 'progress-bar', reason: 'A standalone progress bar for other tasks.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'form-field', 'text-field'] },
    testing: {
      unit: [
        'Named group, prop and ref forwarding; images named by alt text with a fallback.',
        'Empty alt text is invalid with a described error; typing reports changes and clears it.',
        'Use image and More like this are keyboard reachable with positional names and pressed state.',
        'Pending placeholders, progress text, disabled fields and aria-busy; progressive previews without actions.',
        'javascript: and data: sources are refused and show a placeholder; blob: is allowed.',
        'Error alert with retry; polite completion announcement only on transition; localised strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

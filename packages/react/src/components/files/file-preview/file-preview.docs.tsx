import { useEffect, useState } from 'react';
import { FilePreview } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SAMPLE_TEXT = `Release checklist

1. Freeze the branch
2. Run the full test suite
3. Write the change notes
4. Tag and publish`;

const SAMPLE_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 180"><rect width="320" height="180" fill="lightsteelblue"/><circle cx="250" cy="50" r="24" fill="gold"/><path d="M0 140 90 70l60 50 50-30 120 90H0z" fill="seagreen"/></svg>`;

/** Previews a local file the way an app previews a picked file: through a blob: URL. */
function ImagePreviewDemo() {
  const [src, setSrc] = useState<string>();
  useEffect(() => {
    const url = URL.createObjectURL(new Blob([SAMPLE_SVG], { type: 'image/svg+xml' }));
    setSrc(url);
    return () => URL.revokeObjectURL(url);
  }, []);
  return (
    <div style={{ width: '100%', maxWidth: 480 }}>
      <FilePreview
        name="landscape.svg"
        contentType="image/svg+xml"
        src={src}
        alt="Hills under a sun"
      />
    </div>
  );
}

export default {
  id: 'file-preview',
  title: 'FilePreview',
  summary:
    'Shows a file’s content in place (image, PDF, text, video, audio) with a download fallback.',
  sections: {
    purpose: {
      summary:
        'Lets people look at a file without leaving the product: pictures, PDFs in the browser’s own viewer, text and code, video and audio. Anything else, or anything that fails, gets a clear message and a download link. Needed by: Rook Atlas and Rook Intellect file detail panels, Rook Tickets attachments and Rook Chat shared files.',
      useWhen: [
        'A file detail panel, side sheet or Lightbox should show the content.',
        'An attachment can be opened in place instead of downloaded.',
      ],
      avoidWhen: [
        'Galleries of many images with next and previous: use Lightbox.',
        'Office documents that need a converter: render the converted PDF or image instead, or rely on the download fallback.',
      ],
    },
    designPrinciples: {
      items: [
        'Use the browser’s own viewers (img, object, video, audio) instead of shipping one.',
        'Never run the file: text from a URL is shown in a frame with an empty `sandbox`, PDFs in an `<object>`, and only http(s), blob and relative URLs load.',
        'Every path ends somewhere useful: a failure or an unsupported type shows the type, a message and a download.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`filePreviewKind({ name, type })` returns the kind the component would choose.',
        'For text and code, pass `text` when you already have the content (it is rendered as text, never HTML); otherwise `src` loads in a sandboxed frame.',
        'The ref is the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from app previews (`<img>`/`<iframe src={signedUrl}>` in file dialogs): pass the signed URL as `src`, the stored name and content type as `name` and `contentType`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'kind',
          values: ['image', 'pdf', 'text', 'video', 'audio', 'none'],
          description:
            'Detected from `contentType` and `name`; set to override. `none` always shows the fallback.',
        },
        {
          prop: 'alwaysShowDownload',
          values: ['true', 'false'],
          description: 'Adds a download bar under every preview.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Kind',
          attribute: 'data-kind',
          description: 'What is shown, including `none` for the fallback.',
        },
        {
          name: 'Failed',
          attribute: 'data-failed',
          description: 'An image, video or audio failed to load; the fallback says so.',
        },
      ],
    },
    interaction: {
      items: [
        'Video and audio use the browser controls; PDFs use the browser viewer’s own toolbar.',
        'The download link downloads with the file’s name when the URL is same-origin or a blob.',
        'Where the browser has no PDF viewer (most phones) the download link shows inside the object.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves into the text area, media controls, PDF viewer or download link.',
        },
        {
          keys: 'Arrow keys / Page Up / Page Down',
          action: 'Scroll the text preview when it has focus.',
        },
      ],
    },
    accessibility: {
      role: 'img, object, iframe, video or audio (as appropriate)',
      name: 'Images use `alt` (default: the file name); frames and media are named “Preview of {name}”.',
      items: [
        'The text preview is a focusable, labelled `<pre>` so keyboard users can scroll it.',
        'Videos take WebVTT captions with `captionsSrc`; provide a transcript when there are none.',
        'The download link is named “Download {name}”.',
      ],
    },
    responsive: {
      items: [
        'Content fills the width and is capped at 70% of the viewport height.',
        'Images keep their ratio (`object-fit: contain`).',
      ],
    },
    theme: {
      items: [
        'The frame is `--mn-color-bg-subtle` with a `--mn-color-border-default` border; text sits on `--mn-color-bg-surface` in `--mn-font-mono`.',
        'Forced colours: a `CanvasText` border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Image from a picked file',
          stack: true,
          render: () => <ImagePreviewDemo />,
        },
        {
          title: 'Text with a download bar',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 480 }}>
              <FilePreview
                name="checklist.txt"
                text={SAMPLE_TEXT}
                downloadHref="#"
                alwaysShowDownload
              />
            </div>
          ),
        },
        {
          title: 'No preview available',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 480 }}>
              <FilePreview name="minutes.docx" src="#" />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `contentType` from storage; names alone can be wrong.',
        'Give images meaningful `alt` when the picture is the content.',
      ],
      dont: [
        'Do not render untrusted HTML or SVG files in a frame with scripts; this component never does.',
        'Do not hide the download when the preview may not be enough.',
      ],
    },
    related: {
      items: [
        { id: 'lightbox', reason: 'Full-screen viewing of image sets.' },
        { id: 'file-card', reason: 'Opens into a preview.' },
        { id: 'file-type-icon', reason: 'The icon in the fallback.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'file-type-icon'] },
    testing: {
      unit: [
        'Kind detection for every family.',
        'Image with alt and fallback on error with download.',
        'PDF object with the download inside.',
        'Text as a focusable pre or a sandboxed frame.',
        'Video with captions and audio controls.',
        'Unsupported types, unsafe URLs and custom fallback; forced kind, download bar and translated text; axe; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

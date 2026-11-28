import { useEffect, useState } from 'react';
import { AvatarUpload } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './avatar-upload.fixture.json';

/** Keeps the cropped picture in a blob: URL, as an app would until the upload finishes. */
function ProfilePhoto({ shape }: { shape?: 'circle' | 'square' }) {
  const [src, setSrc] = useState<string | null>(null);
  useEffect(
    () => () => {
      if (src) URL.revokeObjectURL(src);
    },
    [src],
  );
  return (
    <AvatarUpload
      name={shape === 'square' ? 'Northwind Studio' : fixture.data.name}
      shape={shape}
      src={src}
      maxSize={fixture.data.maxSize}
      onChange={(blob) => setSrc(URL.createObjectURL(blob))}
      onRemove={() => setSrc(null)}
    />
  );
}

export default {
  id: 'avatar-upload',
  title: 'AvatarUpload',
  summary:
    'Pick a profile or workspace picture and crop it to a circle or square, by pointer or keyboard.',
  sections: {
    purpose: {
      summary:
        'Sets a person’s or workspace’s picture: choose an image, position and zoom it inside a circle (or square), and hand a square, resized image to the product. Needed by: Rook ID (`components/WorkspaceAvatarUpload.tsx`, `app/profile/page.tsx`, workspace settings) and every product’s profile settings.',
      useWhen: [
        'Profile, team or workspace settings let people set a picture.',
        'A square, consistently sized image is needed whatever the original shape.',
      ],
      avoidWhen: [
        'General image uploads (banners, attachments): use FileUpload.',
        'Choosing from existing pictures: use a picker of those images.',
      ],
    },
    designPrinciples: {
      items: [
        'The crop is fully usable without a pointer: arrow keys move, plus and minus or the slider zoom, Enter saves, Escape cancels.',
        'The picture always fills the crop shape: positions and zoom are clamped so no empty edges are saved.',
        'The output is predictable: a square image of `outputSize` pixels in `outputType`, drawn with a canvas and no crop library.',
        'The component never uploads; `onChange` gets the image and may return a promise to show progress and failure.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`onChange(blob, file, crop)`: the cropped image, the original file and the crop square in the original’s pixels (for servers that crop themselves). Where canvas is unavailable `blob` is the original file.',
        '`avatarCropRect(natural, view)` is the pure maths, exported for server-side cropping or tests.',
        'Files are checked with `validateFiles` (FileUpload) against `accept` and `maxSize`.',
        'The ref is the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Rook ID `WorkspaceAvatarUpload`: `ACCEPTED`/`MAX_SIZE` → `accept`/`maxSize`; the upload call moves into `onChange` (return its promise); the delete AlertDialog stays in the product and calls the removal from `onRemove`; toasts for type and size errors are replaced by the inline alert.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'shape',
          values: ['circle', 'square'],
          description:
            'Crop mask and the Avatar shape: circles for people, squares for workspaces.',
        },
        {
          prop: 'outputType',
          values: ['image/png', 'image/jpeg', 'image/webp'],
          description: 'Format of the produced image.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Editing',
          attribute: 'data-editing',
          description: 'A picture was picked; the crop editor replaces the buttons.',
        },
        { name: 'Shape', attribute: 'data-shape', description: '`circle` or `square`.' },
        {
          name: 'Saving',
          description: 'The Save button is busy while the returned promise is pending.',
        },
        {
          name: 'Error',
          description: 'A refused file or failed save shows an alert under the controls.',
        },
      ],
    },
    interaction: {
      items: [
        'Upload (or Change) opens the file picker; a valid image opens the crop editor with focus in the crop area.',
        'Dragging the picture moves it; the zoom slider scales it from 100% to 400%.',
        'Save draws the crop and calls `onChange`; on success the editor closes and focus returns to the Change button. Cancel closes without saving.',
        'Remove calls `onRemove`; confirm in the product if the picture cannot be restored.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Arrow keys', action: 'Move the picture (Shift moves further).' },
        { keys: '+ / -', action: 'Zoom in or out.' },
        { keys: 'Home', action: 'Reset position and zoom.' },
        { keys: 'Enter', action: 'Save the crop.' },
        { keys: 'Escape', action: 'Cancel the crop.' },
        { keys: 'Arrow keys (on the slider)', action: 'Change the zoom.' },
      ],
    },
    accessibility: {
      role: 'group (crop area, role description “crop area”); slider (zoom); buttons',
      name: '“Photo position”, described by instructions; the slider is labelled “Zoom” and reports a percentage.',
      items: [
        'Refusals and save failures are announced through `role="alert"` and describe the upload button.',
        'The current picture uses Avatar, named by `name`.',
        'The editor moves focus into the crop area and returns it to the Change button when it closes.',
      ],
    },
    responsive: {
      items: [
        'The crop area is square and up to 288px wide, shrinking with its container.',
        'Buttons wrap under the picture on narrow screens; the slider is 44px tall on coarse pointers.',
        'Touch dragging pans the picture (`touch-action: none` on the crop area).',
      ],
    },
    theme: {
      items: [
        'The area outside the crop is dimmed with `--mn-color-overlay-scrim`; the crop edge is `--mn-color-bg-surface`.',
        'Forced colours: the crop edge is `Highlight` and the area has a `CanvasText` border.',
      ],
    },
    examples: {
      items: [
        { title: 'Profile photo', render: () => <ProfilePhoto /> },
        { title: 'Workspace logo', render: () => <ProfilePhoto shape="square" /> },
      ],
    },
    doDont: {
      do: [
        'Return the upload promise from `onChange` so failures show in place.',
        'Pick `outputSize` from the largest size the picture is shown at, times two for sharp screens.',
      ],
      dont: [
        'Do not upload the original file when the crop is what people approved.',
        'Do not remove a picture without a way back; confirm or offer undo.',
      ],
    },
    related: {
      items: [
        { id: 'avatar', reason: 'Shows the picture everywhere else.' },
        { id: 'file-upload', reason: 'General uploads; shares `validateFiles`.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['avatar', 'button', 'file-upload'] },
    testing: {
      unit: [
        'Crop maths for wide images and zoom.',
        'Current picture, change from the keyboard, remove.',
        'Refused types announced and describing the button.',
        'Keyboard pan with clamping, slider and key zoom, reset, save with the right canvas draw, focus return and URL revoke.',
        'Pointer drag pans and stops on release.',
        'Enter saves, failure keeps the editor open with an alert, Escape cancels; translated and disabled; axe; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;

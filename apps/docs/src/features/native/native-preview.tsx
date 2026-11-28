import type { Previews } from '../../../plugins/native.js';
import { PLATFORM_NAMES, type Platform } from './labels.js';

/**
 * A rendered image of the native component, when the build found one at
 * `platforms/<platform>/previews/<id>.<theme>.png`. With both themes, the one
 * matching the site theme is shown (native.css); the other is never fetched,
 * because a hidden lazy image is not loaded. Without an image this renders
 * nothing, or, with `fallback`, one line of text saying there is no image yet:
 * never a placeholder picture and never a drawing of native UI.
 */
export function NativePreview({
  platform,
  title,
  previews,
  fallback = false,
}: {
  platform: Platform;
  title: string;
  previews: Previews;
  /** Say so in text when there is no image (a component page; a gallery just leaves the item out). */
  fallback?: boolean;
}) {
  const themes = (['light', 'dark'] as const).filter((t) => previews[t]);
  if (themes.length === 0) {
    return fallback ? (
      <p className="docs-native-preview-missing docs-text">
        No preview image of {title} on {PLATFORM_NAMES[platform]} is published yet. The examples and
        API below are complete.
      </p>
    ) : null;
  }
  return (
    <div className="docs-native-preview" data-themes={themes.length === 2 ? 'both' : 'one'}>
      {themes.map((theme) => {
        const image = previews[theme];
        return image ? (
          <img
            key={theme}
            className="docs-native-preview__img"
            data-theme={theme}
            src={image.src}
            width={image.width}
            height={image.height}
            loading="lazy"
            decoding="async"
            alt={`${title} rendered on ${PLATFORM_NAMES[platform]} in the ${theme} theme`}
          />
        ) : null;
      })}
    </div>
  );
}

/** Typed view of `@rookcom/monarch-tokens/tokens.json` for the foundations pages. */
import tokens from '@rookcom/monarch-tokens/tokens.json';

type Dim = { $type: string; $value: string | number };
export const T = tokens as unknown as {
  font: {
    family: Record<string, Dim>;
    size: Record<string, Dim>;
    weight: Record<string, Dim>;
  };
  space: Record<string, Dim>;
  radius: Record<string, Dim>;
  duration: Record<string, Dim>;
  themes: Record<string, unknown>;
};

/** `0.5` → `0-5`, as in `--mn-space-0-5`. */
export const cssKey = (k: string) => k.replace('.', '-');

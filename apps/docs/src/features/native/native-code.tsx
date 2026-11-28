import { CodeBlock } from '../code/code-block.js';
import { highlightNative } from '../code/highlight-native.js';
import type { Platform } from './labels.js';

const kotlin = (code: string) => highlightNative(code, 'kotlin');
const swift = (code: string) => highlightNative(code, 'swift');

/** A code block of the platform's language: Kotlin on Android, Swift on iOS. */
export function NativeCode({
  platform,
  code,
  label,
}: {
  platform: Platform;
  code: string;
  /** Accessible name of the region, unique on the page. */
  label: string;
}) {
  return platform === 'android' ? (
    <CodeBlock code={code} label={label} lang="kotlin" tokenize={kotlin} />
  ) : (
    <CodeBlock code={code} label={label} lang="swift" tokenize={swift} />
  );
}

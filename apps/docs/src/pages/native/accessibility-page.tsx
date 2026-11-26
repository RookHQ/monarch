import guide from 'virtual:monarch-native-guide/accessibility';
import { NativeGuide } from '../../features/native/native-guide.js';

/** `/native/accessibility/`, rendered at build time from the repository's Markdown (plugins/native.ts). */
export function AccessibilityPage() {
  return <NativeGuide guide={guide} />;
}

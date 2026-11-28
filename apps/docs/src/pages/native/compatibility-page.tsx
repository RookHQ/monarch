import guide from 'virtual:monarch-native-guide/compatibility';
import { NativeGuide } from '../../features/native/native-guide.js';

/** `/native/compatibility/`, rendered at build time from the repository's Markdown (plugins/native.ts). */
export function CompatibilityPage() {
  return <NativeGuide guide={guide} />;
}

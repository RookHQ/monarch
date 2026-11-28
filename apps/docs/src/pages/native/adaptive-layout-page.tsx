import guide from 'virtual:monarch-native-guide/adaptive-layout';
import { NativeGuide } from '../../features/native/native-guide.js';

/** `/native/adaptive-layout/`, rendered at build time from the repository's Markdown (plugins/native.ts). */
export function AdaptiveLayoutPage() {
  return <NativeGuide guide={guide} />;
}

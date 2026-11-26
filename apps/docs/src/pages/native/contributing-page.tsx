import guide from 'virtual:monarch-native-guide/contributing';
import { NativeGuide } from '../../features/native/native-guide.js';

/** `/native/contributing/`, rendered at build time from the repository's Markdown (plugins/native.ts). */
export function ContributingPage() {
  return <NativeGuide guide={guide} />;
}

import nativeFacts from 'virtual:monarch-native-facts';
import android from 'virtual:monarch-native-platform/android';
import { PlatformLanding } from '../../features/native/platform-landing.js';

/**
 * `/android/`: the landing page of Monarch for Android. The long form is
 * `/android/get-started/` and `/android/guide/`.
 */
export function AndroidPage() {
  return <PlatformLanding platform="android" facts={nativeFacts.android} data={android.landing} />;
}

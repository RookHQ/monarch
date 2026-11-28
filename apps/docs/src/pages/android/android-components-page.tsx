import nativeFacts from 'virtual:monarch-native-facts';
import rows from 'virtual:monarch-native-catalogue/android';
import { NativeCatalogue } from '../../features/native/catalogue.js';

/** `/android/components/`: the Android inventory and plan, from `platforms/android`. */
export function AndroidComponentsPage() {
  return <NativeCatalogue platform="android" rows={rows} facts={nativeFacts.android} />;
}

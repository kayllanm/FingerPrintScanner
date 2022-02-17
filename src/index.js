import authenticate from './authenticate';
import isSensorAvailable from './isSensorAvailable';
import hasBiometricSettingsChanged from './hasBiometricSettingsChanged';
import release from './release';

export default {
  authenticate,
  release,
  isSensorAvailable,
  hasBiometricSettingsChanged,
};

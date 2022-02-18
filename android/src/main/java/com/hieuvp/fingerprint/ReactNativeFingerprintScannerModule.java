package com.hieuvp.fingerprint;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt.PromptInfo;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.fragment.app.FragmentActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.bridge.UiThreadUtil;

// for Samsung/MeiZu compat, Android v16-23
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;
import com.wei.android.lib.fingerprintidentify.FingerprintIdentify;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint.ExceptionListener;
import com.wei.android.lib.fingerprintidentify.base.BaseFingerprint.IdentifyListener;


@ReactModule(name="ReactNativeFingerprintScanner")
public class ReactNativeFingerprintScannerModule
        extends ReactContextBaseJavaModule
        implements LifecycleEventListener
{
    public static final int MAX_AVAILABLE_TIMES = Integer.MAX_VALUE;
    public static final String TYPE_BIOMETRICS = "Biometrics";
    public static final String TYPE_FINGERPRINT_LEGACY = "Fingerprint";

    private final ReactApplicationContext mReactContext;
    private BiometricPrompt biometricPrompt;
    private final String LAST_KEY_ID = "LAST_KEY_ID";
    private SharedPreferences spref;

    // for Samsung/MeiZu compat, Android v16-23
    private FingerprintIdentify mFingerprintIdentify;

    public ReactNativeFingerprintScannerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
    }

    @Override
    public String getName() {
        return "ReactNativeFingerprintScanner";
    }

    @Override
    public void onHostResume() {
    }

    @Override
    public void onHostPause() {
    }

    @Override
    public void onHostDestroy() {
        this.release();
    }

    private int currentAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }

    private boolean requiresLegacyAuthentication() {
        return currentAndroidVersion() < 23;
    }

    public class AuthCallback extends BiometricPrompt.AuthenticationCallback {
        private Promise promise;

        public AuthCallback(final Promise promise) {
            super();
            this.promise = promise;
        }

        @Override
        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            this.promise.reject(biometricPromptErrName(errorCode), TYPE_BIOMETRICS);
        }

        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            this.promise.resolve(true);
        }
    }

    public BiometricPrompt getBiometricPrompt(final FragmentActivity fragmentActivity, final Promise promise) {
        // memoize so can be accessed to cancel
        if (biometricPrompt != null) {
            return biometricPrompt;
        }

        // listen for onHost* methods
        mReactContext.addLifecycleEventListener(this);

        AuthCallback authCallback = new AuthCallback(promise);
        Executor executor = Executors.newSingleThreadExecutor();
        biometricPrompt = new BiometricPrompt(
            fragmentActivity,
            executor,
            authCallback
        );

        return biometricPrompt;
    }

    private void biometricAuthenticate(final String title, final String subtitle, final String description, final String cancelButton, final Promise promise) {
        UiThreadUtil.runOnUiThread(
            new Runnable() {
                @Override
                public void run() {
                    FragmentActivity fragmentActivity = (FragmentActivity) mReactContext.getCurrentActivity();

                    if(fragmentActivity == null) return;

                    BiometricPrompt bioPrompt = getBiometricPrompt(fragmentActivity, promise);

                    PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                        .setDeviceCredentialAllowed(false)
                        .setConfirmationRequired(false)
                        .setNegativeButtonText(cancelButton)
                        .setDescription(description)
                        .setSubtitle(subtitle)
                        .setTitle(title)
                        .build();

                    bioPrompt.authenticate(promptInfo);
                }
            });

    }
    // the below constants are consistent across BiometricPrompt and BiometricManager
    private String biometricPromptErrName(int errCode) {
        switch (errCode) {
            case BiometricPrompt.ERROR_CANCELED:
                return "SystemCancel";
            case BiometricPrompt.ERROR_HW_NOT_PRESENT:
                return "FingerprintScannerNotSupported";
            case BiometricPrompt.ERROR_HW_UNAVAILABLE:
                return "FingerprintScannerNotAvailable";
            case BiometricPrompt.ERROR_LOCKOUT:
                return "DeviceLocked";
            case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                return "DeviceLockedPermanent";
            case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
                return "UserCancel";
            case BiometricPrompt.ERROR_NO_BIOMETRICS:
                return "FingerprintScannerNotEnrolled";
            case BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL:
                return "PasscodeNotSet";
            case BiometricPrompt.ERROR_NO_SPACE:
                return "DeviceOutOfMemory";
            case BiometricPrompt.ERROR_TIMEOUT:
                return "AuthenticationTimeout";
            case BiometricPrompt.ERROR_UNABLE_TO_PROCESS:
                return "AuthenticationProcessFailed";
            case BiometricPrompt.ERROR_USER_CANCELED:  // actually 'user elected another auth method'
                return "UserFallback";
            case BiometricPrompt.ERROR_VENDOR:
                // hardware-specific error codes
                return "HardwareError";
            default:
                return "FingerprintScannerUnknownError";
        }
    }

    private String getSensorError() {
        BiometricManager biometricManager = BiometricManager.from(mReactContext);
        int authResult = biometricManager.canAuthenticate();

        if (authResult == BiometricManager.BIOMETRIC_SUCCESS) {
            return null;
        }
        if (authResult == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            return "FingerprintScannerNotSupported";
        } else if (authResult == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            return "FingerprintScannerNotEnrolled";
        } else if (authResult == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
            return "FingerprintScannerNotAvailable";
        }

        return null;
    }

    @ReactMethod
    public void authenticate(String title, String subtitle, String description, String cancelButton, final Promise promise) {
        if (requiresLegacyAuthentication()) {
            legacyAuthenticate(promise);
        }
        else {
            final String errorName = getSensorError();
            if (errorName != null) {
                promise.reject(errorName, TYPE_BIOMETRICS);
                ReactNativeFingerprintScannerModule.this.release();
                return;
            }
            int fingersId = 0;
            try {
                fingersId = getFingerprintInfo(mReactContext);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            SharedPreferences sharedPreferences = mReactContext.getSharedPreferences("MySharedPref",MODE_PRIVATE);
            SharedPreferences.Editor myEdit = sharedPreferences.edit();
            myEdit.putInt("bioSize", fingersId);
            myEdit.commit();
            biometricAuthenticate(title, subtitle, description, cancelButton, promise);
        }
    }

    @ReactMethod
    public void release() {
        if (requiresLegacyAuthentication()) {
            getFingerprintIdentify().cancelIdentify();
            mFingerprintIdentify = null;
        }

        // consistent across legacy and current API
        if (biometricPrompt != null) {
            biometricPrompt.cancelAuthentication();  // if release called from eg React
        }
        biometricPrompt = null;
        mReactContext.removeLifecycleEventListener(this);
    }

    @ReactMethod
    public void isSensorAvailable(final Promise promise) {
        if (requiresLegacyAuthentication()) {
            String errorMessage = legacyGetErrorMessage();
            if (errorMessage != null) {
                promise.reject(errorMessage, TYPE_FINGERPRINT_LEGACY);
            } else {
                promise.resolve(TYPE_FINGERPRINT_LEGACY);
            }
            return;
        }

        // current API
        String errorName = getSensorError();
        if (errorName != null) {
            promise.reject(errorName, TYPE_BIOMETRICS);
        } else {
            promise.resolve(TYPE_BIOMETRICS);
        }
    }

    @ReactMethod
    public void hasBiometricSettingsChanged(final Promise promise) {
        try {
            // get current fingers id sum
            int fingersId = getFingerprintInfo(mReactContext);
            // last saved key
            SharedPreferences sh = mReactContext.getSharedPreferences("MySharedPref", MODE_PRIVATE);

            int bioSize = sh.getInt("bioSize", 0);

            if (fingersId == bioSize) {
                promise.resolve(false);
            }else {
                promise.resolve(true);
            }

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            promise.resolve(false);
        }
    }

    private boolean hasFingerprintHardware(Context mContext) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Fingerprint API only available on from Android 6.0 (M)
            FingerprintManager fingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);

            if (fingerprintManager == null) {
                return false;
            }

            return fingerprintManager.isHardwareDetected();
        } else {
            // Supporting devices with SDK < 23
            FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(getReactApplicationContext());

            if (fingerprintManager == null) {
                return false;
            }

            return fingerprintManager.isHardwareDetected();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private int getFingerprintInfo(Context context) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        Method method = FingerprintManager.class.getDeclaredMethod("getEnrolledFingerprints");
        Object obj = method.invoke(fingerprintManager);

        return ((ArrayList) obj).size();

    }

    // for Samsung/MeiZu compat, Android v16-23
    private FingerprintIdentify getFingerprintIdentify() {
        if (mFingerprintIdentify != null) {
            return mFingerprintIdentify;
        }
        mReactContext.addLifecycleEventListener(this);
        mFingerprintIdentify = new FingerprintIdentify(mReactContext);
        mFingerprintIdentify.setSupportAndroidL(true);
        mFingerprintIdentify.setExceptionListener(
            new ExceptionListener() {
                @Override
                public void onCatchException(Throwable exception) {
                    mReactContext.removeLifecycleEventListener(ReactNativeFingerprintScannerModule.this);
                }
            }
        );
        mFingerprintIdentify.init();
        return mFingerprintIdentify;
    }

    private String legacyGetErrorMessage() {
        if (!getFingerprintIdentify().isHardwareEnable()) {
            return "FingerprintScannerNotSupported";
        } else if (!getFingerprintIdentify().isRegisteredFingerprint()) {
            return "FingerprintScannerNotEnrolled";
        } else if (!getFingerprintIdentify().isFingerprintEnable()) {
            return "FingerprintScannerNotAvailable";
        }

        return null;
    }


    private void legacyAuthenticate(final Promise promise) {
        final String errorMessage = legacyGetErrorMessage();
        if (errorMessage != null) {
            promise.reject(errorMessage, TYPE_FINGERPRINT_LEGACY);
            ReactNativeFingerprintScannerModule.this.release();
            return;
        }

        getFingerprintIdentify().resumeIdentify();
        getFingerprintIdentify().startIdentify(MAX_AVAILABLE_TIMES, new IdentifyListener() {
            @Override
            public void onSucceed() {
                promise.resolve(true);
            }

            @Override
            public void onNotMatch(int availableTimes) {
                if (availableTimes <= 0) {
                    mReactContext.getJSModule(RCTDeviceEventEmitter.class)
                            .emit("FINGERPRINT_SCANNER_AUTHENTICATION", "DeviceLocked");

                } else {
                    mReactContext.getJSModule(RCTDeviceEventEmitter.class)
                            .emit("FINGERPRINT_SCANNER_AUTHENTICATION", "AuthenticationNotMatch");
                }
            }

            @Override
            public void onFailed(boolean isDeviceLocked) {
                if(isDeviceLocked){
                    promise.reject("AuthenticationFailed", "DeviceLocked");
                } else {
                    promise.reject("AuthenticationFailed", TYPE_FINGERPRINT_LEGACY);
                }
                ReactNativeFingerprintScannerModule.this.release();
            }

            @Override
            public void onStartFailedByDeviceLocked() {
                // the first start failed because the device was locked temporarily
                promise.reject("AuthenticationFailed", "DeviceLocked");
            }
        });
    }
}

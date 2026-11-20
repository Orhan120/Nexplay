package com.omstudio.nexplay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.unity3d.mediation.LevelPlay;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.LevelPlayConfiguration;
import com.unity3d.mediation.LevelPlayInitError;
import com.unity3d.mediation.LevelPlayInitListener;
import com.unity3d.mediation.LevelPlayInitRequest;
import com.unity3d.mediation.LevelPlayReward;
import com.unity3d.mediation.LevelPlayRewardedAd;
import com.unity3d.mediation.LevelPlayRewardedAdListener;

/**
 * NEXPLAY Capacitor bridge for Unity LevelPlay rewarded ads.
 * The game receives a reward only after LevelPlay calls onAdRewarded.
 *
 * All state is touched on the main thread only (Capacitor calls plugin methods
 * on its own thread, so showRewarded() hops to the main looper first).
 */
@CapacitorPlugin(name = "NexplayAds")
public class NexplayAdsPlugin extends Plugin {
    private static final String TAG = "NEXPLAY-Ads";

    // LevelPlay dashboard values (App Key + Rewarded Ad Unit ID).
    private static final String APP_KEY = "29b6a262d";
    private static final String REWARDED_AD_UNIT_ID = "41n8w7vfzpf05dib";

    private static final long REQUEST_TIMEOUT_MS = 45000;   // wait for load + show
    private static final long CLOSE_GRACE_MS = 3000;        // onAdRewarded may come after onAdClosed
    private static final long INIT_RETRY_MS = 15000;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean sdkReady = false;
    private boolean initializing = false;
    private boolean loading = false;
    private boolean adClosed = false;
    private boolean rewardReceived = false;

    private LevelPlayRewardedAd rewardedAd;
    private PluginCall pendingCall;
    private Runnable closeFallback;
    private Runnable requestTimeout;

    @Override
    public void load() {
        handler.post(this::initSdk);
    }

    // ------------------------------------------------------------------ init

    private void initSdk() {
        if (sdkReady || initializing) return;
        initializing = true;

        LevelPlayInitRequest request = new LevelPlayInitRequest.Builder(APP_KEY).build();
        LevelPlay.init(getContext(), request, new LevelPlayInitListener() {
            @Override
            public void onInitSuccess(@NonNull LevelPlayConfiguration configuration) {
                handler.post(() -> {
                    initializing = false;
                    sdkReady = true;
                    Log.i(TAG, "LevelPlay initialized.");
                    createRewardedAd();
                    if (pendingCall != null) loadNextAd();
                });
            }

            @Override
            public void onInitFailed(@NonNull LevelPlayInitError error) {
                handler.post(() -> {
                    initializing = false;
                    sdkReady = false;
                    Log.e(TAG, "LevelPlay init failed: " + error);
                    if (pendingCall != null) finishRequest(false);
                    handler.postDelayed(NexplayAdsPlugin.this::initSdk, INIT_RETRY_MS);
                });
            }
        });
    }

    private void createRewardedAd() {
        if (rewardedAd != null) return;
        rewardedAd = new LevelPlayRewardedAd(REWARDED_AD_UNIT_ID);
        rewardedAd.setListener(new LevelPlayRewardedAdListener() {
            @Override
            public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
                handler.post(() -> {
                    loading = false;
                    Log.i(TAG, "Rewarded ad loaded.");
                    if (pendingCall != null && !adClosed) showLoadedAd();
                });
            }

            @Override
            public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
                handler.post(() -> {
                    loading = false;
                    Log.e(TAG, "Rewarded load failed: " + error);
                    if (pendingCall != null) finishRequest(false);
                });
            }

            @Override
            public void onAdDisplayed(@NonNull LevelPlayAdInfo adInfo) {
                Log.i(TAG, "Rewarded ad displayed.");
                // The ad is on screen: the user may watch it as long as needed.
                handler.post(() -> cancel(requestTimeout));
            }

            @Override
            public void onAdDisplayFailed(@NonNull LevelPlayAdError error,
                                          @NonNull LevelPlayAdInfo adInfo) {
                handler.post(() -> {
                    Log.e(TAG, "Rewarded display failed: " + error);
                    finishRequest(false);
                    loadNextAd();
                });
            }

            @Override
            public void onAdRewarded(@NonNull LevelPlayReward reward,
                                     @NonNull LevelPlayAdInfo adInfo) {
                handler.post(() -> {
                    rewardReceived = true;
                    Log.i(TAG, "Reward received: " + reward.getName() + " x" + reward.getAmount());
                    finishRequest(true);
                });
            }

            @Override
            public void onAdClosed(@NonNull LevelPlayAdInfo adInfo) {
                handler.post(() -> {
                    adClosed = true;
                    // onAdRewarded can arrive after onAdClosed, so wait a moment before failing.
                    if (!rewardReceived && pendingCall != null) {
                        cancel(closeFallback);
                        closeFallback = () -> {
                            if (pendingCall != null && !rewardReceived) finishRequest(false);
                        };
                        handler.postDelayed(closeFallback, CLOSE_GRACE_MS);
                    }
                    loadNextAd();
                });
            }

            @Override
            public void onAdClicked(@NonNull LevelPlayAdInfo adInfo) { }

            @Override
            public void onAdInfoChanged(@NonNull LevelPlayAdInfo adInfo) { }
        });

        loadNextAd();
    }

    // ------------------------------------------------------------ plugin API

    @PluginMethod
    public void showRewarded(PluginCall call) {
        handler.post(() -> {
            if (pendingCall != null) {          // another request is already running
                resolve(call, false);
                return;
            }
            if (!sdkReady || rewardedAd == null) {
                initSdk();                      // retry in the background for next time
                resolve(call, false);
                return;
            }

            pendingCall = call;
            adClosed = false;
            rewardReceived = false;

            cancel(requestTimeout);
            requestTimeout = () -> {
                if (pendingCall != null) finishRequest(false);
            };
            handler.postDelayed(requestTimeout, REQUEST_TIMEOUT_MS);

            if (rewardedAd.isAdReady()) {
                showLoadedAd();
            } else {
                loadNextAd();
            }
        });
    }

    // --------------------------------------------------------------- helpers

    private void showLoadedAd() {
        Activity activity = getActivity();
        if (activity == null || pendingCall == null || adClosed) return;
        if (!rewardedAd.isAdReady()) {
            loadNextAd();
            return;
        }
        try {
            rewardedAd.showAd(activity);
        } catch (Exception error) {
            Log.e(TAG, "Could not show rewarded ad.", error);
            finishRequest(false);
            loadNextAd();
        }
    }

    private void loadNextAd() {
        if (!sdkReady || rewardedAd == null || loading || rewardedAd.isAdReady()) return;
        loading = true;
        rewardedAd.loadAd();
    }

    private void finishRequest(boolean rewarded) {
        PluginCall call = pendingCall;
        if (call == null) return;
        pendingCall = null;
        cancel(closeFallback);
        cancel(requestTimeout);
        closeFallback = null;
        requestTimeout = null;
        resolve(call, rewarded);
    }

    private void resolve(PluginCall call, boolean rewarded) {
        JSObject result = new JSObject();
        result.put("rewarded", rewarded);
        call.resolve(result);
    }

    private void cancel(Runnable runnable) {
        if (runnable != null) handler.removeCallbacks(runnable);
    }
}

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
import com.unity3d.mediation.LevelPlayConfiguration;
import com.unity3d.mediation.LevelPlayInitError;
import com.unity3d.mediation.LevelPlayInitListener;
import com.unity3d.mediation.LevelPlayInitRequest;
import com.unity3d.mediation.LevelPlayAdInfo;
import com.unity3d.mediation.LevelPlayAdError;
import com.unity3d.mediation.LevelPlayReward;
import com.unity3d.mediation.LevelPlayRewardedAd;
import com.unity3d.mediation.LevelPlayRewardedAdListener;

/**
 * NEXPLAY Capacitor bridge for Unity LevelPlay rewarded ads.
 * The app receives a reward only after LevelPlay calls onAdRewarded.
 */
@CapacitorPlugin(name = "NexplayAds")
public class NexplayAdsPlugin extends Plugin {
    private static final String TAG = "NEXPLAY-LevelPlay";
    private static final String APP_KEY = "29b6a262d";
    private static final String REWARDED_AD_UNIT_ID = "41n8w7vfzpf05dib";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private volatile boolean sdkReady = false;
    private boolean loading = false;
    private boolean adClosed = false;
    private boolean rewardReceived = false;

    private LevelPlayRewardedAd rewardedAd;
    private PluginCall pendingCall;
    private Runnable closeFallback;
    private Runnable requestTimeout;

    @Override
    public void load() {
        LevelPlayInitRequest request =
            new LevelPlayInitRequest.Builder(APP_KEY).build();

        LevelPlay.init(getContext(), request, new LevelPlayInitListener() {
            @Override
            public void onInitSuccess(LevelPlayConfiguration configuration) {
                sdkReady = true;
                Log.i(TAG, "LevelPlay initialized.");
                createRewardedAd();
            }

            @Override
            public void onInitFailed(@NonNull LevelPlayInitError error) {
                sdkReady = false;
                Log.e(TAG, "LevelPlay init failed: " + error);
            }
        });
    }

    private void createRewardedAd() {
        rewardedAd = new LevelPlayRewardedAd(REWARDED_AD_UNIT_ID);
        rewardedAd.setListener(new LevelPlayRewardedAdListener() {
            @Override
            public void onAdLoaded(@NonNull LevelPlayAdInfo adInfo) {
                loading = false;
                Log.i(TAG, "Rewarded ad loaded.");
                if (pendingCall != null && !adClosed) {
                    showLoadedAd();
                }
            }

            @Override
            public void onAdLoadFailed(@NonNull LevelPlayAdError error) {
                loading = false;
                Log.e(TAG, "Rewarded load failed: " + error);
                if (pendingCall != null && !adClosed) {
                    finishRequest(false);
                }
            }

            @Override
            public void onAdDisplayed(@NonNull LevelPlayAdInfo adInfo) {
                Log.i(TAG, "Rewarded ad displayed.");
            }

            @Override
            public void onAdDisplayFailed(
                @NonNull LevelPlayAdError error,
                @NonNull LevelPlayAdInfo adInfo
            ) {
                Log.e(TAG, "Rewarded display failed: " + error);
                finishRequest(false);
                loadNextAd();
            }

            @Override
            public void onAdRewarded(
                @NonNull LevelPlayReward reward,
                @NonNull LevelPlayAdInfo adInfo
            ) {
                // LevelPlay documents that this callback can arrive AFTER onAdClosed.
                rewardReceived = true;
                Log.i(TAG, "Reward callback received: " +
                    reward.getName() + " x" + reward.getAmount());
                finishRequest(true);
            }

            @Override
            public void onAdClosed(@NonNull LevelPlayAdInfo adInfo) {
                adClosed = true;

                // Do not reject immediately: onAdRewarded and onAdClosed are asynchronous.
                if (!rewardReceived && pendingCall != null) {
                    cancel(closeFallback);
                    closeFallback = () -> {
                        if (pendingCall != null && !rewardReceived) {
                            finishRequest(false);
                        }
                    };
                    handler.postDelayed(closeFallback, 3000);
                }

                loadNextAd();
            }

            @Override
            public void onAdClicked(@NonNull LevelPlayAdInfo adInfo) { }

            @Override
            public void onAdInfoChanged(@NonNull LevelPlayAdInfo adInfo) { }
        });

        loadNextAd();
    }

    @PluginMethod
    public void showRewarded(PluginCall call) {
        if (pendingCall != null) {
            resolve(call, false);
            return;
        }
        if (!sdkReady || rewardedAd == null || getActivity() == null) {
            resolve(call, false);
            return;
        }

        pendingCall = call;
        adClosed = false;
        rewardReceived = false;

        cancel(requestTimeout);
        requestTimeout = () -> {
            if (pendingCall != null) {
                finishRequest(false);
            }
        };
        handler.postDelayed(requestTimeout, 120000);

        if (rewardedAd.isAdReady()) {
            showLoadedAd();
        } else {
            loadNextAd();
        }
    }

    private void showLoadedAd() {
        Activity activity = getActivity();
        if (activity == null || pendingCall == null || adClosed) {
            return;
        }
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
        if (!sdkReady || rewardedAd == null || loading || rewardedAd.isAdReady()) {
            return;
        }
        loading = true;
        rewardedAd.loadAd();
    }

    private void finishRequest(boolean rewarded) {
        PluginCall call = pendingCall;
        if (call == null) {
            return;
        }
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
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}

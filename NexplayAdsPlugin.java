package com.omstudio.nexplay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.VideoListener;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

@CapacitorPlugin(name = "NexplayAds")
public class NexplayAdsPlugin extends Plugin {
    private static final String TAG = "NEXPLAY-Ads";

    private static final String APP_ID = "210717033";
    private static final boolean TEST_ADS = false;

    private static final long LOAD_TIMEOUT_MS = 30000;
    private static final long CLOSE_GRACE_MS = 1500;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean sdkReady = false;
    private PluginCall pendingCall;
    private StartAppAd currentAd;
    private boolean rewardReceived = false;
    private int loadAttempts = 0;
    private Runnable loadTimeout;
    private Runnable closeFallback;
    private String lastReason = "";

    @Override
    public void load() {
        handler.post(this::initSdk);
    }

    private void initSdk() {
        if (sdkReady) return;
        try {
            StartAppSDK.setTestAdsEnabled(TEST_ADS);
            StartAppSDK.init(getContext(), APP_ID, false);
            sdkReady = true;
            Log.i(TAG, "Start.io initialized.");
        } catch (Throwable t) {
            lastReason = "init: " + t.getMessage();
            Log.e(TAG, "Start.io init failed", t);
        }
    }

    @PluginMethod
    public void showRewarded(PluginCall call) {
        handler.post(() -> {
            if (pendingCall != null) {
                resolve(call, false);
                return;
            }
            initSdk();
            Activity activity = getActivity();
            if (!sdkReady || activity == null) {
                if (lastReason.isEmpty()) lastReason = "SDK hazir degil";
                resolve(call, false);
                return;
            }

            pendingCall = call;
            rewardReceived = false;
            lastReason = "";
            loadAttempts = 0;

            cancel(loadTimeout);
            loadTimeout = () -> {
                if (pendingCall != null) {
                    lastReason = "yukleme zaman asimi";
                    finishRequest(false);
                }
            };
            handler.postDelayed(loadTimeout, LOAD_TIMEOUT_MS);

            final StartAppAd ad = new StartAppAd(activity);
            currentAd = ad;

            ad.setVideoListener(new VideoListener() {
                @Override
                public void onVideoCompleted() {
                    handler.post(() -> {
                        rewardReceived = true;
                        Log.i(TAG, "Rewarded video completed.");
                        finishRequest(true);
                    });
                }
            });

            ad.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
                @Override
                public void onReceiveAd(Ad received) {
                    handler.post(() -> {
                        if (pendingCall == null || currentAd != ad) return;
                        Log.i(TAG, "Rewarded video loaded.");
                        showAd(ad);
                    });
                }

                @Override
                public void onFailedToReceiveAd(Ad failed) {
                    handler.post(() -> {
                        String msg = failed != null ? failed.getErrorMessage() : null;
                        lastReason = "load: " + (msg == null ? "reklam yok" : msg);
                        Log.e(TAG, "Rewarded video load failed: " + lastReason);
                        if (pendingCall == null || currentAd != ad) return;
                        if (loadAttempts++ < 1) {      // one quick retry
                            final AdEventListener self = this;
                            handler.postDelayed(() -> {
                                if (pendingCall != null && currentAd == ad) {
                                    ad.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, self);
                                }
                            }, 2000);
                            return;
                        }
                        finishRequest(false);
                    });
                }
            });
        });
    }

    private void showAd(StartAppAd ad) {
        try {
            ad.showAd(new AdDisplayListener() {
                @Override
                public void adHidden(Ad shown) {
                    handler.post(() -> {
                        if (pendingCall != null && !rewardReceived) {
                            cancel(closeFallback);
                            closeFallback = () -> {
                                if (pendingCall != null && !rewardReceived) {
                                    lastReason = "reklam kapatildi, odul yok";
                                    finishRequest(false);
                                }
                            };
                            handler.postDelayed(closeFallback, CLOSE_GRACE_MS);
                        }
                    });
                }

                @Override
                public void adDisplayed(Ad shown) {
                    handler.post(() -> cancel(loadTimeout));
                }

                @Override
                public void adClicked(Ad shown) { }

                @Override
                public void adNotDisplayed(Ad shown) {
                    handler.post(() -> {
                        lastReason = "reklam gosterilemedi";
                        if (pendingCall != null) finishRequest(false);
                    });
                }
            });
        } catch (Throwable t) {
            lastReason = "show: " + t.getMessage();
            Log.e(TAG, "Could not show rewarded video", t);
            finishRequest(false);
        }
    }

    private void finishRequest(boolean rewarded) {
        PluginCall call = pendingCall;
        if (call == null) return;
        pendingCall = null;
        currentAd = null;
        cancel(loadTimeout);
        cancel(closeFallback);
        loadTimeout = null;
        closeFallback = null;
        resolve(call, rewarded);
    }

    private void resolve(PluginCall call, boolean rewarded) {
        JSObject result = new JSObject();
        result.put("rewarded", rewarded);
        result.put("reason", rewarded ? "" : lastReason);
        call.resolve(result);
    }

    private void cancel(Runnable runnable) {
        if (runnable != null) handler.removeCallbacks(runnable);
    }
}

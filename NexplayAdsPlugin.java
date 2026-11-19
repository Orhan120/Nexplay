package com.omstudio.nexplay;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsShowOptions;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;

/** Native Unity Ads rewarded-video bridge for NEXPLAY. */
@CapacitorPlugin(name = "NexplayAds")
public class NexplayAdsPlugin extends Plugin {
    // Unity dashboard identifiers supplied for this Android app.
    private static final String UNITY_GAME_ID = "29b6a262d";
    private static final String REWARDED_AD_UNIT_ID = "41n8w7vfzpf05dib";
    // Keep test mode enabled until the integration is verified in the Unity dashboard.
    private static final boolean TEST_MODE = true;

    private volatile boolean initialized = false;
    private volatile boolean initializationFailed = false;

    @Override
    public void load() {
        UnityAds.initialize(getContext(), UNITY_GAME_ID, TEST_MODE,
            new IUnityAdsInitializationListener() {
                @Override public void onInitializationComplete() {
                    initialized = true;
                    initializationFailed = false;
                }
                @Override public void onInitializationFailed(
                    UnityAds.UnityAdsInitializationError error, String message) {
                    initialized = false;
                    initializationFailed = true;
                    android.util.Log.e("NEXPLAY", "Unity Ads init failed: " + error + " / " + message);
                }
            });
    }

    @PluginMethod
    public void showRewarded(PluginCall call) {
        if (!initialized || initializationFailed) {
            resolveReward(call, false);
            return;
        }

        UnityAds.load(REWARDED_AD_UNIT_ID, new IUnityAdsLoadListener() {
            @Override public void onUnityAdsAdLoaded(String adUnitId) {
                getActivity().runOnUiThread(() -> UnityAds.show(
                    getActivity(), adUnitId, new UnityAdsShowOptions(), new IUnityAdsShowListener() {
                        @Override public void onUnityAdsShowFailure(String placementId,
                            UnityAds.UnityAdsShowError error, String message) {
                            android.util.Log.e("NEXPLAY", "Unity Ads show failed: " + error + " / " + message);
                            resolveReward(call, false);
                        }
                        @Override public void onUnityAdsShowStart(String placementId) { }
                        @Override public void onUnityAdsShowClick(String placementId) { }
                        @Override public void onUnityAdsShowComplete(String placementId,
                            UnityAds.UnityAdsShowCompletionState state) {
                            resolveReward(call, state == UnityAds.UnityAdsShowCompletionState.COMPLETED);
                        }
                    }
                ));
            }
            @Override public void onUnityAdsFailedToLoad(String adUnitId,
                UnityAds.UnityAdsLoadError error, String message) {
                android.util.Log.e("NEXPLAY", "Unity Ads load failed: " + error + " / " + message);
                resolveReward(call, false);
            }
        });
    }

    private void resolveReward(PluginCall call, boolean rewarded) {
        JSObject result = new JSObject();
        result.put("rewarded", rewarded);
        call.resolve(result);
    }
}

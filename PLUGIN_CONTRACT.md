# NEXPLAY native ads status

`www/nexplay-capacitor.js` calls `Capacitor.Plugins.NexplayAds.showRewarded()`.

Current Android plugin status:
- Capacitor plugin is copied into generated Android project by `scripts/prepare-android.mjs`.
- Plugin is registered in MainActivity.
- It deliberately returns `{ rewarded: false }` until real LevelPlay integration is configured.
- No demo ad is shown and no reward is granted.

To complete real LevelPlay:
1. Create the Android app in LevelPlay using `com.omstudio.nexplay`.
2. Create a Rewarded Ad Unit and obtain its Ad Unit ID and App Key.
3. Add the LevelPlay SDK dependency from the current official Unity docs.
4. Initialize LevelPlay and wait for `onInitSuccess`.
5. Create the rewarded ad object after initialization, attach the listener, load it.
6. Show only when ready; resolve rewarded=true only from `onAdRewarded`.
7. Resolve false for load/display errors or close without reward.
8. Test with Unity's integration test tools and a physical Android device.

Official docs:
- https://docs.unity.com/en-us/grow/levelplay/sdk/android/sdk-integration
- https://docs.unity.com/en-us/grow/levelplay/sdk/android/rewarded-ads-integration

# NEXPLAY native ads status

`www/nexplay-capacitor.js` calls `Capacitor.Plugins.NexplayAds.showRewarded()`.

The Android bridge now uses the Unity Ads Android SDK and the supplied identifiers:
- Game ID: `29b6a262d`
- Rewarded Ad Unit ID: `41n8w7vfzpf05dib`

The build preparation script adds `com.unity3d.ads:unity-ads:4.12.5` to the generated Android app. The bridge initializes Unity Ads, loads the rewarded ad, and reports `rewarded: true` only when Unity Ads reports `COMPLETED`.

`TEST_MODE` is deliberately `true` for initial validation. Before serving live ads, verify the identifiers in the Unity Monetization dashboard, configure consent for your users, and change `TEST_MODE` to `false` only after testing. The supplied Game ID is accepted as provided here, but confirm it is the Android Game ID (Unity's documented identifier) and not an App Store ID or another dashboard identifier.

Official Unity documentation:
- https://docs.unity.com/en-us/ads-android/4.22.0/getting-started/publisher-process-overview
- https://docs.unity.com/en-us/grow/ads/android-sdk/android-api

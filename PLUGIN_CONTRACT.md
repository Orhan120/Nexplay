# NEXPLAY native ads

`www/nexplay-capacitor.js` calls `Capacitor.Plugins.NexplayAds.showRewarded()` and expects
`{ rewarded: boolean }`. The Android plugin (`native-ads/android/NexplayAdsPlugin.java`) uses the
Unity LevelPlay SDK 9.8.0 and resolves `rewarded: true` only from `onAdRewarded`.
Docs: https://docs.unity.com/grow/levelplay/sdk/android/rewarded-ads-integration

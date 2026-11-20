# NEXPLAY native ads

`www/nexplay-capacitor.js` calls `Capacitor.Plugins.NexplayAds.showRewarded()` and expects
`{ rewarded: boolean, reason: string }`. The Android plugin (`native-ads/android/NexplayAdsPlugin.java`)
uses the Start.io SDK (`com.startapp:inapp-sdk:5.3.1`) and resolves `rewarded: true` only from
`VideoListener.onVideoCompleted()`.

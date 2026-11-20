# NEXPLAY — Android Debug APK (GitHub Actions)

Oyun kodu (`www/index.html`) korunur. GitHub Actions her çalıştırmada Android
Capacitor projesini üretir, Start.io reklam eklentisini enjekte eder ve **debug APK** çıkarır.

## GitHub'a yükleme

1. ZIP'i aç, **içindeki tüm dosya ve klasörleri** (`.github`, `www`, `native-ads`,
   `scripts`, `package.json` …) repository ana dizinine yükle. `.github` gizli klasördür.
2. Varsayılan dal `main` (veya `master`) olsun.
3. **Actions → Build Nexplay Android APK → Run workflow**.
4. Bitince **Artifacts → NEXPLAY-debug-apk** içindeki `app-debug.apk` dosyasını indir.

## Klasör yapısı

| Yol | Görev |
|---|---|
| `www/index.html` | Oyun |
| `www/nexplay-capacitor.js` | `window.NEX_AD.show()` → native eklenti köprüsü |
| `native-ads/android/NexplayAdsPlugin.java` | Start.io rewarded video eklentisi |
| `scripts/prepare-android.mjs` | SDK bağımlılığı, izinler, App ID (manifest), eklenti kaydı |

## Reklam entegrasyonu (Start.io)

- SDK: `com.startapp:inapp-sdk:5.3.1`
- App ID: `210717033` (`NexplayAdsPlugin.java` ve `prepare-android.mjs` içinde)
- Ödül yalnızca `onVideoCompleted` gelince verilir.
- **Test:** `NexplayAdsPlugin.java` içinde `TEST_ADS = true` yapıp APK al; test reklamı gelirse
  entegrasyon sağlamdır. Yayından önce tekrar `false` yap.
- Reklam gelmiyorsa: Start.io panelinde uygulamanın paket adı `com.omstudio.nexplay` ile
  eklendiğinden ve App ID'nin doğru olduğundan emin ol; VPN/Özel DNS kapalı olsun.

# NEXPLAY — Android Debug APK (GitHub Actions)

Oyun kodu (`www/index.html`) korunur. GitHub Actions her çalıştırmada Android
Capacitor projesini üretir, reklam eklentisini enjekte eder ve **debug APK** çıkarır.

## GitHub'a yükleme

1. ZIP'i aç, **içindeki tüm dosya ve klasörleri** (`.github`, `www`, `native-ads`,
   `scripts`, `package.json` …) repository ana dizinine yükle. `.github` gizli klasördür, yüklendiğinden emin ol.
2. Varsayılan dal `main` (veya `master`) olsun.
3. **Actions → Build Nexplay Android APK → Run workflow**.
4. Bitince **Artifacts → NEXPLAY-debug-apk** içindeki `app-debug.apk` dosyasını indir.

## Klasör yapısı

| Yol | Görev |
|---|---|
| `www/index.html` | Oyun (orijinalden tek fark: `adsDemo:false` + köprü script'i) |
| `www/nexplay-capacitor.js` | `window.NEX_AD.show()` → native eklenti köprüsü |
| `native-ads/android/NexplayAdsPlugin.java` | Unity LevelPlay rewarded reklam eklentisi |
| `scripts/prepare-android.mjs` | SDK bağımlılığı, izinler, eklenti kaydı |

## Yerel geliştirme

Node.js 22+, JDK 21, Android SDK gerekir:

```bash
npm install
npx cap add android
npm run build:android
cd android && ./gradlew assembleDebug
```

## Reklam entegrasyonu (Unity LevelPlay)

- SDK: `com.unity3d.ads-mediation:mediation-sdk:9.8.0`
- App Key: `29b6a262d` — Rewarded Ad Unit ID: `41n8w7vfzpf05dib`
  (`NexplayAdsPlugin.java` başında)
- Bu kimlik biçimleri **LevelPlay** panelindendir (klasik "Unity Ads Game ID" 7 haneli sayıdır),
  bu yüzden eski `unity-ads` SDK'sı değil LevelPlay SDK'sı kullanılır.
- Ödül yalnızca SDK `onAdRewarded` çağırınca verilir; yükleme/gösterim hatasında veya
  reklam kapatılırsa verilmez.
- **Test:** LevelPlay panelinde Test Suite / test cihazı ekleyip test reklamlarıyla dene.
  Panelde Unity Ads (veya başka) ağ etkin ve rewarded ad unit'e bağlı olmalı; ağ/ad unit
  hazır değilse "Reklam tamamlanmadı" görürsün.
- Yayın öncesi kullanıcı rızası (GDPR/UMP vb.) akışını yapılandır.

Kapsam: yalnızca Android debug APK. Play Store için imzalı release üretmez.

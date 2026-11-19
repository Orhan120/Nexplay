# NEXPLAY — Android Debug APK (GitHub Actions)

Bu paket, mevcut HTML oyununun görünümünü ve oyun kodunu korur. GitHub Actions,
Android Capacitor projesini her çalıştırmada üretir ve imzasız/test amaçlı
**debug APK** dosyasını artifact olarak yükler.

## GitHub'a yükleme

1. ZIP'i bilgisayarında aç.
2. İçindeki `NEXPLAY_Capacitor_Starter` klasörünün **içindeki tüm dosyaları**
   GitHub repository ana dizinine yükle (ZIP dosyasının kendisini değil).
3. Repository'nin varsayılan dalı `main` olsun.
4. GitHub'da **Actions** sekmesine gir.
5. `NEXPLAY Android Debug APK` iş akışını seçip **Run workflow** düğmesine bas.
6. İşlem başarılı olunca workflow çalışmasının altındaki **Artifacts** bölümünden
   `NEXPLAY-debug-apk` dosyasını indir. ZIP içindeki `app-debug.apk` Android'e kurulabilir.

Her `main` veya `master` push işleminde de otomatik derleme başlar.

## Yerel geliştirme

Node.js 22+, Android Studio ve Android SDK gerekir:

```bash
npm install
npx cap add android
npx cap sync android
npm run android:prepare
cd android
./gradlew assembleDebug
```

APK yolu:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Önemli: LevelPlay reklam durumu

- `NexplayAdsPlugin.kt` şu an güvenli bir yer tutucu köprüdür.
- Gerçek reklam göstermez ve ödül vermez; `rewarded:false` döndürür.
- Bu nedenle APK reklam entegrasyonu olmadan test edilebilir.
- Gerçek reklamı etkinleştirmek için LevelPlay panelinde Android uygulaması ve
  Rewarded Ad Unit oluşturulmalı; App Key ve Ad Unit ID alınmalıdır.
- Ardından gerçek LevelPlay SDK init/load/show ve `onAdRewarded` callback'i
  native plugin'e eklenmelidir. Kimlikler olmadan bu kısım etkinleştirilmez.
- Gizli anahtarları HTML'e, JavaScript'e veya herkese açık GitHub dosyalarına
  koyma. Gerçek entegrasyonda GitHub Actions Secrets / native build config kullan.

## Kapsam

Bu workflow yalnızca Android **debug APK** üretir. Play Store için imzalı
release AAB/APK üretmez. iOS bu pakette şimdilik hedeflenmiyor.

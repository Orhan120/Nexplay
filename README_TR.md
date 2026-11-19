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

## Unity Ads reklam entegrasyonu

- Android native köprüsü Unity Ads SDK ile güncellendi.
- Game ID: `29b6a262d`
- Rewarded Ad Unit ID: `41n8w7vfzpf05dib`
- GitHub Actions derlemesinde Unity Ads SDK bağımlılığı otomatik eklenir.
- İlk testler için `TEST_MODE=true` bırakılmıştır; Unity test reklamları gösterilir.
- Gerçek reklamları açmadan önce Unity Monetization panelinde Android Game ID ve Rewarded Ad Unit ID değerlerini doğrulayın, kullanıcı rızası/izin akışını yapılandırın ve ardından `NexplayAdsPlugin.java` içindeki `TEST_MODE` değerini `false` yapın.
- Ödül yalnızca SDK reklamın `COMPLETED` durumunu bildirdiğinde verilir. Yükleme/gösterim hatasında veya reklam tamamlanmadığında ödül verilmez.

Not: Bu paket Capacitor tabanlı Android projesidir; Unity Editor projesi değildir. Unity'nin Android SDK'sı native Android köprüsüne eklenmiştir.

## Kapsam

Bu workflow yalnızca Android **debug APK** üretir. Play Store için imzalı
release AAB/APK üretmez. iOS bu pakette şimdilik hedeflenmiyor.

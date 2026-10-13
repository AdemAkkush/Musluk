# Görev Harcamaları (Android)

Haftalık **Görev Bildirim Formu (FR.048)** için Android uygulaması. Excel dosyasındaki alanları doldurur:

- Görev çıkış / dönüş tarihi, görevlendirilen firma ve şehirler
- Hafta içi görev, gece geç dönüş, otel alacağı (günlük ücret × gün sayısı)
- Harcama fişleri (firma, fiş no, tarih, tutar, şirket kartı) – yemek ve ekstra harcamalar
- Toplamlar (görev alacağı, harcamalar, genel alacak)

**E-posta ile gönder** düğmesi formu Excel (.xlsx) dosyası olarak üretir (toplamlar Excel formülü) ve ayarlarda kayıtlı adrese e-posta uygulamasıyla gönderir.

Tarihler seçildiğinde hafta içi gün sayısı ve otel gecesi otomatik önerilir, sonradan elle değiştirilebilir.
Ücretler ve alıcı adresi sağ üstteki Ayarlar'dan düzenlenir.

## Derleme
Android Studio ile `GorevHarcama` klasörünü açın (JDK 17, AGP 8.5, minSdk 26) ya da:

```
cd GorevHarcama && ./gradlew assembleDebug
```

# Játszótér Kereső - Android App

Google Maps alapú játszótérkereső alkalmazás Magyarország és a szomszédos országok területére.

## Technológiák

- **Kotlin** + **Jetpack Compose** (UI)
- **Material 3** (design rendszer)
- **Google Maps SDK for Android** (térkép)
- **Google Places API** (játszótér keresés)
- **Hilt** (dependency injection)
- **Room** (kedvencek helyi tárolása)
- **Retrofit + OkHttp** (hálózati hívások)
- **Kotlin Coroutines + Flow** (aszinkron műveletek)
- **MVVM architektúra**

## Projekt struktúra

```
app/src/main/java/com/playgroundfinder/app/
├── PlaygroundFinderApp.kt      # Application osztály (Hilt)
├── MainActivity.kt             # Fő Activity
├── di/                         # Dependency Injection modulok
│   ├── AppModule.kt            # FusedLocationProviderClient
│   ├── NetworkModule.kt        # Retrofit, OkHttp
│   └── DatabaseModule.kt      # Room database
├── data/
│   ├── remote/                 # API hívások
│   │   ├── PlacesApiService.kt
│   │   └── dto/                # API válasz modellek
│   ├── local/                  # Room adatbázis
│   │   ├── PlaygroundEntity.kt
│   │   ├── PlaygroundDao.kt
│   │   └── PlaygroundDatabase.kt
│   └── repository/
│       └── PlaygroundRepository.kt
├── domain/
│   └── model/                  # Domain modellek
│       ├── Playground.kt
│       └── Location.kt
├── util/
│   └── Resource.kt             # Sealed class az API eredményekhez
└── ui/
    ├── theme/                  # Material 3 téma
    └── map/                    # Térkép képernyő
        ├── MapScreen.kt
        ├── MapViewModel.kt
        ├── MapState.kt
        ├── MapEvent.kt
        └── PlaygroundDetailsBottomSheet.kt
```

## Beállítás

### 1. Google Cloud Console

1. Látogass el: https://console.cloud.google.com/
2. Hozz létre egy projektet
3. Engedélyezd az alábbi API-kat:
   - **Maps SDK for Android**
   - **Places API**
   - **Directions API**
4. Hozz létre API kulcsokat és korlátozd őket az alkalmazásodra

### 2. API kulcsok konfigurálása

**AndroidManifest.xml** - Maps SDK kulcs:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY" />
```

**PlaygroundRepository.kt** - Places API kulcs:
```kotlin
private val API_KEY = "YOUR_GOOGLE_PLACES_API_KEY"
```

> ⚠️ **FONTOS:** Éles alkalmazásban ne kódold be az API kulcsokat! Használj `local.properties`-t vagy `BuildConfig`-ot!

### 3. Android Studio

1. Nyisd meg az `android/` mappát Android Studio-ban
2. Szinkronizáld a Gradle-t (Sync Now)
3. Futtasd az alkalmazást emulátoron vagy fizikai eszközön

## Funkciók

- ✅ Google Térkép megjelenítése
- ✅ GPS alapú helymeghatározás
- ✅ Játszóterek keresése a felhasználó helye körül (5 km sugarú körben)
- ✅ Játszótér jelölők a térképen (zöld = normál, piros = kedvenc)
- ✅ Részletes nézet (BottomSheet) kattintásra
- ✅ Kedvenc mentése helyi Room adatbázisba
- ✅ Útvonaltervezés Google Maps-ben
- ✅ Kereső mező
- ✅ Material 3 design + sötét téma támogatás

## Fejlesztési terv

- [ ] Kedvencek lista nézet (külön képernyő)
- [ ] Szűrés értékelés és nyitvatartás alapján
- [ ] Térkép clustering sok jelölő esetén
- [ ] Offline mód (cached adatok)
- [ ] Fotók megjelenítése a részletes nézetben
- [ ] Navigáció Jetpack Navigation Component-tel
- [ ] Widget a közelben lévő játszóterekhez

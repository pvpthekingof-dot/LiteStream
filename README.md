# LiteStream — MVP

Text-only, zero-ad, low-RAM streaming shell for Android TV boxes (targets 1GB RAM devices, minSdk 21).

## What's in this package

```
LiteStream/
├── build.gradle                 (project-level)
├── settings.gradle
├── app/
│   ├── build.gradle              (app-level)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/litestream/app/MainActivity.java
│       └── res/layout/
│           ├── activity_main.xml
│           └── list_item.xml
```

## How to build the APK

**Option A — Android Studio (easiest)**
1. Open Android Studio → "Open" → select the `LiteStream` folder.
2. Let Gradle sync (it will download the Android Gradle Plugin automatically).
3. `Build → Build Bundle(s)/APK(s) → Build APK(s)`.
4. Grab the APK from `app/build/outputs/apk/debug/app-debug.apk`.

**Option B — Command line (works fine from LDPlayer's host PC too)**
```bash
cd LiteStream
./gradlew assembleDebug
# APK lands at app/build/outputs/apk/debug/app-debug.apk
```
You'll need a JDK 17 and the Android SDK command-line tools on PATH (Android Studio installs both — point `ANDROID_HOME`/`local.properties` at that SDK if building standalone).

**Installing on the TV box**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Or copy the APK to a USB drive and sideload with a file manager if the box has no ADB access.

## How the app is optimized for 1GB RAM

- No image/thumbnail loading at all — `ListView` rows are a single plain `TextView` (`list_item.xml`), so there's no bitmap decoding, no Glide/Picasso, nothing cached in memory beyond text.
- `ListView` (not `RecyclerView`) — for a handful of text rows this avoids pulling in the `androidx.recyclerview` library and keeps the dependency graph, and thus APK/method count, smaller.
- Playback uses `VideoView` (native `MediaPlayer` wrapper) rather than a `WebView`. A `WebView` spins up a full Chromium engine in-process, which is heavy on 1GB devices — `VideoView` just talks to the device's hardware decoder directly, so it's both lighter and smoother for direct `.mp4` links.
- No ad SDKs, no analytics, no animation libraries — `dependencies {}` in `app/build.gradle` is intentionally empty.
- `android:hardwareAccelerated="true"` and `minifyEnabled true` in release builds keep the APK lean and let the GPU take video decoding load off the CPU where supported.

## Scaling the catalog later

Right now the catalog lives in `MainActivity.SAMPLE_DATA` (a hardcoded `String[][]`). To move to a remote list without adding libraries:

```java
// Replace loadSampleData() with something like:
private void loadRemoteCatalog(String jsonUrl) {
    new Thread(() -> {
        try {
            java.net.URL url = new java.net.URL(jsonUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            java.io.InputStream in = conn.getInputStream();
            java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
            String json = s.hasNext() ? s.next() : "";
            org.json.JSONArray arr = new org.json.JSONArray(json);
            runOnUiThread(() -> {
                fullCatalog.clear();
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject obj = arr.optJSONObject(i);
                    if (obj != null) {
                        fullCatalog.put(obj.optString("title"), obj.optString("url"));
                    }
                }
                filterList("");
            });
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Failed to load catalog", Toast.LENGTH_SHORT).show());
        }
    }).start();
}
```
Expected JSON shape: `[{"title":"Naruto Episode 1","url":"http://..."}, ...]`.
`org.json` and `HttpURLConnection` ship with the Android SDK — no new dependencies needed.

## Sample data included

5 public-domain test clips (Google's standard test video bucket) so playback can be verified immediately:
Big Buck Bunny, Elephants Dream, Sintel, For Bigger Blazes, For Bigger Escapes.

## Known MVP limitations (by design, to keep it minimal)

- No persistence — catalog resets to `SAMPLE_DATA` each launch until you wire up the remote-JSON loader above.
- No DRM/HLS/adaptive-bitrate support — `VideoView` handles direct progressive files (mp4, 3gp, etc.) well; for `.m3u8` streams you'd want ExoPlayer instead, which is heavier and a tradeoff against the RAM budget.
- Single activity, no settings screen — add links by editing `SAMPLE_DATA` or pointing at your remote JSON.

# Nuvio Launcher (Google TV)

A Home-screen replacement that opens the official Nuvio app (`com.nuvio.tv`) and adds a separate **Sports** tab fed by your sports add-ons, so they stay out of Nuvio's Continue Watching row.

## 1. Get the APK
1. Create a free GitHub repo and upload this folder's contents.
2. Open the **Actions** tab → **Build APK** → **Run workflow**.
3. When it finishes, download the `NuvioLauncher-apk` artifact and unzip it to get `app-debug.apk`.

(Or open the folder in Android Studio and press Build > Build APK.)

## 2. Install on your TCL Google TV
1. On the TV: Settings > System > About, tap **Android TV OS build** 7 times to enable Developer options.
2. Settings > System > Developer options: turn on **USB debugging** (and **Wireless debugging** if offered).
3. Easiest install: put the APK on a USB stick, or use "Downloader" / "Send Files to TV".
   Or from a computer on the same network:
   ```
   adb connect <TV-IP>:5555
   adb install app-debug.apk
   ```

## 3. Make it the default Home app (and keep it)
```
adb shell cmd package set-home-activity com.nuvio.launcher/.MainActivity
```
This writes the choice into the system, so it survives standby and power cycles.
Google TV updates can reset it; just run the command again. If the Home button still opens the stock launcher, you can optionally disable it:
```
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```
(Reversible with `pm enable`. Riskier: on some TCL builds this removes Google's home screen entirely.)

## 4. Add your sports add-ons
Open the launcher > **Settings** > paste each sports add-on's manifest URL (the same link you installed in Nuvio, `https://.../manifest.json` or `stremio://...`) and press Add. They show up in **Sports**.

## Known limits
- Launchers can't read another app's add-on list, so add-on URLs are entered here once.
- Selecting a sports item tries a Stremio-style deep link into Nuvio; if Nuvio doesn't handle it, it just opens Nuvio.
- Only catalogs that don't require search/filter input are shown.

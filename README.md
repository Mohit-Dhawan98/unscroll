# Unscroll

Tiny Android app with one job: notice when you're in **YouTube Shorts** or **Instagram Reels**, put a full-screen reminder of who you are in front of you, and send you Home.

- No network, no accounts, no analytics. A 60 KB APK with zero library dependencies.
- Uses an Accessibility Service to read the current screen of YouTube/Instagram only (the service config limits it to those packages).
- The reminder shows one **wake line** ("Remember who you are...") plus one **quote** (Marcus Aurelius, Lao Tzu, Seneca, Epictetus, Hormozi, Chris Williamson, Nischa, Jocko, Goggins, James Clear, Cal Newport, Naval, ...). It cycles through the whole pool before repeating.
- Edit `shared/quotes.json` to add your own lines; it's bundled into the APK at build time.

## Why it is built this way

- **One job.** It does not block apps, count minutes or shame you with charts. It interrupts the one behaviour (short-form video) at the moment it starts.
- **Detect the shape, not the name.** View IDs change with every YouTube and Instagram release. Three or more Like/Comment/Share buttons stacked on the right edge is what a short-form player *is*, so that is what the detector looks for.
- **Nothing leaves the phone.** No network permission, no accounts, no analytics. The accessibility service is scoped to two packages.
- **Small enough to read.** About 450 lines of Kotlin, no libraries. You can audit the whole thing before granting it accessibility access, which you should do for any app that asks.

## Build

```sh
./build.sh        # -> ./Unscroll.apk (~60 KB release build)
```

Toolchain: JDK 17, Android command-line tools (platform 35, build-tools 35) and adb. On macOS: `brew install openjdk@17 android-commandlinetools android-platform-tools`. `build.sh` defaults `JAVA_HOME` and `ANDROID_HOME` to the Homebrew paths; export your own to override.

## Install (tested on a Galaxy S23, Android 13+)

1. Phone: Settings → About phone → Software information → tap **Build number** 7× → back → **Developer options** → enable **USB debugging**.
2. Plug in, accept the RSA prompt, then:
   ```sh
   ./install.sh
   ```
   Installing via adb matters: Android 13+ marks apps sideloaded from a browser/file manager as "restricted" and blocks accessibility for them. adb installs are exempt.
   (If you did sideload another way: App info → ⋮ → **Allow restricted settings**.)
3. Open **Unscroll** → *Open accessibility settings* → Samsung puts it under **Installed apps** → **Unscroll** → On → Allow.
4. Tap *Preview the reminder* to see it. Then open YouTube, tap Shorts, and get thrown out.

## Tuning

- **Send me to Home** off → it just presses Back (lands on the YouTube/Instagram home feed instead of the launcher).
- **Layout detection** needs no view IDs at all: it looks for 3+ Like/Comment/Share buttons stacked on the right edge of the screen, which is the signature of every short-form player and of nothing else in those apps. This is the layer that keeps working when YouTube/Instagram rename things.
- **Also fire when the Shorts/Reels tab is selected** is a fallback for when YouTube/Instagram rename their view IDs. Turn it off if it misfires (e.g. on a profile's Reels grid).
- If an update to YouTube/Instagram breaks detection: turn on the **Debug** switch, open Shorts, run `adb logcat -s Unscroll`, find the player's view ID in the dump, and paste it into the **Extra view-id patterns** field in the app. No rebuild needed.

## License

MIT. Quotes belong to their authors.

# Brand Assets for AI Destiny Master

This folder contains the app logo used for:
- Android app icon foreground (`res/drawable/ic_launcher_foreground.xml` references the same geometry/colors)
- Google Cloud Console OAuth consent screen logo (120x120 PNG, <1 MB)
- Play Store / marketing (future use)

## Files
- `logo.svg`: Master vector source (512x512). Colors:
  - Background: #10131A (deep space)
  - Accent (gold): #C9A46A
  - Foreground (white): #FFFFFF

## Exporting a 120x120 PNG for OAuth consent
You can export with Inkscape (recommended) using the script below, or export manually.

### Option A: PowerShell script (requires Inkscape in PATH)
1. Install Inkscape for Windows (MSI) and ensure `inkscape.exe` is in PATH.
2. Run:
   ```powershell
   pwsh -File scripts/export_logo.ps1
   ```
3. Output:
   - `docs/brand/logo_120.png` (120x120, <1 MB)

### Option B: Inkscape GUI
1. Open `docs/brand/logo.svg`.
2. File → Export PNG → Export Area: Page.
3. Set width/height to 120 × 120 px.
4. Export to `docs/brand/logo_120.png`.

## Android icon wiring
- Foreground vector: `app/src/main/res/drawable/ic_launcher_foreground.xml` (star + orbits)
- Adaptive icons: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`
- Colors: `app/src/main/res/values/colors.xml`
- Manifest: `app/src/main/AndroidManifest.xml` has `android:icon` and `android:roundIcon` set to `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round`.

## Usage on Google Cloud Console
- Upload `logo_120.png` to the OAuth consent screen (JPG/PNG/BMP, ≤1 MB, square 120×120 recommended)
- App name (suggested):
  - zh-TW: AI命理大師：命盤・紫微・星盤
  - en: AI Destiny Master: Bazi・Ziwei・Astro

## Exported PNG sizes (transparent background)
| Size | Path                          | Suggested use                    |
|------|-------------------------------|----------------------------------|
| 48   | `docs/brand/logo_48.png`      | Small badge, favicon-like usage  |
| 120  | `docs/brand/logo_120.png`     | Google OAuth consent screen      |
| 192  | `docs/brand/logo_192.png`     | Android legacy launcher (xxxhdpi downscaled), PWA icons |
| 256  | `docs/brand/logo_256.png`     | General marketing                |
| 512  | `docs/brand/logo_512.png`     | Play Store (marketing assets)    |
| 1024 | `docs/brand/logo_1024.png`    | High-res marketing/source export |

All PNGs keep a transparent outer background with a central dark sphere to match in-app branding.

## Android legacy mipmap bitmaps (PNG)
While the app primarily uses adaptive icons, a full set of bitmap fallbacks have been generated:

`ic_launcher.png`
- `app/src/main/res/mipmap-mdpi/ic_launcher.png` (48×48)
- `app/src/main/res/mipmap-hdpi/ic_launcher.png` (72×72)
- `app/src/main/res/mipmap-xhdpi/ic_launcher.png` (96×96)
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` (144×144)
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` (192×192)

`ic_launcher_round.png`
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.png` (48×48)
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.png` (72×72)
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.png` (96×96)
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png` (144×144)
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png` (192×192)

Notes
- Adaptive icons remain the default via `@mipmap/ic_launcher` and `@mipmap/ic_launcher_round` XML in `mipmap-anydpi-v26/`.
- The bitmap set is useful for very old devices or external integrations that require fixed PNGs.

## License
All brand assets in this folder are original work for this project. You may use them within this app and its store listings.

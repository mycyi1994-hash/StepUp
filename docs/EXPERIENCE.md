# StepUp interaction, sound and typography

This update applies to the native Android consumer app. The existing Volt/Carbon
identity, artwork, reward rules and repository architecture remain its foundation.

## Experience by module

| Module | Motion and type | Sound / haptics |
| --- | --- | --- |
| Entry and login | Shorter prepared splash, quiet crossfade, restrained speed field | No startup sound |
| Home | Card entrances, smoothly changing metrics and energy, tabular Barlow figures | Controls respond once; opening a run does not announce a started session |
| Run and courses | Readable distance/time, result entrance and bounded celebration, smooth meters | Actual service start/resume, pause, lap and settlement; selection/save after persistence |
| Community and crews | Consistent selection, route transitions, per-digit party countdown | Posted comment/post, crew creation/join, selection, each countdown beat |
| Items and sneaker detail | Production artwork now honors its animation flag; mint reveal and celebration | Mint, equip, upgrade and boost success only after repository success; distinct rejection cue |
| Events | Reward total celebration and animated progress | Successful claim vs unavailable claim |
| Profile, analytics and achievements | Numeric hierarchy, growing charts, card entrances | Goal/avatar changes after persistence |
| Wallet, notifications, settings and support | Shared typography, navigation, controls; FAQ transition | Short control feedback; no invented settlement sounds |

## Controls

Profile → **Sound & motion** controls sound, haptics and reduced motion separately.
The settings persist in DataStore and are localized in English, Korean, Japanese
and Chinese. A preview plays the same reward cue used in the app.

SoundPool is owned by the Activity composition. Sounds are bundled mono PCM
earcons, 70–705 ms long, with short attack/release envelopes and headroom. They do
not request audio focus or change volume. Background state, app mute, system
touch-sound mute, silent/vibrate mode, Do Not Disturb, zero system volume and
active music playback suppress audio. Existing streams stop on pause/disposal
or app mute. Android's haptic setting controls vibration independently.

System animation scale zero and the in-app reduced-motion option remove screen
travel, number interpolation and decorative loops. Decorative transitions are
not composed while the Activity is paused. Visual and textual outcomes always
remain available without sound or motion. Controls retain disabled and selected
semantics; major custom buttons have a minimum 48 dp height and grow with text.

## Typography and assets

Pretendard 400/500/600/700/800 is bundled for body copy and headings. Barlow
600/700 provides distinct, tabular workout figures. The platform supplies glyphs
outside each font's coverage, including emoji. No font request is made at runtime.
Original font files, upstream commit links and SIL OFL licenses are included in
`app/src/main/assets/licenses/`. The original sounds can be reproduced with
`python tools/build_experience_assets.py`.

## Validation

```sh
python tools/check_experience_assets.py
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

`ExperienceUiTest` renders the modules on Android in four languages, records
compact-phone / 160% text captures, exercises primary navigation and checks
setting persistence and disabled controls. The **Experience QA** workflow
preserves PNG captures, Android test results, lint reports and the debug APK for
review. Rendering checks and manual visual review complement each other; merely
producing a PNG does not establish that its layout is correct.

Release signing still uses the existing external signing configuration described
in `RELEASE-SIGNING.md`. No production signing key is stored in this repository.

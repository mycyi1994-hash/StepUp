# Native shoes capture inventory

User request: collect all current My Shoes screens without redesigning them.
Base: main 0d134178aed9fe145ce8841dbc1f7da855572721, version 1.18.0.
Production sources and resources remain unchanged. Screens are native Android
instrumentation captures with explicitly synthetic shoe/balance fixtures.

Inventory: My Shoes, vault, grade variants, stat explanation, sorting/filtering,
rename, detail, equipment, records/manage, point allocation/reset, repair,
upgrade/material selection/results, free/premium draw/history/rules/results,
catalog, and linked market destinations. Existing tests include loading, empty,
failure, light-theme, large-font and narrow-screen states.

Capture workflow builds once, then runs each group on a fresh API 35 emulator.
Artifacts include original PNGs, revision hashes, instrumentation and Android logs.
No production account, transaction, release upload or application UI is changed.

Validation: run 42247814249 built the app/test APKs and passed resources/copy/design
checks. MyShoes, detail flow/states, repair, upgrade, draw scenes/sizes passed.
Linked destinations and the first two draw-flow methods passed. Two final methods
stalled after earlier methods in their emulator sessions: catalog shoe selection
and signed-out/load-failed draw. Their partial job failures are preserved. A recovery
workflow reuses the identical APK artifacts and runs each remaining method on its
own fresh emulator; no app or test source changes are required.

## Completed capture package

- 216 distinct original PNGs: 67 whole-app captures and 149 native component/state
  captures, including theme/viewport variations. Component swatches and blank
  failure screenshots are excluded; originals are preserved with SHA-256 hashes.
- Recovery run 42250134438 completed the catalog shoe variants. The signed-out
  navigation regression still stalled and is not reported as passing.
- Supplemental run 42251227906 passed both capture methods: native signed-out/load
  failure states and the scrolled in-app detail actions/record sheet. Only capture
  tests/workflows changed; production UI/resources/build configuration are unchanged.
- Latest main 156f1ef844b6d8fc040f690f1e353f9b0a630f62 was checked during capture.
  Its entire app tree equals base 0d13417: e6d4f638ad3622bd8344aa40622bc4add3a38851.
- Delivery includes a searchable local HTML gallery, 16-screen overview, original
  PNGs, manifest, and instrumentation results. All 216 hashes and ZIP CRC verified.
- The original signed-out navigation timeout remains a separate automation issue;
  native state captures cover its missing images without claiming that flow passed.

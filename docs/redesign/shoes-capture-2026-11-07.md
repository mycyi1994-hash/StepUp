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

Validation: pending cloud build and per-group capture results. Failed assertions
remain failures; any partial captures must be labelled separately from test results.

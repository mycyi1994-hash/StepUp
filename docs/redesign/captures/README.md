# Wardrobe checkpoint captures

App source: `1bc6e9bb59286d62b31035d083911a4c6ae3a256`.

Native capture run: https://github.com/mycyi1994-hash/StepUp/actions/runs/35941934970

These are unedited Android 14 display captures at 1080 × 2400. The wardrobe-specific instrumentation passed on Android 14 and 15 (one test each; zero failures/errors/skips).

- `wardrobe-02-background.png`: actual starter ownership, after the background button changed the scene.
- `wardrobe-03-shoes.png`: the existing equipped starter sneaker.
- `wardrobe-04-demo-grid.png`: explicitly enabled demo inventory; five additional outfits are marked 체험 and are not owned.

Full original artifacts, including options, RUNO and large-type captures, are retained by the linked workflow and locally at `C:/Users/gana0/StepUp-captures/wardrobe-1bc6e9b/`.

Capture limitation: `wardrobe-01-owned.png` still contains the loading frame despite ready semantics. It is deliberately not used as loaded-screen evidence. The post-interaction captures above are loaded native frames. This checkpoint does not establish first-entry frame timing, every viewport/theme, or whole-app regression coverage.

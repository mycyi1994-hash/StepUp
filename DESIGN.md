# StepUp design contract — 2026-09 redesign

> Current user decision, 2026-09-25: ship the character-free, record-first direction from selected 05-record.png. Remove mascot/clothing/wardrobe presentation, keep shoe collection/market/draw and existing user data. Navigation is Running / Shoes / Draw / Community / Profile. Scenery is an independent horizontal banner. Older character-related requirements below describe history, not current release requirements. See docs/redesign/CHARACTER-FREE-ASSETS-2026-09-25.md.


Authority: the user's direction and eight approved concepts, not inconsistent incidental details inside generated images. This contract is the single specification for their actual implementation.

## Visual world

An atmospheric running world: dimensional RUNO/LUMI, faithful equipment, riverside depth and blue night / warm sunset lighting. Art leads on home, running and wardrobe. Utility screens use calmer surfaces and compact illustration. No dashboard-card collage. No full-screen bitmap pretending to be interactive UI.

## Fixed chrome

- One root navigation shell. Exactly four destinations in this order: 러닝, 꾸미기, 커뮤니티, 내 정보.
- Header, wordmark, balance pill, back button, primary button and navigation dimensions are owned by shared components and `StepUpDesign` tokens.
- Screens supply content, callbacks, selection and data. Screens cannot choose logo dimensions, button colors/radii or recreate chrome.
- Wordmark presets: Header (26 dp high, original aspect ratio); Launch (52 dp high, constrained to available width). Both use the existing original transparent assets. No text imitation or regenerated logo.
- Main header: stable 64 dp minimum row; 20 dp outer content gutter; minimum 48 dp hit targets. The same available width produces the same logo bounds on every main tab.
- Primary CTA: 60 dp minimum, pill shape, shared blue treatment, 18 sp semibold label, 24 dp icon. Font expansion can increase height; it must never clip.
- Secondary filled/outlined buttons: 48 dp minimum touch height, 14 sp label, 20 dp horizontal and 12 dp vertical padding. Both use the same centralized tokens and center wrapped labels; increased font size may grow their height.
- Bottom navigation: shared minimum 76 dp content plus Android navigation-bar insets. Same icons, label roles, spacing and hit geometry. Selection changes color and indicator only, not font weight or layout.
- Detail header: shared back control and title; any contextual action follows a defined slot. A detail screen does not invent another brand header.
- Startup/reveal/login: no bottom tabs. Running focus and full-screen forms: no bottom tabs. Maps/details otherwise retain their declared parent tab. Route policy is centralized and exhaustively checked.
- Safe-area ownership: system status inset belongs to the root shell, bottom system inset to its navigation/focus footer. Child screens must not add a second copy.

## Tokens and type

Maintain semantic light/dark colors; default dark world uses midnight navy, royal blue, white text and quiet lavender-blue secondary text. Generated references inform art, not raw color overrides in screens. Source UI colors come from theme tokens. Typography: existing bundled font system, role-based sizing; key body/control text at least 14 sp. No miniature explanatory blocks.

## Screen composition

Home: shared logo/balance, large faithful character within the scene, pinned start/resume action above navigation. Secondary features live behind deliberate secondary routes rather than a dashboard on the home viewport.

Run: scenic character, timer, distance and pace, one primary pause/resume control; finish and safety actions remain reachable. Preserve tracking, recovery and persistence.

Wardrobe: large actual-equipped preview, clothes/shoes choice and manageable item grid. Ownership and applying equipment must be truthful; no invented outfit preview.

Community: clear path to a real nearby/group run; honest empty/error states when none is available. Illustrations never fabricate events.

Profile: calm runner identity, small record summary, reachable records/challenges/wallet/settings. All existing features get an explicit destination.

Startup: quiet brand/loading → brief logo reveal → authentication or home. Keep real initialization work. Do not add a forced long delay or fake progress.

## Verification

Static architecture checks reject screen-local logos/navigation and style overrides. Instrumented navigation checks compare shared chrome geometry before/after tab switches and detail returns. Capture compact/regular phone widths, large font and both themes. Animation and live numbers are stabilized only in test fixtures. New imagery must be checked for native text and controls baked into it.

## Work status

This file is the intended final contract; its existence does not mean migration is complete. See `docs/redesign/PROGRESS.md` and the screen inventory for evidence and outstanding work.

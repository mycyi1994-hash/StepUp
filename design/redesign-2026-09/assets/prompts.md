# Production scene artwork

## Starter character sprites

Built-in ImageGen. `runo-cloud-runner-v2.png` preserves the male starter outfit and WND-010 shoes; `lumi-cloud-runner.png` adds the previously missing female base-outfit/WND-010 combination. Converted to versioned WebP runtime resources at quality 94; original resources retained. PNG inspection: RGBA, outside-background samples alpha 0; detail/edge validation and final native screenshots still required. Other equipment sprites are not upgraded by this change.

### RUNO prompt

Use case: identity-preserve. Remaster Image 1 (RUNO wearing WND-010 Cloud Runner sneakers) as a crisp high-resolution production character sprite on a genuinely transparent background with clean alpha. Image 2 is ONLY a rendering quality/material reference, NOT identity. Keep Image 1's male character, exact cap/hair/black smooth face/capsule glowing blue eyes, body proportions, standing pose, navy ZIP hoodie with electric-blue trim, navy shorts, and exact white chunky sneakers with teal details. Do not turn him into the ponytail female in Image 2. Reconstruct fine cloth, laces, sole texture and clean smooth outlines; eliminate pixelation, white halo, jagged blue fringe and compression artifacts. Match the polished dimensional 3D collectible quality of Image 2, without changing the outfit or shoe model. Full body entirely visible cap to soles, center with small clean margins, at least 1024x1536. Keep UP marks already on clothing. No other text, background, floor, platform, cast shadow or props. Genuine transparent PNG.

### LUMI prompt

Use case: identity-preserve. Production LUMI full-body character sprite, genuinely transparent PNG, at least1024x1536. Image 1 is the target female LUMI character: preserve her exact identity, proportions, cap, ponytail, smooth black face and glowing capsule eyes, navy pullover hoodie with bright blue trims, navy shorts and socks, exact relaxed standing pose. Change ONLY her sneakers: wear the white chunky WND-010 Cloud Runner shoe with teal details shown on Image 2 (RUNO). Use Image 2 ONLY for the shoe design, not its male identity or low resolution. Preserve Image 1's crisp polished dimensional 3D rendering and materials, with clean anti-aliased alpha and no blue pixel halo. Both shoes must faithfully match the white and teal model. Full body, no cropping, small transparent margin all around. No background, floor, platform, ground shadow, added text or props.

## Wardrobe terrace

`wardrobe-terrace.png` / `scene_wardrobe_terrace.webp`: built-in ImageGen from `../concepts/06-wardrobe.png`, WebP quality 88. Inspected: clean terrace with no UI or character. Exact prompt:

Use case: precise-object-edit. Production full-bleed portrait 9:19.5 backdrop for StepUp native Android wardrobe. Input is approved wardrobe concept. Reconstruct ONLY its sunset riverside terrace environment. Remove character, all logos, text, icons, top header, balance, status bar, category tabs, item cards, bottom navigation, all UI, and circular glowing platform. Preserve polished stylized 3D city skyline, purple-orange sunset, river reflections, bridge and terrace railing. No new subjects. Composition for separate native character overlay: railing and river horizon around 48 percent image height, flat open wet terrace pavement below, central standing space around 62 percent height, quiet dark navy pavement throughout bottom third for item controls. Top is dusk sky. Same art world, lighting and rich depth as reference. No people, no characters, no lettering, no symbols, no phone frame, no controls.

## Sunset running path

`riverside-sunset.png` / `scene_riverside_sunset.webp`: built-in ImageGen from `../concepts/04-running.png`, WebP quality 88. Inspected: no character, text or controls. Exact prompt:

Use case: precise-object-edit. Production portrait 9:19.5 Android running background. Input is approved StepUp running concept, reference only for its environment. Produce ONLY the full-bleed cinematic sunset riverside running path. Remove the runner entirely and remove all UI: title, back button, status icons, timer, metrics, numbers, letters, buttons. Seamlessly reconstruct those regions as scenery. Preserve stylized high quality 3D sunset clouds orange/pink into navy, bridge and skyline across river to the right, trees and lamps to the left, railing beside the jogging path. Path stretches toward a vanishing point at about 50 percent image height. Leave central path clear for a separate dynamic runner character. Lower 30 percent has dark navy quiet pavement for native legible metrics and controls; top 15 percent quiet dusk sky. Consistent world, depth, lighting and materials with the reference, no figure, no text, no logos, no icons, no UI, no border.

`riverside-night.png` / `scene_riverside_night.webp`: generated with the built-in ImageGen tool from the approved `../concepts/03-home.png`. WebP is a format conversion (quality 88); no semantic edits after generation.

## Prompt

Use case: precise-object-edit. Production Android full-bleed background asset, portrait 9:19.5. Input image is an approved StepUp home UI concept and reference for the environment. Extract/reconstruct ONLY its cinematic blue-night riverside setting: distant city, illuminated bridge and tower, deep navy sky, warm small windows reflecting on water, foreground wet promenade. Remove the character completely, remove glowing circular platform, remove all logos, letters, numbers, icons, status bar, header, balance, buttons, navigation and phone UI. Reconstruct their areas seamlessly as scenery. Keep the same high quality stylized 3D world, blue palette, depth and atmosphere. Lower foreground is open dark blue pavement with room for a separate character rendered later; horizon and railing at about 62 percent height, foreground below it. Top 20 percent quiet dark navy sky for native white header; bottom 20 percent dark quiet pavement for native button and tabs. No text, no characters, no symbols, no UI, no border. Edge to edge environment only.

## Inspection

No character, text, logo, status bar or interactive controls remain in the artwork. Native Compose owns every control. Background only; this is not a screenshot proving implementation.

# StepUp implementation rules

## Latest direction — S2 handoff, 2026-09-26

Read `docs/redesign/s2/README.md` first, then its linked decisions, plan, status and assets. The user selected Figma S2 and asked to proceed with the images already obtained. S2 Android implementation is in progress — see `docs/redesign/s2/STATUS.md`. Navigation is four tabs (Running / Shoes / Community / Profile); Draw lives inside Shoes (user decision, 2026-09-26). The comparison HTML is a proposal, not an approved pixel specification or an APK capture. Old character/wardrobe visual directions below are historical. Preserve existing functions/data and do not restart blocked Figma exports. Reading this handoff does not independently authorize implementation, committing, pushing or deployment; follow the user's current task.

## Current handoff / user priority

Read `docs/redesign/HANDOFF.md` after the S2 handoff when continuing this work. The user explicitly corrected the previous agent for prioritizing internal storage/tests over visual redesign and raised credit concerns. Prioritize reference-faithful visual completion and reviewable actual captures when implementation is requested. Preserve the pending local changes described there; do not automatically restart the previous long-running goal merely to read this handoff.

Read `PRODUCT.md`, `DESIGN.md`, `CLAUDE.md` and `docs/redesign/PROGRESS.md` before changing UI. User decisions in this task are authoritative over older aesthetic notes.

- Keep the full redesign objective and all existing flows in scope. Never mark an unverified item completed.
- All shared chrome comes from the design-system components. Do not render logo assets directly in screens, pass literal logo sizes, duplicate a header or define another bottom bar.
- Route visibility/parent selection is owned by one policy. New routes must have an explicit policy entry and inventory entry.
- Art is imagery; labels, numbers, tabs and buttons are native interactive Compose elements.
- Never swap or remove account, database or preference identities to make tests pass.
- Never invent actual rewards, ownership, events, wallet success or completed integration.
- Update the inventory and progress log with implementation and validation evidence. Keep external blockers separate from local work that can continue.
- Run the design-contract check, string/resource checks and relevant tests before delivering a checkpoint. Compile and capture from the same revision.

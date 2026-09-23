# StepUp implementation rules

## Current handoff / user priority

Read `docs/redesign/HANDOFF.md` first when continuing this work. The user explicitly corrected the previous agent for prioritizing internal storage/tests over visual redesign and raised credit concerns. Prioritize reference-faithful visual completion and reviewable actual captures when implementation is requested. Preserve the pending local changes described there; do not automatically restart the previous long-running goal merely to read this handoff.

Read `PRODUCT.md`, `DESIGN.md`, `CLAUDE.md` and `docs/redesign/PROGRESS.md` before changing UI. User decisions in this task are authoritative over older aesthetic notes.

- Keep the full redesign objective and all existing flows in scope. Never mark an unverified item completed.
- All shared chrome comes from the design-system components. Do not render logo assets directly in screens, pass literal logo sizes, duplicate a header or define another bottom bar.
- Route visibility/parent selection is owned by one policy. New routes must have an explicit policy entry and inventory entry.
- Art is imagery; labels, numbers, tabs and buttons are native interactive Compose elements.
- Never swap or remove account, database or preference identities to make tests pass.
- Never invent actual rewards, ownership, events, wallet success or completed integration.
- Update the inventory and progress log with implementation and validation evidence. Keep external blockers separate from local work that can continue.
- Run the design-contract check, string/resource checks and relevant tests before delivering a checkpoint. Compile and capture from the same revision.

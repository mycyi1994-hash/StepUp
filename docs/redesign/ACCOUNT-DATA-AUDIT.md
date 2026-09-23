# Account ownership — incomplete, release requirement

## Verified source path (2026-09-24)

- `data/local/Entities.kt`: `WalkSessionEntity` stores activity/claim fields but no authenticated owner ID.
- `service/WalkSessionService.kt`: creates that row when saving a run; the recording account is not persisted with it.
- `data/repo/ClaimRepository.kt`: `uploadPending` reads the common pending queue and sends each row without checking ownership.
- `data/repo/ServerSessionRecorder.kt`: invokes `StepUpServer.recordSession`, then crew/course writes as separate authenticated operations.
- `data/remote/StepUpServer.kt`: `authed` obtains the current SessionHolder token for each call.
- `data/remote/SessionHolder.kt`: a successful Google login replaces the stored session; its mutex protects token refresh/login, not the ownership of local rows or an entire multi-request upload.
- `supabase/migrations/0003_ledger.sql`: `record_session` uses `auth.uid()` as the receiving user.
- `ui/screens/settings/ConnectedAccountsScreen.kt`: account deletion clears authentication after server success; this does not partition the local records/equipment/reward store.

This is evidence of a cross-account attribution path, not evidence that a real user's records have actually crossed accounts. No live account or financial operation was performed during this audit.

## Required implementation boundaries

1. Persist the recording account with new sessions, including the active-run checkpoint so restart/finish cannot change ownership mid-run. Guest and unknown legacy ownership must be distinct from an authenticated ID.
2. Preserve legacy rows through a Room migration. Do not backfill every row with the currently signed-in account: historical ownership cannot be inferred from the present token. Keep ambiguous rows accessible without silently uploading them.
3. Validate ownership against the very token used to send each request, not a separate earlier user-ID read. Apply the same rule to follow-up crew/course operations and retry queues. A mismatch must retain the pending row without incrementing financial success or losing the activity.
4. Partition the local reward ledger, equipment, claim receipts, notification preferences and profile state by the selected account with explicit guest/legacy migration behavior. A run-only upload guard is necessary but insufficient to claim full account isolation.
5. User-facing recovery must explain why an old/guest record cannot yet be attributed, and preserve browsing/export/recovery. Do not label ambiguous data empty, rejected, or transferred.

## Required evidence

- A starts a run, B signs in before finish: the record retains A ownership and cannot upload with B's token.
- A has an offline pending run, then B signs in: queue and credit remain isolated.
- Token refresh, logout/login, and an account switch between record and crew/course follow-up cannot change the receiving account.
- Guest/legacy rows survive migration and cannot silently become the new account's claim.
- Account switching and process/database reopening preserve each account's records, receipts, equipment and totals.
- Server-confirmed receipt reconciliation remains separate from local ownership and must also be verified.

Status: source audit completed; implementation and these tests remain outstanding. This requirement prevents a final release-readiness claim.
